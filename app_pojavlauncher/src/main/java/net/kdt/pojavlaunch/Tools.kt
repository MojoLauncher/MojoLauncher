package net.kdt.pojavlaunch

import android.app.Activity
import android.app.ActivityManager
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.res.AssetManager
import android.graphics.Color
import android.hardware.Sensor
import android.hardware.SensorManager
import android.net.Uri
import android.os.Build
import android.os.Build.VERSION
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.os.Process
import android.provider.DocumentsContract
import android.provider.OpenableColumns
import android.util.DisplayMetrics
import android.util.Log
import android.view.Display
import android.view.View
import android.view.View.OnSystemUiVisibilityChangeListener
import android.view.Window
import android.view.WindowInsets
import android.view.WindowInsetsController
import android.view.WindowManager
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationManagerCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentTransaction
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import net.ashmeet.hyperlauncher.R
import net.kdt.pojavlaunch.ShowErrorActivity.RemoteErrorTask
import net.kdt.pojavlaunch.instances.Instance
import net.kdt.pojavlaunch.lifecycle.ContextExecutor.execute
import net.kdt.pojavlaunch.lifecycle.ContextExecutorTask
import net.kdt.pojavlaunch.multirt.MultiRTUtils
import net.kdt.pojavlaunch.multirt.MultiRTUtils.forceReread
import net.kdt.pojavlaunch.multirt.MultiRTUtils.postPrepare
import net.kdt.pojavlaunch.prefs.LauncherPreferences
import net.kdt.pojavlaunch.utils.FileUtils.ensureParentDirectory
import net.kdt.pojavlaunch.utils.FileUtils.getFileName
import net.kdt.pojavlaunch.utils.GLInfoUtils.glInfo
import net.kdt.pojavlaunch.value.DependentLibrary
import net.kdt.pojavlaunch.value.DependentLibrary.LibraryDownloads
import net.kdt.pojavlaunch.value.MinecraftLibraryArtifact
import org.apache.commons.io.IOUtils
import java.io.BufferedInputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.PrintWriter
import java.io.StringWriter
import java.lang.ref.WeakReference
import java.net.URLConnection
import java.nio.charset.StandardCharsets
import java.util.Objects

object Tools {
    const val MAVEN_CENTRAL: String =
        "https://maven-central-eu.storage-download.googleapis.com/maven2/"
    val BYTE_TO_MB: Float = (1024 * 1024).toFloat()
    val MAIN_HANDLER: Handler = Handler(Looper.getMainLooper())
    var APP_NAME: String = "HyperLauncher"

    @JvmField
    val GLOBAL_GSON: Gson = GsonBuilder().setPrettyPrinting().create()

    const val URL_HOME: String = "https://pojavlauncherteam.github.io"
    var NATIVE_LIB_DIR: String? = null
    @JvmField
    var DIR_DATA: String? = null //Initialized later to get context
    @JvmField
    var DIR_CACHE: File? = null
    var MULTIRT_HOME: String? = null
    var DEVICE_ARCHITECTURE: Int = 0

    // New since 3.3.1
    @JvmField
    var DIR_ACCOUNT_NEW: String? = null
    @JvmField
    var DIR_GAME_HOME: String =
        Environment.getExternalStorageDirectory().absolutePath + "/games/PojavLauncher"
    @JvmField
    var DIR_GAME_NEW: String? = null

    // New since 2.4.2
    var DIR_HOME_VERSION: String? = null
    var DIR_HOME_LIBRARY: String? = null

    var DIR_HOME_CRASH: String? = null

    var ASSETS_PATH: String? = null
    var OBSOLETE_RESOURCES_PATH: String? = null
    @JvmField
    var CTRLMAP_PATH: String? = null
    var CTRLDEF_FILE: String? = null


    private fun getPojavStorageRoot(ctx: Context): File? {
        if (VERSION.SDK_INT >= 29) {
            return ctx.getExternalFilesDir(null)
        }
        val externalStorageDirectory = Environment.getExternalStorageDirectory()
        if (externalStorageDirectory == null) return null
        val launcherRoot = File(externalStorageDirectory, "games/PojavLauncher")
        if (Environment.MEDIA_MOUNTED != Environment.getExternalStorageState(launcherRoot)) return null
        return launcherRoot
    }

    /**
     * Checks if the Pojav's storage root is accessible and read-writable
     * @param context context to get the storage root if it's not set yet
     * @return true if storage is fine, false if storage is not accessible
     */
    fun checkStorageRoot(context: Context): Boolean {
        return getPojavStorageRoot(context) != null
    }

    /**
     * Checks if the Pojav's storage root is accessible and read-writable. If it's not, starts
     * the MissingStorageActivity and finishes the supplied activity.
     * @param context the Activity that checks for storage availability
     * @return whether the storage is available or not.
     */
    fun checkStorageInteractive(context: Activity): Boolean {
        if (!checkStorageRoot(context)) {
            context.startActivity(Intent(context, MissingStorageActivity::class.java))
            context.finish()
            return false
        }
        return true
    }

