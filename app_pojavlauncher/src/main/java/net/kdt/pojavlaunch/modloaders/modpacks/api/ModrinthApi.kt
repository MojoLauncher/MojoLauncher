package net.kdt.pojavlaunch.modloaders.modpacks.api

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.kdt.mcgui.ProgressLayout
import net.ashmeet.hyperlauncher.R
import net.kdt.pojavlaunch.Tools
import net.kdt.pojavlaunch.downloader.Downloader
import net.kdt.pojavlaunch.downloader.TaskMetadata
import net.kdt.pojavlaunch.mirrors.DownloadMirror
import net.kdt.pojavlaunch.modloaders.modpacks.api.ModpackInstaller.InstallFunction
import net.kdt.pojavlaunch.modloaders.modpacks.models.Constants
import net.kdt.pojavlaunch.modloaders.modpacks.models.ModDetail
import net.kdt.pojavlaunch.modloaders.modpacks.models.ModItem
import net.kdt.pojavlaunch.modloaders.modpacks.models.ModrinthIndex
import net.kdt.pojavlaunch.modloaders.modpacks.models.ModrinthIndex.ModrinthIndexFile
import net.kdt.pojavlaunch.modloaders.modpacks.models.SearchFilters
import net.kdt.pojavlaunch.modloaders.modpacks.models.SearchResult
import net.kdt.pojavlaunch.utils.FileUtils
import net.kdt.pojavlaunch.utils.ZipUtils
import java.io.File
import java.io.IOException
import java.net.URL
import java.util.zip.ZipFile

class ModrinthApi : ModpackApi {
    private val mApiHandler: ApiHandler

    init {
        mApiHandler = ApiHandler("https://api.modrinth.com/v2")
    }

    override fun searchMod(
        searchFilters: SearchFilters?,
        previousPageResult: SearchResult?
    ): SearchResult? {
        if (searchFilters == null) return null
        var modrinthSearchResult = previousPageResult as ModrinthSearchResult?

        // Fixes an issue where the offset being equal or greater than total_hits is ignored
        if (modrinthSearchResult != null && modrinthSearchResult.previousOffset >= modrinthSearchResult.totalResultCount) {
            val emptyResult = ModrinthSearchResult()
            emptyResult.results = arrayOfNulls<ModItem>(0)
            emptyResult.totalResultCount = modrinthSearchResult.totalResultCount
            emptyResult.previousOffset = modrinthSearchResult.previousOffset
            return emptyResult
        }


        // Build the facets filters
        val params = HashMap<String?, Any?>()
        val facetString = StringBuilder()
        facetString.append("[")
        facetString.append(
            String.format(
                "[\"project_type:%s\"]",
                if (searchFilters.isModpack) "modpack" else "mod"
            )
        )
        if (searchFilters.mcVersion != null && !searchFilters.mcVersion!!.isEmpty()) facetString.append(
            String.format(",[\"versions:%s\"]", searchFilters.mcVersion)
        )
        facetString.append("]")
        params.put("facets", facetString.toString())
        params.put("query", searchFilters.name)
        params.put("limit", 50)
        params.put("index", "relevance")
        if (modrinthSearchResult != null) params.put("offset", modrinthSearchResult.previousOffset)

        val response = mApiHandler.get<JsonObject>("search", params, JsonObject::class.java)
        if (response == null) return null
        val responseHits = response.getAsJsonArray("hits")
        if (responseHits == null) return null

        val items = arrayOfNulls<ModItem>(responseHits.size())
        for (i in 0..<responseHits.size()) {
            val hit = responseHits.get(i).getAsJsonObject()
            items[i] = ModItem(
                Constants.SOURCE_MODRINTH,
                hit.get("project_type").getAsString() == "modpack",
                hit.get("project_id").getAsString(),
                hit.get("title").getAsString(),
                hit.get("description").getAsString(),
                hit.get("icon_url").getAsString()
            )
        }
        if (modrinthSearchResult == null) modrinthSearchResult = ModrinthSearchResult()
        modrinthSearchResult.previousOffset += responseHits.size()
        modrinthSearchResult.results = items
        modrinthSearchResult.totalResultCount = response.get("total_hits").getAsInt()
        return modrinthSearchResult
    }

    override fun getModDetails(item: ModItem?): ModDetail? {
        if (item == null) return null
        val response = mApiHandler.get<JsonArray>(
            String.format("project/%s/version", item.id),
            JsonArray::class.java
        )
        if (response == null) return null
        println(response)
        val names = arrayOfNulls<String>(response.size())
        val mcNames = arrayOfNulls<String>(response.size())
        val urls = arrayOfNulls<String>(response.size())
        val hashes = arrayOfNulls<String>(response.size())

        for (i in 0..<response.size()) {
            val version = response.get(i).getAsJsonObject()
            names[i] = version.get("name").getAsString()
            mcNames[i] = version.get("game_versions").getAsJsonArray().get(0).getAsString()
            urls[i] = version.get("files").getAsJsonArray().get(0).getAsJsonObject().get("url")
                .getAsString()
            // Assume there may not be hashes, in case the API changes
            val hashesMap = version.getAsJsonArray("files").get(0).getAsJsonObject()
                .get("hashes").getAsJsonObject()
            if (hashesMap == null || hashesMap.get("sha1") == null) {
                hashes[i] = null
                continue
            }

            hashes[i] = hashesMap.get("sha1").getAsString()
        }

        return ModDetail(item, names, mcNames, urls, hashes)
    }

