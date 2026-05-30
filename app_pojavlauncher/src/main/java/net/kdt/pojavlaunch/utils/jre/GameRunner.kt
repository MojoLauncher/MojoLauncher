package net.kdt.pojavlaunch.utils.jre

import android.content.DialogInterface
import android.util.ArrayMap
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import net.ashmeet.hyperlauncher.R
import net.kdt.pojavlaunch.Architecture
import net.kdt.pojavlaunch.JMinecraftVersionList
import net.kdt.pojavlaunch.JMinecraftVersionList.Arguments.ArgValue.ArgRules
import net.kdt.pojavlaunch.Tools
import net.kdt.pojavlaunch.authenticator.accounts.MinecraftAccount
import net.kdt.pojavlaunch.instances.Instance
import net.kdt.pojavlaunch.lifecycle.LifecycleAwareAlertDialog
import net.kdt.pojavlaunch.lifecycle.LifecycleAwareAlertDialog.Companion.haltOnDialog
import net.kdt.pojavlaunch.lifecycle.LifecycleAwareAlertDialog.DialogCreator
import net.kdt.pojavlaunch.multirt.MultiRTUtils.forceReread
import net.kdt.pojavlaunch.multirt.MultiRTUtils.getNearestJreName
import net.kdt.pojavlaunch.multirt.MultiRTUtils.read
import net.kdt.pojavlaunch.prefs.LauncherPreferences
import net.kdt.pojavlaunch.utils.DateUtils
import net.kdt.pojavlaunch.utils.FileUtils
import net.kdt.pojavlaunch.utils.GLInfoUtils
import net.kdt.pojavlaunch.utils.GameOptionsUtils
import net.kdt.pojavlaunch.utils.JREUtils
import net.kdt.pojavlaunch.utils.JSONUtils
import net.kdt.pojavlaunch.utils.MCOptionUtils
import net.kdt.pojavlaunch.utils.OldVersionsUtils
import net.kdt.pojavlaunch.utils.RendererCompatUtil
import org.lwjgl.glfw.CallbackBridge
import java.io.File
import java.io.FileFilter
import java.io.IOException
import java.text.ParseException

object GameRunner {
    /**
     * Optimization mods based on Sodium can mitigate the render distance issue. Check if Sodium
     * or its derivative is currently installed to skip the render distance check.
     * @param gameDir current game directory
     * @return whether sodium or a sodium-based mod is installed
     */
    private fun hasSodium(gameDir: File?): Boolean {
        val modsDir = File(gameDir, "mods")
        val mods = modsDir.listFiles(FileFilter { file: File? ->
            file!!.isFile() && file.getName().endsWith(".jar")
        })
        if (mods == null) return false
        for (file in mods) {
            val name = file.getName()
            if (name.contains("sodium") ||
                name.contains("embeddium") ||
                name.contains("rubidium")
            ) return true
        }
        return false
    }

    /**
     * Check if Angelica is currently installed to allow usage of LTW
     * @param gameDir current game directory
     * @return whether Angelica is installed
     */
    private fun hasAngelica(gameDir: File?): Boolean {
        val modsDir = File(gameDir, "mods")
        val mods = modsDir.listFiles(FileFilter { file: File? ->
            file!!.isFile() && file.getName().endsWith(".jar")
        })
        if (mods == null) return false
        for (file in mods) {
            val name = file.getName()
            if (name.contains("angelica")) return true
        }
        return false
    }

    /**
     * Initialize OpenGL and do checks to see if the GPU of the device is affected by the render
     * distance issue.
     * 
     * Currently only checks whether the user has an Adreno GPU capable of OpenGL ES 3.
     * 
     * This issue is caused by a very severe limit on the amount of GL buffer names that could be allocated
     * by the Adreno properietary GLES driver.
     * 
     * @return whether the GPU is affected by the Large Thin Wrapper render distance issue on vanilla
     */
    private fun affectedByRenderDistanceIssue(): Boolean {
        val info = GLInfoUtils.glInfo ?: return false
        return info.isAdreno && info.glesMajorVersion >= 3
    }

