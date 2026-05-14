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
        Log.v("jrelog","Log starts here");
        new Thread(new Runnable(){
            int failTime = 0;
            ProcessBuilder logcatPb;
            @Override
            public void run() {
                try {
                    if (logcatPb == null) {
                        // No filtering by tag anymore as that relied on incorrect log levels set in log.h
                        logcatPb = new ProcessBuilder().command("logcat", /* "-G", "1mb", */ "-v", "brief", "-s", "jrelog", "LIBGL", "NativeInput").redirectErrorStream(true);
                    }

                    Log.i("jrelog-logcat","Clearing logcat");
                    new ProcessBuilder().command("logcat", "-c").redirectErrorStream(true).start();
                    Log.i("jrelog-logcat","Starting logcat");
                    java.lang.Process p = logcatPb.start();

                    byte[] buf = new byte[1024];
                    int len;
                    while ((len = p.getInputStream().read(buf)) != -1) {
                        String currStr = new String(buf, 0, len);
                        Logger.appendToLog(currStr);
                    }

                    if (p.waitFor() != 0) {
                        Log.e("jrelog-logcat", "Logcat exited with code " + p.exitValue());
                        failTime++;
                        Log.i("jrelog-logcat", (failTime <= 10 ? "Restarting logcat" : "Too many restart fails") + " (attempt " + failTime + "/10");
                        if (failTime <= 10) {
                            run();
                        } else {
                            Logger.appendToLog("ERROR: Unable to get more log.");
                        }
                    }
                } catch (Throwable e) {
                    Log.e("jrelog-logcat", "Exception on logging thread", e);
                    Logger.appendToLog("Exception on logging thread:\n" + Log.getStackTraceString(e));
                }
            }
        }).start();
        Log.i("jrelog-logcat","Logcat thread started");

    }

    private static void overrideEnvVars(Map<String, String> envMap) throws IOException {
        File customEnvFile = new File(Tools.DIR_GAME_HOME, "custom_env.txt");
        if(!customEnvFile.exists() || !customEnvFile.isFile()) return;
        BufferedReader reader = new BufferedReader(new FileReader(customEnvFile));
        String line;
        while ((line = reader.readLine()) != null) {
            // Not use split() as only split first one
            int index = line.indexOf("=");
            envMap.put(line.substring(0, index), line.substring(index + 1));
        }
        reader.close();
    }

    // Sets up ANGLE driver environment
    public static void setupAngleEnv(Context ctx, Map<String, String> envMap) {
        if (!LauncherPreferences.PREF_USE_ANGLE) return;
        LibraryPlugin angle = LibraryPlugin.discoverPlugin(ctx, LibraryPlugin.ID_ANGLE_PLUGIN);
        if (angle == null) return;
        String[] angleLibs = {"libEGL_angle.so", "libGLESv2_angle.so"};
        if (!angle.checkLibraries(angleLibs)) {
            Log.e("AngleEnvSetup", "AnglePlugin exists, but the ANGLE libraries are not present. Is the plugin corrupted?");
            return;
        }
        envMap.put("LIBGL_EGL", angle.resolveAbsolutePath(angleLibs[0]));
        envMap.put("LIBGL_GLES", angle.resolveAbsolutePath(angleLibs[1]));
    }

    public static void setupFfmpegEnv(Context ctx, Map<String, String> envMap) {
        LibraryPlugin ffmpeg = LibraryPlugin.discoverPlugin(ctx, LibraryPlugin.ID_FFMPEG_PLUGIN);
        if(ffmpeg == null) return;
        envMap.put("POJAV_FFMPEG_PATH", ffmpeg.resolveAbsolutePath("libffmpeg.so"));
    }

    // Setup environment for mesa-based renderers
    public static void setupRendererEnv(Map<String, String> envMap, String renderer) {
        switch(renderer) {
            case "vulkan_zink":
                envMap.put("GALLIUM_DRIVER", "zink");
                envMap.put("MESA_LOADER_DRIVER_OVERRIDE", "zink");
                // HACK: GLSL version override for Mesa-based renderers (i.e. Zink)
                // Required to run the game properly on some mobile Vulkan drivers (Minecraft fails to compile shaders without)
                envMap.put("MESA_GLSL_VERSION_OVERRIDE", "460");
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
    public static void setEnviroimentForGame(Context context, String renderer) throws Throwable {
        Map<String, String> envMap = new ArrayMap<>();
        envMap.put("LIBGL_MIPMAP", "3");

        // Prevent OptiFine (and other error-reporting stuff in Minecraft) from balooning the log
        envMap.put("LIBGL_NOERROR", "1");

        // On certain GLES drivers, overloading default functions shader hack fails, so disable it
        envMap.put("LIBGL_NOINTOVLHACK", "1");

        // Fix white color on banner and sheep, since GL4ES 1.1.5
        envMap.put("LIBGL_NORMALIZE", "1");

        if(PREF_DUMP_SHADERS)
            envMap.put("LIBGL_VGPU_DUMP", "1");
        if(PREF_VSYNC_IN_ZINK)
            envMap.put("POJAV_VSYNC_IN_ZINK", "1");

        // The OPEN GL version is changed according
        envMap.put("LIBGL_ES", (String) ExtraCore.getValue(ExtraConstants.OPEN_GL_VERSION));

        envMap.put("FORCE_VSYNC", String.valueOf(LauncherPreferences.PREF_FORCE_VSYNC));

        envMap.put("MESA_GLSL_CACHE_DIR", Tools.DIR_CACHE.getAbsolutePath());
        envMap.put("force_glsl_extensions_warn", "true");
        envMap.put("allow_higher_compat_version", "true");
        envMap.put("allow_glsl_extension_directive_midshader", "true");
		// This is currently required for YSM mod to function
		File modRuntimeDir = new File(Tools.DIR_CACHE, "app_runtime_mod");
		if (!modRuntimeDir.exists()) {
    		modRuntimeDir.mkdirs();
		}
		envMap.put("MOD_ANDROID_RUNTIME", modRuntimeDir.getAbsolutePath());

        if(!renderer.equals("opengles2")) { // Don't enable ANGLE for GL4ES for now (it's currently broken)
            setupAngleEnv(context, envMap);
        }
        setupFfmpegEnv(context, envMap);
        setupRendererEnv(envMap, renderer);

        // HACK
        envMap.put("POJAV_NATIVEDIR", Tools.NATIVE_LIB_DIR);
        envMap.put("EGL_PLATFORM", "android");

        if(LauncherPreferences.PREF_BIG_CORE_AFFINITY) envMap.put("POJAV_BIG_CORE_AFFINITY", "1");

        if(GLInfoUtils.getGlInfo().isAdreno() && !PREF_ZINK_PREFER_SYSTEM_DRIVER) {
            envMap.put("POJAV_LOAD_TURNIP", "1");
        }

        overrideEnvVars(envMap);

        for (Map.Entry<String, String> env : envMap.entrySet()) {
            Logger.appendToLog("Added custom env: " + env.getKey() + "=" + env.getValue());
            try {
                Os.setenv(env.getKey(), env.getValue(), true);
            }catch (NullPointerException exception){
                Log.e("JREUtils", exception.toString());
            }
        }
    }

    public static void launchJavaVM(final AppCompatActivity activity, final Runtime runtime, File gameDirectory, final List<String> JVMArgs, final String userArgsString) throws Throwable {

        // Force LWJGL to use the Freetype library intended for it, instead of using the one
        // that we ship with Java (since it may be older than what's needed)
        //
        Tools.fullyExit();
    }

    /**
     * Parse and separate java arguments in a user friendly fashion
     * It supports multi line and absence of spaces between arguments
     * The function also supports auto-removal of improper arguments, although it may miss some.
     *
     * @param args The un-parsed argument list.
     * @return Parsed args as an ArrayList
     */
    public static ArrayList<String> parseJavaArguments(String args){
        ArrayList<String> parsedArguments = new ArrayList<>(0);
        args = args.trim().replace(" ", "");
        //For each prefixes, we separate args.
        String[] separators = new String[]{"-XX:-","-XX:+", "-XX:","--", "-D", "-X", "-javaagent:", "-verbose"};
        for(String prefix : separators){
            while (true){
                int start = args.indexOf(prefix);
                if(start == -1) break;
                //Get the end of the current argument by checking the nearest separator
                int end = -1;
                for(String separator: separators){
                    int tempEnd = args.indexOf(separator, start + prefix.length());
                    if(tempEnd == -1) continue;
                    if(end == -1){
                        end = tempEnd;
                        continue;
                    }
                    end = Math.min(end, tempEnd);
                }
                //Fallback
                if(end == -1) end = args.length();

                //Extract it
                String parsedSubString = args.substring(start, end);
                args = args.replace(parsedSubString, "");

                //Check if two args aren't bundled together by mistake
                if(parsedSubString.indexOf('=') == parsedSubString.lastIndexOf('=')) {
                    int arraySize = parsedArguments.size();
                    if(arraySize > 0){
                        String lastString = parsedArguments.get(arraySize - 1);
                        // Looking for list elements
                        if(lastString.charAt(lastString.length() - 1) == ',' ||
                                parsedSubString.contains(",")){
                            parsedArguments.set(arraySize - 1, lastString + parsedSubString);
                            continue;
                        }
                    }
                    parsedArguments.add(parsedSubString);
                }
                else Log.w("JAVA ARGS PARSER", "Removed improper arguments: " + parsedSubString);
            }
        }
        return parsedArguments;
    }

    /**
     * Open the render library in accordance to the settings.
     * It will fallback if it fails to load the library.
     * @return The name of the loaded library
     */
    public static String loadGraphicsLibrary(String renderer){
        String renderLibrary;
        boolean useGles;
        boolean bypassNamespace = false;
        boolean preloadVk = true;
        int glesVersion;
        switch (renderer){
            case "freedreno_kgsl":
                preloadVk = false;
            case "vulkan_zink":
                renderLibrary = "libEGL_mesa.so";
                useGles = false;
                bypassNamespace = true; // Mesa is linked to a bunch of libraries not available in the pojavexec namespace
                glesVersion = 3;
                if(preloadVk) preloadVulkan(); // Zink requires Vulkan library to be preloaded
                break;
            case "opengles3_ltw" :
                renderLibrary = "libltw.so";
                useGles = true;
                glesVersion = 3;
                break;
            case "opengles2":
            case "opengles2_5":
            case "opengles3":
            default:
                renderLibrary = "libgl4es_114.so";
                useGles = true;
                glesVersion = Integer.parseInt((String) ExtraCore.getValue(ExtraConstants.OPEN_GL_VERSION));
                break;
        }

        if (!configureRenderspec(renderLibrary, bypassNamespace, useGles, glesVersion)) {
            Log.e("RENDER_LIBRARY","Failed to load renderer " + renderLibrary );
            return null;
        }
        return renderLibrary;
    }

    public static int getDetectedVersion() {
        return GLInfoUtils.getGlInfo().glesMajorVersion;
    }
    public static native int chdir(String path);

    public static native void setLdLibraryPath(String ldLibraryPath);
    public static native boolean configureRenderspec(String eglPath, boolean useLoaderBypass, boolean useGles, int glesVersion);
    public static native void preloadVulkan();
    //public static native void initializeHooks();
    // Obtain AWT screen pixels to render on Android SurfaceView
    public static native boolean renderAWTScreenFrame(ByteBuffer tempBuffer);
    static {
        System.loadLibrary("pojavexec");
        System.loadLibrary("pojavexec_awt");
    }
}
