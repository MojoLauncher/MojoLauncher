package net.kdt.pojavlaunch.modloaders

import java.io.File

interface ModloaderDownloadListener {
    fun onDownloadFinished(downloadedFile: File?)
    fun onDataNotAvailable()
    fun onDownloadError(e: Exception?)
}
