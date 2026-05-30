package net.kdt.pojavlaunch.downloader

import android.util.Log
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
        } catch (t: Throwable) {
            Log.e("DownloaderTask", "Unexpected error during task execution", t)
            mDownloader.taskException(IOException("Unexpected error: " + t.message, t))
        }
    }

    @Throws(IOException::class)
    protected abstract fun performTask()
}
