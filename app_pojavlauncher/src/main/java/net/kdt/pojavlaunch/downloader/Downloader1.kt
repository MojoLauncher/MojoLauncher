package net.kdt.pojavlaunch.downloader

import com.kdt.mcgui.ProgressLayout
import net.ashmeet.hyperlauncher.R
import net.kdt.pojavlaunch.tasks.SpeedCalculator
import net.kdt.pojavlaunch.utils.DownloadUtils
import java.io.*
import java.net.HttpURLConnection
import java.net.URL
import java.nio.charset.StandardCharsets
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.atomic.AtomicReference

open class Downloader(private val mProgressKey: String) {
    private val mThreadException = AtomicReference<IOException?>()
    private val mDownloadedFileCounter = AtomicInteger()
    private val mDownloadedSizeCounter = AtomicLong()
    private val mInternetUsageCounter = AtomicLong()
    private val mUseSizeProgress = AtomicBoolean(true)
    private val mSpeedCalculator = SpeedCalculator()
    private var mDownloadService: ExecutorService? = null
    private var mVerifyService: ExecutorService? = null

    protected open fun runDownloads(downloads: ArrayList<out TaskMetadata>) {
        try {
            insertMetadata(downloads)
            performDownloads(downloads)
        } catch (e: InterruptedException) {
            taskException(IOException("Interrupted", e))
        }
    }

    @Throws(IOException::class, InterruptedException::class)
    private fun performDownloads(metadata: ArrayList<out TaskMetadata>) {
        mThreadException.set(null)
        mDownloadedFileCounter.set(0)
        mDownloadedSizeCounter.set(0)
        mDownloadService = Executors.newFixedThreadPool(3)
        val verifyThreads = (Runtime.getRuntime().availableProcessors() - 2).coerceAtLeast(2)
        mVerifyService = Executors.newFixedThreadPool(verifyThreads) { r: Runnable? ->
            val thread = Thread(r)
            thread.priority = 10
            thread.name = "verify thread"
            thread
        }
        var totalSize: Long = 0
        val totalCount = metadata.size
        val sizeCounter = mUseSizeProgress.get()
        for (element in metadata) {
            totalSize += element.size
            mVerifyService!!.submit(CheckFileOnDiskTask(element, this))
        }
        val totalMegabytes = totalSize / ONE_MEGABYTE
        while (mDownloadedFileCounter.get() < totalCount) {
            val exception = mThreadException.get()
            if (exception != null) throw exception
            if (sizeCounter) reportSizeProgress(totalMegabytes) else reportCountProgress(
                R.string.newerdl_downloading_files_count,
                totalCount
            )
            Thread.sleep(33)
        }
        mDownloadService!!.shutdown()
        mVerifyService!!.shutdown()
        
        mDownloadService!!.awaitTermination(100, TimeUnit.MILLISECONDS)
        mVerifyService!!.awaitTermination(100, TimeUnit.MILLISECONDS)
    }

    @Throws(IOException::class, InterruptedException::class)
    private fun insertMetadata(metadata: ArrayList<out TaskMetadata>) {
        mThreadException.set(null)
        mDownloadedFileCounter.set(0)
        val reducedList = ArrayList<TaskMetadata>()
        for (element in metadata) {
            if (!CompleteMetadataTask.shouldCompleteMetadata(element)) continue
            reducedList.add(element)
        }
        if (reducedList.isEmpty()) return
        val executorService = Executors.newFixedThreadPool(4)
        try {
            for (element in reducedList) executorService.submit(CompleteMetadataTask(element, this))
            executorService.shutdown()
            while (!executorService.awaitTermination(33, TimeUnit.MILLISECONDS)) {
                val exception = mThreadException.get()
                if (exception != null) throw exception
                reportCountProgress(R.string.newerdl_inserting_metadata_count, reducedList.size)
            }
        } finally {
            executorService.shutdownNow()
        }
    }

    private val speed: Double
        get() = mSpeedCalculator.feed(mInternetUsageCounter.get()) / ONE_MEGABYTE

    private fun reportCountProgress(resource: Int, total: Int) {
        val downloadedCount = mDownloadedFileCounter.get()
        val progress = if (total > 0) (downloadedCount / total.toFloat() * 100f).toInt() else 0
        ProgressLayout.setProgress(
            mProgressKey, progress, resource,
            downloadedCount, total, speed
        )
    }

