package net.kdt.pojavlaunch

import android.content.res.AssetManager
import android.util.Log
import com.kdt.mcgui.ProgressLayout
import net.ashmeet.hyperlauncher.R
import net.kdt.pojavlaunch.instances.Instances.Companion.loadSelectedInstance
import net.kdt.pojavlaunch.multirt.MultiRTUtils.forceReread
import net.kdt.pojavlaunch.multirt.MultiRTUtils.installRuntimeNamedBinpack
import net.kdt.pojavlaunch.multirt.MultiRTUtils.postPrepare
import net.kdt.pojavlaunch.multirt.MultiRTUtils.read
import net.kdt.pojavlaunch.multirt.MultiRTUtils.readInternalRuntimeVersion
import net.kdt.pojavlaunch.multirt.MultiRTUtils.readLastUpdateTime
import net.kdt.pojavlaunch.multirt.MultiRTUtils.runtimes
import net.kdt.pojavlaunch.multirt.MultiRTUtils.writeLastUpdateTime
import net.kdt.pojavlaunch.multirt.Runtime
import net.kdt.pojavlaunch.progresskeeper.DownloaderProgressWrapper
import net.kdt.pojavlaunch.utils.DownloadUtils.downloadFileMonitored
import net.kdt.pojavlaunch.utils.DownloadUtils.downloadString
import net.kdt.pojavlaunch.utils.MathUtils
import net.kdt.pojavlaunch.utils.SignatureCheckUtil
import net.kdt.pojavlaunch.utils.SignatureCheckUtil.Companion.create
import net.kdt.pojavlaunch.utils.SignatureCheckUtil.Companion.decodeSignatureBundle
import net.kdt.pojavlaunch.utils.jre.RuntimeSelectionException
import java.io.File
import java.io.FileInputStream
import java.io.IOException

object NewJREUtil {
    private const val DOWNLOAD_URL = "https://mojolauncher.github.io/jre-download/"

    @Throws(IOException::class)
    private fun getRemoteRuntimeVersion(internalRuntime: InternalRuntime): String {
        return downloadString(DOWNLOAD_URL + internalRuntime.path + "/version")
    }

    private fun checkLastUpdateTime(internalRuntime: InternalRuntime): Boolean {
        val lastUpdateTime = readLastUpdateTime(internalRuntime.runtimeName)
        val currentTime = System.currentTimeMillis() / 1000L
        return lastUpdateTime != -1L && currentTime - lastUpdateTime < 259200
    }

    private fun writeLastUpdateTime(internalRuntime: InternalRuntime) {
        writeLastUpdateTime(internalRuntime.runtimeName, System.currentTimeMillis() / 1000L)
    }

    @Throws(RuntimeSelectionException::class)
    private fun checkInternalRuntime(assetManager: AssetManager, internalRuntime: InternalRuntime) {
        val remoteRuntimeVersion: String?
        val installedRuntimeVersion = readInternalRuntimeVersion(internalRuntime.runtimeName)
        if (installedRuntimeVersion != null && checkLastUpdateTime(internalRuntime)) return
        try {
            remoteRuntimeVersion = getRemoteRuntimeVersion(internalRuntime)
        } catch (exc: IOException) {
            Log.i("NewJreUtil", "Failed to get remote runtime version", exc)
            if (installedRuntimeVersion == null) throw RuntimeSelectionException(
                RuntimeSelectionException.RUNTIME_STATE_INTERNAL_RUNTIME_MISSING,
                internalRuntime.majorVersion
            )
            return
        }
        if (remoteRuntimeVersion != installedRuntimeVersion) unpackInternalRuntime(
            assetManager,
            internalRuntime,
            remoteRuntimeVersion
        )
        writeLastUpdateTime(internalRuntime)
    }

    @Throws(RuntimeSelectionException::class)
    private fun throwInstallFail(internalRuntime: InternalRuntime, cause: Throwable?) {
        val e = RuntimeSelectionException(
            RuntimeSelectionException.RUNTIME_STATE_INSTALLATION_FAILED,
            internalRuntime.majorVersion
        )
        e.initCause(cause)
        throw e
    }

    @Throws(RuntimeSelectionException::class)
    private fun throwInstallFail(internalRuntime: InternalRuntime) {
        throw RuntimeSelectionException(
            RuntimeSelectionException.RUNTIME_STATE_INSTALLATION_FAILED,
            internalRuntime.majorVersion
        )
    }

