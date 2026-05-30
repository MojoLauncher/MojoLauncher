package net.kdt.pojavlaunch.ui.screens

import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import androidx.compose.runtime.*
import androidx.lifecycle.ViewModel
import net.ashmeet.hyperlauncher.R
import net.kdt.pojavlaunch.Tools
import net.kdt.pojavlaunch.instances.Instance
import net.kdt.pojavlaunch.instances.InstanceIconProvider
import net.kdt.pojavlaunch.instances.Instances.Companion.loadSelectedInstance
import net.kdt.pojavlaunch.multirt.MultiRTUtils
import net.kdt.pojavlaunch.multirt.Runtime
import java.io.IOException

class InstanceEditorViewModel : ViewModel() {
    var instance by mutableStateOf<Instance?>(null)
    var instanceIcon by mutableStateOf<Drawable?>(null)
    
    var name by mutableStateOf("")
    var versionId by mutableStateOf("")
    var controlLayout by mutableStateOf("")
    var jvmArgs by mutableStateOf("")
    var sharedData by mutableStateOf(false)
    
    var availableRuntimes by mutableStateOf<List<Runtime>>(emptyList())
    var selectedRuntime by mutableStateOf<Runtime?>(null)
    
    var rendererIds by mutableStateOf<List<String?>>(emptyList())
    var rendererDisplayNames by mutableStateOf<List<String>>(emptyList())
    var selectedRendererIndex by mutableIntStateOf(0)

    fun init(context: Context) {
        val selectedInstance = loadSelectedInstance() ?: return
        instance = selectedInstance
        instanceIcon = InstanceIconProvider.fetchIcon(context.resources, selectedInstance)
        
        name = selectedInstance.name ?: ""
        versionId = selectedInstance.versionId ?: ""
        controlLayout = selectedInstance.controlLayout ?: ""
        jvmArgs = selectedInstance.jvmArgs ?: ""
        sharedData = selectedInstance.sharedData

        // Runtimes
        val runtimes = MultiRTUtils.runtimes
        availableRuntimes = runtimes
        selectedRuntime = runtimes.find { it.name == selectedInstance.selectedRuntime } ?: runtimes.lastOrNull()

        // Renderers: Use the full list from resources to match SettingsScreen
        val allRendererIds = context.resources.getStringArray(R.array.renderer_values).toList()
        val allRendererNames = context.resources.getStringArray(R.array.renderer).toList()
        
        rendererIds = allRendererIds
        rendererDisplayNames = allRendererNames + context.getString(R.string.global_default)
        
        val rIndex = rendererIds.indexOf(selectedInstance.launchRenderer)
        selectedRendererIndex = if (rIndex == -1) rendererDisplayNames.size - 1 else rIndex
    }

    fun save() {
        val inst = instance ?: return
        inst.name = name.ifEmpty { null }
        inst.versionId = versionId.ifEmpty { "latest_release" }
        inst.controlLayout = controlLayout.ifEmpty { null }
        inst.jvmArgs = jvmArgs.ifEmpty { null }
        inst.sharedData = sharedData
        
        inst.selectedRuntime = if (selectedRuntime?.name == "<Default>" || selectedRuntime?.versionString == null) null else selectedRuntime?.name
        
        inst.renderer = if (selectedRendererIndex >= rendererIds.size) null else rendererIds[selectedRendererIndex]

        try {
            inst.write()
        } catch (e: IOException) {
            Tools.showErrorRemote(e)
        }
    }

    fun updateIcon(bitmap: Bitmap) {
        try {
            instance?.encodeNewIcon(bitmap)
        } catch (e: IOException) {
            Tools.showErrorRemote(e)
        }
    }
}
