package net.kdt.pojavlaunch.utils.jre;

import android.util.ArrayMap;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import net.kdt.pojavlaunch.Architecture;
import net.kdt.pojavlaunch.JMinecraftVersionList;
import net.kdt.pojavlaunch.Tools;
import net.kdt.pojavlaunch.authenticator.accounts.MinecraftAccount;
import net.kdt.pojavlaunch.instances.Instance;
import net.kdt.pojavlaunch.lifecycle.LifecycleAwareAlertDialog;
import net.kdt.pojavlaunch.multirt.MultiRTUtils;
import net.kdt.pojavlaunch.multirt.Runtime;
import net.kdt.pojavlaunch.prefs.LauncherPreferences;
import net.kdt.pojavlaunch.utils.DateUtils;
import net.kdt.pojavlaunch.utils.FileUtils;
import net.kdt.pojavlaunch.utils.GLInfoUtils;
import net.kdt.pojavlaunch.utils.JREUtils;
import net.kdt.pojavlaunch.utils.JSONUtils;
import net.kdt.pojavlaunch.utils.MCOptionUtils;
import net.kdt.pojavlaunch.utils.OldVersionsUtils;
import net.kdt.pojavlaunch.utils.RendererCompatUtil;
import net.kdt.pojavlaunch.value.DependentLibrary;

import org.lwjgl.glfw.CallbackBridge;

import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;

import git.artdeell.mojo.R;

public class GameRunner {
    /**
     * Optimization mods based on Sodium can mitigate the render distance issue. Check if Sodium
     * or its derivative is currently installed to skip the render distance check.
     * @param gameDir current game directory
     * @return whether sodium or a sodium-based mod is installed
     */
    private static boolean hasSodium(File gameDir) {
        File modsDir = new File(gameDir, "mods");
        File[] mods = modsDir.listFiles(file -> file.isFile() && file.getName().endsWith(".jar"));
        if(mods == null) return false;
        for(File file : mods) {
            String name = file.getName();
            if(name.contains("sodium") ||
                    name.contains("embeddium") ||
                    name.contains("rubidium")) return true;
        }
        return false;
    }

