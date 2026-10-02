package net.kdt.pojavlaunch.utils;

import android.util.Log;

public class GameOptionsUtils {
    /**
     * Parse an integer. If the input value is null or not a valid integer, return the default value.
     * @param value the String to parse
     * @param defaultValue the default value
     * @return the parsed value or default
     */
    public static int parseIntDefault(String value, int defaultValue) {
        if(value == null) return defaultValue;
        try {
            return Integer.parseInt(value);
        }catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    /**
     * Decrease cloud rendering distance in order to avoid the Mali cloud rendering slowdown bug
     */
    private static void fixDeathCloud() {
        GLInfoUtils.GLInfo info = GLInfoUtils.getGlInfo();
        if(!info.isArm()) return; // Not an affected GPU
        int cloudRange = parseIntDefault(MCOptionUtils.get("cloudRange"), 128);
        if(cloudRange <= 64) return; // Not affected below 117 (but let's err on the safe side)
        MCOptionUtils.set("cloudRange", "64");
    }

    /**
     * Disable the Narrator. Clicking on the button, even though it says "Not Supported", turns it
     * on and causes MC to generate insanely large log files when starting again
     */
    private static void disableNarrator() {
        if(parseIntDefault(MCOptionUtils.get("narrator"), 0) == 0) return;
        MCOptionUtils.set("narrator", "0");
    }

    /**
     * Disable fullscreen. The launcher runs always in fullscreen anyway, and this
     * helps with some mods that can't tolerate an empty video mode list
     */
    private static void disableFullscreen() {
        String fullscreen = MCOptionUtils.get("fullscreen");
        if(fullscreen == null) return;
        if(fullscreen.equals("true")) MCOptionUtils.set("fullscreen", "false");
        else if(fullscreen.equals("1")) MCOptionUtils.set("fullscreen","0");
    }

    public static void fixOptions(boolean isLtw) {
        try {
            MCOptionUtils.load();
        }catch (Exception e) {
            Log.e("Tools", "Failed to load config", e);
        }

        if(isLtw) fixDeathCloud();
        disableFullscreen();
        disableNarrator();

        try {
            MCOptionUtils.save();
        }catch (Exception e) {
            Log.e("Tools", "Failed to save config", e);
        }
    }
}
