package net.kdt.pojavlaunch.modloaders.modpacks.imagecache

import android.graphics.BitmapFactory
import android.util.Log
import net.kdt.pojavlaunch.Tools
import net.kdt.pojavlaunch.instances.Instance
import net.kdt.pojavlaunch.utils.FileUtils
import java.io.File
import java.io.IOException
import java.lang.ref.WeakReference
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit

class ModIconCache {
    var cacheLoaderPool: ThreadPoolExecutor = ThreadPoolExecutor(
        10,
        10,
        1000,
        TimeUnit.MILLISECONDS,
        LinkedBlockingQueue()
    )
    var cachePath: File? = imageCachePath
    private val mCancelledReceivers = ArrayList<WeakReference<ImageReceiver?>?>()

    init {
        val path = cachePath
        if (path == null || !FileUtils.ensureDirectorySilently(path)) {
            throw RuntimeException("Failed to create icon cache directory")
        }
    }

    /**
     * Get an image for a mod with the associated tag and URL to download it in case if its not cached
     * @param imageReceiver the receiver interface that would get called when the image loads
     * @param imageTag the tag of the image to keep track of it
     * @param imageUrl the URL of the image in case if it's not cached
     */
    fun getImage(imageReceiver: ImageReceiver?, imageTag: String?, imageUrl: String?) {
        if (imageReceiver == null) return
        cacheLoaderPool.execute(ReadFromDiskTask(this, imageReceiver, imageTag, imageUrl))
    }

    /**
     * Mark the image obtainment task requested with this receiver as "cancelled". This means that
     * this receiver will not be called back and that some tasks related to this image may be
     * prevented from happening or interrupted.
     * @param imageReceiver the receiver to cancel
     */
    fun cancelImage(imageReceiver: ImageReceiver?) {
        synchronized(mCancelledReceivers) {
            mCancelledReceivers.add(WeakReference<ImageReceiver?>(imageReceiver))
        }
    }

    fun checkCancelled(imageReceiver: ImageReceiver): Boolean {
        var isCanceled = false
        synchronized(mCancelledReceivers) {
            val iterator = mCancelledReceivers.iterator()
            while (iterator.hasNext()) {
                val reference = iterator.next()
                if (reference?.get() == null) {
                    iterator.remove()
                    continue
                }
                if (reference.get() === imageReceiver) {
                    isCanceled = true
                }
            }
        }
        if (isCanceled) Log.i(
            "IconCache",
            "checkCancelled(${imageReceiver.hashCode()}) == true"
        )
        return isCanceled
    }

    companion object {
        val imageCachePath: File
            get() = File(Tools.DIR_CACHE, "mod_icons")

        /**
         * Get the base64-encoded version of a cached icon by its tag.
         * Note: this functions performs I/O operations, and should not be called on the UI
         * thread.
         * @param imageTag the icon tag
         * @return the base64 encoded image or null if not cached
         */
        fun writeInstanceImage(instance: Instance, imageTag: String?) {
            val imagePath = File(Tools.DIR_CACHE, "mod_icons/$imageTag.ca")
            Log.i("IconCache", "Creating base64 version of icon $imageTag")
            if (!imagePath.canRead() || !imagePath.isFile) {
                Log.i("IconCache", "Icon does not exist")
                return
            }
            try {
                val bitmap = BitmapFactory.decodeFile(imagePath.absolutePath) ?: return
                instance.encodeNewIcon(bitmap)
            } catch (e: IOException) {
                Log.i("ModIconCache", "Failed to reencode icon for instance")
                e.printStackTrace()
            }
        }
    }
}
