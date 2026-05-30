package net.kdt.pojavlaunch.ui.screens

import android.content.Context
import android.util.Log
import androidx.compose.runtime.*
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.kdt.pojavlaunch.instances.Instance
import net.kdt.pojavlaunch.instances.Instances.Companion.loadAllInstances
import net.kdt.pojavlaunch.instances.Instances.Companion.loadSelectedInstance
import net.kdt.pojavlaunch.instances.Instances.Companion.removeInstance
import net.kdt.pojavlaunch.instances.Instances.Companion.setSelectedInstance
import java.io.IOException
import java.util.Locale

class ProfileSelectionViewModel : ViewModel() {
    var fullList by mutableStateOf<List<Instance>>(emptyList())
    var filteredList by mutableStateOf<List<Instance>>(emptyList())
    var selectedInstancePathName by mutableStateOf("")
    var searchQuery by mutableStateOf("")

    var showReleases by mutableStateOf(true)
    var showSnapshots by mutableStateOf(true)
    var showModded by mutableStateOf(true)

    var isLoading by mutableStateOf(false)

    fun loadProfiles() {
        isLoading = true
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val instances = loadAllInstances().filterNotNull()
                val selected = loadSelectedInstance()
                
                withContext(Dispatchers.Main) {
                    fullList = instances
                    selectedInstancePathName = selected?.mInstanceRoot?.name ?: ""
                    filter()
                    isLoading = false
                }
            } catch (e: IOException) {
                withContext(Dispatchers.Main) {
                    isLoading = false
                    // Handle error if needed, maybe a toast via a flow
                }
            }
        }
    }

    fun updateSearchQuery(query: String) {
        searchQuery = query
        filter()
    }

    fun updateFilters(releases: Boolean, snapshots: Boolean, modded: Boolean) {
        showReleases = releases
        showSnapshots = snapshots
        showModded = modded
        filter()
    }

    private fun filter() {
        val lowerQuery = searchQuery.lowercase(Locale.getDefault())
        filteredList = fullList.filter { instance ->
            val matchesSearch = searchQuery.isEmpty() ||
                    (instance.name?.lowercase(Locale.getDefault())?.contains(lowerQuery) == true) ||
                    (instance.versionId?.lowercase(Locale.getDefault())?.contains(lowerQuery) == true)

            if (!matchesSearch) return@filter false

            val isSnapshot = instance.versionId?.let { 
                it.contains("w") || it.contains("pre") || it.contains("rc") 
            } ?: false
            val isRelease = instance.versionId?.let { 
                !isSnapshot && (it != Instance.VERSION_LATEST_RELEASE) && (it != Instance.VERSION_LATEST_SNAPSHOT)
            } ?: false
            val isModded = instance.installer != null

            var shouldShow = false
            if (isModded && showModded) shouldShow = true
            else if (isSnapshot && showSnapshots) shouldShow = true
            else if (isRelease && showReleases) shouldShow = true
            else if (instance.versionId == Instance.VERSION_LATEST_RELEASE && showReleases) shouldShow = true
            else if (instance.versionId == Instance.VERSION_LATEST_SNAPSHOT && showSnapshots) shouldShow = true
            else if (!isModded && !isSnapshot && !isRelease) {
                shouldShow = showReleases || showSnapshots || showModded
            }
            
            shouldShow
        }
    }

    fun selectInstance(instance: Instance) {
        instance.mInstanceRoot?.name?.let {
            selectedInstancePathName = it
            setSelectedInstance(instance)
        }
    }

    fun deleteInstance(instance: Instance, onDone: () -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                removeInstance(instance)
                withContext(Dispatchers.Main) {
                    loadProfiles()
                    onDone()
                }
            } catch (e: IOException) {
                // Handle error
            }
        }
    }
}
