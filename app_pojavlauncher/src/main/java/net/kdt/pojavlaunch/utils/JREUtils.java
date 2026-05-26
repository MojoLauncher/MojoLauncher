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

                    Log.i("jrelog-logcat", "Clearing logcat");
                    new ProcessBuilder().command("logcat", "-c")
                            .redirectErrorStream(true).start();

                    Log.i("jrelog-logcat", "Starting logcat");
                    java.lang.Process p = logcatPb.start();

                    byte[] buf = new byte[1024];
                    int len;

                    while ((len = p.getInputStream().read(buf)) != -1) {
                        String currStr = new String(buf, 0, len);
                        Logger.appendToLog(currStr);
                    }

                    if (p.waitFor() != 0) {
                        Log.e("jrelog-logcat",
                                "Logcat exited with code " + p.exitValue());

                        failTime++;
                        if (failTime <= 10) {
                            run();
                        } else {
                            Logger.appendToLog("ERROR: Unable to get more log.");
                        }
                    }

                } catch (Throwable e) {
                    Log.e("jrelog-logcat",
                            "Exception on logging thread", e);

                    Logger.appendToLog(
                            "Exception on logging thread:\n" +
                                    Log.getStackTraceString(e)
                    );
                }
            }
        }).start();

        Log.i("jrelog-logcat", "Logcat thread started");
    }

    private static void overrideEnvVars(Map<String, String> envMap)
            throws IOException {

        File customEnvFile = new File(
                Tools.DIR_GAME_HOME, "custom_env.txt"
        );

        if (!customEnvFile.exists() || !customEnvFile.isFile()) return;

        BufferedReader reader =
                new BufferedReader(new FileReader(customEnvFile));

        String line;
        while ((line = reader.readLine()) != null) {
            int index = line.indexOf("=");
            if (index == -1) continue;

            envMap.put(
                    line.substring(0, index),
                    line.substring(index + 1)
            );
        }

        reader.close();
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

    public static void setEnvironmentForGame(Context context,
                                             String renderer)
            throws Throwable {

        Map<String, String> envMap = new ArrayMap<>();

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

        envMap.put(
                "LIBGL_ES",
                (String) ExtraCore.getValue(
                        ExtraConstants.OPEN_GL_VERSION
                )
        );

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
            modRuntimeDir.mkdirs();
        }

        envMap.put("MOD_ANDROID_RUNTIME",
                modRuntimeDir.getAbsolutePath());

        if (!renderer.equals("opengles2")) {
            setupAngleEnv(context, envMap);
        }

        setupFfmpegEnv(context, envMap);

        envMap.put("MOJO_RENDERER", renderer);

        if (renderer.equals("mobileglues")) {
            envMap.put("LIBGL_GLES", "libmobileglues.so");
            envMap.put("MOBILEGLUES_INFO_GETTER",
                    "libmobileglues_info_getter.so");

            envMap.put("LIBGL_ES", "3");

            // Correct MobileGlues values to fix Mali multi_draw crash
            envMap.put("MG_multidrawMode", "1"); 
            envMap.put("MG_enableNoError", "1");
            envMap.put("MG_enableExtComputeShader", "0");
            envMap.put("MG_enableExtTimerQuery", "0");
            envMap.put("MG_enableExtDirectStateAccess", "0");

            // FIX: Point explicitly to system EGL framework so pojavInitOpenGL binds cleanly
            envMap.put("POJAVEXEC_EGL", "libEGL.so");
            envMap.remove("MESA_GL_VERSION_OVERRIDE");
            envMap.remove("MESA_GLSL_VERSION_OVERRIDE");
        }
        
        if (renderer.equals("opengles3_ltw")) {
            envMap.put("POJAVEXEC_EGL", "libltw.so");
        }
        
        if (LauncherPreferences.PREF_BIG_CORE_AFFINITY)
            envMap.put("POJAV_BIG_CORE_AFFINITY", "1");

        if (GLInfoUtils.getGlInfo().isAdreno()
                && !PREF_ZINK_PREFER_SYSTEM_DRIVER) {
            envMap.put("POJAV_LOAD_TURNIP", "1");
        }

        overrideEnvVars(envMap);

        for (Map.Entry<String, String> env : envMap.entrySet()) {
            Logger.appendToLog(
                    "Added custom env: " +
                            env.getKey() + "=" + env.getValue()
            );

            try {
                Os.setenv(env.getKey(), env.getValue(), true);
            } catch (NullPointerException exception) {
                Log.e("JREUtils", exception.toString());
            }
        }

        // ====================================================================
        // CRITICAL FIX: Native libraries are dynamically loaded ONLY after
        // the environment hooks are systematically exported into the shell system layer.
        // ====================================================================
        try {
            System.loadLibrary("exithook");
            System.loadLibrary("pojavexec");
            System.loadLibrary("pojavexec_awt");
            Log.i("JREUtils", "Native hook modules bound successfully with system properties.");
        } catch (UnsatisfiedLinkError linkError) {
            Log.e("JREUtils", "Failed loading platform runtime libraries: " + linkError.getMessage());
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

        // FIX: Removed global blank replacement execution to preserve parameters containing spaces
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
