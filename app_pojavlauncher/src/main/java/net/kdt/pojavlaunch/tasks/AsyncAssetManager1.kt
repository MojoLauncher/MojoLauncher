package net.kdt.pojavlaunch.tasks

import android.content.Context
import android.content.res.AssetManager
import android.util.Log
import com.kdt.mcgui.ProgressLayout
import net.kdt.pojavlaunch.Architecture
import net.kdt.pojavlaunch.PojavApplication
import net.kdt.pojavlaunch.Tools
import net.kdt.pojavlaunch.multirt.MultiRTUtils.getExactJreName
import net.kdt.pojavlaunch.multirt.MultiRTUtils.installRuntimeNamedBinpack
import net.kdt.pojavlaunch.multirt.MultiRTUtils.postPrepare
import net.kdt.pojavlaunch.multirt.MultiRTUtils.readInternalRuntimeVersion
import org.apache.commons.io.FileUtils
import org.apache.commons.io.IOUtils
import java.io.File
import java.io.FileInputStream
import java.io.IOException
import java.nio.charset.StandardCharsets


object AsyncAssetManager {
    /**
     * Attempt to install the java 8 runtime, if necessary
     * @param am App context
     */
    @JvmStatic
    fun unpackRuntime(am: AssetManager) {
        /* Check if JRE is included */
        var rtVersion: String? = null
        val currentRtVersion = readInternalRuntimeVersion("Internal")
        try {
            rtVersion = Tools.read(am.open("components/jre/version"))
        } catch (e: IOException) {
            Log.e("JREAuto", "JRE was not included on this APK.", e)
        }
        val exactJREName = getExactJreName(8)
        if (currentRtVersion == null && exactJREName != null && (exactJREName != "Internal") /*this clause is for when the internal runtime is goofed*/) return
        if (rtVersion == null) return
        if (rtVersion == currentRtVersion) return

        // Install the runtime in an async manner, hope for the best
        val finalRtVersion = rtVersion
        PojavApplication.sExecutorService.execute {
            try {
                installRuntimeNamedBinpack(
                    am.open("components/jre/universal.tar.xz"),
                    am.open("components/jre/bin-" + Architecture.archAsString(Tools.DEVICE_ARCHITECTURE) + ".tar.xz"),
                    "Internal", finalRtVersion
                )
                postPrepare("Internal")
            } catch (e: IOException) {
                Log.e("JREAuto", "Internal JRE unpack failed", e)
            }
        }
    }

    /** Unpack single files, with no regard to version tracking  */
    @JvmStatic
    fun unpackSingleFiles(ctx: Context?) {
        if (ctx == null) return
        ProgressLayout.setProgress(ProgressLayout.EXTRACT_SINGLE_FILES, 0)
        PojavApplication.sExecutorService.execute {
            try {
                Tools.copyAssetFile(ctx, "default.json", Tools.CTRLMAP_PATH, false)
                Tools.copyAssetFile(ctx, "launcher_profiles.json", Tools.DIR_GAME_NEW, false)
                Tools.copyAssetFile(ctx, "resolv.conf", Tools.DIR_DATA, false)
            } catch (e: Throwable) {
                Log.e("AsyncAssetManager", "Failed to unpack critical components !", e)
            } finally {
                ProgressLayout.clearProgress(ProgressLayout.EXTRACT_SINGLE_FILES)
            }
        }
    }

    @JvmStatic
    fun unpackComponents(ctx: Context) {
        ProgressLayout.setProgress(ProgressLayout.EXTRACT_COMPONENTS, 0)
        PojavApplication.sExecutorService.execute {
            try {
                tryUnpackComponent(ctx, "caciocavallo", false)
                tryUnpackComponent(ctx, "caciocavallo17", false)
                tryUnpackComponent(ctx, "lwjgl3", false)

                tryUnpackComponent(ctx, "security", true)
                tryUnpackComponent(ctx, "arc_dns_injector", true)
                tryUnpackComponent(ctx, "forge_installer", true)
                tryUnpackComponent(ctx, "authlib-injector", true)
            } catch (e: Throwable) {
                Log.e("AsyncAssetManager", "Failed to unpack components !", e)
            } finally {
                ProgressLayout.clearProgress(ProgressLayout.EXTRACT_COMPONENTS)
            }
        }
    }

    private fun readInstalledComponentVersion(componentRoot: File?): String? {
        val localVersionFile = File(componentRoot, "version")
        try {
            FileInputStream(localVersionFile).use { fileInputStream ->
                return IOUtils.toString(fileInputStream, StandardCharsets.UTF_8)
            }
        } catch (_: IOException) {
        }
        return null
    }

    private fun readBuiltinComponentVersion(
        assetManager: AssetManager,
        componentName: String
    ): String? {
        val componentVersionLocation = "components/$componentName/version"
        try {
            assetManager.open(componentVersionLocation).use { inputStream ->
                return IOUtils.toString(inputStream, StandardCharsets.UTF_8)
            }
        } catch (_: IOException) {
        }
        return null
    }

    private fun tryUnpackComponent(ctx: Context, component: String, privateDirectory: Boolean) {
        try {
            unpackComponent(ctx, component, privateDirectory)
        } catch (e: IOException) {
            Log.e("AssetUnpacker", "Failed to unpack component $component", e)
        }
    }

    @Throws(IOException::class)
    private fun unpackComponent(ctx: Context, component: String, privateDirectory: Boolean) {
        val am = ctx.assets
        val rootDir = if (privateDirectory) Tools.DIR_DATA else Tools.DIR_GAME_HOME
        val componentTarget = File(rootDir, component)
        val installedVersion = readInstalledComponentVersion(componentTarget)
        val builtinVersion = readBuiltinComponentVersion(am, component)
        if (installedVersion != null && installedVersion == builtinVersion) {
            Log.i("AssetUnpacker", "Component $component is up-to-date, continuing...")
            return
        }
        Log.i("AssetUnpacker", "Updating $component")

        if (componentTarget.exists()) {
            FileUtils.deleteDirectory(componentTarget)
        }
        if (!componentTarget.mkdirs()) {
            throw IOException("Failed to create directory for $component")
        }

        val componentSource = "components/$component"

        val fileList = am.list(componentSource)
        if (fileList != null) {
            for (fileName in fileList) {
                if (fileName == "version") continue
                val sourcePath = "$componentSource/$fileName"
                Tools.copyAssetFile(ctx, sourcePath, componentTarget.absolutePath, true)
            }
        }

        // Always write the version file separately after extracting everything else, to improve
        // reliability.
        Tools.write(File(componentTarget, "version"), builtinVersion)
    }

    @JvmStatic
    fun extractDefaultSettings(context: Context?, gamedir: File) {
        if (context == null) return
        try {
            val gameDirPath = gamedir.absolutePath
            Tools.copyAssetFile(context, "options.txt", gameDirPath, false)
        } catch (e: IOException) {
            Tools.showError(context, e)
        }
    }
}
