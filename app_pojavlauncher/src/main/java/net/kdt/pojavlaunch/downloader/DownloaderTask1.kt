package net.kdt.pojavlaunch.downloader

import java.io.IOException

abstract class DownloaderTask protected constructor(
    protected val mMetadata: TaskMetadata,
    protected val mDownloader: Downloader
) : Runnable {
    override fun run() {
        try {
            performTask()
        } catch (e: IOException) {
            mDownloader.taskException(e)
        }
    }

    @Throws(IOException::class)
    protected abstract fun performTask()
}
