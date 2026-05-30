package com.kdt.mcgui

import androidx.annotation.StringRes
import net.kdt.pojavlaunch.progresskeeper.ProgressKeeper.submitProgress

/** 
 * Legacy wrapper for ProgressKeeper constants and static methods.
 * UI logic moved to Compose LauncherScreen.
 */
object ProgressLayout {
    const val UNPACK_RUNTIME: String = "unpack_runtime"
    const val DOWNLOAD_MINECRAFT: String = "download_minecraft"
    const val DOWNLOAD_VERSION_LIST: String = "download_verlist"
    const val AUTHENTICATE: String = "authenticate"
    const val INSTALL_MODPACK: String = "install_modpack"
    const val EXTRACT_COMPONENTS: String = "extract_components"
    const val EXTRACT_SINGLE_FILES: String = "extract_single_files"
    const val INSTANCE_INSTALL: String = "instance_install"
    const val CONTENT_INSTALL: String = "content_install"

    /** Update the progress bar content  */
    @JvmStatic
    fun setProgress(progressKey: String?, progress: Int) {
        submitProgress(progressKey, progress, -1, null as Any?)
    }

    /** Update the text and progress content  */
    @JvmStatic
    fun setProgress(
        progressKey: String?,
        progress: Int,
        @StringRes resource: Int,
        vararg message: Any?
    ) {
        submitProgress(progressKey, progress, resource, *message)
    }

    /** Update the text and progress content  */
    @JvmStatic
    fun setProgress(progressKey: String?, progress: Int, message: String?) {
        setProgress(progressKey, progress, -1, message as Any?)
    }

    /** Update the text and progress content  */
    @JvmStatic
    fun clearProgress(progressKey: String?) {
        setProgress(progressKey, -1, -1)
    }
}
