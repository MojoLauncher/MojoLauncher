package net.kdt.pojavlaunch;

import static net.kdt.pojavlaunch.Architecture.archAsString;

import android.content.res.AssetManager;
import android.util.Log;

import com.kdt.mcgui.ProgressLayout;

import net.kdt.pojavlaunch.instances.Instance;
import net.kdt.pojavlaunch.instances.Instances;
import net.kdt.pojavlaunch.multirt.MultiRTUtils;
import net.kdt.pojavlaunch.multirt.Runtime;
import net.kdt.pojavlaunch.progresskeeper.DownloaderProgressWrapper;
import net.kdt.pojavlaunch.utils.DownloadUtils;
import net.kdt.pojavlaunch.utils.MathUtils;
import net.kdt.pojavlaunch.utils.SignatureCheckUtil;
import net.kdt.pojavlaunch.utils.jre.RuntimeSelectionException;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import git.artdeell.mojo.R;

public class NewJREUtil {
    private static final String DOWNLOAD_URL = "https://mojolauncher.github.io/jre-download/";
    
    private static String getRemoteRuntimeVersion(InternalRuntime internalRuntime) throws IOException{
        return DownloadUtils.downloadString(DOWNLOAD_URL+internalRuntime.path+"/version");
    }

    private static boolean checkLastUpdateTime(InternalRuntime internalRuntime) {
        long lastUpdateTime = MultiRTUtils.readLastUpdateTime(internalRuntime.name);
        long currentTime = System.currentTimeMillis() / 1000L;
        return lastUpdateTime != -1 && currentTime - lastUpdateTime < 259200;
    }

    private static void writeLastUpdateTime(InternalRuntime internalRuntime) {
        MultiRTUtils.writeLastUpdateTime(internalRuntime.name, System.currentTimeMillis() / 1000L);
    }

    private static void checkInternalRuntime(AssetManager assetManager, InternalRuntime internalRuntime) throws RuntimeSelectionException {
        String remote_runtime_version;
        String installed_runtime_version = MultiRTUtils.readInternalRuntimeVersion(internalRuntime.name);
        if(installed_runtime_version != null && checkLastUpdateTime(internalRuntime)) return;
        try {
            remote_runtime_version = getRemoteRuntimeVersion(internalRuntime);
        }catch (IOException exc) {
            Log.i("NewJreUtil", "Failed to get remote runtime version", exc);
            // We failed to get the version of the runtime available on the web server.
            // Let's just hope that we have an internal version installed in that case.
            if(installed_runtime_version == null)
                throw new RuntimeSelectionException(RuntimeSelectionException.RUNTIME_STATE_INTERNAL_RUNTIME_MISSING, internalRuntime.majorVersion);
            return;
        }
        // this implicitly checks for null, so it will unpack the runtime even if we don't have one installed
        if(!remote_runtime_version.equals(installed_runtime_version)) unpackInternalRuntime(assetManager, internalRuntime, remote_runtime_version);
        writeLastUpdateTime(internalRuntime);
    }

    private static class RuntimeDownloaderVerifier {
        
        private final Map<String, byte[]> mSignatures;
        private final String mRuntimePath;
        private final byte[] mDownloadBuffer = new byte[8192];
        private final SignatureCheckUtil mSignatureCheckUtil;

        public RuntimeDownloaderVerifier(Map<String, byte[]> signatures, InternalRuntime internalRuntime, SignatureCheckUtil mSignatureCheckUtil) {
            this.mSignatures = signatures;
            this.mRuntimePath = DOWNLOAD_URL + internalRuntime.path + "/";
            this.mSignatureCheckUtil = mSignatureCheckUtil;
        }

        public boolean downloadAndVerify(String component, File output, int progressString) throws IOException {
            DownloadUtils.downloadFileMonitored(
                    mRuntimePath + component, output, mDownloadBuffer,
                    new DownloaderProgressWrapper(progressString, ProgressLayout.UNPACK_RUNTIME)
            );
            byte[] signature = mSignatures.get(component);
            try (FileInputStream fileInputStream = new FileInputStream(output)){
                return mSignatureCheckUtil.verify(fileInputStream, signature);
            }
        }
    }

    private static void throwInstallFail(InternalRuntime internalRuntime, Throwable cause) throws RuntimeSelectionException {
        RuntimeSelectionException e = new RuntimeSelectionException(RuntimeSelectionException.RUNTIME_STATE_INSTALLATION_FAILED, internalRuntime.majorVersion);
        e.initCause(cause);
        throw e;
    }

    private static void throwInstallFail(InternalRuntime internalRuntime) throws RuntimeSelectionException {
        throw new RuntimeSelectionException(RuntimeSelectionException.RUNTIME_STATE_INSTALLATION_FAILED, internalRuntime.majorVersion);
    }