    private fun reportSizeProgress(totalMegabytes: Double) {
        val downloadedMegabytes = mDownloadedSizeCounter.get() / ONE_MEGABYTE
        val progress = if (totalMegabytes > 0) (downloadedMegabytes / totalMegabytes * 100.0).toInt() else 0
        ProgressLayout.setProgress(
            mProgressKey, progress, R.string.newerdl_downloading_files_size,
            downloadedMegabytes, totalMegabytes, speed
        )
    }

    fun taskException(e: IOException?) {
        mThreadException.set(e)
    }

    fun disableSizeCounter() {
        mUseSizeProgress.lazySet(false)
    }

    fun submitFileForDownload(taskMetadata: TaskMetadata?) {
        if (taskMetadata != null) {
            mDownloadService!!.submit(DownloadFileTask(taskMetadata, this))
        }
    }

    fun submitFileForRecheck(taskMetadata: TaskMetadata?) {
        if (taskMetadata != null) {
            mVerifyService!!.submit(CheckFileOnDiskTask(taskMetadata, this, true))
        }
    }

    fun fileComplete() {
        mDownloadedFileCounter.getAndIncrement()
    }

    fun addSize(bytes: Long) {
        mDownloadedSizeCounter.getAndAdd(bytes)
    }

    @Throws(IOException::class)
    private fun copy(
        inputStream: InputStream,
        outputStream: OutputStream,
        listener: BytesCopiedListener?
    ) {
        val buffer = buffer
        var readLen: Int
        while (inputStream.read(buffer).also { readLen = it } != -1) {
            outputStream.write(buffer, 0, readLen)
            listener?.onBytesCopied(readLen)
            mInternetUsageCounter.getAndAdd(readLen.toLong())
        }
    }

    @Throws(IOException::class)
    protected open fun downloadToStream(
        connection: HttpURLConnection,
        outputStream: OutputStream,
        listener: BytesCopiedListener?
    ) {
        connection.inputStream.use { inputStream ->
            copy(inputStream, outputStream, listener)
        }
    }

    @Throws(IOException::class)
    protected open fun downloadString(url: URL): String {
        val connection = openConnection(url)
        val length = connection.contentLength
        val effectiveLength = if (length < 0) 32 else length
        ByteArrayOutputStream(effectiveLength).use { outputStream ->
            downloadToStream(connection, outputStream, null)
            return String(outputStream.toByteArray(), StandardCharsets.UTF_8)
        }
    }

    @Throws(IOException::class)
    fun downloadFile(file: File, url: URL, listener: BytesCopiedListener?) {
        val connection = openConnection(url)
        try {
            FileOutputStream(file).use { outputStream ->
                downloadToStream(connection, outputStream, listener)
            }
        } finally {
            connection.disconnect()
        }
    }

    @Throws(IOException::class)
    fun tryContinueDownload(
        file: File,
        wantedLength: Long,
        url: URL,
        listener: BytesCopiedListener?
    ): Boolean {
        val connection = openConnection(url)
        val range = String.format(Locale.ENGLISH, "bytes=%d-%d", file.length(), wantedLength - 1)
        connection.setRequestProperty("Range", range)
        return try {
            connection.connect()
            val responseCode = connection.responseCode
            if (responseCode != 206) {
                return false
            }
            FileOutputStream(file, true).use { outputStream ->
                downloadToStream(connection, outputStream, listener)
                true
            }
        } finally {
            connection.disconnect()
        }
    }

    @Throws(IOException::class)
    fun getFileContentLength(url: URL): Long {
        val connection = openConnection(url)
        connection.connectTimeout = 2000
        connection.readTimeout = 2000
        connection.requestMethod = "HEAD"
        return try {
            connection.connect()
            val response = connection.responseCode
            if (response >= 400) {
                -1
            } else {
                connection.contentLength.toLong()
            }
        } finally {
            connection.disconnect()
        }
    }

    interface BytesCopiedListener {
        fun onBytesCopied(bytes: Int)
    }

    companion object {
        private const val ONE_MEGABYTE = (1024.0 * 1024.0)
        private val sThreadLocalBuffer = ThreadLocal<ByteArray>()

        @get:Throws(IOException::class)
        val buffer: ByteArray
            get() {
                var buffer = sThreadLocalBuffer.get()
                if (buffer == null) {
                    buffer = ByteArray(8192)
                    sThreadLocalBuffer.set(buffer)
                }
                return buffer
            }

        @Throws(IOException::class)
        private fun openConnection(url: URL): HttpURLConnection {
            val connection = url.openConnection() as HttpURLConnection
            connection.readTimeout = 10000
            connection.setRequestProperty("User-Agent", DownloadUtils.USER_AGENT)
            connection.doInput = true
            connection.doOutput = false
            return connection
        }
    }
}
