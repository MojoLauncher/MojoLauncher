package net.kdt.pojavlaunch.ui.screens

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import android.util.LruCache
import android.widget.Toast
import androidx.compose.runtime.*
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.kdt.mcgui.ProgressLayout
import kotlinx.coroutines.*
import net.ashmeet.hyperlauncher.R
import net.kdt.pojavlaunch.PojavApplication
import net.kdt.pojavlaunch.Tools
import net.kdt.pojavlaunch.instances.Instances.Companion.loadSelectedInstance
import net.kdt.pojavlaunch.modloaders.modpacks.api.ApiHandler
import net.kdt.pojavlaunch.progresskeeper.ProgressKeeper.submitProgress
import net.kdt.pojavlaunch.services.ProgressService.Companion.startService
import net.kdt.pojavlaunch.utils.DownloadUtils.downloadFile
import net.kdt.pojavlaunch.utils.FileUtils.ensureDirectorySilently
import java.io.File
import java.io.IOException
import java.net.URL
import java.util.*
import java.util.concurrent.atomic.AtomicInteger

class ContentInstallerViewModel : ViewModel() {
    private val mModrinthApi = ApiHandler("https://api.modrinth.com/v2")
    private val mSearchToken = AtomicInteger(0)
    private val mIconCache = LruCache<String?, Bitmap?>(64)

    var projects by mutableStateOf<List<ModrinthProject>>(emptyList())
    var isLoading by mutableStateOf(false)
    var statusText by mutableStateOf("")
    
    var versionFilter by mutableStateOf<String?>(null)
    var loaderFilter by mutableStateOf<String?>(null)
    
    var instanceVersion by mutableStateOf<String?>(null)
    var instanceLoader by mutableStateOf<String?>(null)

    var selectedType by mutableStateOf(ContentInstallerType.MODS)
    var viewingProject by mutableStateOf<ModrinthProject?>(null)
    var projectVersions by mutableStateOf<List<ModrinthVersion>>(emptyList())
    
    // For the MC Version selection step within a project
    var availableProjectMCVersions by mutableStateOf<List<String>>(emptyList())
    var selectedProjectMCVersion by mutableStateOf<String?>(null)

    private var mSearchJob: Job? = null

    fun init(context: Context) {
        val inst = loadSelectedInstance() ?: return
        val iv = inst.versionId ?: return

        val parts = iv.split("-").toTypedArray()
        instanceVersion = null
        instanceLoader = null

        for (i in parts.indices.reversed()) {
            val part = parts[i]
            if (part.matches("\\d+\\.\\d+(\\.\\d+)?".toRegex())) {
                instanceVersion = part
                versionFilter = part
                break
            }
        }

        if (instanceVersion == null && parts.isNotEmpty()) {
            instanceVersion = parts[0]
        }

        val ivLower = iv.lowercase(Locale.getDefault())
        if (ivLower.contains("fabric")) instanceLoader = "fabric"
        else if (ivLower.contains("forge")) instanceLoader = "forge"
        else if (ivLower.contains("quilt")) instanceLoader = "quilt"
        else if (ivLower.contains("neoforge")) instanceLoader = "neoforge"

        versionFilter = instanceVersion
        loaderFilter = instanceLoader
        
        triggerSearch("", selectedType)
    }

    fun triggerSearch(query: String, type: ContentInstallerType) {
        mSearchJob?.cancel()
        selectedType = type
        viewingProject = null
        selectedProjectMCVersion = null
        
        val token = mSearchToken.incrementAndGet()
        isLoading = true
        statusText = "Searching..."

        mSearchJob = viewModelScope.launch(Dispatchers.IO) {
            try {
                val results = searchProjects(query, type)
                // Download icons for results
                val updatedResults = results.map { project ->
                    val icon = if (project.iconUrl != null) {
                        mIconCache.get(project.iconUrl) ?: downloadIcon(project.iconUrl)?.also {
                            mIconCache.put(project.iconUrl, it)
                        }
                    } else null
                    project.copy(iconBitmap = icon)
                }

                withContext(Dispatchers.Main) {
                    if (token != mSearchToken.get()) return@withContext
                    projects = updatedResults
                    isLoading = false
                    statusText = if (results.isEmpty()) "No results" else ""
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    if (token != mSearchToken.get()) return@withContext
                    isLoading = false
                    statusText = "Failed to load"
                }
            }
        }
    }

    private fun searchProjects(query: String, type: ContentInstallerType): List<ModrinthProject> {
        val params = HashMap<String?, Any?>()
        params["query"] = query
        params["limit"] = 50
        params["index"] = "relevance"
        params["facets"] = buildFacets(type)

        val response = mModrinthApi.get("search", params, JsonObject::class.java) ?: return emptyList()
        val hits = response.getAsJsonArray("hits") ?: return emptyList()

        val items = mutableListOf<ModrinthProject>()
        for (i in 0 until hits.size()) {
            val hit = hits.get(i).asJsonObject
            val id = if (hit.has("project_id")) hit.get("project_id").asString else null
            val title = if (hit.has("title")) hit.get("title").asString else "(untitled)"
            val desc = if (hit.has("description")) hit.get("description").asString else ""
            val iconUrl = if (hit.has("icon_url") && !hit.get("icon_url").isJsonNull) hit.get("icon_url").asString else null
            if (id != null) items.add(ModrinthProject(id, title, desc, iconUrl))
        }
        return items
    }

