package net.kdt.pojavlaunch.utils

import android.content.Context
import android.system.Os
import android.util.ArrayMap
import android.util.Log
import net.kdt.pojavlaunch.Logger
import net.kdt.pojavlaunch.Tools
import net.kdt.pojavlaunch.extra.ExtraConstants
import net.kdt.pojavlaunch.extra.ExtraCore
import net.kdt.pojavlaunch.plugins.LibraryPlugin
import net.kdt.pojavlaunch.plugins.LibraryPlugin.Companion.discoverAllPlugins
import net.kdt.pojavlaunch.prefs.LauncherPreferences
import net.kdt.pojavlaunch.prefs.LauncherPreferences.PREF_DUMP_SHADERS
import net.kdt.pojavlaunch.prefs.LauncherPreferences.PREF_VSYNC_IN_ZINK
import java.io.BufferedReader
import java.io.File
import java.io.FileReader
import java.io.IOException
import java.nio.ByteBuffer
import java.util.Locale

object JREUtils {
    private const val TAG = "JREUtils"
    @JvmStatic
    fun redirectAndPrintJRELog() {
        Log.v("jrelog", "Log starts here")
        Thread(object : Runnable {
            var failTime: Int = 0
            var logcatPb: ProcessBuilder? = null
            override fun run() {
                try {
                    if (logcatPb == null) {
                        // No filtering by tag anymore as that relied on incorrect log levels set in log.h
                        logcatPb = ProcessBuilder().command(
                            "logcat",  /* "-G", "1mb", */
                            "-v",
                            "brief",
                            "-s",
                            "jrelog",
                            "LIBGL",
                            "NativeInput"
                        ).redirectErrorStream(true)
                    }

                    Log.i("jrelog-logcat", "Clearing logcat")
                    ProcessBuilder().command("logcat", "-c").redirectErrorStream(true).start()
                    Log.i("jrelog-logcat", "Starting logcat")
                    val p = logcatPb!!.start()

                    val buf = ByteArray(1024)
                    var len: Int
                    while ((p.getInputStream().read(buf).also { len = it }) != -1) {
                        val currStr = String(buf, 0, len)
                        Logger.appendToLog(currStr)
                    }

                    if (p.waitFor() != 0) {
                        Log.e("jrelog-logcat", "Logcat exited with code " + p.exitValue())
                        failTime++
                        Log.i(
                            "jrelog-logcat",
                            (if (failTime <= 10) "Restarting logcat" else "Too many restart fails") + " (attempt " + failTime + "/10"
                        )
                        if (failTime <= 10) {
                            run()
                        } else {
                            Logger.appendToLog("ERROR: Unable to get more log.")
                        }
                    }
                } catch (e: Throwable) {
                    Log.e("jrelog-logcat", "Exception on logging thread", e)
                    Logger.appendToLog("Exception on logging thread:\n" + Log.getStackTraceString(e))
                }
            }
        }).start()
        Log.i("jrelog-logcat", "Logcat thread started")
    }

    @Throws(IOException::class)
    private fun overrideEnvVars(envMap: MutableMap<String?, String?>) {
        val customEnvFile = File(Tools.DIR_GAME_HOME, "custom_env.txt")
        if (!customEnvFile.exists() || !customEnvFile.isFile()) return
        val reader = BufferedReader(FileReader(customEnvFile))
        var line: String?
        while ((reader.readLine().also { line = it }) != null) {
            // Not use split() as only split first one
            val index = line!!.indexOf("=")
            envMap.put(line.substring(0, index), line.substring(index + 1))
        }
        reader.close()
    }

    // Sets up ANGLE driver environment
    fun setupAngleEnv(ctx: Context?, envMap: MutableMap<String?, String?>) {
        if (!LauncherPreferences.PREF_USE_ANGLE) return

        val eglPath = resolveLibraryPath(
            ctx,
            "libEGL_angle.so",
            "opengles2"
        ) // ANGLE is usually used with GL4ES or directly
        val glesPath = resolveLibraryPath(ctx, "libGLESv2_angle.so", "opengles2")

        if (File(eglPath).exists() && File(glesPath).exists()) {
            Log.d(TAG, "Using ANGLE: EGL=" + eglPath + ", GLES=" + glesPath)
            envMap.put("LIBGL_EGL", eglPath)
            envMap.put("LIBGL_GLES", glesPath)
        } else {
            Log.e("AngleEnvSetup", "ANGLE libraries are not present.")
        }
    }

