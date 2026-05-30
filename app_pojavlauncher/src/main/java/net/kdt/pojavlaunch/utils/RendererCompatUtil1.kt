package net.kdt.pojavlaunch.utils

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Build.VERSION
import android.util.Log
import net.ashmeet.hyperlauncher.R
import net.kdt.pojavlaunch.Architecture
import net.kdt.pojavlaunch.Tools
import net.kdt.pojavlaunch.plugins.LibraryPlugin
import net.kdt.pojavlaunch.plugins.LibraryPlugin.Companion.discoverAllPlugins
import java.io.File

object RendererCompatUtil {
    private const val TAG = "RendererCompatUtil"
    private var sCompatibleRenderers: RenderersList? = null

    fun checkVulkanSupport(packageManager: PackageManager): Boolean {
        if (VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            return packageManager.hasSystemFeature(PackageManager.FEATURE_VULKAN_HARDWARE_LEVEL) &&
                    packageManager.hasSystemFeature(PackageManager.FEATURE_VULKAN_HARDWARE_VERSION)
        }
        return false
    }

    /** Return the renderers that are compatible with this device  */
    @JvmStatic
    fun getCompatibleRenderers(context: Context): RenderersList {
        if (sCompatibleRenderers != null && !sCompatibleRenderers!!.rendererIds.isEmpty()) {
            return sCompatibleRenderers!!
        }

        Log.d(TAG, "Getting compatible renderers...")
        val resources = context.getResources()
        var defaultRenderers: Array<String>?
        var defaultRendererNames: Array<String>?

        try {
            defaultRenderers = resources.getStringArray(R.array.renderer_values)
            defaultRendererNames = resources.getStringArray(R.array.renderer)
            Log.d(TAG, "Loaded " + defaultRenderers.size + " renderers from resources.")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load renderer arrays!", e)
            defaultRenderers = arrayOf<String>("opengles2")
            defaultRendererNames = arrayOf<String>("Holy GL4ES 1.1.4")
        }

        var deviceHasVulkan = false
        try {
            deviceHasVulkan = checkVulkanSupport(context.getPackageManager())
            Log.d(TAG, "Device has Vulkan: " + deviceHasVulkan)
        } catch (e: Exception) {
            Log.e(TAG, "Vulkan check failed", e)
        }

        var deviceHasZinkBinary = true
        try {
            deviceHasZinkBinary = !(Architecture.is32BitsDevice() && Architecture.isx86Device())
            Log.d(TAG, "Device has Zink binary: " + deviceHasZinkBinary)
        } catch (e: Throwable) {
            Log.e(TAG, "Architecture check failed", e)
        }

        var deviceHasOpenGLES3 = false
        try {
            val detectedVersion = JREUtils.detectedVersion
            deviceHasOpenGLES3 = detectedVersion >= 3
            if (detectedVersion == 0) deviceHasOpenGLES3 = true
            Log.d(
                TAG,
                "Device has GLES 3+: " + deviceHasOpenGLES3 + " (detected: " + detectedVersion + ")"
            )
        } catch (e: Throwable) {
            Log.e(TAG, "GLES version check failed", e)
            deviceHasOpenGLES3 = true
        }

        // Base checks (bundled)
        val detectedRendererIds: MutableSet<String?> = HashSet<String?>()
        if (File(Tools.NATIVE_LIB_DIR, "libltw.so").exists()) {
            Log.d(TAG, "Found bundled libltw.so")
            detectedRendererIds.add("opengles3_ltw")
        }
        if (File(Tools.NATIVE_LIB_DIR, "libholygl4es.so").exists()) {
            Log.d(TAG, "Found bundled libholygl4es.so")
            detectedRendererIds.add("holy-gl4es")
        }
        if (File(Tools.NATIVE_LIB_DIR, "libholyvirgl.so").exists()) {
            Log.d(TAG, "Found bundled libholyvirgl.so")
            detectedRendererIds.add("holy-virgl")
        }
        if (File(Tools.NATIVE_LIB_DIR, "libmobileglues.so").exists()) {
            Log.d(TAG, "Found bundled libmobileglues.so")
            detectedRendererIds.add("mobile-glues")
        }
        if (File(Tools.NATIVE_LIB_DIR, "libng_gl4es.so").exists()) {
            Log.d(TAG, "Found bundled libng_gl4es.so")
            detectedRendererIds.add("krypton")
        }
        if (File(Tools.NATIVE_LIB_DIR, "libOSMesa_8.so").exists()) {
            Log.d(TAG, "Found bundled libOSMesa_8.so")
            detectedRendererIds.add("gallium")
        }


        // Dynamic plugin discovery
        var allPlugins: MutableList<LibraryPlugin> = ArrayList<LibraryPlugin>()
        try {
            allPlugins = discoverAllPlugins(context)
        } catch (e: Exception) {
            Log.e(TAG, "Plugin discovery failed", e)
        }

        for (plugin in allPlugins) {
            Log.d(TAG, "Scanning plugin for libraries: " + plugin.packageName)
            // Check for specific renderer ID defined in plugin metadata
            if (plugin.rendererId != null) {
                Log.d(
                    TAG,
                    "Plugin " + plugin.packageName + " declares renderer ID: " + plugin.rendererId
                )
                detectedRendererIds.add(plugin.rendererId)
            }


            // Check for libraries to be sure
            if (plugin.checkLibraries("libltw.so")) detectedRendererIds.add("opengles3_ltw")
            if (plugin.checkLibraries("libholygl4es.so")) detectedRendererIds.add("holy-gl4es")
            if (plugin.checkLibraries("libholyvirgl.so")) detectedRendererIds.add("holy-virgl")
            if (plugin.checkLibraries("libmobileglues.so")) detectedRendererIds.add("mobile-glues")
            if (plugin.checkLibraries("libng_gl4es.so")) detectedRendererIds.add("krypton")
            if (plugin.checkLibraries("libOSMesa_8.so")) detectedRendererIds.add("gallium")
        }

        val rendererIds: MutableList<String?> = ArrayList<String?>()
        val rendererNamesList: MutableList<String?> = ArrayList<String?>()

        for (i in defaultRenderers.indices) {
            val rendererId = defaultRenderers[i]
            val rendererName: String? =
                if (i < defaultRendererNames.size) defaultRendererNames[i] else rendererId


            // Always show opengles2 (GL4ES 1.1.4)
            if (rendererId == "opengles2") {
                rendererIds.add(rendererId)
                rendererNamesList.add(rendererName)
                continue
            }

            if (rendererId.contains("vulkan") && !deviceHasVulkan) {
                Log.v(TAG, "Skipping " + rendererId + " (No Vulkan)")
                continue
            }
            if (rendererId.contains("zink") && !deviceHasZinkBinary) {
                Log.v(TAG, "Skipping " + rendererId + " (No Zink binary)")
                continue
            }


            // Generic check for other renderers
            if (!detectedRendererIds.contains(rendererId)) {
                // Additional version checks for some specific IDs
                if (rendererId == "opengles3_ltw" && !deviceHasOpenGLES3) continue
                if (rendererId == "mobile-glues" && !deviceHasOpenGLES3) continue


                // If not in detected list and not a basic renderer, skip it
                if (rendererId != "vulkan_zink") {
                    Log.v(TAG, "Skipping " + rendererId + " (Not detected and not Vulkan/Zink)")
                    continue
                }
            }

            Log.d(TAG, "Adding compatible renderer: " + rendererId + " (" + rendererName + ")")
            rendererIds.add(rendererId)
            rendererNamesList.add(rendererName)
        }


        // Final fallback if somehow list is still empty
        if (rendererIds.isEmpty()) {
            Log.w(TAG, "Compatible renderer list is empty! Falling back to opengles2.")
            rendererIds.add("opengles2")
            rendererNamesList.add("Holy GL4ES 1.1.4 (Fallback)")
        }

        sCompatibleRenderers = RenderersList(
            rendererIds,
            rendererNamesList.toTypedArray<String?>()
        )

        return sCompatibleRenderers!!
    }

    /** Checks if the renderer Id is compatible with the current device  */
    @JvmStatic
    fun checkRendererCompatible(context: Context, rendererName: String?): Boolean {
        return getCompatibleRenderers(context).rendererIds.contains(rendererName)
    }

    /** Releases the cache of compatible renderers.  */
    fun releaseRenderersCache() {
        sCompatibleRenderers = null
    }

    class RenderersList(
        @JvmField val rendererIds: MutableList<String?>,
        @JvmField val rendererDisplayNames: Array<String?>?
    )
}