    private static void unpackInternalRuntime(AssetManager assetManager, InternalRuntime internalRuntime, String versionSignatures) throws RuntimeSelectionException {
        Map<String, byte[]> signatures = SignatureCheckUtil.decodeSignatureBundle(versionSignatures);
        String platformBinFile = "bin-"+archAsString(Tools.DEVICE_ARCHITECTURE)+".tar.xz";
        if(!signatures.containsKey("universal.tar.xz") || !signatures.containsKey(platformBinFile)) {
            throwInstallFail(internalRuntime);
        }

        File universalCache = null, platformCache = null;
        try {
            SignatureCheckUtil signatureCheckUtil = SignatureCheckUtil.create(assetManager);
            universalCache = File.createTempFile("jre-install-", "-universal", Tools.DIR_CACHE);
            platformCache = File.createTempFile("jre-install-", "-platform", Tools.DIR_CACHE);
            RuntimeDownloaderVerifier runtimeDownloaderVerifier = new RuntimeDownloaderVerifier(signatures, internalRuntime, signatureCheckUtil);
            if (!runtimeDownloaderVerifier.downloadAndVerify("universal.tar.xz", universalCache, R.string.downloading_java_runtime_uni) ||
                    !runtimeDownloaderVerifier.downloadAndVerify(platformBinFile, platformCache, R.string.downloading_java_runtime_platform)) {
                throwInstallFail(internalRuntime);
            }

            try (FileInputStream universal = new FileInputStream(universalCache); FileInputStream platform = new FileInputStream(platformCache)) {
                MultiRTUtils.installRuntimeNamedBinpack(universal, platform, internalRuntime.name, versionSignatures);
                MultiRTUtils.postPrepare(internalRuntime.name);
                MultiRTUtils.forceReread(internalRuntime.name);
            }
        } catch (IOException e) {
            throwInstallFail(internalRuntime, e);
        } finally {
            ProgressLayout.clearProgress(ProgressLayout.UNPACK_RUNTIME);
            // Those files being deleted are on a "i wish" basis
            if(universalCache != null && universalCache.isFile()) //noinspection ResultOfMethodCallIgnored
                universalCache.delete();
            if(platformCache != null && platformCache.isFile()) //noinspection ResultOfMethodCallIgnored
                platformCache.delete();
        }
    }

    private static InternalRuntime getInternalRuntime(Runtime runtime) {
        for(InternalRuntime internalRuntime : InternalRuntime.values()) {
            if(internalRuntime.name.equals(runtime.name)) return internalRuntime;
        }
        return null;
    }

    private static MathUtils.RankedValue<Runtime> getNearestInstalledRuntime(int targetVersion) {
        List<Runtime> runtimes = MultiRTUtils.getRuntimes();
        return MathUtils.findNearestPositive(targetVersion, runtimes, (runtime)->runtime.javaVersion);
    }

    private static MathUtils.RankedValue<InternalRuntime> getNearestInternalRuntime(int targetVersion) {
        List<InternalRuntime> runtimeList = Arrays.asList(InternalRuntime.values());
        return MathUtils.findNearestPositive(targetVersion, runtimeList, (runtime)->runtime.majorVersion);
    }


    public static void installNewJreIfNeeded(AssetManager assetManager, JMinecraftVersionList.Version versionInfo) throws IOException, RuntimeSelectionException {
        //Now we have the reliable information to check if our runtime settings are good enough
        if (versionInfo.javaVersion == null || versionInfo.javaVersion.component.equalsIgnoreCase("jre-legacy")) return;

        int gameRequiredVersion = versionInfo.javaVersion.majorVersion;

        Instance instance = Instances.loadSelectedInstance();
        String profileRuntime = Tools.getSelectedRuntime(instance);
        Runtime runtime = MultiRTUtils.read(profileRuntime);
        // Partly trust the user with his own selection, if the game can even try to run in this case
        if (runtime.javaVersion >= gameRequiredVersion) {
            // Check whether the selection is an internal runtime
            InternalRuntime internalRuntime = getInternalRuntime(runtime);
            // If it is, check if updates are available from the APK file
            if(internalRuntime != null) {
                // Not calling showRuntimeFail on failure here because we did, technically, find the compatible runtime
                checkInternalRuntime(assetManager, internalRuntime);
            }
            return;
        }

        // If the runtime version selected by the user is not appropriate for this version (which means the game won't run at all)
        // automatically pick from either an already installed runtime, or a runtime packed with the launcher
        MathUtils.RankedValue<?> nearestInstalledRuntime = getNearestInstalledRuntime(gameRequiredVersion);
        MathUtils.RankedValue<?> nearestInternalRuntime = getNearestInternalRuntime(gameRequiredVersion);

        MathUtils.RankedValue<?> selectedRankedRuntime = MathUtils.objectMin(
                nearestInternalRuntime, nearestInstalledRuntime, (value)->value.rank
        );

        // No possible selections
        if(selectedRankedRuntime == null) {
            throw new RuntimeSelectionException(RuntimeSelectionException.RUNTIME_STATE_SELECTION_FAILED, gameRequiredVersion);
        }

        Object selected = selectedRankedRuntime.value;
        String appropriateRuntime;
        InternalRuntime internalRuntime;

        // Perform checks on the picked runtime
        if(selected instanceof Runtime) {
            // If it's an already installed runtime, save its name and check if
            // it's actually an internal one (just in case)
            Runtime selectedRuntime = (Runtime) selected;
            appropriateRuntime = selectedRuntime.name;
            internalRuntime = getInternalRuntime(selectedRuntime);
        } else if (selected instanceof InternalRuntime) {
            // If it's an internal runtime, set it's name as the appropriate one.
            internalRuntime = (InternalRuntime) selected;
            appropriateRuntime = internalRuntime.name;
        } else {
            throw new RuntimeException("Unexpected type of selected: "+selected.getClass().getName());
        }

        // If it turns out the selected runtime is actually an internal one, attempt automatic installation or update
        if(internalRuntime != null) {
            checkInternalRuntime(assetManager, internalRuntime);
        }

        instance.selectedRuntime = appropriateRuntime;
        instance.write();
    }

    private enum InternalRuntime {
        JRE_17(17, "Internal-17", "components/jre-new"),
        JRE_21(21, "Internal-21", "components/jre-21"),
        JRE_25(25, "Internal-25", "components/jre-25");
        public final int majorVersion;
        public final String name;
        public final String path;
        InternalRuntime(int majorVersion, String name, String path) {
            this.majorVersion = majorVersion;
            this.name = name;
            this.path = path;
        }
    }

}