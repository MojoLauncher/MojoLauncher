package net.kdt.pojavlaunch.plugins;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.util.Log;

import java.io.File;

public class AnglePlugin {
    private static boolean available = false;
    private static String eglPath;
    private static String glesPath;

    public static void discover(Context ctx){
        try {
            PackageInfo anglePackage = ctx.getPackageManager().getPackageInfo("git.mojo.angle", PackageManager.GET_SHARED_LIBRARY_FILES);
            File eglPath = new File(anglePackage.applicationInfo.nativeLibraryDir, "libEGL_angle.so");
            File glesPath = new File(anglePackage.applicationInfo.nativeLibraryDir, "libGLESv2_angle.so");
            available = eglPath.exists() && glesPath.exists();
            if(available) {
                AnglePlugin.eglPath = eglPath.getAbsolutePath();
                AnglePlugin.glesPath = glesPath.getAbsolutePath();
            }
        } catch (PackageManager.NameNotFoundException e){
            Log.e("ANGLE_PLUGIN", "Failed to discover: AnglePlugin is not installed");
        } catch (NullPointerException e) {
            Log.e("ANGLE_PLUGIN", "Failed to discover: Unknown error");
        }
    }

    public static boolean isAvailable() {
        return available;
    }

    public static String getEGLPath() {
        return eglPath;
    }

    public static String getGLESPath() {
        return glesPath;
    }
}