    fun setupFfmpegEnv(ctx: Context?, envMap: MutableMap<String?, String?>) {
        val ffmpegPath = resolveLibraryPath(ctx, "libffmpeg.so", null)
        if (File(ffmpegPath).exists()) {
            Log.d(TAG, "Using FFMPEG path: " + ffmpegPath)
            envMap.put("POJAV_FFMPEG_PATH", ffmpegPath)
        }
    }

    @Throws(Throwable::class)
    fun setEnviroimentForGame(context: Context, renderer: String) {
        Log.d(TAG, "Setting environment for game. Selected renderer: " + renderer)
        val envMap: MutableMap<String?, String?> = ArrayMap<String?, String?>()
        if (LauncherPreferences.PREF_ENABLE_MIPMAP) envMap.put("LIBGL_MIPMAP", "3")

        // Prevent OptiFine (and other error-reporting stuff in Minecraft) from balooning the log
        if (LauncherPreferences.PREF_DISABLE_ERROR_CHECK) envMap.put("LIBGL_NOERROR", "1")

        // On certain GLES drivers, overloading default functions shader hack fails, so disable it
        envMap.put("LIBGL_NOINTOVLHACK", "1")

        // Fix white color on banner and sheep, since GL4ES 1.1.5
        envMap.put("LIBGL_NORMALIZE", "1")

        if (PREF_DUMP_SHADERS) envMap.put("LIBGL_VGPU_DUMP", "1")
        if (PREF_VSYNC_IN_ZINK) envMap.put("POJAV_VSYNC_IN_ZINK", "1")
        if (Tools.deviceHasHangingLinker()) envMap.put("POJAV_EMUI_ITERATOR_MITIGATE", "1")


        // The OPEN GL version is changed according
        envMap.put("LIBGL_ES", ExtraCore.getValue(ExtraConstants.OPEN_GL_VERSION) as String?)

        // HACK: GLSL version override for Mesa-based renderers (i.e. Zink)
        // Required to run the game properly on some mobile Vulkan drivers (Minecraft fails to compile shaders without)
        envMap.put("MESA_GLSL_VERSION_OVERRIDE", "460")

        envMap.put("FORCE_VSYNC", LauncherPreferences.PREF_FORCE_VSYNC.toString())

        envMap.put("MESA_GLSL_CACHE_DIR", Tools.DIR_CACHE!!.getAbsolutePath())
        envMap.put("force_glsl_extensions_warn", "true")
        envMap.put("allow_higher_compat_version", "true")
        envMap.put("allow_glsl_extension_directive_midshader", "true")
        // This is currently required for YSM mod to function
        val modRuntimeDir = File(Tools.DIR_CACHE, "app_runtime_mod")
        if (!modRuntimeDir.exists()) {
            modRuntimeDir.mkdirs()
        }
        envMap.put("MOD_ANDROID_RUNTIME", modRuntimeDir.getAbsolutePath())

        if (renderer != "opengles2") { // Don't enable ANGLE for GL4ES for now (it's currently broken)
            setupAngleEnv(context, envMap)
        }
        setupFfmpegEnv(context, envMap)

        envMap.put("MOJO_RENDERER", renderer)

        // Core display bridge provider logic
        if (renderer == "opengles3_ltw") {
            val ltwPath = resolveLibraryPath(context, "libltw.so", renderer)
            Log.d(TAG, "LTW selected, path: " + ltwPath)
            envMap.put("POJAVEXEC_EGL", ltwPath)
        }

        if (LauncherPreferences.PREF_BIG_CORE_AFFINITY) envMap.put("POJAV_BIG_CORE_AFFINITY", "1")

        if (GLInfoUtils.glInfo?.isAdreno == true && !LauncherPreferences.PREF_ZINK_PREFER_SYSTEM_DRIVER) {
            envMap.put("POJAV_LOAD_TURNIP", "1")
        }


        // Krypton Wrapper defaults
        if (renderer == "krypton") {
            envMap.put("LIBGL_USE_MC_COLOR", "1")
            envMap.put("LIBGL_GL", "31")
            envMap.put("LIBGL_ES", "3")
            envMap.put("LIBGL_NORMALIZE", "1")
            envMap.put("LIBGL_NOERROR", "1")
        }

        // Apply dynamic plugin environment variables
        applyPluginEnv(context, renderer, envMap)

        overrideEnvVars(envMap)

        // Final Security Guard: Aggressively remove bridge variables for non-bridge renderers
        // We use an iterator to avoid ConcurrentModificationException and match case-insensitively
        if (renderer != "opengles3_ltw") {
            val it = envMap.keys.iterator()
            while (it.hasNext()) {
                val key = it.next()!!.uppercase(Locale.getDefault())
                if (key == "POJAVEXEC_EGL" || key == "LIBGL_EGL" || key == "LIBGL_GLES") {
                    Log.d(TAG, "Force removing core bridge variable from env: " + key)
                    it.remove()
                }
            }
        }

        for (env in envMap.entries) {
            Logger.appendToLog("Added custom env: " + env.key + "=" + env.value)
            try {
                Os.setenv(env.key, env.value, true)
            } catch (exception: NullPointerException) {
                Log.e("JREUtils", exception.toString())
            }
        }
    }

