package net.kdt.pojavlaunch.plugins;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.util.Log;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

public class LibraryPlugin {
    private static final String TAG = "LibraryPlugin";

    // Known plugins constants
    public static final String ID_ANGLE_PLUGIN = "git.mojo.angle";
    public static final String ID_FFMPEG_PLUGIN = "git.mojo.ffmpeg";

    private static Map<String, LibraryPlugin> loadedPlugins = new HashMap<>();
    private String appId;
    private String libraryPath;
    private LibraryPlugin(String app, String libraryPath){
        this.appId = app;
        this.libraryPath = libraryPath;
    }

    public static LibraryPlugin discoverPlugin(Context ctx, String appId){
        // Do not recreate plugin instance if it was discovered before
       if(loadedPlugins.containsKey(appId))
           return loadedPlugins.get(appId);
        Log.i(TAG, "Discovering plugin " + appId);
        String libraryPath;
        try {
            PackageInfo pluginPackage = ctx.getPackageManager().getPackageInfo(appId, PackageManager.GET_SHARED_LIBRARY_FILES);
            libraryPath = pluginPackage.applicationInfo.nativeLibraryDir;

        } catch (Exception e){
            Log.e(TAG, "Plugin discover failed: " + e.getMessage());
            return null;
        }
       LibraryPlugin plugin = new LibraryPlugin(appId, libraryPath);
       loadedPlugins.put(appId, plugin);
       return plugin;
    }
    public static LibraryPlugin getPlugin(String appId){
        return loadedPlugins.get(appId);
    }
    public static boolean isAvailable(String appId){
        return loadedPlugins.get(appId) != null;
    }

    public String getId(){
        return appId;
    }

    public String getLibraryPath(){
        return libraryPath;
    }
    public String resolveAbsolutePath(String library) {
        return new File(libraryPath, library).getAbsolutePath();
    }

    public boolean checkLibraries(String... libs){
        for(String lib : libs){
            if(!(new File(libraryPath, lib).exists())) return false;
        }
        return true;
    }
}
