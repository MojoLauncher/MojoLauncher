package net.kdt.pojavlaunch.modloaders;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;

import com.kdt.mcgui.ProgressLayout;

import git.artdeell.mojo.R;
import net.kdt.pojavlaunch.Tools;
import net.kdt.pojavlaunch.instances.Instance;
import net.kdt.pojavlaunch.instances.InstanceManager;
import net.kdt.pojavlaunch.progresskeeper.ProgressKeeper;
import net.kdt.pojavlaunch.utils.FileUtils;

import java.io.File;
import java.io.IOException;
import java.net.URL;

public class BTADownloadTask implements Runnable {
    private static final String BASE_JSON = "{\"inheritsFrom\":\"b1.7.3\",\"mainClass\":\"net.minecraft.client.Minecraft\",\"libraries\":[{\"name\":\"bta-client:bta-client:%1$s\",\"downloads\":{\"artifact\":{\"path\":\"bta-client/bta-client-%1$s.jar\",\"url\":\"%2$s\"}}}],\"id\":\"%3$s\"}";
    private final ModloaderDownloadListener mListener;
    private final BTAUtils.BTAVersion mBtaVersion;

    public BTADownloadTask(ModloaderDownloadListener mListener, BTAUtils.BTAVersion mBtaVersion) {
        this.mListener = mListener;
        this.mBtaVersion = mBtaVersion;
    }

    @Override
    public void run() {
        ProgressKeeper.submitProgress(ProgressLayout.INSTALL_MODPACK, 0, R.string.fabric_dl_progress, "BTA");
        try {
            runCatching() ;
            mListener.onDownloadFinished(null);
        }catch (IOException e) {
            mListener.onDownloadError(e);
        }
        ProgressLayout.clearProgress(ProgressLayout.INSTALL_MODPACK);
    }

    private void tryDownloadIcon(Instance targetInstance) {
        try {
            Bitmap iconBitmap = BitmapFactory.decodeStream(new URL(mBtaVersion.iconUrl).openStream());
            targetInstance.encodeNewIcon(iconBitmap);
        }catch (IOException e) {
            Log.w("BTADownloadTask", "Failed to download bta icon", e);
        }
    }

    private void createJson(String btaVersionId) throws IOException {
        String btaJson = String.format(BASE_JSON, mBtaVersion.versionName, mBtaVersion.downloadUrl, btaVersionId);
        File jsonDir = new File(Tools.DIR_HOME_VERSION, btaVersionId);
        File jsonFile = new File(jsonDir, btaVersionId+".json");
        FileUtils.ensureDirectory(jsonDir);
        Tools.write(jsonFile.getAbsolutePath(), btaJson);
    }

    // BTA doesn't have SHA1 checksums in its repositories, so the user may try to reinstall it
    // if it didn't work due to a broken download. So, for reinstalls like that to work,
    // we need to delete the old client jar to force the download of a new one.
    private void removeOldClient() throws IOException{
        File btaClientPath = new File(Tools.DIR_HOME_LIBRARY, String.format("bta-client/bta-client-%1$s.jar", mBtaVersion.versionName));
        if(btaClientPath.exists() && !btaClientPath.delete())
            throw new IOException("Failed to delete old client jar");
    }

    private void createProfile(String btaVersionId) throws IOException {
        Instance instance = InstanceManager.createInstance(i -> {
            i.versionId = btaVersionId;
            i.name = "Better than Adventure!";
        }, "BTA-"+btaVersionId);
        tryDownloadIcon(instance);
    }

    public void runCatching() throws IOException {
        removeOldClient();
        String btaVersionId = "bta-"+mBtaVersion.versionName;
        createJson(btaVersionId);
        createProfile(btaVersionId);
    }
}
