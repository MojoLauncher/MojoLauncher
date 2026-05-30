package net.kdt.pojavlaunch.modloaders

import java.io.File

class ModloaderListenerProxy : ModloaderDownloadListener {
    private var mDestinationListener: ModloaderDownloadListener? = null
    private var mProxyResultObject: Any? = null
    private var mProxyResult: Int = PROXY_RESULT_NONE

    @Synchronized
    override fun onDownloadFinished(downloadedFile: File?) {
        if (mDestinationListener != null) {
            mDestinationListener!!.onDownloadFinished(downloadedFile)
        } else {
            mProxyResult = PROXY_RESULT_FINISHED
            mProxyResultObject = downloadedFile
        }
    }

    @Synchronized
    override fun onDataNotAvailable() {
        if (mDestinationListener != null) {
            mDestinationListener!!.onDataNotAvailable()
        } else {
            mProxyResult = PROXY_RESULT_NOT_AVAILABLE
            mProxyResultObject = null
        }
    }

    @Synchronized
    override fun onDownloadError(e: Exception?) {
        if (mDestinationListener != null) {
            mDestinationListener!!.onDownloadError(e)
        } else {
            mProxyResult = PROXY_RESULT_ERROR
            mProxyResultObject = e
        }
    }

    @Synchronized
    fun attachListener(listener: ModloaderDownloadListener) {
        when (mProxyResult) {
            PROXY_RESULT_FINISHED -> listener.onDownloadFinished(mProxyResultObject as File?)
            PROXY_RESULT_NOT_AVAILABLE -> listener.onDataNotAvailable()
            PROXY_RESULT_ERROR -> listener.onDownloadError(mProxyResultObject as Exception?)
        }
        mDestinationListener = listener
    }

    @Synchronized
    fun detachListener() {
        mDestinationListener = null
    }

    companion object {
        val PROXY_RESULT_NONE: Int = -1
        const val PROXY_RESULT_FINISHED: Int = 0
        const val PROXY_RESULT_NOT_AVAILABLE: Int = 1
        const val PROXY_RESULT_ERROR: Int = 2
    }
}