    /**
     * Initialize context constants most necessary for launcher's early startup phase
     * that are not dependent on user storage.
     * All values that depend on DIR_DATA and are not dependent on DIR_GAME_HOME must
     * be initialized here.
     * @param ctx the context for initialization.
     */
    fun initEarlyConstants(ctx: Context) {
        DIR_CACHE = ctx.cacheDir
        DIR_DATA = ctx.filesDir.parent
        MULTIRT_HOME = DIR_DATA + "/runtimes"
        DIR_ACCOUNT_NEW = DIR_DATA + "/accounts"
        NATIVE_LIB_DIR = ctx.applicationInfo.nativeLibraryDir
    }

    /**
     * Initialize context constants that depend on user storage.
     * Any value (in)directly dependent on DIR_GAME_HOME should be set only here.
     * You ABSOLUTELY MUST check for storage presence using checkStorageRoot() before calling this.
     */
    fun initStorageConstants(ctx: Context) {
        initEarlyConstants(ctx)
        val pojavStorageRoot = getPojavStorageRoot(ctx)
        if (pojavStorageRoot == null) throw RuntimeException("Whoops! You have to put the SD into your phone.")
        DIR_GAME_HOME = pojavStorageRoot.absolutePath
        DIR_GAME_NEW = DIR_GAME_HOME + "/.minecraft"
        DIR_HOME_VERSION = DIR_GAME_NEW + "/versions"
        DIR_HOME_LIBRARY = DIR_GAME_NEW + "/libraries"
        DIR_HOME_CRASH = DIR_GAME_NEW + "/crash-reports"
        ASSETS_PATH = DIR_GAME_NEW + "/assets"
        OBSOLETE_RESOURCES_PATH = DIR_GAME_NEW + "/resources"
        CTRLMAP_PATH = DIR_GAME_HOME + "/controlmap"
        CTRLDEF_FILE = DIR_GAME_HOME + "/controlmap/default.json"
    }