    private fun checkRenderDistance(gamedir: File): Boolean {
        if (!affectedByRenderDistanceIssue()) return false
        if (hasSodium(gamedir)) return false
        try {
            MCOptionUtils.load(gamedir.getAbsolutePath())
        } catch (e: Exception) {
            Log.e("Tools", "Failed to load config", e)
        }
        val renderDistance =
            GameOptionsUtils.parseIntDefault(MCOptionUtils.get("renderDistance"), 12)
        // 7 is the render distance "magic number" above which MC creates too many buffers
        // for Adreno's OpenGL ES implementation
        return renderDistance > 7
    }

    @Throws(Exception::class)
    private fun isGl4esCompatible(version: JMinecraftVersionList.Version): Boolean {
        val releaseDate = DateUtils.getOriginalReleaseDate(version) ?: return false
        return DateUtils.dateBefore(releaseDate, 2025, 1, 7)
    }

    @Throws(Exception::class)
    private fun isCompatContext(version: JMinecraftVersionList.Version): Boolean {
        // Day before the release date of 21w10a, the first OpenGL 3 Core Minecraft version
        val releaseDate = DateUtils.getOriginalReleaseDate(version) ?: return true
        return DateUtils.dateBefore(releaseDate, 2021, 2, 9)
    }

    @Throws(InterruptedException::class)
    private fun showDialog(activity: AppCompatActivity, message: Int): Boolean {
        val dialogCreator = object : DialogCreator {
            override fun createDialog(
                alertDialog: LifecycleAwareAlertDialog?,
                dialogBuilder: AlertDialog.Builder?
            ) {
                dialogBuilder!!.setMessage(activity.getString(message))
                    .setCancelable(false)
                    .setPositiveButton(
                        android.R.string.ok,
                        DialogInterface.OnClickListener { _, _ -> })
            }
        }
        return haltOnDialog(activity.lifecycle, activity, dialogCreator)
    }

    // Autoswitch to LTW if supported, otherwise - crash with resId dialog message. Returns LTW renderer strings if succeeded
    @Throws(InterruptedException::class, IOException::class)
    private fun switchLtw(
        hasLtw: Boolean,
        instance: Instance,
        activity: AppCompatActivity,
        resId: Int
    ): String? {
        if (hasLtw) {
            val ltwRenderer = "opengles3_ltw"
            instance.renderer = ltwRenderer
            instance.write()
            return ltwRenderer
        } else {
            showDialog(activity, resId)
            System.exit(0)
            return null
        }
    }

