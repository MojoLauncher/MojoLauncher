package net.kdt.pojavlaunch.utils;

import android.content.Context;

import net.kdt.pojavlaunch.Architecture;
import net.kdt.pojavlaunch.plugins.LibraryPlugin;
import net.kdt.pojavlaunch.prefs.LauncherPreferences;

import java.util.Map;

/**
 * Utils for Mesa-based renderers (zink, freedreno)
 */
public class MesaUtils {

    public static final String MESA_EGL = "libEGL_mesa.so";
    public static final String MESA_EGL_LEGACY = "libEGL_legacy.so";

    private static LibraryPlugin zink;
    /**
     * Setup environment for the mesa-based renderers. Does nothing if the renderer is not Mesa
     * @param context context
     * @param renderer selected renderer
     * @param envMap environment map
     */
    public static void initEnvironment(Context context, String renderer, Map<String, String> envMap){
        switch(renderer) {
            case "vulkan_zink":

                if(!Architecture.isx86Device() && (LauncherPreferences.PREF_ZINK_FORCE_LEGACY || GpuUtils.getGlInfo().isArm())) {
                    zink = LibraryPlugin.discoverPlugin(context, LibraryPlugin.ID_ZINK_PLUGIN);
                    if(zink == null) return;
                    // Mali additionally wants this
                    envMap.put("MESA_GL_VERSION_OVERRIDE", "3.3");
                }
                break;
            case "freedreno_kgsl":

                break;
        }
    }

    /**
     * Destroy zink plugin instance created during environment init if exists
     */
    public static void destroyZink(){
        if(zink != null) {
            zink = null;
            System.gc();
        }
    }

    /**
     * Get preferred Mesa EGL library - picks legacy Mesa library on ARM (or if forced) if ZINK plugin is installed
     * @return
     */
    public static String getPreferredEGL() {
        if (LauncherPreferences.PREF_ZINK_FORCE_LEGACY || GpuUtils.getGlInfo().isArm()) {
            if (zink == null) return MESA_EGL;
            if (!zink.checkLibraries(MESA_EGL_LEGACY)) return MESA_EGL;
            return zink.resolveAbsolutePath(MESA_EGL_LEGACY);
        } else return MESA_EGL;
    }

    /**
     * Get ZINK plugin library path
     * @return library path string
     */
    public static String getCustomZinkLibraryPath() {
        if ((LauncherPreferences.PREF_ZINK_FORCE_LEGACY || GpuUtils.getGlInfo().isArm()) && zink != null)
            return zink.getLibraryPath();
        return null;
    }
}
