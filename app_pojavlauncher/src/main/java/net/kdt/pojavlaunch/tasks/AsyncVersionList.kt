             package net.kdt.pojavlaunch.tasks

import net.kdt.pojavlaunch.JMinecraftVersionList
import net.kdt.pojavlaunch.PojavApplication
import net.kdt.pojavlaunch.Tools
import net.kdt.pojavlaunch.prefs.LauncherPreferences
import net.kdt.pojavlaunch.utils.DownloadUtils
import net.kdt.pojavlaunch.utils.DownloadUtils.ParseCallback
import net.kdt.pojavlaunch.utils.DownloadUtils.downloadStringCached
import java.io.IOException

/** Class getting the version list, and that's all really  */
class AsyncVersionList {
    private fun getVersionListAsync(versionDoneListener: VersionDoneListener?, retries: Int) {
        try {
            val versionList = getVersionListSync()
            if (versionDoneListener != null) versionDoneListener.onVersionDone(versionList)
        } catch (e: Exception) {
            if (retries < MAX_RETRIES) {
                getVersionListAsync(versionDoneListener, retries + 1)
            } else {
                versionDoneListener!!.onVersionDone(null)
                Tools.showErrorRemote(e)
            }
        }
    }

    fun getVersionList(listener: VersionDoneListener?) {
        PojavApplication.sExecutorService.execute(Runnable { getVersionListAsync(listener, 0) })
    }

    /**
     * Get the version list synchronously.
     * @return the version list, or null if it failed.
     */
    fun getVersionListSync(): JMinecraftVersionList? {
        return try {
            downloadStringCached(
                LauncherPreferences.PREF_VERSION_REPOS,
                "version_list",
                ParseCallback { input: String? -> parseList(input) }
            )
        } catch (e: Exception) {
            null
        }
    }

    /** Basic listener, acting as a callback  */
    interface VersionDoneListener {
        fun onVersionDone(versions: JMinecraftVersionList?)
    }

    companion object {
        private const val MAX_RETRIES = 5

        @Throws(DownloadUtils.ParseException::class)
        private fun parseList(input: String?): JMinecraftVersionList? {
            try {
                return Tools.GLOBAL_GSON.fromJson<JMinecraftVersionList?>(
                    input,
                    JMinecraftVersionList::class.java
                )
            } catch (e: Exception) {
                throw DownloadUtils.ParseException(e)
            }
        }
    }
}
