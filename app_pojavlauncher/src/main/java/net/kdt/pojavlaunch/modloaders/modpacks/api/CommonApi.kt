package net.kdt.pojavlaunch.modloaders.modpacks.api

import android.util.Log
import net.kdt.pojavlaunch.PojavApplication
import net.kdt.pojavlaunch.modloaders.modpacks.models.Constants
import net.kdt.pojavlaunch.modloaders.modpacks.models.ModDetail
import net.kdt.pojavlaunch.modloaders.modpacks.models.ModItem
import net.kdt.pojavlaunch.modloaders.modpacks.models.SearchFilters
import net.kdt.pojavlaunch.modloaders.modpacks.models.SearchResult
import java.io.File
import java.io.IOException
import java.util.concurrent.Callable
import java.util.concurrent.Future
import java.util.zip.ZipFile

/**
 * Group all apis under the same umbrella, as another layer of abstraction
 */
class CommonApi(curseforgeApiKey: String?) : ModpackApi {
    private val mCurseforgeApi: ModpackApi?
    private val mModrinthApi: ModpackApi = ModrinthApi()
    private val mModpackApis: Array<ModpackApi>

    init {
        if ("DUMMY" == curseforgeApiKey) {
            mCurseforgeApi = null
            mModpackApis = arrayOf(mModrinthApi)
        } else {
            val curseforgeApi = CurseforgeApi(curseforgeApiKey)
            mCurseforgeApi = curseforgeApi
            mModpackApis = arrayOf(mModrinthApi, curseforgeApi)
        }
    }

    override fun searchMod(
        searchFilters: SearchFilters?,
        previousPageResult: SearchResult?
    ): SearchResult? {
        var commonApiSearchResult = previousPageResult as CommonApiSearchResult?
        // If there are no previous page results, create a new array. Otherwise, use the one from the previous page
        val results = commonApiSearchResult?.searchResults ?: arrayOfNulls(mModpackApis.size)

        val futures = arrayOfNulls<Future<*>>(mModpackApis.size)
        for (i in mModpackApis.indices) {
            // If there is an array and its length is zero, this means that we've exhausted the results for this
            // search query and we don't need to actually do the search
            if (results[i]?.results?.isEmpty() == true) continue
            // If the previous page result is not null (aka the arrays aren't fresh)
            // and the previous result is null, it means that na error has occured on the previous
            // page. We lost contingency anyway, so don't bother requesting.
            if (previousPageResult != null && results[i] == null) continue
            futures[i] = PojavApplication.sExecutorService.submit<SearchResult?>(
                ApiDownloadTask(
                    i, searchFilters,
                    results[i]
                )
            )
        }

        if (Thread.interrupted()) {
            cancelAllFutures(futures)
            return null
        }
        var hasSuccessful = false
        var totalSize = 0
        // Count up all the results
        for (i in mModpackApis.indices) {
            val future = futures[i] ?: continue
            try {
                results[i] = future.get() as SearchResult?
                val searchResult = results[i]
                if (searchResult != null) {
                    hasSuccessful = true
                    totalSize += searchResult.totalResultCount
                }
            } catch (e: Exception) {
                cancelAllFutures(futures)
                e.printStackTrace()
                return null
            }
        }
        if (!hasSuccessful) {
            return null
        }
        // Then build an array with all the mods
        val filteredResults = ArrayList<Array<ModItem?>?>(results.size)

        // Sanitize returned values
        for (result in results) {
            val searchResults = result?.results ?: continue
            // If the length is zero, we don't need to perform needless copies
            if (searchResults.isEmpty()) continue
            filteredResults.add(searchResults)
        }
        filteredResults.trimToSize()
        if (Thread.interrupted()) return null

        val concatenatedItems = buildFusedResponse(filteredResults)
        if (Thread.interrupted()) return null
        // Recycle or create new search result
        if (commonApiSearchResult == null) commonApiSearchResult = CommonApiSearchResult()
        commonApiSearchResult.searchResults = results
        commonApiSearchResult.totalResultCount = totalSize
        commonApiSearchResult.results = concatenatedItems
        return commonApiSearchResult
    }

