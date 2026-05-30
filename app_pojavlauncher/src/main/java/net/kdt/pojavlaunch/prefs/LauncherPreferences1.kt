package net.kdt.pojavlaunch.prefs

import android.app.Activity
import android.content.Context
import android.content.SharedPreferences
import android.content.res.Configuration
import android.graphics.Color
import android.graphics.Rect
import android.os.Build.VERSION
import android.os.Build.VERSION_CODES
import android.util.Log
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.res.ResourcesCompat
import net.ashmeet.hyperlauncher.R
import net.kdt.pojavlaunch.Architecture
import net.kdt.pojavlaunch.Tools
import net.kdt.pojavlaunch.multirt.MultiRTUtils.runtimes
import net.kdt.pojavlaunch.utils.JREUtils
import java.io.IOException
import kotlin.math.ceil
import kotlin.math.min

object LauncherPreferences {
    const val PREF_KEY_CURRENT_INSTANCE: String = "currentInstance"
    const val PREF_KEY_SKIP_NOTIFICATION_CHECK: String = "skipNotificationPermissionCheck"

    @JvmField
    var DEFAULT_PREF: SharedPreferences? = null
    var PREF_RENDERER: String = "opengles2"

    @JvmField
    var PREF_IGNORE_NOTCH: Boolean = true
    @JvmField
    var PREF_BUTTONSIZE: Float = 100f
    @JvmField
    var PREF_MOUSESCALE: Float = 1f
    @JvmField
    var PREF_LONGPRESS_TRIGGER: Int = 300
    @JvmField
    var PREF_DEFAULTCTRL_PATH: String? = Tools.CTRLDEF_FILE
    @JvmField
    var PREF_CUSTOM_JAVA_ARGS: String? = null
    @JvmField
    var PREF_FORCE_ENGLISH: Boolean = false
    const val PREF_VERSION_REPOS: String =
        "https://piston-meta.mojang.com/mc/game/version_manifest_v2.json"
    @JvmField
    var PREF_DISABLE_GESTURES: Boolean = false
    @JvmField
    var PREF_DISABLE_SWAP_HAND: Boolean = false
    @JvmField
    var PREF_MOUSESPEED: Float = 1f
    @JvmField
    var PREF_RAM_ALLOCATION: Int = 0
    @JvmField
    var PREF_DEFAULT_RUNTIME: String? = null
    @JvmField
    var PREF_SUSTAINED_PERFORMANCE: Boolean = false
    @JvmField
    var PREF_VIRTUAL_MOUSE_START: Boolean = false
    @JvmField
    var PREF_USE_ALTERNATE_SURFACE: Boolean = true
    @JvmField
    var PREF_JAVA_SANDBOX: Boolean = true
    @JvmField
    var PREF_SCALE_FACTOR: Float = 1f

    @JvmField
    var PREF_ENABLE_GYRO: Boolean = false
    @JvmField
    var PREF_GYRO_SENSITIVITY: Float = 1f
    @JvmField
    var PREF_GYRO_SAMPLE_RATE: Int = 16
    @JvmField
    var PREF_GYRO_SMOOTHING: Boolean = true
    @JvmField
    var PREF_GYRO_INVERT_X: Boolean = false
    @JvmField
    var PREF_GYRO_INVERT_Y: Boolean = false

    @JvmField
    var PREF_FORCE_VSYNC: Boolean = false

    @JvmField
    var PREF_USE_ANGLE: Boolean = false

    @JvmField
    var PREF_BUTTON_ALL_CAPS: Boolean = true
    @JvmField
    var PREF_DUMP_SHADERS: Boolean = false
    @JvmField
    var PREF_DEADZONE_SCALE: Float = 1f
    @JvmField
    var PREF_BIG_CORE_AFFINITY: Boolean = false
    @JvmField
    var PREF_ZINK_PREFER_SYSTEM_DRIVER: Boolean = false

    @JvmField
    var PREF_VERIFY_MANIFEST: Boolean = true
    var PREF_DOWNLOAD_SOURCE: String = "default"
    @JvmField
    var PREF_SKIP_NOTIFICATION_PERMISSION_CHECK: Boolean = false
    @JvmField
    var PREF_VSYNC_IN_ZINK: Boolean = true