    private static int parseIntDefault(String value, int defaultValue) {
        try {
            return Integer.parseInt(value);
        }catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    /**
     * Initialize OpenGL and do checks to see if the GPU of the device is affected by the render
     * distance issue.

     * Currently only checks whether the user has an Adreno GPU capable of OpenGL ES 3.

     * This issue is caused by a very severe limit on the amount of GL buffer names that could be allocated
     * by the Adreno properietary GLES driver.

     * @return whether the GPU is affected by the Large Thin Wrapper render distance issue on vanilla
     */
    private static boolean affectedByRenderDistanceIssue() {
        GLInfoUtils.GLInfo info = GLInfoUtils.getGlInfo();
        return info.isAdreno() && info.glesMajorVersion >= 3;
    }

    private static boolean checkRenderDistance(File gamedir) {
        if(!affectedByRenderDistanceIssue()) return false;
        if(hasSodium(gamedir)) return false;
        try {
            MCOptionUtils.load();
        }catch (Exception e) {
            Log.e("Tools", "Failed to load config", e);
        }
        int renderDistance = parseIntDefault(MCOptionUtils.get("renderDistance"),12);
        // 7 is the render distance "magic number" above which MC creates too many buffers
        // for Adreno's OpenGL ES implementation
        return renderDistance > 7;
    }

    /**
     * Decrease clound rendering distance in order to avoid the Mali cloud rendering slowdown bug
     */
    private static void fixDeathCloud() {
        GLInfoUtils.GLInfo info = GLInfoUtils.getGlInfo();
        if(!info.isArm()) return; // Not an affected GPU
        try {
            MCOptionUtils.load();
        }catch (Exception e) {
            Log.e("Tools", "Failed to load config", e);
        }
        int cloudRange = parseIntDefault(MCOptionUtils.get("cloudRange"), 128);
        if(cloudRange <= 64) return; // Not affected below 117 (but let's err on the safe side)
        try {
            MCOptionUtils.set("cloudRange", "64");
            MCOptionUtils.save();
        }catch (Exception e) {
            Log.e("Tools", "Failed to save config", e);
        }
    }

    private static boolean isGl4esCompatible(JMinecraftVersionList.Version version) throws Exception{
        return DateUtils.dateBefore(DateUtils.getOriginalReleaseDate(version), 2025, 1, 7);
    }

    private static boolean isCompatContext(JMinecraftVersionList.Version version) throws Exception{
        // Day before the release date of 21w10a, the first OpenGL 3 Core Minecraft version
        return DateUtils.dateBefore(DateUtils.getOriginalReleaseDate(version), 2021, 3, 9);
    }

    private static boolean showDialog(AppCompatActivity activity, int message) throws InterruptedException {
        LifecycleAwareAlertDialog.DialogCreator dialogCreator = ((alertDialog, dialogBuilder) ->
                dialogBuilder.setMessage(activity.getString(message))
                        .setCancelable(false)
                        .setPositiveButton(android.R.string.ok, (d, w)->{}));
        return LifecycleAwareAlertDialog.haltOnDialog(activity.getLifecycle(), activity, dialogCreator);
    }

    public static void launchMinecraft(final AppCompatActivity activity, MinecraftAccount minecraftAccount,
                                       Instance instance, String versionId, String rendererName) throws Throwable {
        int freeDeviceMemory = Tools.getFreeDeviceMemory(activity);
        int localeString;
        int freeAddressSpace = Architecture.is32BitsDevice() ? Tools.getMaxContinuousAddressSpaceSize() : -1;
        Log.i("MemStat", "Free RAM: " + freeDeviceMemory + " Addressable: " + freeAddressSpace);
        if(freeDeviceMemory > freeAddressSpace && freeAddressSpace != -1) {
            freeDeviceMemory = freeAddressSpace;
            localeString = R.string.address_memory_warning_msg;
        } else {
            localeString = R.string.memory_warning_msg;
        }

        if(LauncherPreferences.PREF_RAM_ALLOCATION > freeDeviceMemory) {
            int finalDeviceMemory = freeDeviceMemory;
            LifecycleAwareAlertDialog.DialogCreator dialogCreator = (dialog, builder) ->
                builder.setMessage(activity.getString(localeString, finalDeviceMemory, LauncherPreferences.PREF_RAM_ALLOCATION))
                        .setPositiveButton(android.R.string.ok, (d, w)->{});

            if(LifecycleAwareAlertDialog.haltOnDialog(activity.getLifecycle(), activity, dialogCreator)) {
                return; // If the dialog's lifecycle has ended, return without
                // actually launching the game, thus giving us the opportunity
                // to start after the activity is shown again
            }
        }
        File gamedir = instance.getGameDirectory();
        JMinecraftVersionList.Version versionInfo = Tools.getVersionInfo(versionId);

        // Switch renderer to GL4ES when running a compat context version on LTW
        if(isCompatContext(versionInfo) && rendererName.equals("opengles3_ltw")) {
            instance.renderer = rendererName = "opengles2";
            instance.write();
        }

        // Switch renderer to LTW when running 1.21.5
        boolean ltwSupported = RendererCompatUtil.getCompatibleRenderers(activity).rendererIds.contains("opengles3_ltw");
        if(!isGl4esCompatible(versionInfo) && rendererName.equals("opengles2")) {
            if(ltwSupported) {
                instance.renderer = rendererName = "opengles3_ltw";
                instance.write();
            }else {
                showDialog(activity, R.string.compat_version_not_supported);
                System.exit(0);
                return;
            }
        }
        RendererCompatUtil.releaseRenderersCache();

        if(rendererName.equals("opengles3_ltw") && checkRenderDistance(gamedir)) {
            if(showDialog(activity, R.string.ltw_render_distance_warning_msg)) return;
            // If the code goes here, it means that the user clicked "OK". Fix the render distance.
            try {
                MCOptionUtils.set("renderDistance", "7");
                MCOptionUtils.save();
            }catch (Exception e) {
                Log.e("Tools", "Failed to fix render distance setting", e);
            }
        }

        if(rendererName.equals("opengles3_ltw")) fixDeathCloud();

        if(rendererName.equals("opengles3_ltw") && GLInfoUtils.getGlInfo().forcedMsaa) {
            if(showDialog(activity, R.string.ltw_4x_msaa_warning_msg)) return;
        }

        int requiredJavaVersion = 8;
        if(versionInfo.javaVersion != null) requiredJavaVersion = versionInfo.javaVersion.majorVersion;

        // Minecraft 1.13+
        CallbackBridge.nativeSetUseInputStackQueue(versionInfo.arguments != null);

        Runtime runtime = MultiRTUtils.forceReread(pickRuntime(instance, requiredJavaVersion));

        // Pre-process specific files
        disableSplash(gamedir);
        List<String> launchArgs = getMinecraftClientArgs(minecraftAccount, versionInfo, gamedir);

        // Select the appropriate openGL version
        OldVersionsUtils.selectOpenGlVersion(versionInfo);

        List<String> launchClassPath = generateLaunchClassPath(versionInfo, versionId);

        List<String> javaArgList = new ArrayList<>();

        if (versionInfo.logging != null && versionInfo.logging.client != null && versionInfo.logging.client.file != null) {
            String configFile = Tools.DIR_DATA + "/security/" + versionInfo.logging.client.file.id.replace("client", "log4j-rce-patch");
            if (!new File(configFile).exists()) {
                configFile = Tools.DIR_GAME_NEW + "/" + versionInfo.logging.client.file.id;
            }
            javaArgList.add("-Dlog4j.configurationFile=" + configFile);
        }

        File versionSpecificNativesDir = new File(Tools.DIR_CACHE, "natives/"+versionId);
        if(versionSpecificNativesDir.exists()) {
            String dirPath = versionSpecificNativesDir.getAbsolutePath();
            javaArgList.add("-Djava.library.path="+dirPath+":"+Tools.NATIVE_LIB_DIR);
            javaArgList.add("-Djna.boot.library.path="+dirPath);
        }

        addAuthlibInjectorArgs(javaArgList, minecraftAccount);

        javaArgList.addAll(getMinecraftJVMArgs(versionId));

        javaArgList.addAll(JREUtils.parseJavaArguments(instance.getLaunchArgs()));

        JREUtils.setEnviroimentForGame(activity, rendererName);
        JREUtils.chdir(instance.getGameDirectory().getAbsolutePath());

        String rendererLibrary = JREUtils.loadGraphicsLibrary(rendererName);
        if(rendererLibrary == null) {
            Log.i("GameRunner", "Falling back to GL4ES 1.1.4");
            rendererName = "opengles2";
            rendererLibrary = JREUtils.loadGraphicsLibrary(rendererName);
        }
        if(rendererLibrary == null) {
            if(showDialog(activity, R.string.gr_err_renderer_load_Failed)) return;
            System.exit(0);
        }
        javaArgList.add("-Dorg.lwjgl.opengl.libname="+rendererLibrary);
        javaArgList.add("-Dorg.lwjgl.freetype.libname="+ Tools.NATIVE_LIB_DIR+"/libfreetype.so");

        activity.runOnUiThread(() -> Toast.makeText(activity, activity.getString(R.string.autoram_info_msg,LauncherPreferences.PREF_RAM_ALLOCATION), Toast.LENGTH_SHORT).show());

        try {
            JavaRunner.nativeSetupExit(activity);
            JavaRunner.startJvm(runtime, javaArgList, launchClassPath, versionInfo.mainClass, launchArgs);
        }catch (VMLoadException e) {
            LifecycleAwareAlertDialog.DialogCreator dialogCreator = (dialog, builder) ->
                builder.setMessage(e.toString(activity)).setPositiveButton(android.R.string.ok, (d, w)->{});

            if(LifecycleAwareAlertDialog.haltOnDialog(activity.getLifecycle(), activity, dialogCreator)) {
                return;
            }
        }

        Tools.fullyExit();
    }

    private static void disableSplash(File dir) {
        File configDir = new File(dir, "config");
        if(FileUtils.ensureDirectorySilently(configDir)) {
            File forgeSplashFile = new File(dir, "config/splash.properties");
            String forgeSplashContent = "enabled=true";
            try {
                if (forgeSplashFile.exists()) {
                    forgeSplashContent = Tools.read(forgeSplashFile.getAbsolutePath());
                }
                if (forgeSplashContent.contains("enabled=true")) {
                    Tools.write(forgeSplashFile.getAbsolutePath(),
                            forgeSplashContent.replace("enabled=true", "enabled=false"));
                }
            } catch (IOException e) {
                Log.w(Tools.APP_NAME, "Could not disable Forge 1.12.2 and below splash screen!", e);
            }
        } else {
            Log.w(Tools.APP_NAME, "Failed to create the configuration directory");
        }
    }

    private static void addAuthlibInjectorArgs(List<String> javaArgList, MinecraftAccount minecraftAccount) {
        String injectorUrl = minecraftAccount.authType.injectorUrl;
        if(injectorUrl == null) return;
        javaArgList.add("-javaagent:"+Tools.DIR_DATA+"/authlib-injector/authlib-injector.jar="+injectorUrl);
    }

    private static List<String> getMinecraftJVMArgs(String versionName) {
        JMinecraftVersionList.Version versionInfo = Tools.getVersionInfo(versionName, true);
        // Parse Forge 1.17+ additional JVM Arguments
        if (versionInfo.inheritsFrom == null || versionInfo.arguments == null || versionInfo.arguments.jvm == null) {
            return Collections.emptyList();
        }

        Map<String, String> varArgMap = new ArrayMap<>();
        varArgMap.put("classpath_separator", ":");
        varArgMap.put("library_directory", Tools.DIR_HOME_LIBRARY);
        varArgMap.put("version_name", versionInfo.id);
        varArgMap.put("natives_directory", Tools.NATIVE_LIB_DIR);

        List<String> minecraftArgs = new ArrayList<>();
        if (versionInfo.arguments != null) {
            for (Object arg : versionInfo.arguments.jvm) {
                if (arg instanceof String) {
                    minecraftArgs.add((String) arg);
                } //TODO: implement (?maybe?)
            }
        }
        return JSONUtils.insertJSONValueList(minecraftArgs, varArgMap);
    }

    private static List<String> getMinecraftClientArgs(MinecraftAccount profile, JMinecraftVersionList.Version versionInfo, File gameDir) {
        String username = profile.username;
        String versionName = versionInfo.id;
        if (versionInfo.inheritsFrom != null) {
            versionName = versionInfo.inheritsFrom;
        }

        String userType = "mojang";
        try {
            Date creationDate = DateUtils.getOriginalReleaseDate(versionInfo);
            // Minecraft 22w43a which adds chat reporting (and signing) was released on
            // 26th October 2022. So, if the date is not before that (meaning it is equal or higher)
            // change the userType to MSA to fix the missing signature
            if(creationDate != null && !DateUtils.dateBefore(creationDate, 2022, 9, 26)) {
                userType = "msa";
            }
        }catch (ParseException e) {
            Log.e("CheckForProfileKey", "Failed to determine profile creation date, using \"mojang\"", e);
        }


        Map<String, String> varArgMap = new ArrayMap<>();
        varArgMap.put("auth_session", profile.accessToken); // For legacy versions of MC
        varArgMap.put("auth_access_token", profile.accessToken);
        varArgMap.put("auth_player_name", username);
        varArgMap.put("auth_uuid", profile.profileId.replace("-", ""));
        varArgMap.put("auth_xuid", profile.xuid);
        varArgMap.put("assets_root", Tools.ASSETS_PATH);
        varArgMap.put("assets_index_name", versionInfo.assets);
        varArgMap.put("game_assets", Tools.ASSETS_PATH);
        varArgMap.put("game_directory", gameDir.getAbsolutePath());
        varArgMap.put("user_properties", "{}");
        varArgMap.put("user_type", userType);
        varArgMap.put("version_name", versionName);
        varArgMap.put("version_type", versionInfo.type);

        List<String> minecraftArgs = new ArrayList<>();
        if (versionInfo.arguments != null && versionInfo.arguments.game != null) {
            // Support Minecraft 1.13+
            for (Object arg : versionInfo.arguments.game) {
                if (arg instanceof String) {
                    minecraftArgs.add((String) arg);
                } //TODO: implement else clause
            }
        }
        if(versionInfo.minecraftArguments != null){
            minecraftArgs.addAll(splitAndFilterEmpty(versionInfo.minecraftArguments));
        }
        return JSONUtils.insertJSONValueList(minecraftArgs, varArgMap);
    }

    private static List<String> splitAndFilterEmpty(String argStr) {
        List<String> strList = new ArrayList<>();
        for (String arg : argStr.split(" ")) {
            if (!arg.isEmpty()) {
                strList.add(arg);
            }
        }
        return strList;
    }

    private static String getClientClasspath(String version) {
        return Tools.DIR_HOME_VERSION + "/" + version + "/" + version + ".jar";
    }

    private static List<String> generateLaunchClassPath(JMinecraftVersionList.Version info, String actualname) {
        File lwjgl3Folder = new File(Tools.DIR_GAME_HOME, "lwjgl3");
        File glfwFatJar = new File(lwjgl3Folder, "lwjgl-glfw-classes.jar");
        File lwjglxJar = new File(lwjgl3Folder, "lwjgl-lwjglx.jar");
        if(!glfwFatJar.exists() || !lwjglxJar.exists()) throw new RuntimeException("Required LWJGL3 files not found");

        ArrayList<String> classpath = new ArrayList<>(info.libraries.length + 3);
        // LWJGL3 comes first - must override any custom LWJGL3 on the classpath
        classpath.add(glfwFatJar.getAbsolutePath());
        // Custom version libraries are inbetween
        boolean usesLWJGL3 = generateLibClasspath(info, classpath);
        // Client is last before LWJGL2 - all libraries must have higher precedence than it.
        classpath.add(getClientClasspath(actualname));
        // Don't add LWJGLX when the client doesn't use LWJGL2
        if(!usesLWJGL3) {
            // LWJGLX (custom LWJGL2) comes last - anything in the client or libs should override it
            classpath.add(lwjglxJar.getAbsolutePath());
        }
        classpath.trimToSize();
        return classpath;
    }

    private static boolean checkRules(JMinecraftVersionList.Arguments.ArgValue.ArgRules[] rules) {
        if(rules == null) return true; // always allow
        for (JMinecraftVersionList.Arguments.ArgValue.ArgRules rule : rules) {
            if (rule.action.equals("allow") && rule.os != null && rule.os.name.equals("osx")) {
                return false; //disallow
            }
        }
        return true; // allow if none match
    }

    /**
     * "Carve out" the version out of a Maven library name
     * @param fullMavenName the full library name
     * @return the library name without the version
     */
    private static String trimLibVersion(String fullMavenName) {
        int first = fullMavenName.indexOf(':');
        if(first == -1) return fullMavenName;
        int second = fullMavenName.indexOf(':', first + 1);
        if(second == -1) return fullMavenName;
        int third = fullMavenName.indexOf(':', second + 1);
        if(third != -1) {
            return fullMavenName.substring(0, second + 1) + fullMavenName.substring(third);
        } else {
            return fullMavenName.substring(0, second + 1);
        }
    }

    /** @return true when LWJGL3 is in use **/
    public static boolean generateLibClasspath(JMinecraftVersionList.Version info, List<String> target) {
        ArrayMap<String, String> libraries = new ArrayMap<>();
        boolean usesLWJGL3 = false;
        for (DependentLibrary libItem : info.libraries) {
            if(libItem.name.startsWith("org.lwjgl:lwjgl:3.")) usesLWJGL3 = true;
            if(!checkRules(libItem.rules) || Tools.shouldSkipLibrary(libItem)) continue;
            File library = new File(Tools.DIR_HOME_LIBRARY, Tools.artifactToPath(libItem));
            if(!library.exists()) continue;
            String name = trimLibVersion(libItem.name);
            // If the lib list has both asm-all and normal asm, something is either terribly wrong
            // or it's just babric. Let's hope for the latter
            if(name.equals("org.ow2.asm:asm:")) {
                libraries.remove("org.ow2.asm:asm-all:");
            }
            libraries.put(name, library.getAbsolutePath());
        }
        target.addAll(libraries.values());
        return usesLWJGL3;
    }

    public static @NonNull String pickRuntime(Instance instance, int targetJavaVersion) {
        String runtime = Tools.getSelectedRuntime(instance);
        String profileRuntime = instance.selectedRuntime;
        Runtime pickedRuntime = MultiRTUtils.read(runtime);
        if(runtime == null || pickedRuntime.javaVersion == 0 || pickedRuntime.javaVersion < targetJavaVersion) {
            String preferredRuntime = MultiRTUtils.getNearestJreName(targetJavaVersion);
            if(preferredRuntime == null) throw new RuntimeException("Failed to autopick runtime!");
            if(profileRuntime != null) {
                instance.selectedRuntime = preferredRuntime;
                instance.maybeWrite();
            }
            runtime = preferredRuntime;
        }
        return runtime;
    }
}