    @JvmStatic
    @Throws(Throwable::class)
    fun launchMinecraft(
        activity: AppCompatActivity, minecraftAccount: MinecraftAccount,
        instance: Instance, versionId: String?, rendererName: String
    ) {
        var rendererName = rendererName
        var freeDeviceMemory = Tools.getFreeDeviceMemory(activity)
        val localeString: Int
        val freeAddressSpace =
            if (Architecture.is32BitsDevice()) Architecture.addressSpaceLimit.toInt() else -1
        Log.i("MemStat", "Free RAM: " + freeDeviceMemory + " Addressable: " + freeAddressSpace)
        if (freeDeviceMemory > freeAddressSpace && freeAddressSpace != -1) {
            freeDeviceMemory = freeAddressSpace
            localeString = R.string.address_memory_warning_msg
        } else {
            localeString = R.string.memory_warning_msg
        }

        if (LauncherPreferences.PREF_RAM_ALLOCATION > freeDeviceMemory) {
            val finalDeviceMemory = freeDeviceMemory
            val dialogCreator = object : DialogCreator {
                override fun createDialog(
                    dialog: LifecycleAwareAlertDialog?,
                    builder: AlertDialog.Builder?
                ) {
                    builder!!.setMessage(
                        activity.getString(
                            localeString,
                            finalDeviceMemory,
                            LauncherPreferences.PREF_RAM_ALLOCATION
                        )
                    )
                        .setPositiveButton(
                            android.R.string.ok,
                            DialogInterface.OnClickListener { _, _ -> })
                }
            }

            if (haltOnDialog(activity.lifecycle, activity, dialogCreator)) {
                return  // If the dialog's lifecycle has ended, return without
                // actually launching the game, thus giving us the opportunity
                // to start after the activity is shown again
            }
        }
        val gamedir: File = instance.gameDirectory ?: return
        val versionInfo = Tools.getVersionInfo(versionId)

        // Switch renderer to GL4ES when running a compat context version on LTW
        if (isCompatContext(versionInfo) && !hasAngelica(gamedir) && rendererName == "opengles3_ltw") {
            rendererName = "opengles2"
            instance.renderer = rendererName
            instance.write()
        }

        val isGl4es = rendererName == "opengles2"
        val ltwSupported =
            RendererCompatUtil.getCompatibleRenderers(activity).rendererIds.contains("opengles3_ltw")
        // Block Sodium from running with GL4ES on 1.17+
        if (!isCompatContext(versionInfo) && isGl4es && hasSodium(gamedir)) {
            rendererName =
                switchLtw(ltwSupported, instance, activity, R.string.compat_sodium_not_supported)!!
        }

        // Switch renderer to LTW when running 1.21.5
        if (!isGl4esCompatible(versionInfo) && isGl4es) {
            rendererName =
                switchLtw(ltwSupported, instance, activity, R.string.compat_version_not_supported)!!
        }
        RendererCompatUtil.releaseRenderersCache()

        val isLtw = rendererName == "opengles3_ltw"

        if (isLtw && checkRenderDistance(gamedir)) {
            if (showDialog(activity, R.string.ltw_render_distance_warning_msg)) return
            // If the code goes here, it means that the user clicked "OK". Fix the render distance.
            try {
                MCOptionUtils.set("renderDistance", "7")
                MCOptionUtils.save()
            } catch (e: Exception) {
                Log.e("Tools", "Failed to fix render distance setting", e)
            }
        }

        GameOptionsUtils.fixOptions(gamedir.getAbsolutePath(), isLtw, rendererName)

        if (isLtw && (GLInfoUtils.glInfo?.forcedMsaa == true)) {
            if (showDialog(activity, R.string.ltw_4x_msaa_warning_msg)) return
        }

        var requiredJavaVersion = 8
        versionInfo.javaVersion?.let { requiredJavaVersion = it.majorVersion }

        // Minecraft 1.13+
        CallbackBridge.nativeSetUseInputStackQueue(versionInfo.arguments != null)

        val runtime = forceReread(pickRuntime(instance, requiredJavaVersion))

        // Pre-process specific files
        disableSplash(gamedir)
        val launchArgs = getMinecraftClientArgs(minecraftAccount, versionInfo, gamedir)

        // Select the appropriate openGL version
        OldVersionsUtils.selectOpenGlVersion(versionInfo)

        val launchClassPath = generateLaunchClassPath(versionInfo, versionId)

        val javaArgList: MutableList<String> = ArrayList()

        val loggingFile = versionInfo.logging?.client?.file
        if (loggingFile != null) {
            var configFile =
                Tools.DIR_DATA + "/security/" + loggingFile.id?.replace(
                    "client",
                    "log4j-rce-patch"
                ).orEmpty()
            if (!File(configFile).exists()) {
                configFile = Tools.DIR_GAME_NEW + "/" + loggingFile.id
            }
            javaArgList.add("-Dlog4j.configurationFile=" + configFile)
        }

        val versionSpecificNativesDir = File(Tools.DIR_CACHE, "natives/" + versionId)
        if (versionSpecificNativesDir.exists()) {
            val dirPath = versionSpecificNativesDir.getAbsolutePath()
            javaArgList.add("-Djava.library.path=" + dirPath + ":" + Tools.NATIVE_LIB_DIR)
            javaArgList.add("-Djna.boot.library.path=" + dirPath)
        }

        addAuthlibInjectorArgs(javaArgList, minecraftAccount)

        javaArgList.addAll(getMinecraftJVMArgs(versionId).filterNotNull())

        javaArgList.addAll(JREUtils.parseJavaArguments(instance.launchArgs).filterNotNull())

        JREUtils.setEnviroimentForGame(activity, rendererName)
        JREUtils.chdir((instance.gameDirectory ?: return).absolutePath)

        var rendererLibrary = JREUtils.loadGraphicsLibrary(activity, rendererName)
        if (rendererLibrary == null) {
            Log.i("GameRunner", "Falling back to GL4ES 1.1.4")
            rendererName = "opengles2"
            rendererLibrary = JREUtils.loadGraphicsLibrary(activity, rendererName)
        }
        if (rendererLibrary == null) {
            if (showDialog(activity, R.string.gr_err_renderer_load_Failed)) return
            System.exit(0)
        }
        javaArgList.add("-Dorg.lwjgl.opengl.libname=" + rendererLibrary)
        javaArgList.add("-Dorg.lwjgl.freetype.libname=" + Tools.NATIVE_LIB_DIR + "/libfreetype.so")

        activity.runOnUiThread(Runnable {
            Toast.makeText(
                activity,
                activity.getString(
                    R.string.autoram_info_msg,
                    LauncherPreferences.PREF_RAM_ALLOCATION
                ),
                Toast.LENGTH_SHORT
            ).show()
        })

        try {
            JavaRunner.nativeSetupExit(activity)
            JavaRunner.startJvm(
                runtime,
                javaArgList,
                launchClassPath,
                versionInfo.mainClass,
                launchArgs
            )
        } catch (e: VMLoadException) {
            val dialogCreator: DialogCreator =
                object : DialogCreator {
                    override fun createDialog(
                        dialog: LifecycleAwareAlertDialog?,
                        builder: AlertDialog.Builder?
                    ) {
                    builder!!.setMessage(e.toString(activity)).setPositiveButton(
                        android.R.string.ok,
                        DialogInterface.OnClickListener { _, _ -> })
                    }
                }

            if (haltOnDialog(activity.lifecycle, activity, dialogCreator)) {
                return
            }
        }

        Tools.fullyExit()
    }

