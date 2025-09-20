package net.kdt.pojavlaunch.plugins;

import android.content.Context;
import android.util.Log;

import net.kdt.pojavlaunch.utils.JREUtils;

public class MGPlugin {
    public static LibraryPlugin mgPlugin;
    public static String MG_PATH;

    public static LibraryPlugin initialize(Context context){
        mgPlugin = LibraryPlugin.discoverPlugin(context, "com.fcl.plugin.mobileglues");
        if(mgPlugin != null)
            MG_PATH = mgPlugin.resolveAbsolutePath("libmobileglues.so");
        return mgPlugin;
    }
    public static void load(){
        if(mgPlugin == null) return;
        if(!JREUtils.dlopen(mgPlugin.resolveAbsolutePath("libspirv-cross-c-shared.so"))){
            Log.e("MG", "Failed dlopening SPVX!");
        }
        if(!JREUtils.dlopen(mgPlugin.resolveAbsolutePath("libshaderconv.so"))){
            Log.e("MG", "Failed dlopening shaderconv!");
        }
    }
}
