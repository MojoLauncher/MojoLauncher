package net.kdt.pojavlaunch.modloaders

import net.kdt.pojavlaunch.modloaders.OptiFineUtils.OptiFineVersion
import net.kdt.pojavlaunch.tasks.AsyncMinecraftDownloader
import net.kdt.pojavlaunch.tasks.AsyncMinecraftDownloader.DoneListener
import net.kdt.pojavlaunch.tasks.MinecraftDownloader
import java.util.regex.Matcher
import java.util.regex.Pattern

class OptiFineDownloadTask(private val mOptiFineVersion: OptiFineVersion) : DoneListener {
    private val mMinecraftDownloadLock = Any()
    private var mDownloaderThrowable: Throwable? = null

    @Throws(Exception::class)
    fun prepareForInstall() {
        val minecraftVersion = determineMinecraftVersion()
        if (minecraftVersion == null) return
        if (!downloadMinecraft(minecraftVersion)) {
            if (mDownloaderThrowable is Exception) {
                throw mDownloaderThrowable as Exception
            } else {
                throw Exception(mDownloaderThrowable)
            }
        }
    }

    fun determineMinecraftVersion(): String? {
        val matcher: Matcher = sMcVersionPattern.matcher(mOptiFineVersion.minecraftVersion)
        if (matcher.find()) {
            val mcVersionBuilder = StringBuilder()
            mcVersionBuilder.append(matcher.group(1))
            mcVersionBuilder.append('.')
            mcVersionBuilder.append(matcher.group(2))
            val thirdGroup = matcher.group(3)
            if (thirdGroup != null && !thirdGroup.isEmpty() && ("0" != thirdGroup)) {
                mcVersionBuilder.append('.')
                mcVersionBuilder.append(thirdGroup)
            }
            return mcVersionBuilder.toString()
        } else {
            return null
        }
    }

    fun downloadMinecraft(minecraftVersion: String): Boolean {
        // the string is always normalized
        val minecraftJsonVersion = AsyncMinecraftDownloader.getListedVersion(minecraftVersion)
        if (minecraftJsonVersion == null) return false
        try {
            synchronized(mMinecraftDownloadLock) {
                MinecraftDownloader().start(null, minecraftJsonVersion, minecraftVersion, this)
                (mMinecraftDownloadLock as Object).wait()
            }
        } catch (e: InterruptedException) {
            e.printStackTrace()
        }
        return mDownloaderThrowable == null
    }

    override fun onDownloadDone() {
        synchronized(mMinecraftDownloadLock) {
            mDownloaderThrowable = null
            (mMinecraftDownloadLock as Object).notifyAll()
        }
    }

    override fun onDownloadFailed(throwable: Throwable?) {
        synchronized(mMinecraftDownloadLock) {
            mDownloaderThrowable = throwable
            (mMinecraftDownloadLock as Object).notifyAll()
        }
    }

    companion object {
        private val sMcVersionPattern: Pattern = Pattern.compile("([0-9]+)\\.([0-9]+)\\.?([0-9]+)?")
    }
}
