package net.kdt.pojavlaunch.utils;

import static net.kdt.pojavlaunch.prefs.LauncherPreferences.PREF_DUMP_SHADERS;
import static net.kdt.pojavlaunch.prefs.LauncherPreferences.PREF_VSYNC_IN_ZINK;
import static net.kdt.pojavlaunch.prefs.LauncherPreferences.PREF_ZINK_PREFER_SYSTEM_DRIVER;

import android.content.*;
import android.system.*;
import android.util.*;

import androidx.appcompat.app.AppCompatActivity;

import java.io.*;
import java.nio.ByteBuffer;
import java.util.*;

import net.kdt.pojavlaunch.*;
import net.kdt.pojavlaunch.extra.ExtraConstants;
import net.kdt.pojavlaunch.extra.ExtraCore;
import net.kdt.pojavlaunch.multirt.Runtime;
import net.kdt.pojavlaunch.plugins.LibraryPlugin;
import net.kdt.pojavlaunch.prefs.*;

public class JREUtils {

    private static final String TAG = "JREUtils";
    private static final String TAG_LOGCAT = "jrelog-logcat";
    private static final String TAG_MOBILEGLUES = "MobileGlues";

    public static void redirectAndPrintJRELog() {
        Log.v("jrelog", "Log starts here");

        new Thread(new Runnable() {
            int failTime = 0;
            ProcessBuilder logcatPb;

            @Override
            public void run() {
                try {
                    if (logcatPb == null) {
                        logcatPb = new ProcessBuilder().command(
                                "logcat", "-v", "brief", "-s",
                                "jrelog", "LIBGL", "NativeInput"
                        ).redirectErrorStream(true);
                    }

                    Log.i(TAG_LOGCAT, "Clearing logcat");
                    new ProcessBuilder().command("logcat", "-c")
                            .redirectErrorStream(true).start();

                    Log.i(TAG_LOGCAT, "Starting logcat");
                    java.lang.Process p = logcatPb.start();

                    byte[] buf = new byte[1024];
                    int len;

                    while ((len = p.getInputStream().read(buf)) != -1) {
                        String currStr = new String(buf, 0, len);
                        Logger.appendToLog(currStr);
                    }

                    if (p.waitFor() != 0) {
                        Log.e(TAG_LOGCAT,
                                "Logcat exited with code " + p.exitValue());

                        failTime++;
                        if (failTime <= 10) {
                            run();
                        } else {
                            Logger.appendToLog("ERROR: Unable to get more log.");
                        }
                    }

                } catch (Throwable e) {
                    Log.e(TAG_LOGCAT,
                            "Exception on logging thread", e);

                    Logger.appendToLog(
                            "Exception on logging thread:\n" +
                                    Log.getStackTraceString(e)
                    );
                }
            }
        }).start();

        Log.i(TAG_LOGCAT, "Logcat thread started");
    }

    private static void overrideEnvVars(Map<String, String> envMap)
            throws IOException {

        File customEnvFile = new File(
                Tools.DIR_GAME_HOME, "custom_env.txt"
        );

        if (!customEnvFile.exists() || !customEnvFile.isFile()) return;

        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new FileReader(customEnvFile));