    private fun disableSplash(dir: File?) {
        val configDir = File(dir, "config")
        if (FileUtils.ensureDirectorySilently(configDir)) {
            val forgeSplashFile = File(dir, "config/splash.properties")
            var forgeSplashContent = "enabled=true"
            try {
                if (forgeSplashFile.exists()) {
                    forgeSplashContent = Tools.read(forgeSplashFile.getAbsolutePath()) ?: forgeSplashContent
                }
                if (forgeSplashContent.contains("enabled=true")) {
                    Tools.write(
                        forgeSplashFile,
                        forgeSplashContent.replace("enabled=true", "enabled=false")
                    )
                }
            } catch (e: IOException) {
                Log.w(Tools.APP_NAME, "Could not disable Forge 1.12.2 and below splash screen!", e)
            }
        } else {
            Log.w(Tools.APP_NAME, "Failed to create the configuration directory")
        }
    }

    private fun addAuthlibInjectorArgs(
        javaArgList: MutableList<String>,
        minecraftAccount: MinecraftAccount
    ) {
        val injectorUrl = minecraftAccount.authType.injectorUrl
        if (injectorUrl == null) return
        javaArgList.add("-javaagent:" + Tools.DIR_DATA + "/authlib-injector/authlib-injector.jar=" + injectorUrl)
    }

