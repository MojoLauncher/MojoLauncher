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
                envMap.put("GALLIUM_DRIVER", "zink");
                envMap.put("MESA_LOADER_DRIVER_OVERRIDE", "zink");
                // HACK: GLSL version override for Mesa-based renderers (i.e. Zink)
                // Required to run the game properly on some mobile Vulkan drivers (Minecraft fails to compile shaders without)
                envMap.put("MESA_GLSL_VERSION_OVERRIDE", "460");
                if(!Architecture.isx86Device() && (LauncherPreferences.PREF_ZINK_FORCE_LEGACY || GLInfoUtils.getGlInfo().isArm())) {
                    zink = LibraryPlugin.discoverPlugin(context, LibraryPlugin.ID_ZINK_PLUGIN);
                    if(zink == null) return;
                    // Mali additionally wants this
                    envMap.put("MESA_GL_VERSION_OVERRIDE", "3.3");
                }
                break;
            case "freedreno_kgsl":
                if(GLInfoUtils.getGlInfo().isAdreno()) {
                    envMap.put("MESA_LOADER_DRIVER_OVERRIDE", "kgsl");
                    // On Adreno 5XX and lower only Core 3.1 is exposed by default due to missing hardware extensions.
                    // 3.3 is required for modern Minecraft so let's force 3.3 if running on such GPU - it's known to be working.
                    if(GLInfoUtils.getGlInfo().isAdreno500Lower()) {
                        envMap.put("MESA_GL_VERSION_OVERRIDE", "3.3");
                        envMap.put("MESA_GLSL_VERSION_OVERRIDE", "330");
                    }
                }
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
        if (LauncherPreferences.PREF_ZINK_FORCE_LEGACY || GLInfoUtils.getGlInfo().isArm()) {
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
        if ((LauncherPreferences.PREF_ZINK_FORCE_LEGACY || GLInfoUtils.getGlInfo().isArm()) && zink != null)
            return zink.getLibraryPath();
        return null;
    }
}