    @JvmField
    var PREF_RAPID_START: Boolean = true
    @JvmField
    var PREF_VERIFY_FILES: Boolean = true

    @JvmField
    var PREF_ANIMATION_TYPE: String = "jelly"
    @JvmField
    var PREF_ANIMATION_INTENSITY: Float = 0.70f

    @JvmField
    var PREF_MOUSE_CURSOR_PATH: String? = null
    @JvmField
    var PREF_MOUSE_HOTSPOT_X: Int = 0
    @JvmField
    var PREF_MOUSE_HOTSPOT_Y: Int = 0

    @JvmField
    var PREF_DRAWER_BUTTON_X: Float = 50f
    @JvmField
    var PREF_DRAWER_BUTTON_Y: Float = 0f
    @JvmField
    var PREF_DRAWER_BUTTON_OPACITY: Float = 0.33f
    @JvmField
    var PREF_DRAWER_BUTTON_BG_OPACITY: Float = 0.33f
    @JvmField
    var PREF_DRAWER_BUTTON_ICON_OPACITY: Float = 1f
    @JvmField
    var PREF_DRAWER_BUTTON_SIZE: Int = 20
    @JvmField
    var PREF_DRAWER_BUTTON_CORNER_RADIUS: Int = 8
    @JvmField
    var PREF_DRAWER_BUTTON_MOVABLE: Boolean = false
    @JvmField
    var PREF_DRAWER_BUTTON_IMAGE_PATH: String? = null
    var PREF_DRAWER_BUTTON_PRESET: String = "custom"
    @JvmField
    var PREF_DRAWER_BUTTON_STROKE_ENABLED: Boolean = false

    @JvmField
    var PREF_DRAWER_LIST_OPACITY: Float = 1f

    var PREF_APP_THEME: String = "dark"
    @JvmField
    var PREF_BACKGROUND_PATH: String? = null
    @JvmField
    var PREF_BACKGROUND_BLUR: Boolean = false
    @JvmField
    var PREF_BACKGROUND_BLUR_INTENSITY: Int = 10
    @JvmField
    var PREF_BACKGROUND_IMAGE_OVERLAY_ENABLED: Boolean = false
    @JvmField
    var PREF_BACKGROUND_IMAGE_OVERLAY_ALPHA: Float = 0.25f
    @JvmField
    var PREF_MINEBUTTON_COLOR: Int = Color.BLACK
    @JvmField
    var PREF_MINEBUTTON_OUTLINE_COLOR: Int = 0x1F888888
    @JvmField
    var PREF_MINEBUTTON_CORNER_RADIUS: Int = 28
    @JvmField
    var PREF_ICON_OUTLINE_COLOR: Int = Color.WHITE
    @JvmField
    var PREF_GLOBAL_ICON_COLOR: Int = Color.BLACK
    var PREF_APPEARANCE_PRESET: String = "default"
    var PREF_ICON_PRESET: String = "default"

    // New experimental options
    @JvmField
    var PREF_ENABLE_MIPMAP: Boolean = false
    @JvmField
    var PREF_DISABLE_ERROR_CHECK: Boolean = false
    var PREF_OPTIMIZE_NETWORK: Boolean = false

    @JvmField
    var PREF_PREFERRED_GRAPHICS_BACKEND: String = "opengl"


