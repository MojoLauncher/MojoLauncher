package net.kdt.pojavlaunch.utils;

import android.content.Context;
import android.util.Log;

import net.kdt.pojavlaunch.Architecture;
import net.kdt.pojavlaunch.plugins.LibraryPlugin;
import net.kdt.pojavlaunch.prefs.LauncherPreferences;

import java.io.File;
import java.util.Map;

/**
 * Environment utils for some renderers (zink, freedreno, ANGLE)
 */
public class RenderEnvUtils {

    public static final String MESA_EGL = "libEGL_mesa.so";
    public static final String MESA_EGL_LEGACY = "libEGL_legacy.so";
    public static final String ANGLE_EGL = "libEGL_angle.so";
    public static final String ANGLE_OPENGL = "libGLESv2_angle.so";


    private static LibraryPlugin zink;
    private static LibraryPlugin angle;

    /**
     * Setup environment for the mesa-based renderers. Does nothing if the renderer is not Mesa
     *
     * @param context  context
     * @param renderer selected renderer
     * @param envMap   environment map
     */
    public static void setupMesaEnv(Context context, String renderer, Map<String, String> envMap) {
        switch (renderer) {
            case "vulkan_zink":
                envMap.put("GALLIUM_DRIVER", "zink");
                envMap.put("MESA_LOADER_DRIVER_OVERRIDE", "zink");
                // HACK: GLSL version override for Mesa-based renderers (i.e. Zink)
                // Required to run the game properly on some mobile Vulkan drivers (Minecraft fails to compile shaders without)
                envMap.put("MESA_GLSL_VERSION_OVERRIDE", "460");
                if (!Architecture.isx86Device() && (LauncherPreferences.PREF_ZINK_FORCE_LEGACY || GLInfoUtils.getGlInfo().isArm())) {
                    zink = LibraryPlugin.discoverPlugin(context, LibraryPlugin.ID_ZINK_PLUGIN);
                    if (zink == null) return;
                    // Mali additionally wants this
                    envMap.put("MESA_GL_VERSION_OVERRIDE", "3.3");
                }
                break;
            case "freedreno_kgsl":
                if (GLInfoUtils.getGlInfo().isAdreno()) {
                    envMap.put("MESA_LOADER_DRIVER_OVERRIDE", "kgsl");
                    // On Adreno 5XX and lower only Core 3.1 is exposed by default due to missing hardware extensions.
                    // 3.3 is required for modern Minecraft so let's force 3.3 if running on such GPU - it's known to be working.
                    if (GLInfoUtils.getGlInfo().isAdreno500Lower()) {
                        envMap.put("MESA_GL_VERSION_OVERRIDE", "3.3");
                        envMap.put("MESA_GLSL_VERSION_OVERRIDE", "330");
                    }
                }
                break;
        }
    }

    /**
     * Setup ANGLE environment for LTW/GL4ES
     *
     * @param ctx    Context
     * @param envMap current environment map
     * @return if setup succeeded
     */
    public static boolean setupAngleEnv(Context ctx, Map<String, String> envMap) {
        angle = LibraryPlugin.discoverPlugin(ctx, LibraryPlugin.ID_ANGLE_PLUGIN);
        if (angle == null) return setupSystemAngle(envMap);
        String[] angleLibs = {ANGLE_EGL, ANGLE_OPENGL};
        if (!angle.checkLibraries(angleLibs)) {
            Log.e("AngleEnvSetup", "AnglePlugin exists, but the ANGLE libraries are not present. Is the plugin corrupted?");
            return setupSystemAngle(envMap);
        }
        Log.i("JREUtils", "Using external ANGLE plugin");
        envMap.put("LIBGL_EGL", angle.resolveAbsolutePath(angleLibs[0]));
        envMap.put("LIBGL_GLES", angle.resolveAbsolutePath(angleLibs[1]));
        return true;
    }

    private static boolean setupSystemAngle(Map<String, String> envMap) {
        File[] system = getSystemAngle();
        if (system == null) return false;
        Log.i("JREUtils", "Using system-provided ANGLE");
        envMap.put("LIBGL_EGL", system[0].getAbsolutePath());
        envMap.put("LIBGL_GLES", system[1].getAbsolutePath());
        return true;
    }

    public static File[] getSystemAngle() {
        File egl = new File(Architecture.is64BitsDevice() ? "/system/lib64" : "/system/lib", ANGLE_EGL);
        File gles = new File(Architecture.is64BitsDevice() ? "/system/lib64" : "/system/lib", ANGLE_OPENGL);
        if (egl.exists() && gles.exists())
            return new File[]{egl, gles};
        else return null;
    }

    /**
     * Cleanup plugin instances created during environment init if exists
     */
    public static void cleanup() {
        if (zink != null) zink = null;
        if (angle != null) angle = null;
        System.gc();
    }

    /**
     * Get preferred Mesa EGL library - picks legacy Mesa library on ARM (or if forced) if ZINK plugin is installed
     *
     * @return EGL library
     */
    public static String getPreferredMesaEGL() {
        if (LauncherPreferences.PREF_ZINK_FORCE_LEGACY || GLInfoUtils.getGlInfo().isArm()) {
            if (zink == null) return MESA_EGL;
            if (!zink.checkLibraries(MESA_EGL_LEGACY)) return MESA_EGL;
            return zink.resolveAbsolutePath(MESA_EGL_LEGACY);
        } else return MESA_EGL;
    }

    /**
     * Get ZINK plugin library path
     *
     * @return library path string
     */
    public static String getCustomZinkLibraryPath() {
        if ((LauncherPreferences.PREF_ZINK_FORCE_LEGACY || GLInfoUtils.getGlInfo().isArm()) && zink != null)
            return zink.getLibraryPath();
        return null;
    }

    public static boolean hasAnglePlugin() {
        return angle != null;
    }
}
