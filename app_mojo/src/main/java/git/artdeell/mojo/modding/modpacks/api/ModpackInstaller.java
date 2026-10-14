package git.artdeell.mojo.modding.modpacks.api;

import android.widget.Toast;

import com.kdt.mcgui.ProgressLayout;

import git.artdeell.mojo.R;

import git.artdeell.mojo.Tools;
import git.artdeell.mojo.instances.InstanceInstaller;
import git.artdeell.mojo.instances.Instances;
import git.artdeell.mojo.instances.Instance;
import git.artdeell.mojo.utils.lifecycle.ContextExecutor;
import git.artdeell.mojo.modding.modpacks.api.modloader.LoaderInstaller;
import git.artdeell.mojo.modding.modpacks.imagecache.ModIconCache;
import git.artdeell.mojo.modding.modpacks.models.ModDetail;
import git.artdeell.mojo.progresskeeper.DownloaderProgressWrapper;
import git.artdeell.mojo.utils.DownloadUtils;
import git.artdeell.mojo.utils.FileUtils;

import java.io.File;
import java.io.IOException;
import java.util.Locale;

public class ModpackInstaller {

    public static LoaderInstaller installModpack(String modpackName, String title, File modpackFile, String icon, InstallFunction installFunction) throws IOException {
        // Build a new minecraft instance, folder first
        LoaderInstaller loaderInstaller;
        Instance instance = Instances.createInstance(i-> i.name = title, modpackName.substring(0, Math.min(16,modpackName.length())));
        try {
            // Install the modpack
            loaderInstaller = installFunction.installModpack(modpackFile, instance.getGameDirectory());

            if(loaderInstaller == null) throw new IOException("Unknown modpack mod loader information");

            if(loaderInstaller.requiresGuiInstallation()) {
                InstanceInstaller instanceInstaller = loaderInstaller.createInstaller();
                if(instanceInstaller == null) throw new IOException("Failed to prepare data for instance installation");
                instance.installer = instanceInstaller;
            } else {
                String versionId = loaderInstaller.installHeadlessly();
                if(versionId == null) throw new IOException("Unknown mod loader version");
                instance.versionId = versionId;
            }
            instance.write();
            ModIconCache.writeInstanceImage(instance, icon);

            Instances.setSelectedInstance(instance);
            if(loaderInstaller.requiresGuiInstallation()) {
                instance.installer.start();
            }
            else ContextExecutor.executeActivity(activity -> Toast.makeText(activity, R.string.modpack_install_toast_success, Toast.LENGTH_SHORT).show());
        } catch (IOException e) {
            Instances.removeInstance(instance);
            throw e;
        } finally {
            modpackFile.delete();
            ProgressLayout.clearProgress(ProgressLayout.INSTALL_MODPACK);
        }

        return loaderInstaller;
    }

    public static LoaderInstaller downloadModpack(ModDetail modDetail, int selectedVersion, InstallFunction installFunction) throws IOException {
        String versionUrl = modDetail.versionUrls[selectedVersion];
        String versionHash = modDetail.versionHashes[selectedVersion];
        String modpackName = FileUtils.escapeFileName(modDetail.title.toLowerCase(Locale.ROOT) + " " + modDetail.versionNames[selectedVersion]);
        String name = modDetail.title;
        String icon = modDetail.getIconCacheTag();

        if (versionHash != null) {
            modpackName += "_" + versionHash;
        }

        if (modpackName.length() > 255){
            modpackName = modpackName.substring(0,255);
        }

        File modpackFile = new File(Tools.DIR_CACHE, modpackName + ".cf");

        byte[] downloadBuffer = new byte[8192];
        try {
            DownloadUtils.ensureSha1(modpackFile, versionHash, () -> {
                DownloadUtils.downloadFileMonitored(versionUrl, modpackFile, downloadBuffer,
                        new DownloaderProgressWrapper(R.string.modpack_download_downloading_metadata,
                                ProgressLayout.INSTALL_MODPACK
                        )
                );
            });
        } catch (IOException e) {
            modpackFile.delete();
            throw e;
        }

        return installModpack(modpackName, name, modpackFile, icon, installFunction);
    }

    public interface InstallFunction {
        LoaderInstaller installModpack(File modpackFile, File instanceDestination) throws IOException;
    }
}
