package net.kdt.pojavlaunch.downloader

import java.io.IOException

class CompleteMetadataTask(metadata: TaskMetadata, host: Downloader) : DownloaderTask(metadata, host) {
    override fun performTask() {
        val url = mMetadata.url ?: return
        try {
            val length = mDownloader.getFileContentLength(url)
            if (length != -1L) {
                mMetadata.size = length
            }
        } catch (e: IOException) {
            // Failure to get metadata shouldn't necessarily abort the whole process
        }
    }

    companion object {
        @JvmStatic
        fun shouldCompleteMetadata(metadata: TaskMetadata): Boolean {
            return metadata.size <= 0
        }
    }
}
