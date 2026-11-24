package git.artdeell.mojo.modding.bta;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;

import com.kdt.mcgui.ProgressLayout;

import git.artdeell.mojo.R;
import git.artdeell.mojo.Tools;
import git.artdeell.mojo.game.renderer.def.Renderers;
import git.artdeell.mojo.instances.Instance;
import git.artdeell.mojo.instances.Instances;
import git.artdeell.mojo.modding.ModloaderDownloadListener;
import git.artdeell.mojo.utils.jre.classfile.ClassFormatException;
import git.artdeell.mojo.progresskeeper.DownloaderProgressWrapper;
import git.artdeell.mojo.progresskeeper.ProgressKeeper;
import git.artdeell.mojo.utils.DownloadUtils;
import git.artdeell.mojo.utils.FileUtils;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Locale;

public class BTADownloadTask implements Runnable {
    private static final String BASE_JSON = "{\"inheritsFrom\":\"b1.7.3\",\"mainClass\":\"net.minecraft.client.Minecraft\",\"libraries\":[{\"name\":\"bta-client:bta-client:%1$s\",\"downloads\":{\"artifact\":{\"path\":\"bta-client/bta-client-%1$s.jar\",\"url\":\"%2$s\"}}}],\"id\":\"%3$s\",\"javaVersion\":{\"majorVersion\":%4$d}}";
    private static final String BASE_JSON_LWJGL3 = "{\"inheritsFrom\": \"b1.7.3\", \"mainClass\": \"net.minecraft.client.Minecraft\", \"libraries\": [ { \"name\": \"org.lwjgl.lwjgl:lwjgl:*\", \"rules\": [ { \"action\": \"disallow\" } ] }, { \"name\": \"org.lwjgl.lwjgl:lwjgl_util:*\", \"rules\": [ { \"action\": \"disallow\" } ] }, { \"name\": \"org.lwjgl.lwjgl:lwjgl-platform:*\", \"rules\": [ { \"action\": \"disallow\" } ] }, { \"name\": \"bta-client:bta-client:%1$s\", \"downloads\": { \"artifact\": { \"path\": \"bta-client/bta-client-%1$s.jar\", \"url\": \"%2$s\" } } }, { \"name\": \"org.lwjgl:lwjgl:%4$s\" }, { \"name\": \"org.lwjgl:lwjgl-glfw:%4$s\" }, { \"name\": \"org.lwjgl:lwjgl-openal:%4$s\" }, { \"name\": \"org.lwjgl:lwjgl-opengl:%4$s\" }, { \"name\": \"org.lwjgl:lwjgl-stb:%4$s\" }, { \"name\": \"org.lwjgl:lwjgl-tinyfd:%4$s\" } ], \"id\": \"%3$s\", \"javaVersion\":{\"majorVersion\":%5$d}}";
    private static final String BASE_JSON_LWJGL3_GLCORE = "{\"inheritsFrom\": \"b1.7.3\", \"mainClass\": \"net.minecraft.client.Minecraft\", \"disableRendererChecks\": true, \"environment\": { \"MESA_GL_VERSION_OVERRIDE\": \"4.1\" }, \"libraries\": [ { \"name\": \"org.lwjgl.lwjgl:lwjgl:*\", \"rules\": [ { \"action\": \"disallow\" } ] }, { \"name\": \"org.lwjgl.lwjgl:lwjgl_util:*\", \"rules\": [ { \"action\": \"disallow\" } ] }, { \"name\": \"org.lwjgl.lwjgl:lwjgl-platform:*\", \"rules\": [ { \"action\": \"disallow\" } ] }, { \"name\": \"bta-client:bta-client:%1$s\", \"downloads\": { \"artifact\": { \"path\": \"bta-client/bta-client-%1$s.jar\", \"url\": \"%2$s\" } } }, { \"name\": \"org.lwjgl:lwjgl:%4$s\" }, { \"name\": \"org.lwjgl:lwjgl-glfw:%4$s\" }, { \"name\": \"org.lwjgl:lwjgl-openal:%4$s\" }, { \"name\": \"org.lwjgl:lwjgl-opengl:%4$s\" }, { \"name\": \"org.lwjgl:lwjgl-stb:%4$s\" }, { \"name\": \"org.lwjgl:lwjgl-tinyfd:%4$s\" } ], \"id\": \"%3$s\", \"javaVersion\":{\"majorVersion\":%5$d}}";
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
        }catch (IOException | ClassFormatException e) {
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

    private void createJson(String btaVersionId, BTAHeuristics heuristics, boolean requiresCore) throws IOException {
        String btaJson;
        if(heuristics.lwjglVersion == null) {
            btaJson = String.format(Locale.US, BASE_JSON, mBtaVersion.versionName, mBtaVersion.downloadUrl, btaVersionId, heuristics.runtimeMajorVersion);
        }else {
            btaJson = String.format(Locale.US,
                    requiresCore ? BASE_JSON_LWJGL3_GLCORE : BASE_JSON_LWJGL3,
                    mBtaVersion.versionName, mBtaVersion.downloadUrl, btaVersionId, heuristics.lwjglVersion, heuristics.runtimeMajorVersion
            );
        }
        File jsonDir = new File(Tools.DIR_HOME_VERSION, btaVersionId);
        File jsonFile = new File(jsonDir, btaVersionId+".json");
        FileUtils.ensureDirectory(jsonDir);
        Tools.write(jsonFile, btaJson);
    }

    private BTAHeuristics installClient() throws IOException, ClassFormatException {
        File btaClientPath = new File(Tools.DIR_HOME_LIBRARY, String.format("bta-client/bta-client-%1$s.jar", mBtaVersion.versionName));
        if(btaClientPath.exists() && !btaClientPath.delete())
            throw new IOException("Failed to delete old client jar");

        FileUtils.ensureParentDirectory(btaClientPath);

        DownloaderProgressWrapper progressWrapper = new DownloaderProgressWrapper(R.string.mcl_launch_downloading_progress, ProgressLayout.INSTALL_MODPACK);
        progressWrapper.extraString = "BTA "+mBtaVersion.versionName;
        DownloadUtils.downloadFileMonitored(mBtaVersion.downloadUrl, btaClientPath, null, progressWrapper);

        BTAHeuristics heuristics = BTAHeuristics.detect(btaClientPath);

        Log.i("BTADownloadTask", "Detected LWJGL3 version: "+ heuristics.lwjglVersion);
        Log.i("BTADownloadTask", "Detected java version: "+ heuristics.runtimeMajorVersion);

        return heuristics;
    }

    private void createProfile(String btaVersionId) throws IOException {
        // BTA 8.0+ uses Core context
        final boolean core = BTAUtils.isNightlyVersion(mBtaVersion) || BTAUtils.parseBTAVersion(mBtaVersion)[0] >= 8;
        Instance instance = Instances.createInstance(i -> {
            i.versionId = btaVersionId;
            if(core) i.renderer = Renderers.LTW_RENDERER;
            i.name = "Better than Adventure!";
        }, "BTA-"+btaVersionId);
        tryDownloadIcon(instance);
    }

    public void runCatching() throws IOException, ClassFormatException {
        BTAHeuristics heuristics = installClient();
        String btaVersionId = "bta-"+mBtaVersion.versionName;
        createJson(btaVersionId, heuristics, heuristics.runtimeMajorVersion >= 17);
        createProfile(btaVersionId);
    }
}