    private fun buildFacets(type: ContentInstallerType): String {
        val sb = StringBuilder("[")
        sb.append(String.format("[\"project_type:%s\"]", type.projectType))
        if (versionFilter != null) sb.append(String.format(",[\"versions:%s\"]", versionFilter))
        if (type == ContentInstallerType.MODS && loaderFilter != null) sb.append(
            String.format(",[\"categories:%s\"]", loaderFilter)
        )
        sb.append("]")
        return sb.toString()
    }

    fun loadVersions(project: ModrinthProject) {
        val token = mSearchToken.incrementAndGet()
        viewingProject = project
        projectVersions = emptyList()
        availableProjectMCVersions = emptyList()
        selectedProjectMCVersion = null
        isLoading = true
        statusText = "Loading versions..."

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val raw = mModrinthApi.get("project/${project.id}/version", JsonArray::class.java)
                val versions = if (raw != null) parseVersions(raw) else emptyList()

                withContext(Dispatchers.Main) {
                    if (token != mSearchToken.get()) return@withContext
                    isLoading = false
                    projectVersions = versions
                    
                    // Extract unique MC versions available for this project
                    availableProjectMCVersions = versions.flatMap { it.gameVersions }.distinct().sortedDescending()
                    
                    statusText = if (versions.isEmpty()) "No downloadable versions found" else ""
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    if (token != mSearchToken.get()) return@withContext
                    isLoading = false
                    statusText = "Failed to load versions"
                }
            }
        }
    }

    private fun parseVersions(versions: JsonArray): List<ModrinthVersion> {
        val items = mutableListOf<ModrinthVersion>()
        for (i in 0 until versions.size()) {
            val v = versions.get(i).asJsonObject ?: continue
            val name = if (v.has("name")) v.get("name").asString else "Version"

            val gameVersions = mutableListOf<String>()
            if (v.has("game_versions") && v.get("game_versions").isJsonArray) {
                val arr = v.getAsJsonArray("game_versions")
                for (j in 0 until arr.size()) gameVersions.add(arr.get(j).asString)
            }

            val loaders = mutableListOf<String>()
            if (v.has("loaders") && v.get("loaders").isJsonArray) {
                val arr = v.getAsJsonArray("loaders")
                for (j in 0 until arr.size()) loaders.add(arr.get(j).asString)
            }

            var url: String? = null
            var filename: String? = null
            if (v.has("files") && v.get("files").isJsonArray) {
                val files = v.getAsJsonArray("files")
                if (files.size() > 0) {
                    val f = files.get(0).asJsonObject
                    if (f != null) {
                        if (f.has("url")) url = f.get("url").asString
                        if (f.has("filename")) filename = f.get("filename").asString
                    }
                }
            }
            if (url != null) {
                items.add(ModrinthVersion(name, url, filename, gameVersions, loaders))
            }
        }
        return items
    }

    fun downloadVersion(context: Context, version: ModrinthVersion, type: ContentInstallerType) {
        val target = File(getTargetDir(context, type), version.filename ?: "download")
        
        Toast.makeText(context, "Downloading in background...", Toast.LENGTH_SHORT).show()
        startService(context)
        submitProgress(ProgressLayout.CONTENT_INSTALL, 0, 0, "Downloading: ${target.name}")

        viewModelScope.launch(Dispatchers.IO) {
            try {
                downloadFile(version.url, target)
                withContext(Dispatchers.Main) {
                    submitProgress(ProgressLayout.CONTENT_INSTALL, -1, -1)
                    Toast.makeText(context, "Saved: ${target.name}", Toast.LENGTH_LONG).show()
                }
            } catch (e: IOException) {
                withContext(Dispatchers.Main) {
                    submitProgress(ProgressLayout.CONTENT_INSTALL, -1, -1)
                    Tools.showError(context, e)
                }
            }
        }
    }

    private fun getTargetDir(context: Context, type: ContentInstallerType): File {
        val instance = loadSelectedInstance() ?: return context.cacheDir
        val base = if (instance.sharedData) File(Tools.DIR_GAME_NEW) else instance.gameDirectory!!
        val dotMc = File(base, ".minecraft")
        val finalBase = if (dotMc.exists() && dotMc.isDirectory) dotMc else base

        val subfolder = when (type) {
            ContentInstallerType.MODS -> "mods"
            ContentInstallerType.SHADERS -> "shaderpacks"
            ContentInstallerType.RESOURCES -> "resourcepacks"
        }
        
        val target = File(finalBase, subfolder)
        ensureDirectorySilently(target)
        return target
    }

    private fun downloadIcon(url: String): Bitmap? {
        return try {
            URL(url).openStream().use { BitmapFactory.decodeStream(it) }
        } catch (e: IOException) {
            null
        }
    }
}