    private fun getMinecraftJVMArgs(versionName: String?): MutableList<String?> {
        val versionInfo = Tools.getVersionInfo(versionName, true)
        val arguments = versionInfo.arguments
        // Parse Forge 1.17+ additional JVM Arguments
        if (versionInfo.inheritsFrom == null || arguments == null || arguments.jvm == null) {
            return mutableListOf<String?>()
        }

        val varArgMap: MutableMap<String?, String?> = ArrayMap<String?, String?>()
        varArgMap.put("classpath_separator", ":")
        varArgMap.put("library_directory", Tools.DIR_HOME_LIBRARY)
        varArgMap.put("version_name", versionInfo.id)
        varArgMap.put("natives_directory", Tools.NATIVE_LIB_DIR)

        val minecraftArgs: MutableList<String?> = ArrayList<String?>()
        for (arg in arguments.jvm!!) {
                if (arg is String) {
                    minecraftArgs.add(arg)
                } //TODO: implement (?maybe?)
        }
        return JSONUtils.insertJSONValueList(minecraftArgs, varArgMap)
    }

    private fun getMinecraftClientArgs(
        profile: MinecraftAccount,
        versionInfo: JMinecraftVersionList.Version,
        gameDir: File
    ): MutableList<String?> {
        val username = profile.username
        var versionName = versionInfo.id
        if (versionInfo.inheritsFrom != null) {
            versionName = versionInfo.inheritsFrom
        }

        var userType = "mojang"
        try {
            val creationDate = DateUtils.getOriginalReleaseDate(versionInfo)
            // Minecraft 22w43a which adds chat reporting (and signing) was released on
            // 26th October 2022. So, if the date is not before that (meaning it is equal or higher)
            // change the userType to MSA to fix the missing signature
            if (creationDate != null && !DateUtils.dateBefore(creationDate, 2022, 8, 26)) {
                userType = "msa"
            }
        } catch (e: ParseException) {
            Log.e(
                "CheckForProfileKey",
                "Failed to determine profile creation date, using \"mojang\"",
                e
            )
        }


        val varArgMap: MutableMap<String?, String?> = ArrayMap<String?, String?>()
        varArgMap.put("auth_session", profile.accessToken) // For legacy versions of MC
        varArgMap.put("auth_access_token", profile.accessToken)
        varArgMap.put("auth_player_name", username)
        varArgMap.put("auth_uuid", profile.profileId.replace("-", ""))
        varArgMap.put("auth_xuid", profile.xuid)
        varArgMap.put("assets_root", Tools.ASSETS_PATH)
        varArgMap.put("assets_index_name", versionInfo.assets)
        varArgMap.put("game_assets", Tools.ASSETS_PATH)
        varArgMap.put("game_directory", gameDir.getAbsolutePath())
        varArgMap.put("user_properties", "{}")
        varArgMap.put("user_type", userType)
        varArgMap.put("version_name", versionName)
        varArgMap.put("version_type", versionInfo.type)

        val minecraftArgs: MutableList<String?> = ArrayList<String?>()
        val arguments = versionInfo.arguments
        if (arguments?.game != null) {
            // Support Minecraft 1.13+
            for (arg in arguments.game!!) {
                if (arg is String) {
                    minecraftArgs.add(arg)
                } //TODO: implement else clause
            }
        }
        versionInfo.minecraftArguments?.let {
            minecraftArgs.addAll(splitAndFilterEmpty(it))
        }
        return JSONUtils.insertJSONValueList(minecraftArgs, varArgMap)
    }

