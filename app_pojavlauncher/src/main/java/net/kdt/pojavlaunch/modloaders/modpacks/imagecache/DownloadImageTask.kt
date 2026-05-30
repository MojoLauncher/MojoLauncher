package net.kdt.pojavlaunch.modloaders.modpacks.imagecache

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import net.kdt.pojavlaunch.utils.DownloadUtils
import java.io.FileOutputStream
import java.io.IOException
import kotlin.math.min

internal class DownloadImageTask(private val mParentTask: ReadFromDiskTask) : Runnable {
    private var mRetryCount = 0

    override fun run() {
        var wasSuccessful = false
        while (mRetryCount < 5 && !(runCatching().also { wasSuccessful = it })) {
            mRetryCount++
        }
        // restart the parent task to read the image and send it to the receiver
        // if it wasn't cancelled. If it was, then we just die here
        if (wasSuccessful && !mParentTask.taskCancelled()) mParentTask.iconCache?.cacheLoaderPool?.execute(
            mParentTask
        )
    }

    fun runCatching(): Boolean {
        try {
            IconCacheJanitor.waitForJanitorToFinish()
            DownloadUtils.downloadFile(mParentTask.imageUrl, mParentTask.cacheFile)
            val bitmap = BitmapFactory.decodeFile(mParentTask.cacheFile.getAbsolutePath())
            if (bitmap == null) return false
            val bitmapWidth = bitmap.getWidth()
            val bitmapHeight = bitmap.getHeight()
            if (bitmapWidth <= BITMAP_FINAL_DIMENSION && bitmapHeight <= BITMAP_FINAL_DIMENSION) {
                bitmap.recycle()
                return true
            }
            val imageRescaleRatio: Float =
                min(BITMAP_FINAL_DIMENSION / bitmapWidth, BITMAP_FINAL_DIMENSION / bitmapHeight)
            val resizedBitmap = Bitmap.createScaledBitmap(
                bitmap,
                (bitmapWidth * imageRescaleRatio).toInt(),
                (bitmapHeight * imageRescaleRatio).toInt(),
                true
            )
            bitmap.recycle()
            if (resizedBitmap == bitmap) return true
            try {
                FileOutputStream(mParentTask.cacheFile).use { fileOutputStream ->
                    resizedBitmap.compress(Bitmap.CompressFormat.JPEG, 80, fileOutputStream)
                }
            } finally {
                resizedBitmap.recycle()
            }
            return true
        } catch (e: IOException) {
            e.printStackTrace()
            return false
        }
    }

    companion object {
        private const val BITMAP_FINAL_DIMENSION = 256f
    }
}