    override fun getModDetails(item: ModItem?): ModDetail? {
        if (item == null) return null
        Log.i(
            "CommonApi",
            "Invoking getModDetails on item.apiSource=${item.apiSource} item.title=${item.title}"
        )
        return getModpackApi(item.apiSource)?.getModDetails(item)
    }

    @Throws(IOException::class)
    override fun installModpack(modDetail: ModDetail?, selectedVersion: Int): ModLoader? {
        if (modDetail == null) return null
        return getModpackApi(modDetail.apiSource)?.installModpack(modDetail, selectedVersion)
    }

    @Throws(IOException::class)
    override fun installLocalModpack(
        modpackName: String?,
        modpackFile: File?,
        icon: String?
    ): ModLoader? {
        if (modpackFile == null) return null
        val s: Short = checkModpack(modpackFile)
        return when (s) {
            PACK_MODRINTH.toShort() -> mModrinthApi.installLocalModpack(modpackName, modpackFile, icon)
            PACK_CURSEFORGE.toShort() -> mCurseforgeApi?.installLocalModpack(modpackName, modpackFile, icon)
            PACK_UNDEFINED.toShort() -> {
                modpackFile.delete()
                null
            }

            else -> null
        }
    }

    private fun getModpackApi(apiSource: Int): ModpackApi? {
        return when (apiSource) {
            Constants.SOURCE_MODRINTH -> mModrinthApi
            Constants.SOURCE_CURSEFORGE -> mCurseforgeApi

            else -> throw UnsupportedOperationException("Unknown API source: $apiSource")
        }
    }

    /** Fuse the arrays in a way that's fair for every endpoint  */
    private fun buildFusedResponse(modMatrix: MutableList<Array<ModItem?>?>): Array<ModItem?> {
        var totalSize = 0

        // Calculate the total size of the merged array
        for (array in modMatrix) {
            totalSize += array?.size ?: 0
        }

        val fusedItems = arrayOfNulls<ModItem>(totalSize)

        var mergedIndex = 0
        var maxLength = 0

        // Find the maximum length of arrays
        for (array in modMatrix) {
            val size = array?.size ?: 0
            if (size > maxLength) {
                maxLength = size
            }
        }

        // Populate the merged array
        for (i in 0 until maxLength) {
            for (matrix in modMatrix) {
                if (matrix != null && i < matrix.size) {
                    fusedItems[mergedIndex] = matrix[i]
                    mergedIndex++
                }
            }
        }

        return fusedItems
    }

    private fun cancelAllFutures(futures: Array<Future<*>?>) {
        for (future in futures) {
            future?.cancel(true)
        }
    }

    private inner class ApiDownloadTask(
        private val mModApi: Int,
        private val mSearchFilters: SearchFilters?,
        private val mPreviousPageResult: SearchResult?
    ) : Callable<SearchResult?> {
        override fun call(): SearchResult? {
            return mModpackApis[mModApi].searchMod(mSearchFilters, mPreviousPageResult)
        }
    }

    internal inner class CommonApiSearchResult : SearchResult() {
        var searchResults: Array<SearchResult?> = arrayOfNulls(mModpackApis.size)
    }

    companion object {
        const val PACK_MODRINTH: Byte = 1
        const val PACK_CURSEFORGE: Byte = 2
        const val PACK_UNDEFINED: Byte = 0

        fun checkModpack(outFile: File?): Short {
            try {
                ZipFile(outFile).use { zipFile ->
                    val modrinth = zipFile.getEntry("modrinth.index.json")
                    val curseforge = zipFile.getEntry("manifest.json")
                    if (modrinth != null) {
                        return PACK_MODRINTH.toShort()
                    }
                    if (curseforge != null) {
                        return PACK_CURSEFORGE.toShort()
                    }
                    return PACK_UNDEFINED.toShort() // return this if no modpack was detected
                }
            } catch (e: Exception) {
                return -1
            }
        }
    }
}