    private fun splitAndFilterEmpty(argStr: String): MutableList<String?> {
        val strList: MutableList<String?> = ArrayList<String?>()
        for (arg in argStr.split(" ".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()) {
            if (!arg.isEmpty()) {
                strList.add(arg)
            }
        }
        return strList
    }

    private fun getClientClasspath(version: String?): String {
        return Tools.DIR_HOME_VERSION + "/" + version + "/" + version + ".jar"
    }

    private fun generateLaunchClassPath(
        info: JMinecraftVersionList.Version,
        actualname: String?
    ): MutableList<String?> {
        val lwjgl3Folder = File(Tools.DIR_GAME_HOME, "lwjgl3")
        val glfwFatJar = File(lwjgl3Folder, "lwjgl-glfw-classes.jar")
        val lwjglxJar = File(lwjgl3Folder, "lwjgl-lwjglx.jar")
        if (!glfwFatJar.exists() || !lwjglxJar.exists()) throw RuntimeException("Required LWJGL3 files not found")

        val classpath = ArrayList<String?>((info.libraries?.size ?: 0) + 3)
        // LWJGL3 comes first - must override any custom LWJGL3 on the classpath
        classpath.add(glfwFatJar.getAbsolutePath())
        // Custom version libraries are inbetween
        val usesLWJGL3 = generateLibClasspath(info, classpath)
        // Client is last before LWJGL2 - all libraries must have higher precedence than it.
        classpath.add(getClientClasspath(actualname))
        // Don't add LWJGLX when the client doesn't use LWJGL2
        if (!usesLWJGL3) {
            // LWJGLX (custom LWJGL2) comes last - anything in the client or libs should override it
            classpath.add(lwjglxJar.getAbsolutePath())
        }
        classpath.trimToSize()
        return classpath
    }

    private fun checkRules(rules: Array<ArgRules?>?): Boolean {
        if (rules == null) return true // always allow

        for (rule in rules) {
            if (rule?.action == "allow" && rule.os?.name == "osx") {
                return false //disallow
            }
        }
        return true // allow if none match
    }

    /**
     * "Carve out" the version out of a Maven library name
     * @param fullMavenName the full library name
     * @return the library name without the version
     */
    private fun trimLibVersion(fullMavenName: String): String {
        val first = fullMavenName.indexOf(':')
        if (first == -1) return fullMavenName
        val second = fullMavenName.indexOf(':', first + 1)
        if (second == -1) return fullMavenName
        val third = fullMavenName.indexOf(':', second + 1)
        if (third != -1) {
            return fullMavenName.substring(0, second + 1) + fullMavenName.substring(third)
        } else {
            return fullMavenName.substring(0, second + 1)
        }
    }

    /** @return true when LWJGL3 is in use
     */
    fun generateLibClasspath(
        info: JMinecraftVersionList.Version,
        target: MutableList<String?>
    ): Boolean {
        val libraries = ArrayMap<String?, String?>()
        var usesLWJGL3 = false
        val infoLibraries = info.libraries ?: emptyArray()
        for (libItemNullable in infoLibraries) {
            val libItem = libItemNullable ?: continue
            val libName = libItem.name ?: continue
            if (libName.startsWith("org.lwjgl:lwjgl:3.")) usesLWJGL3 = true
            if (!checkRules(libItem.rules) || Tools.shouldSkipLibrary(libItem)) continue
            val library = File(Tools.DIR_HOME_LIBRARY, Tools.artifactToPath(libItem))
            if (!library.exists()) continue
            val name = trimLibVersion(libName)
            // If the lib list has both asm-all and normal asm, something is either terribly wrong
            // or it's just babric. Let's hope for the latter
            if (name == "org.ow2.asm:asm:") {
                libraries.remove("org.ow2.asm:asm-all:")
            }
            libraries.put(name, library.getAbsolutePath())
        }
        target.addAll(libraries.values)
        return usesLWJGL3
    }

    @JvmStatic
    fun pickRuntime(instance: Instance, targetJavaVersion: Int): String {
        var runtime = Tools.getSelectedRuntime(instance)
        val profileRuntime = instance.selectedRuntime
        val pickedRuntime = read(runtime)
        if (runtime == null || pickedRuntime.javaVersion == 0 || pickedRuntime.javaVersion < targetJavaVersion) {
            val preferredRuntime = getNearestJreName(targetJavaVersion)
            if (preferredRuntime == null) throw RuntimeException("Failed to autopick runtime!")
            if (profileRuntime != null) {
                instance.selectedRuntime = preferredRuntime
                instance.maybeWrite()
            }
            runtime = preferredRuntime
        }
        return runtime
    }
}
