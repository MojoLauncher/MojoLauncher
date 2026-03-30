package net.kdt.pojavlaunch.utils;

import android.content.Context;

import net.kdt.pojavlaunch.Tools;
import net.kdt.pojavlaunch.prefs.LauncherPreferences;

public class PerformancePreset {
    public enum Mode {
        POTATO, NORMAL, EXTREME
    }

    public static void applyPreset(Context context, Mode mode) {
        int deviceRam = Tools.getTotalDeviceMemory(context);
        switch (mode) {
            case POTATO:
                LauncherPreferences.PREF_RAM_ALLOCATION = 512;
                // Other settings if needed
                break;
            case NORMAL:
                LauncherPreferences.PREF_RAM_ALLOCATION = (int) (deviceRam * 0.35);
                break;
            case EXTREME:
                LauncherPreferences.PREF_RAM_ALLOCATION = (int) (deviceRam * 0.55);
                break;
        }
        LauncherPreferences.DEFAULT_PREF.edit().putInt("allocation", LauncherPreferences.PREF_RAM_ALLOCATION).apply();
    }
}