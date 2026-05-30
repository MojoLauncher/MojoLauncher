package net.kdt.pojavlaunch.modloaders.modpacks.api

import android.util.Log
import com.google.gson.JsonObject
import com.kdt.mcgui.ProgressLayout
import net.kdt.pojavlaunch.Tools
import net.kdt.pojavlaunch.downloader.AcquireableTaskMetadata
import net.kdt.pojavlaunch.downloader.Downloader
import net.kdt.pojavlaunch.mirrors.DownloadMirror
import net.kdt.pojavlaunch.modloaders.modpacks.api.ModpackInstaller.InstallFunction
import net.kdt.pojavlaunch.modloaders.modpacks.models.Constants
import net.kdt.pojavlaunch.modloaders.modpacks.models.CurseManifest
import net.kdt.pojavlaunch.modloaders.modpacks.models.CurseManifest.CurseFile
import net.kdt.pojavlaunch.modloaders.modpacks.models.CurseManifest.CurseMinecraft
import net.kdt.pojavlaunch.modloaders.modpacks.models.CurseManifest.CurseModLoader
import net.kdt.pojavlaunch.modloaders.modpacks.models.ModDetail
import net.kdt.pojavlaunch.modloaders.modpacks.models.ModItem
import net.kdt.pojavlaunch.modloaders.modpacks.models.SearchFilters
import net.kdt.pojavlaunch.modloaders.modpacks.models.SearchResult
import net.kdt.pojavlaunch.utils.FileUtils
import net.kdt.pojavlaunch.utils.GsonJsonUtils
import net.kdt.pojavlaunch.utils.ZipUtils
import java.io.File
import java.io.IOException
import java.net.URL
import java.net.URLDecoder
import java.util.regex.Pattern
import java.util.zip.ZipFile

class CurseforgeApi(apiKey: String?) : ModpackApi {
    private val mApiHandler: ApiHandler

    init {
        mApiHandler = ApiHandler("https://api.curseforge.com/v1", apiKey)
    }

    override fun searchMod(
        searchFilters: SearchFilters?,
        previousPageResult: SearchResult?
    ): SearchResult? {
        if (searchFilters == null) return null
        var curseforgeSearchResult = previousPageResult as CurseforgeSearchResult?

        val params = HashMap<String?, Any?>()
        params.put("gameId", CURSEFORGE_MINECRAFT_GAME_ID)
        params.put(
            "classId",
            if (searchFilters.isModpack) CURSEFORGE_MODPACK_CLASS_ID else CURSEFORGE_MOD_CLASS_ID
        )
        params.put("searchFilter", searchFilters.name)
        params.put("sortField", CURSEFORGE_SORT_RELEVANCY)
        params.put("sortOrder", "desc")
        if (searchFilters.mcVersion != null && !searchFilters.mcVersion!!.isEmpty()) params.put(
            "gameVersion",
            searchFilters.mcVersion
        )
        if (previousPageResult != null) params.put("index", curseforgeSearchResult!!.previousOffset)

        val response = mApiHandler.get<JsonObject>("mods/search", params, JsonObject::class.java)
        if (response == null) return null
        val dataArray = response.getAsJsonArray("data")
        if (dataArray == null) return null
        val paginationInfo = response.getAsJsonObject("pagination")
        val modItemList = ArrayList<ModItem?>(dataArray.size())
        for (i in 0..<dataArray.size()) {
            val dataElement = dataArray.get(i).getAsJsonObject()
            val allowModDistribution = dataElement.get("allowModDistribution")
            // Gson automatically casts null to false, which leans to issues
            // So, only check the distribution flag if it is non-null
            if (!allowModDistribution.isJsonNull() && !allowModDistribution.getAsBoolean()) {
                Log.i(
                    "CurseforgeApi",
                    "Skipping modpack " + dataElement.get("name")
                        .getAsString() + " because curseforge sucks"
                )
                continue
            }
            val modItem = ModItem(
                Constants.SOURCE_CURSEFORGE,
                searchFilters.isModpack,
                dataElement.get("id").getAsString(),
                dataElement.get("name").getAsString(),
                dataElement.get("summary").getAsString(),
                dataElement.getAsJsonObject("logo").get("thumbnailUrl").getAsString()
            )
            modItemList.add(modItem)
        }
        if (curseforgeSearchResult == null) curseforgeSearchResult = CurseforgeSearchResult()
        curseforgeSearchResult.results = modItemList.toTypedArray<ModItem?>()
        curseforgeSearchResult.totalResultCount = paginationInfo.get("totalCount").getAsInt()
        curseforgeSearchResult.previousOffset += dataArray.size()
        return curseforgeSearchResult
    }