    @JvmStatic
    fun loadPreferences(ctx: Context) {
        //Required for CTRLDEF_FILE and MultiRT
        Tools.initStorageConstants(ctx)
        val isDevicePowerful = isDevicePowerful(ctx)

        PREF_RENDERER = DEFAULT_PREF!!.getString("renderer", "opengles2")!!
        PREF_BUTTONSIZE = DEFAULT_PREF!!.getInt("buttonscale", 100).toFloat()
        PREF_MOUSESCALE = DEFAULT_PREF!!.getInt("mousescale", 100) / 100f
        PREF_MOUSESPEED = (DEFAULT_PREF!!.getInt("mousespeed", 100).toFloat()) / 100f
        PREF_IGNORE_NOTCH = DEFAULT_PREF!!.getBoolean("ignoreNotch", true)
        PREF_LONGPRESS_TRIGGER = DEFAULT_PREF!!.getInt("timeLongPressTrigger", 300)
        PREF_DEFAULTCTRL_PATH = DEFAULT_PREF!!.getString("defaultCtrl", Tools.CTRLDEF_FILE)
        PREF_FORCE_ENGLISH = DEFAULT_PREF!!.getBoolean("force_english", false)
        PREF_DISABLE_GESTURES = DEFAULT_PREF!!.getBoolean("disableGestures", false)
        PREF_DISABLE_SWAP_HAND = DEFAULT_PREF!!.getBoolean("disableDoubleTap", false)
        PREF_RAM_ALLOCATION = DEFAULT_PREF!!.getInt("allocation", findBestRAMAllocation(ctx))
        PREF_CUSTOM_JAVA_ARGS = DEFAULT_PREF!!.getString("javaArgs", "")
        PREF_SUSTAINED_PERFORMANCE =
            DEFAULT_PREF!!.getBoolean("sustainedPerformance", isDevicePowerful)
        PREF_VIRTUAL_MOUSE_START = DEFAULT_PREF!!.getBoolean("mouse_start", false)
        PREF_USE_ALTERNATE_SURFACE = DEFAULT_PREF!!.getBoolean(
            "alternate_surface",
            true
        ) // Default to true as it helps low-end devices
        PREF_JAVA_SANDBOX = DEFAULT_PREF!!.getBoolean("java_sandbox", true)
        PREF_SCALE_FACTOR = DEFAULT_PREF!!.getInt(
            "resolutionRatio",
            findBestResolution(ctx, isDevicePowerful)
        ) / 100f
        PREF_ENABLE_GYRO = DEFAULT_PREF!!.getBoolean("enableGyro", false)
        PREF_GYRO_SENSITIVITY = (DEFAULT_PREF!!.getInt("gyroSensitivity", 100).toFloat()) / 100f
        PREF_GYRO_SAMPLE_RATE = DEFAULT_PREF!!.getInt("gyroSampleRate", 16)
        PREF_GYRO_SMOOTHING = DEFAULT_PREF!!.getBoolean("gyroSmoothing", true)
        PREF_GYRO_INVERT_X = DEFAULT_PREF!!.getBoolean("gyroInvertX", false)
        PREF_GYRO_INVERT_Y = DEFAULT_PREF!!.getBoolean("gyroInvertY", false)
        PREF_FORCE_VSYNC = DEFAULT_PREF!!.getBoolean("force_vsync", isDevicePowerful)
        PREF_USE_ANGLE = DEFAULT_PREF!!.getBoolean("use_angle", false)
        PREF_BUTTON_ALL_CAPS = DEFAULT_PREF!!.getBoolean("buttonAllCaps", true)
        PREF_DUMP_SHADERS = DEFAULT_PREF!!.getBoolean("dump_shaders", false)
        PREF_DEADZONE_SCALE =
            (DEFAULT_PREF!!.getInt("gamepad_deadzone_scale", 100).toFloat()) / 100f
        PREF_BIG_CORE_AFFINITY = DEFAULT_PREF!!.getBoolean("bigCoreAffinity", false)
        PREF_ZINK_PREFER_SYSTEM_DRIVER = DEFAULT_PREF!!.getBoolean("zinkPreferSystemDriver", false)
        PREF_DOWNLOAD_SOURCE = DEFAULT_PREF!!.getString("downloadSource", "default")!!
        PREF_VERIFY_MANIFEST = DEFAULT_PREF!!.getBoolean("verifyManifest", true)
        PREF_SKIP_NOTIFICATION_PERMISSION_CHECK =
            DEFAULT_PREF!!.getBoolean(PREF_KEY_SKIP_NOTIFICATION_CHECK, false)
        PREF_VSYNC_IN_ZINK = DEFAULT_PREF!!.getBoolean("vsync_in_zink", false)
        PREF_VERIFY_FILES = DEFAULT_PREF!!.getBoolean("checkGameFiles", true)
        PREF_RAPID_START = DEFAULT_PREF!!.getBoolean("fastStartupCheck", true)

        PREF_ANIMATION_TYPE = DEFAULT_PREF!!.getString("animationType", "jelly")!!
        PREF_ANIMATION_INTENSITY = DEFAULT_PREF!!.getInt("animationIntensity", 70) / 100f

        PREF_MOUSE_CURSOR_PATH = DEFAULT_PREF!!.getString("mouseCursorPath", null)
        PREF_MOUSE_HOTSPOT_X = DEFAULT_PREF!!.getInt("mouseHotspotX", 0)
        PREF_MOUSE_HOTSPOT_Y = DEFAULT_PREF!!.getInt("mouseHotspotY", 0)

        PREF_DRAWER_BUTTON_X = DEFAULT_PREF!!.getInt("drawerButtonX", 50).toFloat()
        PREF_DRAWER_BUTTON_Y = DEFAULT_PREF!!.getInt("drawerButtonY", 0).toFloat()
        PREF_DRAWER_BUTTON_OPACITY = DEFAULT_PREF!!.getInt("drawerButtonOpacity", 33) / 100f
        PREF_DRAWER_BUTTON_BG_OPACITY = DEFAULT_PREF!!.getInt("drawerButtonBgOpacity", 33) / 100f
        PREF_DRAWER_BUTTON_ICON_OPACITY =
            DEFAULT_PREF!!.getInt("drawerButtonIconOpacity", 100) / 100f
        PREF_DRAWER_BUTTON_SIZE = DEFAULT_PREF!!.getInt("drawerButtonSize", 40)
        PREF_DRAWER_BUTTON_CORNER_RADIUS = DEFAULT_PREF!!.getInt("drawerButtonCornerRadius", 8)
        PREF_DRAWER_BUTTON_MOVABLE = DEFAULT_PREF!!.getBoolean("drawerButtonMovable", false)
        PREF_DRAWER_BUTTON_IMAGE_PATH = DEFAULT_PREF!!.getString("drawerButtonImagePath", null)
        PREF_DRAWER_BUTTON_PRESET = DEFAULT_PREF!!.getString("drawerButtonPreset", "custom")!!
        PREF_DRAWER_BUTTON_STROKE_ENABLED =
            DEFAULT_PREF!!.getBoolean("drawerButtonStrokeEnabled", true)

        PREF_DRAWER_LIST_OPACITY = DEFAULT_PREF!!.getInt("drawerListOpacity", 100) / 100f

        PREF_APP_THEME = DEFAULT_PREF!!.getString("appTheme", "dark")!!
        applyTheme()
        PREF_BACKGROUND_PATH = DEFAULT_PREF!!.getString("appBackgroundPath", null)
        PREF_BACKGROUND_BLUR = DEFAULT_PREF!!.getBoolean("appBackgroundBlur", false)
        PREF_BACKGROUND_BLUR_INTENSITY = DEFAULT_PREF!!.getInt("appBackgroundBlurIntensity", 10)
        PREF_BACKGROUND_IMAGE_OVERLAY_ENABLED =
            DEFAULT_PREF!!.getBoolean("backgroundImageOverlayEnabled", true)
        var overlayOpacity = DEFAULT_PREF!!.getInt("backgroundImageOverlayOpacity", 25)
        if (overlayOpacity < 0) overlayOpacity = 0
        if (overlayOpacity > 100) overlayOpacity = 100
        PREF_BACKGROUND_IMAGE_OVERLAY_ALPHA = overlayOpacity / 100f


        // Resolve "default" preset colors from resources for the selected app theme
        // (values/colors.xml for light, values-night/colors.xml for dark).
        var colorCtx = ctx
        try {
            val config = Configuration(ctx.getResources().getConfiguration())
            when (PREF_APP_THEME) {
                "light" -> config.uiMode =
                    (config.uiMode and Configuration.UI_MODE_NIGHT_MASK.inv()) or Configuration.UI_MODE_NIGHT_NO

                "dark" -> config.uiMode =
                    (config.uiMode and Configuration.UI_MODE_NIGHT_MASK.inv()) or Configuration.UI_MODE_NIGHT_YES

                "system" -> {}
                else -> {}
            }
            colorCtx = ctx.createConfigurationContext(config)
        } catch (ignored: Exception) {
            colorCtx = ctx
        }

        val defaultMineColor = ResourcesCompat.getColor(
            colorCtx.getResources(),
            R.color.minebutton_color,
            colorCtx.getTheme()
        )
        val defaultIconColor = ResourcesCompat.getColor(
            colorCtx.getResources(),
            R.color.primary_text,
            colorCtx.getTheme()
        )


        // Load Minecraft Button Style
        PREF_APPEARANCE_PRESET = DEFAULT_PREF!!.getString("appearancePreset", "default")!!
        when (PREF_APPEARANCE_PRESET) {
            "default" -> {
                PREF_MINEBUTTON_COLOR = defaultMineColor
                PREF_MINEBUTTON_CORNER_RADIUS = 28
            }

            "modern" -> {
                PREF_MINEBUTTON_COLOR = -0xde690d
                PREF_MINEBUTTON_CORNER_RADIUS = 12
            }

            "classic" -> {
                PREF_MINEBUTTON_COLOR = -0x9f8275
                PREF_MINEBUTTON_CORNER_RADIUS = 0
            }

            "custom" -> {
                PREF_MINEBUTTON_COLOR = DEFAULT_PREF!!.getInt("mineButtonColor", defaultMineColor)
                PREF_MINEBUTTON_CORNER_RADIUS =
                    DEFAULT_PREF!!.getInt("minebutton_corner_radius", 28)
            }
        }

        // Load Icon Style
        PREF_ICON_PRESET = DEFAULT_PREF!!.getString("iconPreset", "default")!!
        when (PREF_ICON_PRESET) {
            "default" -> {
                PREF_GLOBAL_ICON_COLOR = defaultIconColor
                PREF_ICON_OUTLINE_COLOR = Color.WHITE
            }

            "white" -> {
                PREF_GLOBAL_ICON_COLOR = Color.WHITE
                PREF_ICON_OUTLINE_COLOR = Color.BLACK
            }

            "black" -> {
                PREF_GLOBAL_ICON_COLOR = Color.BLACK
                PREF_ICON_OUTLINE_COLOR = Color.WHITE
            }

            "accent" -> {
                PREF_GLOBAL_ICON_COLOR = -0xde690d
                PREF_ICON_OUTLINE_COLOR = Color.WHITE
            }

            "custom" -> {
                PREF_GLOBAL_ICON_COLOR = DEFAULT_PREF!!.getInt("globalIconColor", defaultIconColor)
                PREF_ICON_OUTLINE_COLOR = DEFAULT_PREF!!.getInt("iconOutlineColor", Color.WHITE)
            }
        }

        PREF_MINEBUTTON_OUTLINE_COLOR = 0x1F888888 // Subtle fixed outline for MineButton

        PREF_ENABLE_MIPMAP = DEFAULT_PREF!!.getBoolean("enableMipmap", false)
        PREF_DISABLE_ERROR_CHECK = DEFAULT_PREF!!.getBoolean("disableErrorCheck", false)
        PREF_OPTIMIZE_NETWORK = DEFAULT_PREF!!.getBoolean("optimizeNetwork", false)

        PREF_PREFERRED_GRAPHICS_BACKEND =
            DEFAULT_PREF!!.getString("preferredGraphicsBackend", "opengl")!!

        val argLwjglLibname = "-Dorg.lwjgl.opengl.libname="
        for (arg in JREUtils.parseJavaArguments(PREF_CUSTOM_JAVA_ARGS)) {
            if (arg != null && arg.startsWith(argLwjglLibname)) {
                // purge arg
                DEFAULT_PREF?.edit()?.putString(
                    "javaArgs",
                    PREF_CUSTOM_JAVA_ARGS?.replace(arg, "") ?: ""
                )?.apply()
            }
        }
        if (DEFAULT_PREF!!.contains("defaultRuntime")) {
            PREF_DEFAULT_RUNTIME = DEFAULT_PREF!!.getString("defaultRuntime", "")
        } else {
            if (runtimes.isEmpty()) {
                PREF_DEFAULT_RUNTIME = ""
                return
            }
            PREF_DEFAULT_RUNTIME = runtimes.get(0).name
            DEFAULT_PREF!!.edit().putString("defaultRuntime", PREF_DEFAULT_RUNTIME).apply()
        }
    }

