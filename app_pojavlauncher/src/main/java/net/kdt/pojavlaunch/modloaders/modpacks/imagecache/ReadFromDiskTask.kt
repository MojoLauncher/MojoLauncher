package net.kdt.pojavlaunch.modloaders.modpacks.imagecache

import android.graphics.BitmapFactory
import net.kdt.pojavlaunch.Tools
import java.io.File

class ReadFromDiskTask internal constructor(
    iconCache: ModIconCache,
    imageReceiver: ImageReceiver,
    cacheTag: String?,
    imageUrl: String?
) : Runnable {
    val iconCache: ModIconCache?
    val imageReceiver: ImageReceiver
    val cacheFile: File
    val imageUrl: String?

    init {
        this.iconCache = iconCache
        this.imageReceiver = imageReceiver
        this.cacheFile = File(iconCache.cachePath, cacheTag + ".ca")
        this.imageUrl = imageUrl
    }

    fun runDownloadTask() {
        iconCache?.cacheLoaderPool?.execute(DownloadImageTask(this))
    }

    override fun run() {
        if (cacheFile.isDirectory()) {
            return
        }
        if (cacheFile.canRead()) {
            IconCacheJanitor.waitForJanitorToFinish()
            val bitmap = BitmapFactory.decodeFile(cacheFile.getAbsolutePath())
            if (bitmap != null) {
                Tools.runOnUiThread {
                    if (taskCancelled()) {
                        bitmap.recycle() // do not leak the bitmap if the task got cancelled right at the end
                        return@runOnUiThread
                    }
                    imageReceiver.onImageAvailable(bitmap)
                }
                return
            }
        }
        if (iconCache?.cachePath?.canWrite() == true &&
            !taskCancelled()
        ) { // don't run the download task if the task got canceled
            runDownloadTask()
        }
    }

    fun taskCancelled(): Boolean {
        return iconCache?.checkCancelled(imageReceiver) ?: true
    }
}