    override fun getModDetails(item: ModItem?): ModDetail? {
        if (item == null) return null
        val allModDetails = ArrayList<JsonObject>()
        var index = 0
        while (index != CURSEFORGE_PAGINATION_END_REACHED &&
            index != CURSEFORGE_PAGINATION_ERROR
        ) {
            index = getPaginatedDetails(allModDetails, index, item.id)
        }
        if (index == CURSEFORGE_PAGINATION_ERROR) return null
        val length = allModDetails.size
        val versionNames = arrayOfNulls<String>(length)
        val mcVersionNames = arrayOfNulls<String>(length)
        val versionUrls = arrayOfNulls<String>(length)
        val hashes = arrayOfNulls<String>(length)
        for (i in allModDetails.indices) {
            val modDetail = allModDetails.get(i)
            versionNames[i] = modDetail.get("displayName").getAsString()

            val downloadUrl = modDetail.get("downloadUrl")
            versionUrls[i] = downloadUrl.getAsString()

            val gameVersions = modDetail.getAsJsonArray("gameVersions")
            for (jsonElement in gameVersions) {
                val gameVersion = jsonElement.getAsString()
                if (!sMcVersionPattern.matcher(gameVersion).matches()) {
                    continue
                }
                mcVersionNames[i] = gameVersion
                break
            }

            hashes[i] = getSha1FromModData(modDetail)
        }
        return ModDetail(item, versionNames, mcVersionNames, versionUrls, hashes)
    }

    @Throws(IOException::class)
    override fun installModpack(modDetail: ModDetail?, selectedVersion: Int): ModLoader? {
        if (modDetail == null) return null
        //TODO considering only modpacks for now
        return ModpackInstaller.downloadModpack(
            modDetail,
            selectedVersion,
            InstallFunction { zipFile: File, instanceDestination: File ->
                this.installCurseforgeZip(
                    zipFile,
                    instanceDestination
                )
            })
    }

    @Throws(IOException::class)
    override fun installLocalModpack(
        modpackName: String?,
        modpackFile: File?,
        icon: String?
    ): ModLoader? {
        if (modpackFile == null || modpackName == null) return null
        return ModpackInstaller.installModpack(
            modpackName,
            modpackName,
            modpackFile,
            icon,
            InstallFunction { zipFile: File, instanceDestination: File ->
                this.installCurseforgeZip(
                    zipFile,
                    instanceDestination
                )
            })
    }

    private fun getPaginatedDetails(
        objectList: ArrayList<JsonObject>,
        index: Int,
        modId: String?
    ): Int {
        val params = HashMap<String?, Any?>()
        params.put("index", index)
        params.put("pageSize", CURSEFORGE_PAGINATION_SIZE)

        val response =
            mApiHandler.get<JsonObject>("mods/" + modId + "/files", params, JsonObject::class.java)
        val data = GsonJsonUtils.getJsonArraySafe(response, "data")
        Log.i("CurseforgeApi", "data...")
        if (data == null) return CURSEFORGE_PAGINATION_ERROR
        Log.i("CurseforgeApi", "filtering...")
        for (i in 0..<data.size()) {
            val fileInfo = data.get(i).getAsJsonObject()
            if (fileInfo.get("isServerPack").getAsBoolean()) continue
            objectList.add(fileInfo)
        }
        Log.i("CurseforgeApi", "pag_end")
        if (data.size() < CURSEFORGE_PAGINATION_SIZE) {
            return CURSEFORGE_PAGINATION_END_REACHED // we read the remainder! yay!
        }
        return index + data.size()
    }