    fun applyTheme() {
        when (PREF_APP_THEME) {
            "light" -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
            "dark" -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
            "system" -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
        }
    }

    /**
     * This functions aims at finding the best default RAM allocation,
     * according to the RAM allocation of the physical device.
     * Put not enough RAM ? Minecraft will lag and crash.
     * Put too much RAM ?
     * The GC will lag, android won't be able to breathe properly.
     * @param ctx Context needed to get the total memory of the device.
     * @return The best default value found.
     */
    private fun findBestRAMAllocation(ctx: Context): Int {
        val deviceRam = Tools.getTotalDeviceMemory(ctx)
        if (deviceRam < 1024) return 296
        if (deviceRam < 1536) return 448
        if (deviceRam < 2048) return 656
        // Limit the max for 32 bits devices more harshly
        if (Architecture.is32BitsDevice()) return 696

        if (deviceRam < 3064) return 936
        if (deviceRam < 4096) return 1144
        if (deviceRam < 6144) return 1536
        return 2048 //Default RAM allocation for 64 bits
    }

    /** Find a correct resolution for the device
     * 
     * Some devices are shipped with a ridiculously high resolution, which can cause performance issues
     * This function will try to find a resolution that is good enough for the device */
    private fun findBestResolution(context: Context, isDevicePowerful: Boolean): Int {
        val metrics = context.getResources().getDisplayMetrics()
        val minSide = min(metrics.widthPixels, metrics.heightPixels)
        val targetSide = if (isDevicePowerful) 1080 else 720
        if (minSide <= targetSide) return 100 // No need to scale down


        val ratio = (100f * targetSide / minSide)
        // The value must match the seekbar values
        val increment = context.getResources().getInteger(R.integer.resolution_seekbar_increment)
        return (ceil((ratio / increment).toDouble()) * increment).toInt()
    }

