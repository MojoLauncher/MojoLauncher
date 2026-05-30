package net.kdt.pojavlaunch.modloaders

import android.util.Log
import androidx.annotation.Keep
import com.google.gson.JsonParseException
import net.kdt.pojavlaunch.Tools
import net.kdt.pojavlaunch.utils.DownloadUtils
import net.kdt.pojavlaunch.utils.DownloadUtils.ParseCallback
import java.io.IOException

object BTAUtils {
    private const val BASE_DOWNLOADS_URL = "https://downloads.betterthanadventure.net/bta-client/"
    private const val CLIENT_JAR_URL = "${BASE_DOWNLOADS_URL}%s/%s/client.jar"
    private const val ICON_URL = "${BASE_DOWNLOADS_URL}%s/%s/auto/%s.png"
    private const val MANIFEST_URL = "${BASE_DOWNLOADS_URL}%s/versions.json"
    private const val BUILD_TYPE_RELEASE = "release"
    private const val BUILD_TYPE_NIGHTLY = "nightly"
    private val BTA_TESTED_VERSIONS = mutableListOf<String?>()

    init {
        BTA_TESTED_VERSIONS.add("v7.3")
        BTA_TESTED_VERSIONS.add("v7.2_01")
        BTA_TESTED_VERSIONS.add("v7.2")
        BTA_TESTED_VERSIONS.add("v7.1_01")
        BTA_TESTED_VERSIONS.add("v7.1")
    }

    private fun getIconUrl(version: String, buildType: String): String {
        var iconName = version.replace('.', '_')
        if (buildType == "nightly") iconName = "v$iconName"
        return String.format(ICON_URL, buildType, version, iconName)
    }

    private fun getClientJarUrl(version: String?, buildType: String): String {
        return String.format(CLIENT_JAR_URL, buildType, version)
    }

    private fun getManifestUrl(buildType: String): String {
        return String.format(MANIFEST_URL, buildType)
    }

    @Throws(DownloadUtils.ParseException::class, IOException::class)
    private fun <T> getManifest(buildType: String, parser: ParseCallback<T?>): T? {
        val manifestUrl = getManifestUrl(buildType)
        return DownloadUtils.downloadStringCached(manifestUrl, "bta_$manifestUrl", parser)
    }

    private fun createVersionList(
        versionStrings: MutableList<String?>,
        buildType: String
    ): MutableList<BTAVersion?> {
        val iterator = versionStrings.listIterator(versionStrings.size)
        val btaVersions = ArrayList<BTAVersion?>(versionStrings.size)
        // The original list is guaranteed to be in ascending order - the earliest versions
        // are at the top, but for user convenience we need to put the newest versions at the top,
        // so the BTAVersion list is made from the reverse of the string list.
        while (iterator.hasPrevious()) {
            val version = iterator.previous() ?: continue
            btaVersions.add(
                BTAVersion(
                    version,
                    getClientJarUrl(version, buildType),
                    getIconUrl(version, buildType)
                )
            )
        }
        btaVersions.trimToSize()
        return btaVersions
    }

    @Throws(JsonParseException::class)
    private fun processNightliesJson(nightliesInfo: String?): MutableList<BTAVersion?> {
        val manifest: BTAVersionsManifest = Tools.GLOBAL_GSON.fromJson(
            nightliesInfo,
            BTAVersionsManifest::class.java
        )
        return createVersionList(manifest.versions!!, BUILD_TYPE_NIGHTLY)
    }

    @Throws(JsonParseException::class)
    private fun processReleasesJson(releasesInfo: String?): BTAVersionList {
        val manifest: BTAVersionsManifest = Tools.GLOBAL_GSON.fromJson(
            releasesInfo,
            BTAVersionsManifest::class.java
        )
        val stringVersions = manifest.versions!!
        val testedVersions = mutableListOf<String?>()
        val untestedVersions = mutableListOf<String?>()
        for (version in stringVersions) {
            if (version == null) break
            // Checking for presence in testing array here to avoid accidentally adding nonexistent
            // versions if some of them end up getting removed.
            if (BTA_TESTED_VERSIONS.contains(version)) {
                testedVersions.add(version)
            } else {
                untestedVersions.add(version)
            }
        }

        return BTAVersionList(
            createVersionList(testedVersions, BUILD_TYPE_RELEASE),
            createVersionList(untestedVersions, BUILD_TYPE_RELEASE),
            null
        )
    }

    @Throws(IOException::class)
    fun downloadVersionList(): BTAVersionList? {
        try {
            val releases = getManifest(
                BUILD_TYPE_RELEASE,
                { processReleasesJson(it) })!!
            val nightlies = getManifest(
                BUILD_TYPE_NIGHTLY,
                { processNightliesJson(it) })
            return BTAVersionList(releases.testedVersions, releases.untestedVersions, nightlies)
        } catch (e: DownloadUtils.ParseException) {
            Log.e("BTAUtils", "Failed to process json", e)
            return null
        }
    }

    private class BTAVersionsManifest {
        @Keep
        var versions: MutableList<String?>? = null
    }

    class BTAVersion(val versionName: String?, val downloadUrl: String?, val iconUrl: String?)
    class BTAVersionList(
        val testedVersions: MutableList<BTAVersion?>?,
        val untestedVersions: MutableList<BTAVersion?>?,
        val nightlyVersions: MutableList<BTAVersion?>?
    )
}