    private fun applyPluginEnv(
        context: Context,
        rendererId: String?,
        envMap: MutableMap<String?, String?>
    ) {
        val plugin = findPluginForRenderer(context, rendererId)
        if (plugin != null) {
            Log.d(TAG, "Applying extra environment from plugin: " + plugin.packageName)
            envMap.putAll(plugin.env)


            // Re-apply absolute path fix for known library keys that might have been overwritten
            val libKeys = arrayOf<String?>("MESA_LIBRARY", "LIB_MESA_NAME", "POJAV_FFMPEG_PATH")
            for (key in libKeys) {
                val `val` = envMap.get(key)
                if (`val` != null && !`val`.startsWith("/")) {
                    envMap.put(key, plugin.resolveAbsolutePath(`val`))
                }
            }

            // Only use plugin EGL for LTW-like wrappers that explicitly support the Pojav bridge
            if (plugin.eglName != null && plugin.eglName!!.lowercase(Locale.getDefault())
                    .contains("ltw")
            ) {
                val eglPath = plugin.resolveAbsolutePath(plugin.eglName)
                if (File(eglPath).exists()) {
                    Log.d(TAG, "Setting plugin EGL (Wrapper mode): " + eglPath)
                    envMap.put("POJAVEXEC_EGL", eglPath)
                }
            }
        }
    }

    private fun findPluginForRenderer(context: Context, rendererId: String?): LibraryPlugin? {
        val plugins = discoverAllPlugins(context)
        for (plugin in plugins) {
            if (rendererId != null && rendererId == plugin.rendererId) {
                return plugin
            }
            // Fallback: check if the default lib name matches
            val libName = getRendererLibName(rendererId)
            if (libName != null && plugin.checkLibraries(libName)) {
                return plugin
            }
        }
        return null
    }

    private fun getRendererLibName(rendererId: String?): String? {
        if (rendererId == null) return null
        when (rendererId) {
            "holy-gl4es" -> return "libholygl4es.so"
            "holy-virgl" -> return "libholyvirgl.so"
            "mobile-glues" -> return "libmobileglues.so"
            "krypton" -> return "libng_gl4es.so"
            "gallium" -> return "libOSMesa_8.so"
            "opengles3_ltw" -> return "libltw.so"
            else -> return null
        }
    }

