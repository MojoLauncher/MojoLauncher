package net.kdt.pojavlaunch.modloaders.modpacks.imagecache

import android.util.Log
import net.kdt.pojavlaunch.PojavApplication
import java.io.File
import java.util.concurrent.ExecutionException
import java.util.concurrent.Future

/**
 * This image is intended to keep the mod icon cache tidy (aka under 100 megabytes)
 */
class IconCacheJanitor private constructor() : Runnable {
    override fun run() {
        val modIconCachePath: File = ModIconCache.imageCachePath
        if (!modIconCachePath.isDirectory || !modIconCachePath.canRead()) return
        val modIconFiles = modIconCachePath.listFiles() ?: return
        val writableModIconFiles = ArrayList<File>(modIconFiles.size)
        var directoryFileSize: Long = 0
        for (modIconFile in modIconFiles) {
            if (!modIconFile.isFile || !modIconFile.canRead()) continue
            directoryFileSize += modIconFile.length()
            if (!modIconFile.canWrite()) continue
            writableModIconFiles.add(modIconFile)
        }
        if (directoryFileSize < CACHE_SIZE_LIMIT) {
            Log.i("IconCacheJanitor", "Skipping cleanup because there's not enough to clean up")
            return
        }

        modIconFiles.sortWith { x, y ->
            y.lastModified().compareTo(x.lastModified())
        }

        var filesCleanedUp = 0
        for (modFile in writableModIconFiles) {
            if (directoryFileSize < CACHE_BRINGDOWN) break
            val modFileSize = modFile.length()
            if (modFile.delete()) {
                directoryFileSize -= modFileSize
                filesCleanedUp++
            }
        }
        Log.i("IconCacheJanitor", "Cleaned up $filesCleanedUp files")
        synchronized(IconCacheJanitor::class.java) {
            sJanitorFuture = null
            sJanitorRan = true
        }
    }

    companion object {
        const val CACHE_SIZE_LIMIT: Long = 104857600 // The cache size limit, 100 megabytes
        const val CACHE_BRINGDOWN: Long =
            52428800 // The size to which the cache should be brought

        // in case of an overflow, 50 mb
        private var sJanitorFuture: Future<*>? = null
        private var sJanitorRan = false

        /**
         * Runs the janitor task, unless there was one running already or one has ran already
         */
        @JvmStatic
        fun runJanitor() {
            synchronized(IconCacheJanitor::class.java) {
                if (sJanitorFuture != null || sJanitorRan) return
                sJanitorFuture = PojavApplication.sExecutorService.submit(IconCacheJanitor())
            }
        }

        /**
         * Waits for the janitor task to finish, if there is one running already
         * Note that the thread waiting must not be interrupted.
         */
        fun waitForJanitorToFinish() {
            synchronized(IconCacheJanitor::class.java) {
                val future = sJanitorFuture ?: return
                try {
                    future.get()
                } catch (e: ExecutionException) {
                    throw RuntimeException("Should not happen!", e)
                } catch (e: InterruptedException) {
                    throw RuntimeException("Should not happen!", e)
                }
            }
        }
    }
}
