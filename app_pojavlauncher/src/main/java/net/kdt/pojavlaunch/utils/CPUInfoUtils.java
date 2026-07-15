package net.kdt.pojavlaunch.utils;

import android.os.Build;

import java.util.HashMap;
import java.util.Map;

public class CPUInfoUtils {

    private static final Map<String, String> VENDOR_MAP = new HashMap<>();
    private static final Map<String, String> BRAND_MAP = new HashMap<>();

    static {
        // Vendor (manufacturer) map
        VENDOR_MAP.put("QTI", "Qualcomm(R)");
        // More to come

        BRAND_MAP.put("SM8750", "Snapdragon(TM) 8 Elite");
        BRAND_MAP.put("SM8650", "Snapdragon(TM) 8 Gen 3");
        BRAND_MAP.put("SM8550", "Snapdragon(TM) 8 Gen 2");
        BRAND_MAP.put("SM8450", "Snapdragon(TM) 8 Gen 1");
        // same
    }

    public static String getVendor(){
        return Build.VERSION.SDK_INT < 31 ? null : VENDOR_MAP.getOrDefault(Build.SOC_MANUFACTURER, Build.SOC_MANUFACTURER);
    }
    public static String getName(){
        return Build.VERSION.SDK_INT < 31 ? Build.BOARD : BRAND_MAP.getOrDefault(Build.SOC_MODEL, Build.SOC_MODEL);
    }
    public static String getRawVendor(){
        return Build.VERSION.SDK_INT < 31 ? null : Build.SOC_MANUFACTURER;
    }
    public static String getRawName(){
        return Build.VERSION.SDK_INT < 31 ? Build.BOARD : Build.SOC_MODEL;
    }
    public static String getFullString(){
        String vendor = getVendor();
        String name = getName();
        return vendor == null ? name : vendor + " " + name;
    }
}
