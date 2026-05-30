package net.kdt.pojavlaunch.downloader

import java.io.IOException
import java.util.concurrent.atomic.AtomicLong

class DownloadFileTask internal constructor(
    mMetadata: TaskMetadata,
    mHostDownloader: Downloader
) : DownloaderTask(mMetadata, mHostDownloader), Downloader.BytesCopiedListener {
    private val mBytesDownloaded = AtomicLong()

    @Throws(IOException::class)
    override fun performTask() {
        tryDownload(0, true)
        mDownloader.submitFileForRecheck(mMetadata)
    }

    @Throws(IOException::class)
    private fun performRetry(attempt: Int, rangeAllowed: Boolean) {
        mDownloader.addSize(-mBytesDownloaded.get()) // It will get readded again on next tryDownload() if range is allowed
        
        // ✅ Add a small delay between retries to potentially fix "aborted" connections
        try {
            Thread.sleep((200 * attempt).toLong())
        } catch (ignored: InterruptedException) {
        }

        tryDownload(attempt + 1, rangeAllowed)
    }

    @Throws(IOException::class)
    private fun tryDownload(attempt: Int, rangeAllowed: Boolean) {
        var currentRangeAllowed = rangeAllowed
        val path = mMetadata.path ?: throw IOException("Missing target path in metadata")
        val url = mMetadata.url ?: throw IOException("Missing URL in metadata")
        try {
            if (!path.exists() || !currentRangeAllowed) {
                mBytesDownloaded.set(0)
                mDownloader.downloadFile(path, url, this)
            } else {
                val alreadyDownloaded = path.length()
                mBytesDownloaded.set(alreadyDownloaded)
                mDownloader.addSize(alreadyDownloaded)
                currentRangeAllowed = mDownloader.tryContinueDownload(
                    path,
                    mMetadata.size,
                    url,
                    this
                )
                if (!currentRangeAllowed) performRetry(attempt, false)
            }
        } catch (e: IOException) {
            if (attempt == 5) throw e
            performRetry(attempt, currentRangeAllowed)
        }
    }

    override fun onBytesCopied(nbytes: Int) {
        mBytesDownloaded.getAndAdd(nbytes.toLong())
        mDownloader.addSize(nbytes.toLong())
    }
}