    @Throws(IOException::class)
    private fun installCurseforgeZip(zipFile: File, instanceDestination: File): ModLoader? {
        ZipFile(zipFile).use { modpackZipFile ->
            val curseManifest = Tools.GLOBAL_GSON.fromJson<CurseManifest>(
                Tools.read(ZipUtils.getEntryStream(modpackZipFile, "manifest.json")),
                CurseManifest::class.java
            )
            if (!verifyManifest(curseManifest)) {
                Log.i("CurseforgeApi", "manifest verification failed")
                return null
            }
            try {
                CurseDownloader().start(curseManifest, instanceDestination)
            } catch (e: InterruptedException) {
                throw IOException("NIY: InterruptedException", e)
            }
            var overridesDir = "overrides"
            if (curseManifest.overrides != null) overridesDir = curseManifest.overrides!!
            ZipUtils.zipExtract(modpackZipFile, overridesDir, instanceDestination)
            return createInfo(curseManifest.minecraft)
        }
    }

    private fun createInfo(minecraft: CurseMinecraft?): ModLoader? {
        if (minecraft == null) return null
        val modLoaders = minecraft.modLoaders ?: return null
        var primaryModLoader: CurseModLoader? = null
        for (modLoader in modLoaders) {
            if (modLoader != null && modLoader.primary) {
                primaryModLoader = modLoader
                break
            }
        }
        if (primaryModLoader == null) primaryModLoader = modLoaders.firstOrNull()
        val modLoaderId = primaryModLoader?.id ?: return null
        val dashIndex = modLoaderId.indexOf('-')
        val modLoaderName = modLoaderId.substring(0, dashIndex)
        val modLoaderVersion = modLoaderId.substring(dashIndex + 1)
        Log.i("CurseforgeApi", modLoaderId + " " + modLoaderName + " " + modLoaderVersion)
        val modLoaderTypeInt: Int
        when (modLoaderName) {
            "forge" -> modLoaderTypeInt = ModLoader.Companion.MOD_LOADER_FORGE
            "fabric" -> modLoaderTypeInt = ModLoader.Companion.MOD_LOADER_FABRIC
            "neoforge" -> modLoaderTypeInt = ModLoader.Companion.MOD_LOADER_NEOFORGE
            else -> return null
        }
        return ModLoader(modLoaderTypeInt, modLoaderVersion, minecraft.version)
    }

    @Throws(IOException::class)
    private fun getDownloadUrl(fileMetadata: JsonObject): String {
        if (fileMetadata.get("modId").isJsonNull() || fileMetadata.get("id")
                .isJsonNull()
        ) throw IOException("Bad metadata schema!")
        val projectID = fileMetadata.get("modId").getAsLong()
        val fileID = fileMetadata.get("id").getAsLong()

        // First try the official api endpoint
        val response = mApiHandler.get<JsonObject>(
            "mods/" + projectID + "/files/" + fileID + "/download-url",
            JsonObject::class.java
        )
        if (response != null && !response.get("data").isJsonNull()) return response.get("data")
            .getAsString()

        // Otherwise, fallback to building an edge link
        return String.format(
            "https://edge.forgecdn.net/files/%s/%s/%s",
            fileID / 1000,
            fileID % 1000,
            fileMetadata.get("fileName").getAsString()
        )
    }

    @Throws(IOException::class)
    private fun checkRequiredFileFields(fileMetadata: JsonObject) {
        if (fileMetadata.isJsonNull()) throw IOException("File metadata is null!")
        val hasProjectId = fileMetadata.has("modId")
        val hasFileId = fileMetadata.has("id")
        val hasLength = fileMetadata.has("fileLength")
        if (!hasProjectId || !hasFileId || !hasLength) {
            val builder = StringBuilder().append("File metadata is mising the following fields:")
            if (!hasProjectId) builder.append(" modId")
            if (!hasFileId) builder.append(" id")
            if (!hasLength) builder.append(" fileLength")
            throw IOException(builder.toString())
        }
    }