    @Throws(IOException::class)
    override fun installModpack(modDetail: ModDetail?, selectedVersion: Int): ModLoader? {
        if (modDetail == null) return null
        //TODO considering only modpacks for now
        return ModpackInstaller.downloadModpack(
            modDetail,
            selectedVersion,
            InstallFunction { mrpackFile: File, instanceDestination: File ->
                this.installMrpack(
                    mrpackFile,
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
        if (modpackName == null || modpackFile == null) return null
        return ModpackInstaller.installModpack(
            modpackName,
            modpackName,
            modpackFile,
            icon,
            InstallFunction { mrpackFile: File, instanceDestination: File ->
                this.installMrpack(
                    mrpackFile,
                    instanceDestination
                )
            })
    }

    @Throws(IOException::class)
    private fun installMrpack(mrpackFile: File, instanceDestination: File): ModLoader? {
        ZipFile(mrpackFile).use { modpackZipFile ->
            val modrinthIndex = Tools.GLOBAL_GSON.fromJson<ModrinthIndex>(
                Tools.read(ZipUtils.getEntryStream(modpackZipFile, "modrinth.index.json")),
                ModrinthIndex::class.java
            )
            val files = modrinthIndex.files ?: return null
            try {
                ModrinthDownloader().startDownloads(files, instanceDestination)
            } catch (e: InterruptedException) {
                throw IOException("NIY: InterruptedException", e)
            }
            ProgressLayout.setProgress(
                ProgressLayout.INSTALL_MODPACK,
                0,
                R.string.modpack_download_applying_overrides,
                1,
                2
            )
            ZipUtils.zipExtract(modpackZipFile, "overrides/", instanceDestination)
            ProgressLayout.setProgress(
                ProgressLayout.INSTALL_MODPACK,
                50,
                R.string.modpack_download_applying_overrides,
                2,
                2
            )
            ZipUtils.zipExtract(modpackZipFile, "client-overrides/", instanceDestination)
            return createInfo(modrinthIndex)
        }
    }

    internal inner class ModrinthSearchResult : SearchResult() {
        var previousOffset: Int = 0
    }

    internal class ModrinthDownloader : Downloader(ProgressLayout.INSTALL_MODPACK) {
        @Throws(IOException::class, InterruptedException::class)
        fun startDownloads(indexFiles: Array<ModrinthIndexFile?>, instanceDestination: File) {
            val absoluteInstancePath = instanceDestination.getAbsolutePath()
            val taskMetadatas = ArrayList<TaskMetadata>(indexFiles.size)
            for (file in indexFiles) {
                if (file == null) continue
                val path = file.path ?: continue
                val downloads = file.downloads ?: continue
                if (downloads.isEmpty()) continue
                val hashes = file.hashes
                val sha1 = hashes?.sha1
                val targetPath = File(instanceDestination, path)
                if (!targetPath.getAbsolutePath()
                        .startsWith(absoluteInstancePath)
                ) throw IOException("Bad path!")
                FileUtils.ensureParentDirectory(targetPath)
                taskMetadatas.add(
                    TaskMetadata(
                        targetPath, URL(downloads[0]),  // TODO source selection
                        file.fileSize.toLong(), sha1,
                        DownloadMirror.DOWNLOAD_CLASS_NONE
                    )
                )
            }
            runDownloads(taskMetadatas)
        }
    }

    companion object {
        private fun createInfo(modrinthIndex: ModrinthIndex?): ModLoader? {
            if (modrinthIndex == null) return null
            val dependencies = modrinthIndex.dependencies ?: return null
            val mcVersion = dependencies.get("minecraft")
            if (mcVersion == null) return null
            val forge = dependencies.get("forge")
            if (forge != null) {
                return ModLoader(ModLoader.Companion.MOD_LOADER_FORGE, forge, mcVersion)
            }
            val fabric = dependencies.get("fabric-loader")
            if (fabric != null) {
                return ModLoader(ModLoader.Companion.MOD_LOADER_FABRIC, fabric, mcVersion)
            }
            val quilt = dependencies.get("quilt-loader")
            if (quilt != null) {
                return ModLoader(ModLoader.Companion.MOD_LOADER_QUILT, quilt, mcVersion)
            }
            val neoforge = dependencies.get("neoforge")
            if (neoforge != null) {
                return ModLoader(
                    ModLoader.Companion.MOD_LOADER_NEOFORGE,
                    neoforge,
                    mcVersion
                )
            }

            return null
        }
    }
}
