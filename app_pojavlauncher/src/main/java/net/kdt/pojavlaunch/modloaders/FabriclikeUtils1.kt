package net.kdt.pojavlaunch.modloaders

import android.util.Log
import com.google.gson.JsonSyntaxException
import net.kdt.pojavlaunch.Tools
import net.kdt.pojavlaunch.utils.DownloadUtils
import net.kdt.pojavlaunch.utils.DownloadUtils.ParseCallback
import net.kdt.pojavlaunch.utils.FileUtils
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.io.File
import java.io.IOException
import java.io.UnsupportedEncodingException
import java.net.URLEncoder

class FabriclikeUtils private constructor(
    private val mApiUrl: String,
    private val mCachePrefix: String?,
    val name: String?,
    val iconName: String?
) {
    @Throws(IOException::class)
    fun downloadGameVersions(): Array<FabricVersion?>? {
        try {
            return DownloadUtils.downloadStringCached<Array<FabricVersion?>?>(
                String.format(GAME_METADATA_URL, mApiUrl), mCachePrefix + "_game_versions",
                ParseCallback { jsonArrayIn: String? -> deserializeRawVersions(jsonArrayIn) }
            )
        } catch (ignored: DownloadUtils.ParseException) {
        }
        return null
    }

    @Throws(IOException::class)
    fun downloadLoaderVersions(gameVersion: String?): Array<FabricVersion?>? {
        try {
            val urlEncodedGameVersion = URLEncoder.encode(gameVersion, "UTF-8")
            return DownloadUtils.downloadStringCached<Array<FabricVersion?>?>(
                String.format(LOADER_METADATA_URL, mApiUrl, urlEncodedGameVersion),
                mCachePrefix + "_loader_versions." + urlEncodedGameVersion,
                ParseCallback { input: String? ->
                    try {
                        return@ParseCallback deserializeLoaderVersions(input)
                    } catch (e: JSONException) {
                        throw DownloadUtils.ParseException(e)
                    }
                })
        } catch (e: DownloadUtils.ParseException) {
            e.printStackTrace()
        }
        return null
    }

    fun createJsonDownloadUrl(gameVersion: String?, loaderVersion: String?): String {
        var gVer = gameVersion
        var lVer = loaderVersion
        try {
            gVer = URLEncoder.encode(gVer, "UTF-8")
            lVer = URLEncoder.encode(lVer, "UTF-8")
        } catch (e: UnsupportedEncodingException) {
            throw RuntimeException(e)
        }
        return String.format(JSON_DOWNLOAD_URL, mApiUrl, gVer, lVer)
    }

    @Throws(IOException::class)
    fun install(gameVersion: String?, loaderVersion: String?): String? {
        val fabricJson =
            DownloadUtils.downloadString(createJsonDownloadUrl(gameVersion, loaderVersion))
        val versionId: String?
        val versionJson: String
        try {
            val fabricJsonObject = JSONObject(fabricJson)
            versionId = fabricJsonObject.getString("id")
            normalizeClientMainClass(fabricJsonObject)
            versionJson = fabricJsonObject.toString()
        } catch (e: JSONException) {
            e.printStackTrace()
            return null
        }
        val versionJsonDir = File(Tools.DIR_HOME_VERSION, versionId)
        val versionJsonFile = File(versionJsonDir, versionId + ".json")
        FileUtils.ensureDirectory(versionJsonDir)
        Tools.write(versionJsonFile, versionJson)
        return versionId
    }

    @Throws(JSONException::class)
    private fun normalizeClientMainClass(jsonObject: JSONObject) {
        val currentMainClass = jsonObject.optString("mainClass", "")
        val lowerApiUrl = mApiUrl.lowercase()
        val isFabricFamily = lowerApiUrl.contains("fabric")
        val isQuilt = lowerApiUrl.contains("quilt")

        val fallbackMainClass = when {
            isQuilt -> "org.quiltmc.loader.impl.launch.knot.KnotClient"
            isFabricFamily -> "net.fabricmc.loader.impl.launch.knot.KnotClient"
            else -> null
        } ?: return

        val shouldReplace = currentMainClass.isBlank()
                || currentMainClass.endsWith("KnotServer")
                || currentMainClass == "net.minecraft.client.main.Main"
        if (!shouldReplace) return

        Log.w(
            "FabriclikeUtils",
            "Replacing suspicious mainClass \"$currentMainClass\" with \"$fallbackMainClass\""
        )
        jsonObject.put("mainClass", fallbackMainClass)
    }

    companion object {
        @JvmField
        val FABRIC_UTILS: FabriclikeUtils =
            FabriclikeUtils("https://meta.fabricmc.net/v2", "fabric", "Fabric", "fabric")
        @JvmField
        val QUILT_UTILS: FabriclikeUtils =
            FabriclikeUtils("https://meta.quiltmc.org/v3", "quilt", "Quilt", "quilt")
        @JvmField
        val LEGACY_FABRIC_UTILS: FabriclikeUtils = FabriclikeUtils(
            "https://meta.legacyfabric.net/v2",
            "legacy_fabric",
            "Legacy Fabric",
            "fabric"
        )
        private const val LOADER_METADATA_URL = "%s/versions/loader/%s"
        private const val GAME_METADATA_URL = "%s/versions/game"

        private const val JSON_DOWNLOAD_URL = "%s/versions/loader/%s/%s/profile/json"

        @Throws(JSONException::class)
        private fun deserializeLoaderVersions(input: String?): Array<FabricVersion?> {
            val jsonArray = JSONArray(input)
            val fabricVersions = arrayOfNulls<FabricVersion>(jsonArray.length())
            for (i in 0..<jsonArray.length()) {
                val jsonObject = jsonArray.getJSONObject(i).getJSONObject("loader")
                val fabricVersion = FabricVersion()
                fabricVersion.version = jsonObject.getString("version")
                //Quilt has a skill issue and does not say which versions are stable or not
                if (jsonObject.has("stable")) {
                    fabricVersion.stable = jsonObject.getBoolean("stable")
                } else {
                    fabricVersion.stable = fabricVersion.version?.contains("beta") != true
                }
                fabricVersions[i] = fabricVersion
            }
            return fabricVersions
        }

        @Throws(DownloadUtils.ParseException::class)
        private fun deserializeRawVersions(jsonArrayIn: String?): Array<FabricVersion?>? {
            try {
                return Tools.GLOBAL_GSON.fromJson<Array<FabricVersion?>?>(
                    jsonArrayIn,
                    Array<FabricVersion>::class.java
                )
            } catch (e: JsonSyntaxException) {
                e.printStackTrace()
                throw DownloadUtils.ParseException(null)
            }
        }
    }
}
