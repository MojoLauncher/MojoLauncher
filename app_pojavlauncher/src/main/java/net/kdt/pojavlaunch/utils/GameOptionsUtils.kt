package net.kdt.pojavlaunch.utils

import android.util.Log
import net.kdt.pojavlaunch.prefs.LauncherPreferences

object GameOptionsUtils {
    /**
     * Parse an integer. If the input value is null or not a valid integer, return the default value.
     * @param value the String to parse
     * @param defaultValue the default value
     * @return the parsed value or default
     */
    fun parseIntDefault(value: String?, defaultValue: Int): Int {
        if (value == null) return defaultValue
        try {
            return value.toInt()
        } catch (e: NumberFormatException) {
            return defaultValue
        }
    }

    /**
     * Decrease cloud rendering distance in order to avoid the Mali cloud rendering slowdown bug
     */
    private fun fixDeathCloud() {
        val info = GLInfoUtils.glInfo
        if (info?.isArm != true) return  // Not an affected GPU

        val cloudRange = parseIntDefault(MCOptionUtils.get("cloudRange"), 128)
        if (cloudRange <= 64) return  // Not affected below 117 (but let's err on the safe side)

        MCOptionUtils.set("cloudRange", "64")
    }

    /**
     * Disable the Narrator. Clicking on the button, even though it says "Not Supported", turns it
     * on and causes MC to generate insanely large log files when starting again
     */
    private fun disableNarrator() {
        if (parseIntDefault(MCOptionUtils.get("narrator"), 0) == 0) return
        MCOptionUtils.set("narrator", "0")
    }

    /**
     * Disable fullscreen. The launcher runs always in fullscreen anyway, and this
     * helps with some mods that can't tolerate an empty video mode list
     */
    private fun disableFullscreen() {
        val fullscreen = MCOptionUtils.get("fullscreen")
        if (fullscreen == null) return
        if (fullscreen == "true") MCOptionUtils.set("fullscreen", "false")
        else if (fullscreen == "1") MCOptionUtils.set("fullscreen", "0")
    }

    /**
     * Set the preferred graphics backend based on the selected renderer and user preference
     * @param renderer the selected renderer
     */
    private fun fixGraphicsBackend(renderer: String) {
        var backend = LauncherPreferences.PREF_PREFERRED_GRAPHICS_BACKEND
        // Force vulkan if using zink renderer
        if (renderer == "vulkan_zink") {
            backend = "vulkan"
        }
        MCOptionUtils.set("preferredGraphicsBackend", "\"" + backend + "\"")
    }

    fun fixOptions(gameDir: String, isLtw: Boolean, renderer: String) {
        try {
            MCOptionUtils.load(gameDir)
        } catch (e: Exception) {
            Log.e("Tools", "Failed to load config", e)
        }

        if (isLtw) fixDeathCloud()
        disableFullscreen()
        disableNarrator()
        fixGraphicsBackend(renderer)

        try {
            MCOptionUtils.save()
        } catch (e: Exception) {
            Log.e("Tools", "Failed to save config", e)
        }
    }
}
