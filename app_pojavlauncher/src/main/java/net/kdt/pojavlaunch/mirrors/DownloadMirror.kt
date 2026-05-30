package net.kdt.pojavlaunch.mirrors

import net.kdt.pojavlaunch.prefs.LauncherPreferences
import net.kdt.pojavlaunch.utils.DownloadUtils
import java.io.File
import java.io.IOException
import java.net.MalformedURLException

object DownloadMirror {
    @JvmField
    val DOWNLOAD_CLASS_NONE: Int = -1
    const val DOWNLOAD_CLASS_LIBRARIES: Int = 0
    const val DOWNLOAD_CLASS_METADATA: Int = 1
    const val DOWNLOAD_CLASS_ASSETS: Int = 2

    private const val URL_PROTOCOL_TAIL = "://"
    private val MIRROR_BMCLAPI = arrayOf<String?>(
        "https://bmclapi2.bangbang93.com/maven",
        "https://bmclapi2.bangbang93.com",
        "https://bmclapi2.bangbang93.com/assets"
    )

    /**
     * Download a file with the current mirror (or no mirror)
     * @param downloadClass Class of the download. Can either be DOWNLOAD_CLASS_LIBRARIES,
     * DOWNLOAD_CLASS_METADATA or DOWNLOAD_CLASS_ASSETS
     * @param urlInput The original (Mojang) URL for the download
     * @param outputFile The output file for the download
     */
    @JvmStatic
    @Throws(IOException::class)
    fun downloadFileMirrored(downloadClass: Int, urlInput: String, outputFile: File?) {
        if (outputFile == null) return
        DownloadUtils.downloadFile(
            getMirrorMapping(downloadClass, urlInput),
            outputFile
        )
    }

    @JvmStatic
    val isMirrored: Boolean
        /**
         * Check if the current download source is a mirror and not an official source.
         * @return true if the source is a mirror, false otherwise
         */
        get() = LauncherPreferences.PREF_DOWNLOAD_SOURCE != "default"

    private val mirrorSettings: Array<String?>?
        get() {
            when (LauncherPreferences.PREF_DOWNLOAD_SOURCE) {
                "bmclapi" -> return MIRROR_BMCLAPI
                "default" -> return null
                else -> return null
            }
        }

    //TODO make use of this
    /**
     * Get the transformed URL for downloading a file through a mirror.
     * @param downloadClass the download class (one of the constants above)
     * @param mojangUrl the original URL
     * @return the transformed URL
     * @throws MalformedURLException if the URL isn't formatted correctly
     */
    @Throws(MalformedURLException::class)
    fun getMirrorMapping(downloadClass: Int, mojangUrl: String): String? {
        if (downloadClass == DOWNLOAD_CLASS_NONE) return mojangUrl
        val mirrorSettings: Array<String?>? = mirrorSettings
        if (mirrorSettings == null) return mojangUrl
        val urlTail = getBaseUrlTail(mojangUrl)
        var baseUrl: String? = mojangUrl.substring(0, urlTail)
        val path = mojangUrl.substring(urlTail)
        when (downloadClass) {
            DOWNLOAD_CLASS_ASSETS, DOWNLOAD_CLASS_METADATA -> baseUrl =
                mirrorSettings[downloadClass]

            DOWNLOAD_CLASS_LIBRARIES -> {
                if (baseUrl!!.endsWith("libraries.minecraft.net")) {
                    baseUrl = mirrorSettings[downloadClass]
                }
            }
        }
        return baseUrl + path
    }

    @Throws(MalformedURLException::class)
    private fun getBaseUrlTail(wholeUrl: String): Int {
        var protocolNameEnd = wholeUrl.indexOf(URL_PROTOCOL_TAIL)
        if (protocolNameEnd == -1) throw MalformedURLException("No protocol, or non path-based URL")
        protocolNameEnd += URL_PROTOCOL_TAIL.length
        var hostnameEnd = wholeUrl.indexOf('/', protocolNameEnd)
        if (protocolNameEnd >= wholeUrl.length || hostnameEnd == protocolNameEnd) throw MalformedURLException(
            "No hostname"
        )
        if (hostnameEnd == -1) hostnameEnd = wholeUrl.length
        return hostnameEnd
    }
}