    private fun getFile(projectID: Long, fileID: Long): JsonObject? {
        val response = mApiHandler.get<JsonObject>(
            "mods/" + projectID + "/files/" + fileID,
            JsonObject::class.java
        )
        return GsonJsonUtils.getJsonObjectSafe(response, "data")
    }

    private fun getSha1FromModData(`object`: JsonObject): String? {
        val hashes = GsonJsonUtils.getJsonArraySafe(`object`, "hashes")
        if (hashes == null) return null
        for (jsonElement in hashes) {
            // The sha1 = 1; md5 = 2;
            val jsonObject = GsonJsonUtils.getJsonObjectSafe(jsonElement)
            if (GsonJsonUtils.getIntSafe(
                    jsonObject,
                    "algo",
                    -1
                ) == ALGO_SHA_1
            ) {
                return GsonJsonUtils.getStringSafe(jsonObject, "value")
            }
        }
        return null
    }

    private fun verifyManifest(manifest: CurseManifest): Boolean {
        if ("minecraftModpack" != manifest.manifestType) return false
        if (manifest.manifestVersion != 1) return false
        val minecraft = manifest.minecraft ?: return false
        if (minecraft.version == null) return false
        val modLoaders = minecraft.modLoaders ?: return false
        return modLoaders.isNotEmpty()
    }

    internal class CurseforgeSearchResult : SearchResult() {
        var previousOffset: Int = 0
    }

    internal inner class CurseDownloader : Downloader(ProgressLayout.INSTALL_MODPACK) {
        @Throws(IOException::class, InterruptedException::class)
        fun start(curseManifest: CurseManifest, instanceDestination: File) {
            val files = curseManifest.files ?: return
            val taskMetadatas = ArrayList<AcquireableTaskMetadata>(files.size)
            for (file in files) {
                if (file == null) continue
                taskMetadatas.add(CurseTaskMetadata(file, instanceDestination))
            }
            runDownloads(taskMetadatas)
        }
    }

    internal inner class CurseTaskMetadata(
        private val mFile: CurseFile,
        private val mInstanceDestination: File
    ) : AcquireableTaskMetadata(DownloadMirror.DOWNLOAD_CLASS_METADATA) {
        @Throws(IOException::class)
        override fun acquireMetadata() {
            val fileMetadata = getFile(mFile.projectID, mFile.fileID)
            if (fileMetadata == null) throw IOException("Failed to fetch file metadata")
            checkRequiredFileFields(fileMetadata)
            val urlStr = getDownloadUrl(fileMetadata)
            this.url = URL(urlStr)
            val targetPath = File(
                mInstanceDestination,
                "mods/" + URLDecoder.decode(FileUtils.getFileName(urlStr), "UTF-8")
            )
            this.path = targetPath
            FileUtils.ensureParentDirectorySilently(targetPath)
            this.sha1Hash = getSha1FromModData(fileMetadata)
            this.size = fileMetadata.get("fileLength").getAsLong()
        }
    }

    companion object {
        private val sMcVersionPattern: Pattern = Pattern.compile("([0-9]+)\\.([0-9]+)\\.?([0-9]+)?")
        private const val ALGO_SHA_1 = 1

        // Stolen from
        // https://github.com/AnzhiZhang/CurseForgeModpackDownloader/blob/6cb3f428459f0cc8f444d16e54aea4cd1186fd7b/utils/requester.py#L93
        private const val CURSEFORGE_MINECRAFT_GAME_ID = 432
        private const val CURSEFORGE_MODPACK_CLASS_ID = 4471

        // https://api.curseforge.com/v1/categories?gameId=432 and search for "Mods" (case-sensitive)
        private const val CURSEFORGE_MOD_CLASS_ID = 6
        private const val CURSEFORGE_SORT_RELEVANCY = 1
        private const val CURSEFORGE_PAGINATION_SIZE = 50
        private val CURSEFORGE_PAGINATION_END_REACHED = -1
        private val CURSEFORGE_PAGINATION_ERROR = -2
    }
}