    /**
     * Open the render library in accordance to the settings.
     * It will fallback if it fails to load the library.
     * @return The name of the loaded library
     */
    fun loadGraphicsLibrary(context: Context, renderer: String): String? {
        Log.d(TAG, "Loading graphics library for renderer: " + renderer)
        val plugin = findPluginForRenderer(context, renderer)


        // Update linker path to include plugin native libs
        var ldPath = Tools.NATIVE_LIB_DIR
        if (plugin != null) {
            ldPath = plugin.libraryPath + ":" + ldPath
        }
        Log.d(TAG, "Setting LD_LIBRARY_PATH: " + ldPath)
        setLdLibraryPath(ldPath)

        if (plugin != null) {
            Log.d(TAG, "Loading supplemental libraries for plugin: " + plugin.packageName)


            // Pre-load libc++_shared.so if present in plugin or app
            val cppShared = resolveLibraryPath(context, "libc++_shared.so", renderer)
            if (File(cppShared).exists()) {
                Log.d(TAG, "Pre-loading libc++_shared.so from: " + cppShared)
                dlopen(cppShared)
            }

            for (lib in plugin.dlopen) {
                val fullPath = plugin.resolveAbsolutePath(lib)
                if (File(fullPath).exists()) {
                    Log.d(TAG, "dlopen supplemental: " + fullPath)
                    dlopen(fullPath)
                } else {
                    Log.w(TAG, "dlopen supplemental (fallback): " + lib)
                    dlopen(lib)
                }
            }
        }

        val renderLibrary: String?
        when (renderer) {
            "opengles2", "opengles2_5", "opengles3" -> renderLibrary = "libgl4es_114.so"
            "vulkan_zink" -> renderLibrary = "libOSMesa.so"
            "opengles3_ltw" -> renderLibrary = resolveLibraryPath(context, "libltw.so", renderer)
            "holy-gl4es" -> renderLibrary = resolveLibraryPath(context, "libholygl4es.so", renderer)
            "holy-virgl" -> renderLibrary = resolveLibraryPath(context, "libholyvirgl.so", renderer)
            "mobile-glues" -> renderLibrary =
                resolveLibraryPath(context, "libmobileglues.so", renderer)

            "krypton" -> renderLibrary = resolveLibraryPath(context, "libng_gl4es.so", renderer)
            "gallium" -> renderLibrary = resolveLibraryPath(context, "libOSMesa_8.so", renderer)
            else -> {
                Log.w("RENDER_LIBRARY", "No renderer selected, defaulting to opengles2")
                renderLibrary = "libgl4es_114.so"
            }
        }

        Log.d(TAG, "Attempting to dlopen main library: " + renderLibrary)
        if (!dlopen(renderLibrary)) {
            Log.e("RENDER_LIBRARY", "Failed to load renderer " + renderLibrary)
            return null
        }
        Log.d(TAG, "Main library loaded successfully.")
        return renderLibrary
    }

    private fun resolveLibraryPath(
        context: Context?,
        libraryName: String,
        targetRenderer: String?
    ): String {
        // If a plugin for the current renderer exists, prioritize its library path
        if (context != null && targetRenderer != null) {
            val plugin = findPluginForRenderer(context, targetRenderer)
            if (plugin != null) {
                // If the plugin specifies a custom GL library name in metadata
                if (libraryName == plugin.glName) {
                    val path: String = plugin.resolveAbsolutePath(libraryName)!!
                    if (File(path).exists()) return path
                }


                // Check if the requested library exists in this plugin's folder
                val pluginLibPath: String = plugin.resolveAbsolutePath(libraryName)!!
                if (File(pluginLibPath).exists()) {
                    Log.d(
                        TAG,
                        "Resolved " + libraryName + " from current renderer plugin: " + pluginLibPath
                    )
                    return pluginLibPath
                }
            }
        }

        // Try bundled second
        val bundled = File(Tools.NATIVE_LIB_DIR, libraryName)
        if (bundled.exists()) {
            return bundled.getAbsolutePath()
        }


        // Try other discovered plugins as last resort
        if (context != null) {
            val plugins = discoverAllPlugins(context)
            for (plugin in plugins) {
                val path: String = plugin.resolveAbsolutePath(libraryName)!!
                if (File(path).exists()) return path
            }
        }

        return libraryName // Fallback
    }

    val detectedVersion: Int
        get() = GLInfoUtils.glInfo?.glesMajorVersion ?: 0

    /**
     * Splits a string into a list of arguments, filtering out empty ones.
     * @param argStr the string to split
     * @return a list of arguments
     */
    @JvmStatic
    fun parseJavaArguments(argStr: String?): MutableList<String?> {
        if (argStr == null || argStr.isEmpty()) return ArrayList<String?>()
        val strList: MutableList<String?> = ArrayList<String?>()
        for (arg in argStr.split(" ".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()) {
            if (!arg.isEmpty()) {
                strList.add(arg)
            }
        }
        return strList
    }

    external fun chdir(path: String?): Int
    external fun dlopen(libPath: String?): Boolean
    external fun setLdLibraryPath(ldLibraryPath: String?)
    @JvmStatic
    external fun setupBridgeWindow(surface: Any?)
    @JvmStatic
    external fun releaseBridgeWindow()
    @JvmStatic
    external fun applyWindowSize()
    external fun initializeHooks()

    // Obtain AWT screen pixels to render on Android SurfaceView
    @JvmStatic
    external fun renderAWTScreenFrame(tempBuffer: ByteBuffer?): Boolean

    init {
        try {
            System.loadLibrary("exithook")
            System.loadLibrary("pojavexec")
            System.loadLibrary("pojavexec_awt")
        } catch (t: Throwable) {
            Log.e(TAG, "Failed to load core native libraries", t)
        }
    }
}
