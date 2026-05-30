package net.kdt.pojavlaunch.plugins

import android.content.Context
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import java.io.File
import java.util.Locale

class LibraryPlugin private constructor(
    @JvmField val packageName: String?,
    @JvmField val libraryPath: String?,
    metaData: Bundle?,
    val label: String?
) {
    @JvmField
    val env: MutableMap<String?, String?> = HashMap<String?, String?>()
    val dlopen: MutableList<String?> = ArrayList<String?>()
    var description: String? = null
        private set
    var rendererId: String? = null
        private set
    var glName: String? = null
        private set
    var eglName: String? = null
        private set

    init {
        if (metaData != null) {
            this.description = metaData.getString("des")


            // Parse renderer metadata: "id:glName:eglName"
            val rendererString = metaData.getString("renderer")
            if (rendererString != null) {
                val parts = rendererString.split(":".toRegex()).dropLastWhile { it.isEmpty() }
                    .toTypedArray()
                if (parts.size > 0) this.rendererId = parts[0]
                if (parts.size > 1) this.glName = parts[1]
                if (parts.size > 2) {
                    this.eglName = parts[2]
                }
            }

            // Parse environment and special keys
            val pojavEnv = metaData.getString("pojavEnv")
            if (pojavEnv != null) {
                for (entry in pojavEnv.split(":".toRegex()).dropLastWhile { it.isEmpty() }
                    .toTypedArray()) {
                    val kv =
                        entry.split("=".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
                    if (kv.size == 2) {
                        val key = kv[0].trim { it <= ' ' }
                        val value = kv[1].trim { it <= ' ' }


                        // Security/Stability Filter: Don't let plugins override core bridge variables
                        val upperKey = key.uppercase(Locale.getDefault())
                        if (upperKey == "POJAVEXEC_EGL" || upperKey == "LIBGL_EGL" || upperKey == "LIBGL_GLES") {
                            Log.w(
                                TAG,
                                "Plugin " + packageName + " tried to override core variable: " + key + ". Ignored."
                            )
                            continue
                        }

                        when (upperKey) {
                            "POJAV_RENDERER" -> this.rendererId = value
                            "DLOPEN" -> for (lib in value.split(",".toRegex())
                                .dropLastWhile { it.isEmpty() }.toTypedArray()) {
                                dlopen.add(lib.trim { it <= ' ' })
                            }

                            "LIB_MESA_NAME", "MESA_LIBRARY" -> env.put(
                                key,
                                libraryPath + "/" + value
                            )

                            else -> env.put(key, value)
                        }
                    }
                }
            }
        }
        Log.d(
            TAG,
            "Initialized plugin: " + packageName + " (ID: " + rendererId + ", GL: " + glName + ")"
        )
    }

    fun resolveAbsolutePath(library: String?): String? {
        if (library == null) return null
        if (library.startsWith("/")) return library
        if (libraryPath == null) return library
        return File(libraryPath, library).getAbsolutePath()
    }

    fun checkLibraries(vararg libs: String): Boolean {
        if (libraryPath == null) return false
        for (lib in libs) {
            if (!(File(libraryPath, lib).exists())) return false
        }
        return true
    }

    companion object {
        private const val TAG = "LibraryPlugin"

        const val ID_ANGLE_PLUGIN: String = "git.mojo.angle"
        const val ID_FFMPEG_PLUGIN: String = "git.mojo.ffmpeg"
        const val ID_HOLY_RENDERER_PLUGIN: String = "io.github.shirosakimio.fclrendererplugin"
        const val ID_GLUES_RENDERER_PLUGIN: String = "com.mobilegl.glues"

        fun discoverPlugin(ctx: Context, appId: String): LibraryPlugin? {
            try {
                val pm = ctx.getPackageManager()
                val pluginPackage = pm.getPackageInfo(
                    appId,
                    PackageManager.GET_SHARED_LIBRARY_FILES or PackageManager.GET_META_DATA
                )
                val appInfo = pluginPackage.applicationInfo
                return LibraryPlugin(
                    appId,
                    appInfo!!.nativeLibraryDir,
                    appInfo.metaData,
                    appInfo.loadLabel(pm).toString()
                )
            } catch (e: Exception) {
                return null
            }
        }

        @JvmStatic
        fun discoverAllPlugins(ctx: Context): MutableList<LibraryPlugin> {
            val plugins: MutableList<LibraryPlugin> = ArrayList<LibraryPlugin>()
            val pm = ctx.getPackageManager()


            // Use known IDs to bypass visibility issues if possible
            val knownIds = arrayOf<String?>(
                ID_HOLY_RENDERER_PLUGIN,
                ID_GLUES_RENDERER_PLUGIN,
                ID_ANGLE_PLUGIN,
                ID_FFMPEG_PLUGIN,
                "com.mio.plugin.renderer.gl4esplus",
                "com.fcl.plugin.mobileglues",
                "com.mio.plugin.renderer.ltw"
            )
            for (id in knownIds) {
                val p: LibraryPlugin? = Companion.discoverPlugin(ctx, id!!)
                if (p != null) plugins.add(p)
            }

            try {
                val apps = pm.getInstalledApplications(PackageManager.GET_META_DATA)
                if (apps != null) {
                    for (app in apps) {
                        // Check if already added via known IDs
                        var exists = false
                        for (p in plugins) {
                            if (p.packageName == app.packageName) {
                                exists = true
                                break
                            }
                        }
                        if (exists) continue

                        if (app.metaData != null) {
                            val isFcl = app.metaData.getBoolean("fclPlugin", false)
                            val isZalith = app.metaData.getBoolean("zalithRendererPlugin", false)
                            if (isFcl || isZalith) {
                                plugins.add(
                                    LibraryPlugin(
                                        app.packageName,
                                        app.nativeLibraryDir,
                                        app.metaData,
                                        app.loadLabel(pm).toString()
                                    )
                                )
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Broad plugin scan failed: " + e.message)
            }

            return plugins
        }
    }
}
