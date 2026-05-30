package net.kdt.pojavlaunch.downloader

import net.kdt.pojavlaunch.prefs.LauncherPreferences
import net.kdt.pojavlaunch.utils.HashUtils.compareSHA1
import java.io.IOException

class CheckFileOnDiskTask : DownloaderTask {
    private val mAfterDownload: Boolean

    internal constructor(mMetadata: TaskMetadata, mHostDownloader: Downloader) : super(
        mMetadata,
        mHostDownloader
    ) {
        this.mAfterDownload = false
    }

    internal constructor(
        mMetadata: TaskMetadata,
        mHostDownloader: Downloader,
        mAfterDownload: Boolean
    ) : super(mMetadata, mHostDownloader) {
        this.mAfterDownload = mAfterDownload
    }

    @Throws(IOException::class)
    override fun performTask() {
        val checkResult = checkFile()
        if (checkResult) {
            if (!mAfterDownload) mDownloader.addSize(mMetadata.size)
            mDownloader.fileComplete()
        } else {
            if (!mAfterDownload) mDownloader.submitFileForDownload(mMetadata)
            else throw IOException("Failed to verify " + mMetadata.toString())
        }
    }

    @Throws(IOException::class)
    private fun checkFile(): Boolean {
        val localFile = mMetadata.path ?: return false
        if (!localFile.exists()) return false
        if (!LauncherPreferences.PREF_VERIFY_FILES) return true
        if (mMetadata.size != -1L) {
            if (mMetadata.size != localFile.length()) return false
            if (LauncherPreferences.PREF_RAPID_START && !mAfterDownload) return true
        }
        val sha1 = mMetadata.sha1Hash
        return sha1 == null || compareSHA1(localFile, sha1)
    }
}