            String line;
            while ((line = reader.readLine()) != null) {
                int index = line.indexOf("=");
                if (index == -1) continue;

                envMap.put(
                        line.substring(0, index),
                        line.substring(index + 1)
                );
            }
        } finally {
            if (reader != null) {
                reader.close();
            }
        }
    }

    public static void setupAngleEnv(Context ctx,
                                     Map<String, String> envMap) {
        if (!LauncherPreferences.PREF_USE_ANGLE) return;

        LibraryPlugin angle = LibraryPlugin.discoverPlugin(
                ctx, LibraryPlugin.ID_ANGLE_PLUGIN
        );

        if (angle == null) return;

        String[] angleLibs = {
                "libEGL_angle.so",
                "libGLESv2_angle.so"
        };

        if (!angle.checkLibraries(angleLibs)) {
            Log.e("AngleEnvSetup",
                    "ANGLE libraries are missing.");
            return;
        }

        envMap.put("LIBGL_EGL",
                angle.resolveAbsolutePath(angleLibs[0]));

        envMap.put("LIBGL_GLES",
                angle.resolveAbsolutePath(angleLibs[1]));
    }

    public static void setupFfmpegEnv(Context ctx,
                                      Map<String, String> envMap) {
        LibraryPlugin ffmpeg = LibraryPlugin.discoverPlugin(
                ctx, LibraryPlugin.ID_FFMPEG_PLUGIN
        );

        if (ffmpeg == null) return;

        envMap.put(
                "POJAV_FFMPEG_PATH",
                ffmpeg.resolveAbsolutePath("libffmpeg.so")
        );
    }

    /**
     * Setup MobileGlues renderer environment variables.
     * Fixes Mali GPU multi-draw crashes and EGL binding issues.
     */
    private static void setupMobileGluesEnv(Map<String, String> envMap) {
        Log.i(TAG_MOBILEGLUES, "Configuring MobileGlues environment");

        try {
            // Step 1: Set immediate environment variable before native library loading
            // This ensures the custom wrapper layer is available to native hooks
            Os.setenv("POJAVEXEC_EGL", "libmobileglues.so", true);
            Logger.appendToLog("MOBILEGLUES: Set POJAVEXEC_EGL=libmobileglues.so (immediate)");
            Log.d(TAG_MOBILEGLUES, "Immediate POJAVEXEC_EGL export successful");
        } catch (Exception e) {
            Log.e(TAG_MOBILEGLUES, "Failed to set immediate POJAVEXEC_EGL: " + e.getMessage());
            Logger.appendToLog("MOBILEGLUES ERROR: Failed immediate export - " + e.getMessage());
        }

        // Step 2: Configure MobileGlues-specific environment variables
        envMap.put("MOBILEGLUES_INFO_GETTER", "libmobileglues_info_getter.so");
        Logger.appendToLog("MOBILEGLUES: Set MOBILEGLUES_INFO_GETTER=libmobileglues_info_getter.so");

        // Step 3: Apply Mali GPU fixes for multi-draw compatibility
        envMap.put("MG_multidrawMode", "1");
        envMap.put("MG_enableNoError", "1");
        envMap.put("MG_enableExtComputeShader", "0");
        envMap.put("MG_enableExtTimerQuery", "0");
        envMap.put("MG_enableExtDirectStateAccess", "0");
        Logger.appendToLog("MOBILEGLUES: Applied Mali GPU multi-draw fixes");

        // Step 4: Bind to system EGL framework for proper OpenGL initialization
        // This prevents POJAVEXEC_EGL conflicts and ensures pojavInitOpenGL succeeds
        envMap.put("POJAVEXEC_EGL", "libEGL.so");
        envMap.remove("MESA_GL_VERSION_OVERRIDE");
        envMap.remove("MESA_GLSL_VERSION_OVERRIDE");
        Logger.appendToLog("MOBILEGLUES: Set final POJAVEXEC_EGL=libEGL.so");

        Log.i(TAG_MOBILEGLUES, "MobileGlues environment configured successfully");
    }

    public static void setEnvironmentForGame(Context context,
                                             String renderer)
            throws Throwable {

        Map<String, String> envMap = new ArrayMap<>();

        // Base OpenGL configuration
        envMap.put("LIBGL_MIPMAP", "3");
        envMap.put("LIBGL_NOERROR", "1");
        envMap.put("LIBGL_NOINTOVLHACK", "1");
        envMap.put("LIBGL_NORMALIZE", "1");

        if (PREF_DUMP_SHADERS)
            envMap.put("LIBGL_VGPU_DUMP", "1");

        if (PREF_VSYNC_IN_ZINK)
            envMap.put("POJAV_VSYNC_IN_ZINK", "1");

        if (Tools.deviceHasHangingLinker())
            envMap.put("POJAV_EMUI_ITERATOR_MITIGATE", "1");

        String glVersion = (String) ExtraCore.getValue(ExtraConstants.OPEN_GL_VERSION);
        if (glVersion != null) {
            envMap.put("LIBGL_ES", glVersion);
        }

        @SuppressWarnings("deprecation")
        String forceVsyncVal = String.valueOf(LauncherPreferences.PREF_FORCE_VSYNC);
        envMap.put("FORCE_VSYNC", forceVsyncVal);

        envMap.put("MESA_GLSL_CACHE_DIR",
                Tools.DIR_CACHE.getAbsolutePath());

        envMap.put("force_glsl_extensions_warn", "true");
        envMap.put("allow_higher_compat_version", "true");
        envMap.put("allow_glsl_extension_directive_midshader", "true");

        File modRuntimeDir = new File(
                Tools.DIR_CACHE, "app_runtime_mod"
        );

        if (!modRuntimeDir.exists()) {
            if (!modRuntimeDir.mkdirs()) {
                Log.w(TAG, "Failed to create mod runtime directory");
            }
        }

        envMap.put("MOD_ANDROID_RUNTIME",
                modRuntimeDir.getAbsolutePath());

        // Setup renderer-specific configurations
        if (!renderer.equals("opengles2")) {
            setupAngleEnv(context, envMap);
        }

        setupFfmpegEnv(context, envMap);

        envMap.put("MOJO_RENDERER", renderer);

        // Renderer-specific setup
        if (renderer.equals("mobileglues")) {
            setupMobileGluesEnv(envMap);
        } else if (renderer.equals("opengles3_ltw")) {
            envMap.put("POJAVEXEC_EGL", "libltw.so");
            Logger.appendToLog("Set POJAVEXEC_EGL=libltw.so for OpenGLES 3 LTW");
        }

        // GPU-specific optimizations
        if (LauncherPreferences.PREF_BIG_CORE_AFFINITY) {
            envMap.put("POJAV_BIG_CORE_AFFINITY", "1");
            Logger.appendToLog("Enabled big core affinity optimization");
        }

        if (GLInfoUtils.getGlInfo().isAdreno()
                && !PREF_ZINK_PREFER_SYSTEM_DRIVER) {
            envMap.put("POJAV_LOAD_TURNIP", "1");
            Logger.appendToLog("Enabled Turnip driver for Adreno GPU");
        }

        // Apply custom environment overrides
        try {
            overrideEnvVars(envMap);
        } catch (IOException e) {
            Log.e(TAG, "Failed to load custom environment variables: " + e.getMessage());
            Logger.appendToLog("ERROR: Failed to load custom_env.txt - " + e.getMessage());
        }

        // Export all environment variables to system
        int successCount = 0;
        int failCount = 0;

        for (Map.Entry<String, String> env : envMap.entrySet()) {
            String key = env.getKey();
            String value = env.getValue();

            Logger.appendToLog("Set env: " + key + "=" + value);

            try {
                Os.setenv(key, value, true);
                successCount++;
            } catch (NullPointerException e) {
                Log.e(TAG, "NullPointerException setting " + key + ": " + e.getMessage());
                failCount++;
            } catch (Exception e) {
                Log.e(TAG, "Exception setting " + key + ": " + e.getMessage());
                failCount++;
            }
        }

        Log.i(TAG, "Environment variables set: " + successCount + " success, " + failCount + " failed");
        Logger.appendToLog("Environment export complete: " + successCount + " set, " + failCount + " failed");

        // ====================================================================
        // CRITICAL: Load native libraries AFTER environment is fully configured
        // This ensures all environment hooks are available to native code
        // ====================================================================
        loadNativeLibraries();
    }

    /**
     * Load native libraries required for Java/OpenGL integration.
     * Must be called AFTER all environment variables are set.
     */
    private static void loadNativeLibraries() {
        String[] nativeLibs = {"exithook", "pojavexec", "pojavexec_awt"};
        int loaded = 0;
        int failed = 0;

        Log.i(TAG, "Loading native libraries...");
        Logger.appendToLog("Loading native runtime libraries");

        for (String libName : nativeLibs) {
            try {
                System.loadLibrary(libName);
                Log.i(TAG, "Successfully loaded: " + libName);
                Logger.appendToLog("Loaded: " + libName);
                loaded++;
            } catch (UnsatisfiedLinkError e) {
                Log.e(TAG, "Failed to load " + libName + ": " + e.getMessage());
                Logger.appendToLog("ERROR: Failed to load " + libName);
                failed++;
            }
        }

        if (failed == 0) {
            Log.i(TAG, "All native libraries loaded successfully");
            Logger.appendToLog("Native runtime binding successful");
        } else {
            Log.w(TAG, "Failed to load " + failed + " native libraries");
            Logger.appendToLog("WARNING: " + failed + " native libraries failed to load");
        }
    }

    public static void launchJavaVM(
            final AppCompatActivity activity,
            final Runtime runtime,
            File gameDirectory,
            final List<String> JVMArgs,
            final String userArgsString
    ) throws Throwable {
        Tools.fullyExit();
    }

    public static ArrayList<String> parseJavaArguments(String args) {
        ArrayList<String> parsedArguments = new ArrayList<>(0);
        if (args == null || args.trim().isEmpty()) return parsedArguments;

        // FIX: Removed global blank replacement to preserve space-containing parameters
        args = args.trim();

        String[] separators = new String[]{
                "-XX:-", "-XX:+", "-XX:", "--",
                "-D", "-X", "-javaagent:", "-verbose"
        };

        for (String prefix : separators) {
            while (true) {
                int start = args.indexOf(prefix);
                if (start == -1) break;

                int end = -1;

                for (String separator : separators) {
                    int tempEnd =
                            args.indexOf(separator,
                                    start + prefix.length());

                    if (tempEnd == -1) continue;

                    if (end == -1) {
                        end = tempEnd;
                        continue;
                    }

                    end = Math.min(end, tempEnd);
                }

                if (end == -1) end = args.length();

                String parsedSubString =
                        args.substring(start, end);

                args = args.replace(parsedSubString, "");

                if (parsedSubString.indexOf('=')
                        == parsedSubString.lastIndexOf('=')) {
                    parsedArguments.add(parsedSubString.trim());
                }
            }
        }

        return parsedArguments;
    }

    public static String loadGraphicsLibrary(String renderer) {
        String renderLibrary;

        switch (renderer) {
            case "opengles2":
            case "opengles2_5":
            case "opengles3":
                renderLibrary = "libgl4es_114.so";
                break;

            case "vulkan_zink":
                renderLibrary = "libOSMesa.so";
                break;

            case "opengles3_ltw":
                renderLibrary = "libltw.so";
                break;

            case "mobileglues":
                renderLibrary = "libmobileglues.so";
                break;

            default:
                Log.w("RENDER_LIBRARY",
                        "No renderer selected, defaulting to opengles2");
                renderLibrary = "libgl4es_114.so";
                break;
        }

        if (!dlopen(renderLibrary)) {
            Log.e("RENDER_LIBRARY",
                    "Failed to load renderer " + renderLibrary);
            return null;
        }

        Log.i("RENDER_LIBRARY", "Successfully loaded renderer: " + renderLibrary);
        return renderLibrary;
    }

    public static int getDetectedVersion() {
        return GLInfoUtils.getGlInfo().glesMajorVersion;
    }

    public static native int chdir(String path);
    public static native boolean dlopen(String libPath);
    public static native void setLdLibraryPath(String ldLibraryPath);
    public static native void setupBridgeWindow(Object surface);
    public static native void releaseBridgeWindow();
    public static native void applyWindowSize();
    public static native void initializeHooks();

    public static native boolean renderAWTScreenFrame(
            ByteBuffer tempBuffer
    );
}
