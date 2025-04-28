package net.kdt.pojavlaunch.instances;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.kdt.mcgui.ProgressLayout;

import net.kdt.pojavlaunch.JavaGUILauncherActivity;
import net.kdt.pojavlaunch.LauncherActivity;
import net.kdt.pojavlaunch.PojavApplication;
import net.kdt.pojavlaunch.Tools;
import net.kdt.pojavlaunch.extra.ExtraConstants;
import net.kdt.pojavlaunch.extra.ExtraCore;
import net.kdt.pojavlaunch.instances.profcompat.ProfileWatcher;
import net.kdt.pojavlaunch.lifecycle.ContextExecutor;
import net.kdt.pojavlaunch.lifecycle.ContextExecutorTask;
import net.kdt.pojavlaunch.modloaders.OFDownloadPageScraper;
import net.kdt.pojavlaunch.progresskeeper.DownloaderProgressWrapper;
import net.kdt.pojavlaunch.utils.DownloadUtils;
import net.kdt.pojavlaunch.utils.JSONUtils;
import net.kdt.pojavlaunch.utils.NotificationUtils;

import java.io.File;
import java.io.IOException;
import java.util.Objects;

import git.artdeell.mojo.R;

public class InstanceInstaller implements ContextExecutorTask {
    private static final File sLastInstallInfo = new File(Tools.DIR_CACHE, "last_installer.json");

    public String installerJar;
    private transient File installerJarFile;
    private transient String mTransformedUrl;
    public String commandLineArgs;
    public String installerUrlTransformer;
    public String installerDownloadUrl;
    public String installerSha1;

    private File installerJar() {
        if(installerJarFile == null) return installerJarFile = new File(installerJar);
        return installerJarFile;
    }

    private String installerDownloadUrl() throws IOException{
        if(mTransformedUrl != null) return mTransformedUrl;
        String newUrl;
        if ("optifine".equals(installerUrlTransformer)) {
            newUrl = OFDownloadPageScraper.run(installerDownloadUrl);
        }else {
            newUrl = installerDownloadUrl;
        }
        mTransformedUrl = newUrl;
        return newUrl;
    }

    private void writeLastInstaller() throws IOException {
        JSONUtils.writeToFile(sLastInstallInfo, this);
    }

    public void threadedStart() throws IOException {
        try {
            final byte[] buffer = new byte[8192];
            final DownloaderProgressWrapper wrapper = new DownloaderProgressWrapper(
                    R.string.mcl_launch_downloading_progress, ProgressLayout.INSTANCE_INSTALL
            );
            wrapper.extraString = installerJar().getName();
            DownloadUtils.ensureSha1(installerJar(), installerSha1, ()->{
                DownloadUtils.downloadFileMonitored(installerDownloadUrl(), installerJar(), buffer, wrapper);
                return null;
            });
            ContextExecutor.execute(this);
        } finally {
            ProgressLayout.clearProgress(ProgressLayout.INSTANCE_INSTALL);
        }
    }

    public static void postInstallCheck() throws IOException {
        if(!sLastInstallInfo.exists() || !sLastInstallInfo.isFile()) return;
        InstanceInstaller lastInstaller = JSONUtils.readFromFile(sLastInstallInfo, InstanceInstaller.class);
        lastInstaller.installerJar().delete();
        if(!sLastInstallInfo.delete()) throw new IOException("Failed to delete mod installer info");
        String targetVersionId = ProfileWatcher.consumePendingVersion();
        if(targetVersionId == null) return;
        for(Instance instance : InstanceManager.getImmutableInstanceList()) {
            if(!lastInstaller.equals(instance.installer)) continue;
            instance.installer = null;
            instance.versionId = targetVersionId;
            instance.write();
        }
        ExtraCore.setValue(ExtraConstants.REFRESH_VERSION_SPINNER, null);
    }

    public static void postInstallCheck(Context context) {
        try {
            InstanceInstaller.postInstallCheck();
        }catch (Exception e) {
            Tools.showError(context, e);
        }
    }

    public void start() {
        PojavApplication.sExecutorService.execute(()->{
            try {
                threadedStart();
            }catch (Exception e) {
                Tools.showErrorRemote(e);
            }
        });
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) return true;
        if (!(object instanceof InstanceInstaller)) return false;
        InstanceInstaller that = (InstanceInstaller) object;
        return Objects.equals(installerJar, that.installerJar) &&
                Objects.equals(commandLineArgs, that.commandLineArgs) &&
                Objects.equals(installerDownloadUrl, that.installerDownloadUrl) &&
                Objects.equals(installerUrlTransformer, that.installerUrlTransformer) &&
                Objects.equals(installerSha1, that.installerSha1);
    }

    @Override
    public int hashCode() {
        return Objects.hash(installerJar, commandLineArgs, installerDownloadUrl, installerUrlTransformer, installerSha1);
    }

    @Override
    public void executeWithActivity(Activity activity) {
        try {
            Tools.copyAssetFile(activity, "launcher_profiles.json", Tools.DIR_GAME_NEW, true);
            writeLastInstaller();
        }catch (Exception e) {
            Tools.showError(activity, e);
            return;
        }
        Intent intent = new Intent(activity, JavaGUILauncherActivity.class);
        intent.putExtra("javaArgs", commandLineArgs +" -jar "+installerJar().getAbsolutePath());
        activity.startActivity(intent);
    }

    @Override
    public void executeWithApplication(Context context) {
        Tools.runOnUiThread(() -> NotificationUtils.sendBasicNotification(context,
                R.string.modpack_install_notification_title,
                R.string.modpack_install_notification_success,
                new Intent(context, LauncherActivity.class),
                NotificationUtils.PENDINGINTENT_CODE_DOWNLOAD_SERVICE,
                NotificationUtils.NOTIFICATION_ID_DOWNLOAD_LISTENER
        ));
    }
}