    @Throws(RuntimeSelectionException::class)
    private fun unpackInternalRuntime(
        assetManager: AssetManager,
        internalRuntime: InternalRuntime,
        versionSignatures: String
    ) {
        val signatures = decodeSignatureBundle(versionSignatures)
        val platformBinFile =
            "bin-" + Architecture.archAsString(Tools.DEVICE_ARCHITECTURE) + ".tar.xz"
        if (!signatures.containsKey("universal.tar.xz") || !signatures.containsKey(platformBinFile)) {
            throwInstallFail(internalRuntime)
        }

        var universalCache: File? = null
        var platformCache: File? = null
        try {
            val signatureCheckUtil = create(assetManager)
            universalCache = File.createTempFile("jre-install-", "-universal", Tools.DIR_CACHE)
            platformCache = File.createTempFile("jre-install-", "-platform", Tools.DIR_CACHE)
            val runtimeDownloaderVerifier =
                RuntimeDownloaderVerifier(signatures, internalRuntime, signatureCheckUtil)
            if (!runtimeDownloaderVerifier.downloadAndVerify(
                    "universal.tar.xz",
                    universalCache,
                    R.string.downloading_java_runtime_uni
                ) ||
                !runtimeDownloaderVerifier.downloadAndVerify(
                    platformBinFile,
                    platformCache,
                    R.string.downloading_java_runtime_platform
                )
            ) {
                throwInstallFail(internalRuntime)
            }

            FileInputStream(universalCache).use { universal ->
                FileInputStream(platformCache).use { platform ->
                    installRuntimeNamedBinpack(
                        universal,
                        platform,
                        internalRuntime.runtimeName,
                        versionSignatures
                    )
                    postPrepare(internalRuntime.runtimeName)
                    forceReread(internalRuntime.runtimeName)
                }
            }
        } catch (e: IOException) {
            throwInstallFail(internalRuntime, e)
        } finally {
            ProgressLayout.clearProgress(ProgressLayout.UNPACK_RUNTIME)
            if (universalCache != null && universalCache.isFile)
                universalCache.delete()
            if (platformCache != null && platformCache.isFile)
                platformCache.delete()
        }
    }

    private fun getInternalRuntime(runtime: Runtime): InternalRuntime? {
        for (internalRuntime in InternalRuntime.entries) {
            if (internalRuntime.runtimeName == runtime.name) return internalRuntime
        }
        return null
    }

    private fun getNearestInstalledRuntime(targetVersion: Int): MathUtils.RankedValue<Runtime>? {
        val currentRuntimes = runtimes
        return MathUtils.findNearestPositive(
            targetVersion,
            currentRuntimes,
            { runtime: Runtime -> runtime.javaVersion })
    }

    private fun getNearestInternalRuntime(targetVersion: Int): MathUtils.RankedValue<InternalRuntime>? {
        val runtimeList: MutableList<InternalRuntime> = InternalRuntime.entries.toMutableList()
        return MathUtils.findNearestPositive(
            targetVersion,
            runtimeList,
            { runtime: InternalRuntime -> runtime.majorVersion })
    }


    @Throws(IOException::class, RuntimeSelectionException::class)
    fun installNewJreIfNeeded(
        assetManager: AssetManager,
        versionInfo: JMinecraftVersionList.Version
    ) {
        val javaVersionInfo = versionInfo.javaVersion
        if (javaVersionInfo == null || javaVersionInfo.component.equals(
                "jre-legacy",
                ignoreCase = true
            )
        ) return

        val gameRequiredVersion = javaVersionInfo.majorVersion

        val instance = loadSelectedInstance()
        val profileRuntime = Tools.getSelectedRuntime(instance!!)
        val runtime = read(profileRuntime)
        if (runtime.javaVersion >= gameRequiredVersion) {
            val internalRuntime = getInternalRuntime(runtime)
            if (internalRuntime != null) {
                checkInternalRuntime(assetManager, internalRuntime)
            }
            return
        }

        val nearestInstalledRuntime = getNearestInstalledRuntime(gameRequiredVersion)
        val nearestInternalRuntime = getNearestInternalRuntime(gameRequiredVersion)

        val selectedRankedRuntime = MathUtils.objectMin(
            nearestInternalRuntime,
            nearestInstalledRuntime,
            { value: MathUtils.RankedValue<out Any> -> value.rank }
        )

        if (selectedRankedRuntime == null) {
            throw RuntimeSelectionException(
                RuntimeSelectionException.RUNTIME_STATE_SELECTION_FAILED,
                gameRequiredVersion
            )
        }

        val selected: Any = selectedRankedRuntime.value
        val appropriateRuntime: String
        val internalRuntime: InternalRuntime?

        if (selected is Runtime) {
            val selectedRuntime = selected
            appropriateRuntime = selectedRuntime.name
            internalRuntime = getInternalRuntime(selectedRuntime)
        } else if (selected is InternalRuntime) {
            internalRuntime = selected
            appropriateRuntime = internalRuntime.runtimeName
        } else {
            throw RuntimeException("Unexpected type of selected: " + selected.javaClass.name)
        }

        if (internalRuntime != null) {
            checkInternalRuntime(assetManager, internalRuntime)
        }

        instance.selectedRuntime = appropriateRuntime
        instance.write()
    }

    private class RuntimeDownloaderVerifier(
        private val mSignatures: MutableMap<String?, ByteArray?>,
        internalRuntime: InternalRuntime,
        private val mSignatureCheckUtil: SignatureCheckUtil
    ) {
        private val mRuntimePath: String
        private val mDownloadBuffer = ByteArray(8192)

        init {
            this.mRuntimePath = DOWNLOAD_URL + internalRuntime.path + "/"
        }

        @Throws(IOException::class)
        fun downloadAndVerify(component: String?, output: File, progressString: Int): Boolean {
            downloadFileMonitored(
                mRuntimePath + component, output, mDownloadBuffer,
                DownloaderProgressWrapper(progressString, ProgressLayout.UNPACK_RUNTIME)
            )
            val signature = mSignatures[component]
            FileInputStream(output).use { fileInputStream ->
                return mSignatureCheckUtil.verify(fileInputStream, signature)
            }
        }
    }

    private enum class InternalRuntime(val majorVersion: Int, val runtimeName: String, val path: String) {
        JRE_17(17, "Internal-17", "components/jre-new"),
        JRE_21(21, "Internal-21", "components/jre-21"),
        JRE_25(25, "Internal-25", "components/jre-25")
    }
}
