package net.kdt.pojavlaunch.downloader

import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong

open class Downloader {
    private val mVerifyService: ExecutorService
    private val mDownloadService: ExecutorService
    private val mTaskQueue = ConcurrentLinkedQueue<TaskMetadata>()
    private val mDownloadedSizeCounter = AtomicLong(0)
    private val mInternetUsageCounter = AtomicLong(0)
    private val mDownloadedFileCounter = AtomicInteger(0)
    private val mTotalFileCount = AtomicInteger(0)
    private val mAborted = AtomicBoolean(false)
    private var mOnCompletedListener: Runnable? = null
    private var mActiveDownloadCount = 0
    private var mSizeCounterDisabled = false
    private var mProgressKey: String? = null

    constructor(threadCount: Int) {
        mDownloadService = Executors.newFixedThreadPool(threadCount)
        mVerifyService = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors())
    }

    constructor(progressKey: String?) : this(Runtime.getRuntime().availableProcessors()) {
        mProgressKey = progressKey
    }

    @Synchronized
    fun submitFileForDownload(metadata: TaskMetadata?) {
        if (!mAborted.get() && metadata != null) {
            mActiveDownloadCount++
            mDownloadService.submit(DownloadFileTask(metadata, this))
        }
    }

    fun abort() {
        mAborted.set(true)
        mVerifyService.shutdownNow()
        mDownloadService.shutdownNow()
        synchronized(this) {
            (this as java.lang.Object).notifyAll()
        }
    }

    fun taskException(e: IOException) {
        abort()
    }

    val progress: Int
        get() {
            val total = mTotalFileCount.get()
            return if (total == 0) 0 else mDownloadedFileCounter.get() * 100 / total
        }

    fun addSize(size: Long) {
        if (!mSizeCounterDisabled) {
            mDownloadedSizeCounter.addAndGet(size)
        }
    }

    @get:Throws(IOException::class)
    protected val buffer: ByteArray
        get() = ByteArray(8192)

    @Throws(IOException::class)
    protected fun openConnection(url: URL): HttpURLConnection {
        val connection = url.openConnection() as HttpURLConnection
        connection.connectTimeout = 10000
        connection.readTimeout = 10000
        connection.instanceFollowRedirects = true
        connection.setRequestProperty(
            "User-Agent",
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/58.0.3029.110 Safari/537.3"
        )
        return connection
    }

    fun runDownloads(metadataList: List<TaskMetadata?>?) {
        if (metadataList == null) return
        
        val validMetadata = metadataList.filterNotNull()
        if (validMetadata.isEmpty()) return
        
        mTotalFileCount.set(validMetadata.size)
        mDownloadedFileCounter.set(0)
        
        for (metadata in validMetadata) {
            mVerifyService.submit(CheckFileOnDiskTask(metadata, this))
        }

        synchronized(this) {
            while (mDownloadedFileCounter.get() < mTotalFileCount.get() && !mAborted.get()) {
                try {
                    (this as java.lang.Object).wait(500)
                } catch (e: InterruptedException) {
                    abort()
                    throw IOException("Interrupted", e)
                }
            }
        }

        if (mAborted.get()) {
            throw IOException("Download aborted or failed")
        }
        
        if (mOnCompletedListener != null) mOnCompletedListener!!.run()
    }

    fun submitFileForRecheck(taskMetadata: TaskMetadata?) {
        if (!mAborted.get() && taskMetadata != null) mVerifyService.submit(CheckFileOnDiskTask(taskMetadata, this, true))
    }

    fun fileComplete() {
        mDownloadedFileCounter.getAndIncrement()
        synchronized(this) {
            (this as java.lang.Object).notifyAll()
        }
    }

    @Throws(IOException::class)
    fun downloadFile(path: File, url: URL, listener: BytesCopiedListener?) {
        val connection = openConnection(url)
        FileOutputStream(path).use { outputStream ->
            downloadToStream(connection, outputStream, listener)
        }
    }

    @Throws(IOException::class)
    fun tryContinueDownload(path: File, totalSize: Long, url: URL, listener: BytesCopiedListener?): Boolean {
        val currentSize = path.length()
        if (currentSize >= totalSize) return true
        
        val connection = openConnection(url)
        connection.setRequestProperty("Range", "bytes=$currentSize-")
        val responseCode = connection.responseCode
        if (responseCode != HttpURLConnection.HTTP_PARTIAL) return false
        
        FileOutputStream(path, true).use { outputStream ->
            downloadToStream(connection, outputStream, listener)
        }
        return true
    }

    @Throws(IOException::class)
    private fun copy(
        inputStream: InputStream,
        outputStream: OutputStream,
        listener: BytesCopiedListener?
    ) {
        val buffer: ByteArray = buffer
        var readLen: Int = 0
        while (!mAborted.get() && (inputStream.read(buffer).also { readLen = it }) != -1) {
            outputStream.write(buffer, 0, readLen)
            if (listener != null) listener.onBytesCopied(readLen)
            mInternetUsageCounter.getAndAdd(readLen.toLong())
        }
        if (mAborted.get()) throw IOException("Aborted")
    }

    @Throws(IOException::class)
    protected fun downloadToStream(
        connection: HttpURLConnection,
        outputStream: OutputStream,
        listener: BytesCopiedListener?
    ) {
        val inputStream = connection.inputStream
        copy(inputStream, outputStream, listener)
    }

    interface BytesCopiedListener {
        fun onBytesCopied(bytes: Int)
    }
}