    fun buildNotificationChannel(context: Context) {
        if (VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val channel = NotificationChannel(
            context.getString(R.string.notif_channel_id),
            context.getString(R.string.notif_channel_name), NotificationManager.IMPORTANCE_DEFAULT
        )
        val manager = NotificationManagerCompat.from(context)
        manager.createNotificationChannel(channel)
    }

    fun artifactToPath(library: DependentLibrary): String {
        if (library.downloads?.artifact?.path != null) return library.downloads!!.artifact!!.path!!
        val libInfos: Array<String> =
            library.name!!.split(":".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
        return libInfos[0].replace(
            "\\.".toRegex(),
            "/"
        ) + "/" + libInfos[1] + "/" + libInfos[2] + "/" + libInfos[1] + "-" + libInfos[2] + ".jar"
    }

    fun getDisplayMetrics(activity: Activity): DisplayMetrics {
        var displayMetrics = DisplayMetrics()

        if (VERSION.SDK_INT >= Build.VERSION_CODES.N && (activity.isInMultiWindowMode || activity.isInPictureInPictureMode)) {
            //For devices with free form/split screen, we need window size, not screen size.
            displayMetrics = activity.resources.displayMetrics
        } else {
            if (VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                Objects.requireNonNull<Display?>(activity.display)
                    .getRealMetrics(displayMetrics)
            } else { // Removed the clause for devices with unofficial notch support, since it also ruins all devices with virtual nav bars before P
                activity.windowManager.defaultDisplay.getRealMetrics(displayMetrics)
            }
        }
        currentDisplayMetrics = displayMetrics
        return displayMetrics
    }

    @RequiresApi(Build.VERSION_CODES.P)
    private fun setCutoutMode(window: Window, ignoreNotch: Boolean) {
        val layoutParams = window.attributes
        if (ignoreNotch) {
            if (VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                layoutParams.layoutInDisplayCutoutMode =
                    WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS
            } else {
                layoutParams.layoutInDisplayCutoutMode =
                    WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
            }
        } else {
            layoutParams.layoutInDisplayCutoutMode =
                WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_NEVER
        }
        window.attributes = layoutParams
        window.addFlags(WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN)
    }

    @Suppress("deprecation")
    private fun setLegacyFullscreen(insetView: View, fullscreen: Boolean) {
        val listener = OnSystemUiVisibilityChangeListener { visibility: Int ->
            if (fullscreen && (visibility and View.SYSTEM_UI_FLAG_FULLSCREEN) == 0) {
                insetView.systemUiVisibility = (View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                            or View.SYSTEM_UI_FLAG_FULLSCREEN
                            or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                            or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                            or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                            or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN)
            } else if (!fullscreen) {
                insetView.systemUiVisibility = View.SYSTEM_UI_FLAG_VISIBLE
            }
        }
        listener.onSystemUiVisibilityChange(insetView.systemUiVisibility)
        insetView.setOnSystemUiVisibilityChangeListener(listener)
    }

    fun setInsetsMode(activity: Activity, noSystemBars: Boolean, ignoreNotch: Boolean) {
        var nBars = noSystemBars
        val window = activity.window


        val insetView = activity.findViewById<View>(android.R.id.content)
        // Don't ignore system bars in window mode (will put game behind window button bar)
        if (VERSION.SDK_INT >= Build.VERSION_CODES.N && activity.isInMultiWindowMode) nBars =
            false

        val bgColor: Int
        // The status bars are completely transparent and will take their color from the inset view
        // background drawable.
        if (!nBars) bgColor = activity.resources.getColor(R.color.background_status_bar)
        else bgColor = Color.BLACK

        // On API 35 onwards, apps are edge-to-edge by default and are controlled entirely though the
        // inset API. On levels below, we still need to set the correct cutout mode.
        if (VERSION.SDK_INT >= Build.VERSION_CODES.P) setCutoutMode(window, ignoreNotch)

        // The AppCompat APIs don't work well, and break when opening alert dialogs on older Android
        // versions. Use the legacy fullscreen flags for lower APIs. (notch is already handled above)
        if (VERSION.SDK_INT < Build.VERSION_CODES.R) {
            setLegacyFullscreen(insetView, nBars)
            return
        }
        // Code below expects this to be set to false, since that's the SDK 35 default.
        if (VERSION.SDK_INT < Build.VERSION_CODES.VANILLA_ICE_CREAM) {
            window.setDecorFitsSystemWindows(false)
        }

        val insetsController = window.insetsController
        if (insetsController != null) {
            insetsController.systemBarsBehavior = WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            if (nBars) insetsController.hide(WindowInsets.Type.systemBars())
            else insetsController.show(WindowInsets.Type.systemBars())
        }

        val fFullscreen = nBars




        insetView.setOnApplyWindowInsetsListener { v: View?, windowInsets: WindowInsets? ->
            var insetMask = 0
            if (!fFullscreen) insetMask = insetMask or WindowInsets.Type.systemBars()
            if (!ignoreNotch) insetMask = insetMask or WindowInsets.Type.displayCutout()
            if (insetMask != 0) {
                val insets = windowInsets!!.getInsets(insetMask)
                v!!.background = InsetBackground(insets, bgColor)
                insetView.setPadding(insets.left, insets.top, insets.right, insets.bottom)
            } else {
                insetView.setPadding(0, 0, 0, 0)
                v!!.background = null
            }
            windowInsets!!
        }
        insetView.requestApplyInsets()
    }

    // Note: this should *NOT* be used for positioning and sizing things on the screen
    @JvmField
    var currentDisplayMetrics: DisplayMetrics? = null

    @JvmStatic
    fun dpToPx(dp: Float): Float {
        //Better hope for the currentDisplayMetrics to be good
        return dp * currentDisplayMetrics!!.density
    }

    @JvmStatic
    fun pxToDp(px: Float): Float {
        //Better hope for the currentDisplayMetrics to be good
        return px / currentDisplayMetrics!!.density
    }

    @Throws(IOException::class)
    fun copyAssetFile(ctx: Context, assetPath: String, output: String?, overwrite: Boolean) {
        var fileName = getFileName(assetPath)
        if (fileName == null) fileName = assetPath
        val outputFile = File(output, fileName)
        copyAssetFile(ctx.assets, assetPath, outputFile, overwrite)
    }

    @JvmStatic
    @Throws(IOException::class)
    fun copyAssetFile(
        assetManager: AssetManager,
        fileName: String,
        output: File,
        overwrite: Boolean
    ) {
        ensureParentDirectory(output)
        if (output.exists() && !overwrite) return
        assetManager.open(fileName).use { inputStream ->
            FileOutputStream(output).use { fileOutputStream ->
                IOUtils.copy(inputStream, fileOutputStream)
            }
        }
    }

    fun printToString(throwable: Throwable): String {
        val stringWriter = StringWriter()
        val printWriter = PrintWriter(stringWriter)
        throwable.printStackTrace(printWriter)
        printWriter.close()
        return stringWriter.toString()
    }

    @JvmStatic
    @JvmOverloads
    fun showError(ctx: Context, e: Throwable, exitIfOk: Boolean = false) {
        showError(ctx, R.string.global_error, null, e, exitIfOk, false)
    }

    fun showError(ctx: Context, rolledMessage: Int, e: Throwable) {
        showError(ctx, R.string.global_error, ctx.getString(rolledMessage), e, false, false)
    }

    @JvmStatic
    fun showError(ctx: Context, rolledMessage: String?, e: Throwable) {
        showError(ctx, R.string.global_error, rolledMessage, e, false, false)
    }

    fun showError(ctx: Context, rolledMessage: String?, e: Throwable, exitIfOk: Boolean) {
        showError(ctx, R.string.global_error, rolledMessage, e, exitIfOk, false)
    }

    fun showError(ctx: Context, titleId: Int, e: Throwable, exitIfOk: Boolean) {
        showError(ctx, titleId, null, e, exitIfOk, false)
    }

    private fun showError(
        ctx: Context,
        titleId: Int,
        rolledMessage: String?,
        e: Throwable,
        exitIfOk: Boolean,
        showMore: Boolean
    ) {
        if (e is ContextExecutorTask) {
            execute(e)
            return
        }

        val runnable = Runnable {
            val errMsg =
                if (showMore) printToString(e) else if (rolledMessage != null) rolledMessage else e.message
            val builder = MaterialAlertDialogBuilder(ctx)
                .setTitle(titleId)
                .setMessage(errMsg)
                .setPositiveButton(
                    android.R.string.ok,
                    DialogInterface.OnClickListener { _, _ ->
                        if (exitIfOk) {
                            if (ctx is MainActivity) {
                                exitToLauncher(ctx)
                            } else if (ctx is Activity) {
                                ctx.finish()
                            }
                        }
                    })
                .setNegativeButton(
                    if (showMore) R.string.error_show_less else R.string.error_show_more,
                    DialogInterface.OnClickListener { _, _ ->
                        showError(
                            ctx,
                            titleId,
                            rolledMessage,
                            e,
                            exitIfOk,
                            !showMore
                        )
                    })
                .setNeutralButton(
                    android.R.string.copy,
                    DialogInterface.OnClickListener { _, _ ->
                        val mgr =
                            ctx.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        mgr.setPrimaryClip(ClipData.newPlainText("error", printToString(e)))
                        if (exitIfOk) {
                            if (ctx is Activity) {
                                ctx.finish()
                            }
                        }
                    })
                .setCancelable(!exitIfOk)
            builder.show()
        }

        if (ctx is Activity) {
            ctx.runOnUiThread(runnable)
        } else {
            runnable.run()
        }
    }

    /**
     * Show the error remotely in a context-aware fashion. Has generally the same behaviour as
     * Tools.showError when in an activity, but when not in one, sends a notification that opens an
     * activity and calls Tools.showError().
     * NOTE: If the Throwable is a ContextExecutorTask and when not in an activity,
     * its executeWithApplication() method will never be called.
     * @param e the error (throwable)
     */
    @JvmStatic
    fun showErrorRemote(e: Throwable?) {
        showErrorRemote(null, e)
    }

    fun showErrorRemote(context: Context, rolledMessage: Int, e: Throwable?) {
        showErrorRemote(context.getString(rolledMessage), e)
    }

    @JvmStatic
    fun showErrorRemote(rolledMessage: String?, e: Throwable?) {
        // I WILL embrace layer violations because Android's concept of layers is STUPID
        // We live in the same process anyway, why make it any more harder with this needless
        // abstraction?

        // Add your Context-related rage here

        execute(RemoteErrorTask(e, rolledMessage))
    }


    @JvmStatic
    fun dialog(context: Context, title: CharSequence?, message: CharSequence?) {
        MaterialAlertDialogBuilder(context)
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton(android.R.string.ok, null)
            .show()
    }

    @JvmStatic
    fun openURL(act: Activity, url: String?) {
        try {
            val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            browserIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            act.startActivity(browserIntent)
        } catch (e: ActivityNotFoundException) {
            showError(act, e)
        }
    }

    fun shouldSkipLibrary(library: DependentLibrary): Boolean {
        // Don't use lwjgl from libraries, we have our own bundled in.
        return library.name!!.startsWith("org.lwjgl")
    }

    fun preProcessLibraries(libraries: Array<DependentLibrary>) {
        for (libItem in libraries) {
            val name = libItem.name ?: continue
            val parts = name.split(":").dropLastWhile { it.isEmpty() }
            if (parts.size < 3) continue
            val versionParts = parts[2].split(".").dropLastWhile { it.isEmpty() }
            if (versionParts.isEmpty()) continue

            if (name.startsWith("net.java.dev.jna:jna:")) {
                // Special handling for LabyMod 1.8.9, Forge 1.12.2(?) and oshi
                // we have libjnidispatch 5.13.0 in jniLibs directory
                // Force version 5.13.0 for all versions to ensure bundled native compatibility
                if (name != "net.java.dev.jna:jna:5.13.0") {
                    Log.d(APP_NAME, "Library " + name + " has been changed to version 5.13.0")
                    createLibraryInfo(libItem)
                    libItem.name = "net.java.dev.jna:jna:5.13.0"
                    libItem.downloads!!.artifact!!.path = "net/java/dev/jna/jna/5.13.0/jna-5.13.0.jar"
                    libItem.downloads!!.artifact!!.sha1 = "1200e7ebeedbe0d10062093f32925a912020e747"
                    libItem.downloads!!.artifact!!.url =
                        MAVEN_CENTRAL + "net/java/dev/jna/jna/5.13.0/jna-5.13.0.jar"
                    libItem.downloads!!.artifact!!.size = 1879325
                }
                libItem.replaced = true
            } else if (name.startsWith("com.github.oshi:oshi-core:")) {
                //if (Integer.parseInt(version[0]) >= 6 && Integer.parseInt(version[1]) >= 3) return;
                // FIXME: ensure compatibility

                if (versionParts[0].toInt() != 6 || versionParts.getOrElse(1) { "0" }.toInt() != 2) continue
                Log.d(APP_NAME, "Library " + name + " has been changed to version 6.3.0")
                createLibraryInfo(libItem)
                libItem.name = "com.github.oshi:oshi-core:6.3.0"
                libItem.downloads!!.artifact!!.path =
                    "com/github/oshi/oshi-core/6.3.0/oshi-core-6.3.0.jar"
                libItem.downloads!!.artifact!!.sha1 = "9e98cf55be371cafdb9c70c35d04ec2a8c2b42ac"
                libItem.downloads!!.artifact!!.url =
                    MAVEN_CENTRAL + "com/github/oshi/oshi-core/6.3.0/oshi-core-6.3.0.jar"
                libItem.downloads!!.artifact!!.size = 957945
                libItem.replaced = true
            } else if (name.startsWith("org.ow2.asm:asm-all:")) {
                // Early versions of the ASM library get repalced with 5.0.4 because Pojav's LWJGL is compiled for
                // Java 8, which is not supported by old ASM versions. Mod loaders like Forge, which depend on this
                // library, often include lwjgl in their class transformations, which causes errors with old ASM versions.
                if (versionParts[0].toInt() >= 5) continue
                Log.d(APP_NAME, "Library " + name + " has been changed to version 5.0.4")
                createLibraryInfo(libItem)
                libItem.name = "org.ow2.asm:asm-all:5.0.4"
                libItem.url = null
                libItem.downloads!!.artifact!!.path = "org/ow2/asm/asm-all/5.0.4/asm-all-5.0.4.jar"
                libItem.downloads!!.artifact!!.sha1 = "e6244859997b3d4237a552669279780876228909"
                libItem.downloads!!.artifact!!.url =
                    MAVEN_CENTRAL + "org/ow2/asm/asm-all/5.0.4/asm-all-5.0.4.jar"
                libItem.downloads!!.artifact!!.size = 241810
                libItem.replaced = true
            } else if (name.startsWith("com.mojang:jtracy:")) {
                // Fix for missing TracyClient dependency in newer Minecraft versions
                libItem.replaced = true
                createLibraryInfo(libItem)
                // Skip verification as the server-side file might have changed hashes
                libItem.downloads!!.artifact!!.url = "https://libraries.minecraft.net/com/mojang/jtracy/1.0.31/jtracy-1.0.31.jar"
                libItem.downloads!!.artifact!!.path = "com/mojang/jtracy/1.0.31/jtracy-1.0.31.jar"
                libItem.downloads!!.artifact!!.sha1 = null
                libItem.downloads!!.artifact!!.size = -1L
            }
        }
    }

    @JvmStatic
    @Throws(IOException::class)
    fun read(`is`: InputStream): String? {
        val readResult = IOUtils.toString(`is`, StandardCharsets.UTF_8)
        `is`.close()
        return readResult
    }

    @Throws(IOException::class)
    fun read(path: String?): String? {
        return read(FileInputStream(path))
    }

    @JvmStatic
    @Throws(IOException::class)
    fun read(path: File?): String? {
        return read(FileInputStream(path))
    }

    @JvmStatic
    @Throws(IOException::class)
    fun write(path: File, content: String?) {
        ensureParentDirectory(path)
        FileOutputStream(path).use { fileOutputStream ->
            IOUtils.write(content, fileOutputStream)
        }
    }

    @Throws(IOException::class)
    fun write(path: String, content: String?) {
        write(File(path), content)
    }

    @get:JvmStatic
    val isAndroid8OrHigher: Boolean
        get() = VERSION.SDK_INT >= 26

    fun fullyExit() {
        Process.killProcess(Process.myPid())
    }

    fun printLauncherInfo(gameVersion: String?, javaArguments: String?) {
        Logger.appendToLog("Info: Launcher version: " + net.ashmeet.hyperlauncher.BuildConfig.VERSION_NAME)
        Logger.appendToLog("Info: Architecture: " + Architecture.archAsString(DEVICE_ARCHITECTURE))
        Logger.appendToLog("Info: Device model: " + Build.MANUFACTURER + " " + Build.MODEL)
        Logger.appendToLog("Info: API version: " + VERSION.SDK_INT)
        Logger.appendToLog("Info: Selected Minecraft version: " + gameVersion)
        Logger.appendToLog("Info: Custom Java arguments: \"" + javaArguments + "\"")
        val info = glInfo
        Logger.appendToLog("Info: RAM allocated: " + LauncherPreferences.PREF_RAM_ALLOCATION + " Mb")
        Logger.appendToLog("Info: Graphics device: " + info!!.vendor + " " + info.renderer + " (OpenGL ES " + info.glesMajorVersion + ")")
    }

    fun getVersionInfo(versionName: String?): JMinecraftVersionList.Version {
        return getVersionInfo(versionName, false)
    }

    fun getVersionInfo(
        versionName: String?,
        skipInheriting: Boolean
    ): JMinecraftVersionList.Version {
        try {
            var customVer = GLOBAL_GSON.fromJson<JMinecraftVersionList.Version>(
                read(
                    DIR_HOME_VERSION + "/" + versionName + "/" + versionName + ".json"
                ), JMinecraftVersionList.Version::class.java
            )
            if (skipInheriting || customVer.inheritsFrom == null || customVer.inheritsFrom == customVer.id) {
                customVer.libraries?.let { preProcessLibraries(it.filterNotNull().toTypedArray()) }
            } else {
                val inheritsVer: JMinecraftVersionList.Version
                //If it won't download, just search for it
                try {
                    inheritsVer = GLOBAL_GSON.fromJson<JMinecraftVersionList.Version>(
                        read(
                            DIR_HOME_VERSION + "/" + customVer.inheritsFrom + "/" + customVer.inheritsFrom + ".json"
                        ), JMinecraftVersionList.Version::class.java
                    )
                } catch (e: IOException) {
                    throw RuntimeException("Can't find the source version for " + versionName + " (req version=" + customVer.inheritsFrom + ")")
                }
                //inheritsVer.inheritsFrom = inheritsVer.id;
                insertSafety(
                    inheritsVer, customVer,
                    "assetIndex", "assets", "id",
                    "mainClass", "minecraftArguments",
                    "releaseTime", "time", "type"
                )

                // Go through the libraries, remove the ones overridden by the custom version
                val inheritLibraryList = ArrayList<DependentLibrary>()
                inheritsVer.libraries?.filterNotNull()?.let { inheritLibraryList.addAll(it) }

                val customLibraries = customVer.libraries ?: emptyArray()
                outer_loop@ for (library in customLibraries) {
                    if (library == null) continue
                    // Clean libraries overridden by the custom version
                    val libName = library.name!!.substring(0, library.name!!.lastIndexOf(":"))

                    val iterator = inheritLibraryList.iterator()
                    while (iterator.hasNext()) {
                        val inheritLibrary = iterator.next()
                        val inheritLibName = inheritLibrary.name!!.substring(
                            0,
                            inheritLibrary.name!!.lastIndexOf(":")
                        )

                        if (libName == inheritLibName) {
                            Log.d(
                                APP_NAME, "Library " + libName + ": Replaced version " +
                                        libName.substring(libName.lastIndexOf(":") + 1) + " with " +
                                        inheritLibName.substring(inheritLibName.lastIndexOf(":") + 1)
                            )

                            // Remove the library , superseded by the overriding libs
                            iterator.remove()
                            continue@outer_loop
                        }
                    }
                }

                // Fuse libraries
                customVer.libraries?.filterNotNull()?.let { inheritLibraryList.addAll(it) }
                inheritsVer.libraries = inheritLibraryList.toTypedArray()
                preProcessLibraries(inheritsVer.libraries!!.filterNotNull().toTypedArray())


                // Inheriting Minecraft 1.13+ with append custom args
                val inheritsVerArgs = inheritsVer.arguments
                val customVerArgs = customVer.arguments
                if (inheritsVerArgs != null && customVerArgs != null && inheritsVerArgs.game != null && customVerArgs.game != null) {
                    val totalArgList = ArrayList<Any?>()
                    inheritsVerArgs.game?.let { totalArgList.addAll(it) }

                    var nskip = 0
                    val customGameArgs = customVerArgs.game!!
                    for (i in customGameArgs.indices) {
                        if (nskip > 0) {
                            nskip--
                            continue
                        }

                        var perCustomArg = customGameArgs[i]
                        if (perCustomArg is String) {
                            var perCustomArgStr = perCustomArg
                            // Check if there is a duplicate argument on combine
                            if (perCustomArgStr.startsWith("--") && totalArgList.contains(
                                    perCustomArgStr
                                )
                            ) {
                                perCustomArg = customGameArgs.getOrNull(i + 1)
                                if (perCustomArg is String) {
                                    perCustomArgStr = perCustomArg
                                    // If the next is argument value, skip it
                                    if (!perCustomArgStr.startsWith("--")) {
                                        nskip++
                                    }
                                }
                            } else {
                                totalArgList.add(perCustomArgStr)
                            }
                        } else if (!totalArgList.contains(perCustomArg)) {
                            totalArgList.add(perCustomArg)
                        }
                    }

                    inheritsVerArgs.game = totalArgList.toTypedArray()
                }

                customVer = inheritsVer
            }

            // LabyMod 4 sets version instead of majorVersion
            if (customVer.javaVersion != null && customVer.javaVersion!!.majorVersion == 0) {
                customVer.javaVersion!!.majorVersion = customVer.javaVersion!!.version
            }
            return customVer
        } catch (e: Exception) {
            throw RuntimeException(e)
        }
    }

    // Prevent NullPointerException
    private fun insertSafety(
        targetVer: JMinecraftVersionList.Version,
        fromVer: JMinecraftVersionList.Version,
        vararg keyArr: String
    ) {
        for (key in keyArr) {
            var value: Any? = null
            try {
                val fieldA = fromVer.javaClass.getDeclaredField(key)
                fieldA.isAccessible = true
                value = fieldA.get(fromVer)
                if (((value is String) && !value.isEmpty()) || value != null) {
                    val fieldB = targetVer.javaClass.getDeclaredField(key)
                    fieldB.isAccessible = true
                    fieldB.set(targetVer, value)
                }
            } catch (th: Throwable) {
                Log.w(APP_NAME, "Unable to insert " + key + "=" + value, th)
            }
        }
    }

    fun getSelectedRuntime(instance: Instance): String? {
        var runtime = LauncherPreferences.PREF_DEFAULT_RUNTIME
        val profileRuntime = instance.selectedRuntime
        if (profileRuntime != null) {
            if (forceReread(profileRuntime).versionString != null) {
                runtime = profileRuntime
            }
        }
        return runtime
    }

    fun createLibraryInfo(library: DependentLibrary) {
        if (library.downloads == null || library.downloads!!.artifact == null) library.downloads =
            LibraryDownloads(MinecraftLibraryArtifact())
    }

    fun getTotalDeviceMemory(ctx: Context): Int {
        val actManager = ctx.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val memInfo = ActivityManager.MemoryInfo()
        actManager.getMemoryInfo(memInfo)
        return (memInfo.totalMem / 1048576L).toInt()
    }

    fun getFreeDeviceMemory(ctx: Context): Int {
        val actManager = ctx.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val memInfo = ActivityManager.MemoryInfo()
        actManager.getMemoryInfo(memInfo)
        return (memInfo.availMem / 1048576L).toInt()
    }

    fun getDisplayFriendlyRes(displaySideRes: Int, scaling: Float): Int {
        var displayRes = displaySideRes
        displayRes = (displayRes * scaling).toInt()
        if (displayRes % 2 != 0) displayRes--
        return displayRes
    }

    @JvmStatic
    fun getFileName(ctx: Context, uri: Uri): String? {
        try {
            ctx.contentResolver.query(uri, null, null, null, null).use { c ->
                if (c == null) return uri.lastPathSegment // idk myself but it happens on asus file manager

                if (!c.moveToFirst()) return uri.lastPathSegment
                val columnIndex = c.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (columnIndex == -1) return uri.lastPathSegment
                return c.getString(columnIndex)
            }
        } catch (e: Exception) {
            // Turns out that the content resolver can throw you literally anything if the underlying provider crashes
            // Fall back in that case
            return uri.lastPathSegment
        }
    }

    /** Swap the main fragment with another  */
    fun swapFragment(
        fragmentActivity: FragmentActivity, fragmentClass: Class<out Fragment>,
        fragmentTag: String?, bundle: Bundle?
    ) {
        val transaction = fragmentActivity.supportFragmentManager.beginTransaction()
        setFragmentAnimation(transaction, true)
        transaction.setReorderingAllowed(true)
            .addToBackStack(fragmentClass.name)
            .replace(R.id.container_fragment, fragmentClass, bundle, fragmentTag).commit()
    }

    /** Set the fragment animation based on preferences  */
    @JvmStatic
    fun setFragmentAnimation(transaction: FragmentTransaction, opening: Boolean) {
        var enter = 0
        var exit = 0
        var popEnter = 0
        var popExit = 0

        when (LauncherPreferences.PREF_ANIMATION_TYPE) {
            "jelly" -> {
                enter = R.anim.jelly_in
                exit = R.anim.jelly_out
                popEnter = R.anim.jelly_in
                popExit = R.anim.jelly_out
            }
            "slide" -> {
                if (opening) {
                    enter = R.anim.slide_in_right
                    exit = R.anim.slide_out_left
                    popEnter = R.anim.slide_in_left
                    popExit = R.anim.slide_out_right
                } else {
                    enter = R.anim.slide_in_left
                    exit = R.anim.slide_out_right
                    popEnter = R.anim.slide_in_right
                    popExit = R.anim.slide_out_left
                }
            }
            "default" -> {
                enter = R.anim.fade_in
                exit = R.anim.fade_out
                popEnter = R.anim.fade_in
                popExit = R.anim.fade_out
            }
        }
        transaction.setCustomAnimations(enter, exit, popEnter, popExit)
    }

    @JvmStatic
    fun backToMainMenu(fragmentActivity: FragmentActivity) {
        fragmentActivity.supportFragmentManager
            .popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE)
    }

    /** Remove the current fragment  */
    @JvmStatic
    fun removeCurrentFragment(fragmentActivity: FragmentActivity) {
        fragmentActivity.supportFragmentManager.popBackStack()
    }

    /** Launch the mod installer activity. The Uri must be from our own content provider or
     * from ACTION_OPEN_DOCUMENT
     */
    @JvmStatic
    fun launchModInstaller(context: Context, uri: Uri) {
        val intent = Intent(context, JavaGUILauncherActivity::class.java)
        intent.putExtra("modUri", uri)
        context.startActivity(intent)
    }


    fun installRuntimeFromUri(context: Context, uri: Uri) {
        PojavApplication.sExecutorService.execute {
            try {
                val name = getFileName(context, uri)
                MultiRTUtils.installRuntimeNamed(
                    NATIVE_LIB_DIR!!,
                    context.contentResolver.openInputStream(uri)!!,
                    name!!
                )

                postPrepare(name)
            } catch (e: IOException) {
                showError(context, e)
            }
        }
    }

    fun extractUntilCharacter(input: String, whatFor: String, terminator: Char): String? {
        var whatForStart = input.indexOf(whatFor)
        if (whatForStart == -1) return null
        whatForStart += whatFor.length
        val terminatorIndex = input.indexOf(terminator, whatForStart)
        if (terminatorIndex == -1) return null
        return input.substring(whatForStart, terminatorIndex)
    }

    @JvmStatic
    fun isValidString(string: String?): Boolean {
        return string != null && string.isNotEmpty()
    }

    fun validOrNullString(string: String?): String? {
        if (!isValidString(string)) return null
        return string
    }

    @JvmStatic
    fun runOnUiThread(runnable: Runnable) {
        MAIN_HANDLER.post(runnable)
    }

    /** Triggers the share intent chooser, with the latestlog file attached to it  */
    @JvmStatic
    fun shareLog(context: Context) {
        openPath(context, File(DIR_GAME_HOME, "latestlog.txt"), true)
    }

    /**
     * Determine the MIME type of a File.
     * @param file The file to determine the type of
     * @return the type, or the default value *slash* if cannot be determined
     */
    fun getMimeType(file: File): String {
        if (file.isDirectory) return DocumentsContract.Document.MIME_TYPE_DIR
        var mimeType: String? = null
        try {
            FileInputStream(file).use { fileInputStream ->
                BufferedInputStream(fileInputStream).use { bufferedInputStream ->
                    mimeType = URLConnection.guessContentTypeFromStream(bufferedInputStream)
                }
            }
        } catch (e: IOException) {
            Log.w("FileMimeType", "Failed to determine MIME type by stream", e)
        }
        if (mimeType != null) return mimeType
        mimeType = URLConnection.guessContentTypeFromName(file.name)
        if (mimeType != null) return mimeType
        return "*/*"
    }

    /**
     * Open the path specified by a File in a file explorer or in a relevant application.
     * @param context the current Context
     * @param file the File to open
     * @param share whether to open a "Share" or an "Open" dialog.
     */
    @JvmStatic
    fun openPath(context: Context, file: File, share: Boolean) {
        val contentUri = DocumentsContract.buildDocumentUri(
            context.getString(R.string.storageProviderAuthorities),
            file.absolutePath
        )
        val mimeType = getMimeType(file)
        val intent = Intent()
        if (share) {
            intent.action = Intent.ACTION_SEND
            intent.type = getMimeType(file)
            intent.putExtra(Intent.EXTRA_STREAM, contentUri)
        } else {
            intent.action = Intent.ACTION_VIEW
            intent.setDataAndType(contentUri, mimeType)
        }
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        val chooserIntent = Intent.createChooser(intent, file.name)
        context.startActivity(chooserIntent)
    }

    /** Mesure the textview height, given its current parameters  */
    fun mesureTextviewHeight(t: TextView): Int {
        val widthMeasureSpec =
            View.MeasureSpec.makeMeasureSpec(t.width, View.MeasureSpec.AT_MOST)
        val heightMeasureSpec = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
        t.measure(widthMeasureSpec, heightMeasureSpec)
        return t.measuredHeight
    }

    fun dialogForceClose(ctx: Context) {
        MaterialAlertDialogBuilder(ctx)
            .setMessage(R.string.mcn_exit_confirm)
            .setNegativeButton(android.R.string.cancel, null)
            .setPositiveButton(
                android.R.string.ok
            ) { _, _ ->
                try {
                    fullyExit()
                } catch (th: Throwable) {
                    Log.w(APP_NAME, "Could not enable System.exit() method!", th)
                }
            }.show()
    }

    fun restart(ctx: Context) {
        val appContext = ctx.applicationContext
        val intent =
            appContext.packageManager.getLaunchIntentForPackage(appContext.packageName)
        if (intent != null) {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)

            val pendingIntent = PendingIntent.getActivity(
                appContext, 123456, intent,
                (if (VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0) or PendingIntent.FLAG_CANCEL_CURRENT
            )

            val mgr = appContext.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            mgr.set(AlarmManager.RTC, System.currentTimeMillis() + 300, pendingIntent)
        }
        fullyExit()
    }

    fun deviceHasHangingLinker(): Boolean {
        // Android Oreo and onwards have GSIs and most phone firmwares at that point were not modified
        // *that* intrusively. So assume that we are not affected.
        if (VERSION.SDK_INT >= Build.VERSION_CODES.O) return false
        // Since the affected function in LWJGL is rarely used (and when used, it's mainly for debug prints)
        // we can make the search scope a bit more broad and check if we are running on a Huawei device.
        return Build.MANUFACTURER.lowercase().contains("huawei")
    }


    fun <T> getWeakReference(weakReference: WeakReference<T?>?): T? {
        if (weakReference == null) return null
        return weakReference.get()
    }

    fun deviceSupportsGyro(context: Context): Boolean {
        return (context.getSystemService(Context.SENSOR_SERVICE) as SensorManager).getDefaultSensor(
            Sensor.TYPE_GYROSCOPE
        ) != null
    }

    fun exitToLauncher(ctx: Context) {
        try {
            val intent = Intent(ctx, LauncherActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
            ctx.startActivity(intent)
        } catch (e: Exception) {
            restart(ctx)
            return
        }
        if (ctx is Activity) ctx.finish()
    }

    interface DownloaderFeedback {
        fun updateProgress(curr: Int, max: Int)
    }
}
