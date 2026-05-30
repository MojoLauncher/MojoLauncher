package net.kdt.pojavlaunch.utils

import android.util.Log
import net.kdt.pojavlaunch.Tools
import net.kdt.pojavlaunch.Tools.DownloaderFeedback
import org.apache.commons.io.IOUtils
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.nio.charset.StandardCharsets
import java.util.concurrent.Callable

object DownloadUtils {
    @JvmField
    val USER_AGENT: String? = Tools.APP_NAME

    @Throws(IOException::class)
    fun download(url: String?, os: OutputStream?) {
        download(URL(url), os)
    }

    @Throws(IOException::class)
    fun download(url: URL, os: OutputStream?) {
        var `is`: InputStream? = null
        try {
            // System.out.println("Connecting: " + url.toString());
            val conn = url.openConnection() as HttpURLConnection
            conn.setRequestProperty("User-Agent", USER_AGENT)
            conn.setConnectTimeout(10000)
            conn.setDoInput(true)
            conn.connect()
            if (conn.getResponseCode() != HttpURLConnection.HTTP_OK) {
                throw IOException(
                    ("Server returned HTTP " + conn.getResponseCode()
                            + ": " + conn.getResponseMessage())
                )
            }
            `is` = conn.getInputStream()
            IOUtils.copy(`is`, os)
        } catch (e: IOException) {
            throw IOException("Unable to download from " + url, e)
        } finally {
            if (`is` != null) {
                try {
                    `is`.close()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    @JvmStatic
    @Throws(IOException::class)
    fun downloadString(url: String?): String {
        val bos = ByteArrayOutputStream()
        download(url, bos)
        bos.close()
        return String(bos.toByteArray(), StandardCharsets.UTF_8)
    }

    @JvmStatic
    @Throws(IOException::class)
    fun downloadFile(url: String?, out: File) {
        FileUtils.ensureParentDirectory(out)
        FileOutputStream(out).use { fileOutputStream ->
            download(url, fileOutputStream)
        }
    }

    @JvmStatic
    @Throws(IOException::class)
    fun downloadFileMonitored(
        urlInput: String?, outputFile: File, buffer: ByteArray?,
        monitor: DownloaderFeedback
    ) {
        var buffer = buffer
        FileUtils.ensureParentDirectory(outputFile)

        val conn = URL(urlInput).openConnection() as HttpURLConnection
        val readStr = conn.getInputStream()
        FileOutputStream(outputFile).use { fos ->
            var current: Int
            var overall = 0
            val length = conn.getContentLength()

            if (buffer == null) buffer = ByteArray(65535)

            while ((readStr.read(buffer).also { current = it }) != -1) {
                overall += current
                fos.write(buffer, 0, current)
                monitor.updateProgress(overall, length)
            }
            conn.disconnect()
        }
    }

    @Throws(IOException::class, ParseException::class)
    fun <T> downloadStringCached(
        url: String?,
        cacheName: String?,
        parseCallback: ParseCallback<T?>
    ): T? {
        val cacheDestination = File(Tools.DIR_CACHE, "string_cache/" + cacheName)
        if (cacheDestination.isFile() &&
            cacheDestination.canRead() && System.currentTimeMillis() < (cacheDestination.lastModified() + 86400000)
        ) {
            try {
                val cachedString = Tools.read(FileInputStream(cacheDestination))
                return parseCallback.process(cachedString)
            } catch (e: IOException) {
                Log.i("DownloadUtils", "Failed to read the cached file", e)
            } catch (e: ParseException) {
                Log.i("DownloadUtils", "Failed to parse the cached file", e)
            }
        }
        val urlContent = downloadString(url)
        // if we download the file and fail parsing it, we will yeet outta there
        // and not cache the unparseable sting. We will return this after trying to save the downloaded
        // string into cache
        val parseResult = parseCallback.process(urlContent)

        val tryWriteCache: Boolean
        if (cacheDestination.exists()) {
            tryWriteCache = cacheDestination.canWrite()
        } else {
            tryWriteCache = FileUtils.ensureParentDirectorySilently(cacheDestination)
        }

        if (tryWriteCache) try {
            Tools.write(cacheDestination, urlContent)
        } catch (e: IOException) {
            Log.i("DownloadUtils", "Failed to cache the string", e)
        }
        return parseResult
    }

    @Throws(IOException::class)
    private fun <T> downloadFile(downloadFunction: Callable<T?>): T? {
        try {
            return downloadFunction.call()
        } catch (e: IOException) {
            throw e
        } catch (e: Exception) {
            throw RuntimeException(e)
        }
    }

    @Throws(IOException::class)
    private fun verifyFile(file: File, sha1: String): Boolean {
        return file.exists() && HashUtils.compareSHA1(file, sha1)
    }

    @Throws(IOException::class)
    fun <T> ensureSha1(outputFile: File, sha1: String?, downloadFunction: Callable<T?>): T? {
        // Skip if needed
        if (sha1 == null) {
            // If the file exists and we don't know it's SHA1, don't try to redownload it.
            if (outputFile.exists()) return null
            else return downloadFile<T?>(downloadFunction)
        }

        var attempts = 0
        var fileOkay = verifyFile(outputFile, sha1)
        val result: T? = null
        while (attempts < 5 && !fileOkay) {
            attempts++
            downloadFile<T?>(downloadFunction)
            fileOkay = verifyFile(outputFile, sha1)
        }
        if (!fileOkay) throw SHA1VerificationException("SHA1 verifcation failed after 5 download attempts")
        return result
    }

    fun interface ParseCallback<T> {
        @Throws(ParseException::class)
        fun process(input: String?): T?
    }

    class ParseException(e: Exception?) : Exception(e)

    class SHA1VerificationException(message: String?) : IOException(message)
}