    /** Check if the device is considered powerful.
     * Powerful devices will have some energy saving tweaks enabled by default */
    private fun isDevicePowerful(context: Context): Boolean {
        if (VERSION.SDK_INT < VERSION_CODES.Q) return false
        if (Tools.getTotalDeviceMemory(context) <= 4096) return false
        val metrics = context.getResources().getDisplayMetrics()
        if (min(metrics.widthPixels, metrics.heightPixels) < 1080) return false
        if (Runtime.getRuntime().availableProcessors() <= 4) return false
        if (hasAllCoreSameFreq()) return false
        return true
    }

    private fun hasAllCoreSameFreq(): Boolean {
        val coreCount = Runtime.getRuntime().availableProcessors()
        try {
            val freq0 = Tools.read("/sys/devices/system/cpu/cpu0/cpufreq/cpuinfo_max_freq")
            val freqX =
                Tools.read("/sys/devices/system/cpu/cpu" + (coreCount - 1) + "/cpufreq/cpuinfo_max_freq")
            if (freq0 == freqX) return true
        } catch (e: IOException) {
            Log.e("LauncherPreferences", "Failed to read CPU frequencies", e)
        }
        return false
    }

    /** Check if the device has a display cutout  */
    fun hasNotch(activity: Activity): Boolean {
        if (VERSION.SDK_INT < VERSION_CODES.P) return false
        try {
            val cutout: Rect
            if (VERSION.SDK_INT >= VERSION_CODES.S) {
                cutout = activity.getWindowManager().getCurrentWindowMetrics().getWindowInsets()
                    .getDisplayCutout()?.getBoundingRects()?.get(0) ?: Rect()
            } else {
                cutout =
                    activity.getWindow().getDecorView().getRootWindowInsets()?.getDisplayCutout()
                        ?.getBoundingRects()?.get(0) ?: Rect()
            }
            return cutout.width() != 0 || cutout.height() != 0
        } catch (e: Exception) {
            Log.i("NOTCH DETECTION", "No notch detected, or the device if in split screen mode")
            return false
        }
    }
}
