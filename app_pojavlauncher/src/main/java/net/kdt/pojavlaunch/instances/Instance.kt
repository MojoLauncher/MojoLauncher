package net.kdt.pojavlaunch.instances

import android.graphics.Bitmap
import android.os.Build
import android.util.Log
import net.kdt.pojavlaunch.Tools
import net.kdt.pojavlaunch.prefs.LauncherPreferences
import net.kdt.pojavlaunch.utils.JSONUtils
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

class Instance : DisplayInstance() {
    @JvmField
    var installer: InstanceInstaller? = null
    @JvmField
    var renderer: String? = null
    @JvmField
    var jvmArgs: String? = null
    var argsMode: Int = 0
    @JvmField
    var selectedRuntime: String? = null
    @JvmField
    var controlLayout: String? = null
    @JvmField
    var sharedData: Boolean = false

    override fun sanitize() {
        super.sanitize()
        sanitizeArgs()
    }

    private fun sanitizeArgs() {
        if (argsMode > ARGS_MODE_LAST) {
            argsMode = 0
            jvmArgs = null
        }
    }

    /**
     * Write the current contents of the instance to persistent storage.
     * @throws IOException in case of write errors
     */
    @Throws(IOException::class)
    fun write() {
        JSONUtils.writeToFile(Instances.Companion.metadataLocation(mInstanceRoot), this)
    }

    /**
     * Try to write the contents of the instance, ignore any exceptions
     */
    fun maybeWrite() {
        try {
            write()
        } catch (e: IOException) {
            Log.e("Instance", "Failed to write", e)
        }
    }

    /**
     * Encode the Bitmap as the new profile icon with required encoding settings.
     * @param bitmap the target bitmap
     * @throws IOException in case of errors while storing the icon
     */
    @Throws(IOException::class)
    fun encodeNewIcon(bitmap: Bitmap) {
        FileOutputStream(instanceIconLocation).use { fileOutputStream ->
            bitmap.compress(
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R)  // On Android < 30, there was no distinction between "lossy" and "lossless",
                // and the type is picked by the quality parameter. We set the quality to 60.
                // so it should be lossy,
                    Bitmap.CompressFormat.WEBP else  // On Android >= 30, we can explicitly specify that we want lossy compression
                // with the visual quality of 60.
                    Bitmap.CompressFormat.WEBP_LOSSY,
                60,
                fileOutputStream
            )
        }
    }

    val launchRenderer: String?
        get() {
            if (Tools.isValidString(renderer)) return renderer
            return LauncherPreferences.PREF_RENDERER
        }

    val launchArgs: String?
        get() {
            if (!Tools.isValidString(jvmArgs)) return LauncherPreferences.PREF_CUSTOM_JAVA_ARGS
            when (argsMode) {
                ARGS_MODE_REPLACE -> return jvmArgs
                ARGS_MODE_MERGE_DEFAULT_FIRST -> return LauncherPreferences.PREF_CUSTOM_JAVA_ARGS + " " + jvmArgs
                ARGS_MODE_MERGE_INSTANCE_FIRST -> return jvmArgs + " " + LauncherPreferences.PREF_CUSTOM_JAVA_ARGS
                else -> throw RuntimeException("Unknown value for argsMode: " + argsMode)
            }
        }

    val launchControls: String?
        get() {
            if (!Tools.isValidString(controlLayout)) return LauncherPreferences.PREF_DEFAULTCTRL_PATH
            return Tools.CTRLMAP_PATH + "/" + controlLayout
        }

    val gameDirectory: File?
        get() {
            if (sharedData) return Instances.Companion.SHARED_DATA_DIRECTORY
            return mInstanceRoot
        }

    companion object {
        const val ARGS_MODE_REPLACE: Int = 0
        const val ARGS_MODE_MERGE_DEFAULT_FIRST: Int = 1
        const val ARGS_MODE_MERGE_INSTANCE_FIRST: Int = 2
        val ARGS_MODE_LAST: Int = ARGS_MODE_MERGE_INSTANCE_FIRST

        const val VERSION_LATEST_RELEASE: String = "latest_release"
        const val VERSION_LATEST_SNAPSHOT: String = "latest_snapshot"
    }
}
