package net.kdt.pojavlaunch.utils.jre;

import static net.kdt.pojavlaunch.Tools.NATIVE_LIB_DIR;

import android.content.Context;
import android.os.Build;
import android.system.ErrnoException;
import android.system.Os;
import android.util.Log;

import androidx.annotation.NonNull;

import net.kdt.pojavlaunch.AWTCanvasView;
import net.kdt.pojavlaunch.Tools;
import net.kdt.pojavlaunch.multirt.MultiRTUtils;
import net.kdt.pojavlaunch.multirt.Runtime;
import net.kdt.pojavlaunch.prefs.LauncherPreferences;
import net.kdt.pojavlaunch.utils.JREUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.ListIterator;
import java.util.Objects;
import java.util.TimeZone;

public class JavaRunner {

    private static boolean getCacioJavaArgs(List<String> javaArgList, boolean isJava8) {
        // Caciocavallo config AWT-enabled version
        javaArgList.add("-Djava.awt.headless=false");
        javaArgList.add("-Dcacio.managed.screensize=" + AWTCanvasView.AWT_CANVAS_WIDTH + "x" + AWTCanvasView.AWT_CANVAS_HEIGHT);
        javaArgList.add("-Dcacio.font.fontmanager=sun.awt.X11FontManager");
        javaArgList.add("-Dcacio.font.fontscaler=sun.font.FreetypeFontScaler");
        javaArgList.add("-Dswing.defaultlaf=javax.swing.plaf.metal.MetalLookAndFeel");
        if (isJava8) {
            javaArgList.add("-Dawt.toolkit=net.java.openjdk.cacio.ctc.CTCToolkit");
            javaArgList.add("-Djava.awt.graphicsenv=net.java.openjdk.cacio.ctc.CTCGraphicsEnvironment");
            StringBuilder cacioClasspath = createCacioClasspath();
            javaArgList.add(cacioClasspath.toString());
            return false;
        } else {
            File caciocavallo17AgentDir = new File(Tools.DIR_GAME_HOME, "caciocavallo17");
            File[] cacioJars = caciocavallo17AgentDir.listFiles((file, s) ->s.endsWith(".jar"));
            if(cacioJars == null || cacioJars.length < 1) {
                return false;
            }
            javaArgList.add("-javaagent:"+cacioJars[0].getAbsolutePath());
            javaArgList.add("-Dawt.toolkit=com.github.caciocavallosilano.cacio.ctc.CTCToolkit");
            javaArgList.add("-Djava.awt.graphicsenv=com.github.caciocavallosilano.cacio.ctc.CTCGraphicsEnvironment");

            javaArgList.add("--add-exports=java.desktop/java.awt=ALL-UNNAMED");
            javaArgList.add("--add-exports=java.desktop/java.awt.peer=ALL-UNNAMED");
            javaArgList.add("--add-exports=java.desktop/sun.awt.image=ALL-UNNAMED");
            javaArgList.add("--add-exports=java.desktop/sun.java2d=ALL-UNNAMED");
            javaArgList.add("--add-exports=java.desktop/java.awt.dnd.peer=ALL-UNNAMED");
            javaArgList.add("--add-exports=java.desktop/sun.awt=ALL-UNNAMED");
            javaArgList.add("--add-exports=java.desktop/sun.awt.event=ALL-UNNAMED");
            javaArgList.add("--add-exports=java.desktop/sun.awt.datatransfer=ALL-UNNAMED");
            javaArgList.add("--add-exports=java.desktop/sun.font=ALL-UNNAMED");
            javaArgList.add("--add-exports=java.base/sun.security.action=ALL-UNNAMED");
            javaArgList.add("--add-opens=java.base/java.util=ALL-UNNAMED");
            javaArgList.add("--add-opens=java.desktop/java.awt=ALL-UNNAMED");
            javaArgList.add("--add-opens=java.desktop/sun.font=ALL-UNNAMED");
            javaArgList.add("--add-opens=java.desktop/sun.java2d=ALL-UNNAMED");
            javaArgList.add("--add-opens=java.base/java.lang.reflect=ALL-UNNAMED");
            return true;
        }
    }

    @NonNull
    private static StringBuilder createCacioClasspath() {
        StringBuilder cacioClasspath = new StringBuilder();
        cacioClasspath.append("-Xbootclasspath/p");
        File cacioDir = new File(Tools.DIR_GAME_HOME, "caciocavallo");
        File[] cacioFiles = cacioDir.listFiles();
        if (cacioFiles != null) {
            for (File file : cacioFiles) {
                if (file.getName().endsWith(".jar")) {
                    cacioClasspath.append(":").append(file.getAbsolutePath());
                }
            }
        }
        return cacioClasspath;
    }

    /**
     *  Gives an argument list filled with both the user args
     *  and the auto-generated ones (eg. the window resolution).
     * @return A list filled with args.
     */
    private static List<String> getJavaArgs(String runtimeHome, List<String> userArguments) {
        String resolvFile;
        resolvFile = new File(Tools.DIR_DATA,"resolv.conf").getAbsolutePath();

        userArguments.add(0, "-Xms"+LauncherPreferences.PREF_RAM_ALLOCATION+"M");
        userArguments.add(0, "-Xmx"+LauncherPreferences.PREF_RAM_ALLOCATION+"M");

        ArrayList<String> overridableArguments = new ArrayList<>(Arrays.asList(
                "-Djava.home=" + runtimeHome,
                "-Djava.io.tmpdir=" + Tools.DIR_CACHE.getAbsolutePath(),
                "-Djna.boot.library.path=" + NATIVE_LIB_DIR,
                "-Duser.home=" + Tools.DIR_GAME_HOME,
                "-Duser.language=" + System.getProperty("user.language"),
                "-Dos.name=Linux",
                "-Dos.version=Android-" + Build.VERSION.RELEASE,
                "-Dpojav.path.minecraft=" + Tools.DIR_GAME_NEW,
                "-Dpojav.path.private.account=" + Tools.DIR_ACCOUNT_NEW,
                "-Duser.timezone=" + TimeZone.getDefault().getID(),

                "-Dorg.lwjgl.vulkan.libname=libvulkan.so",
                //LWJGL 3 DEBUG FLAGS
                //"-Dorg.lwjgl.util.Debug=true",
                //"-Dorg.lwjgl.util.DebugFunctions=true",
                //"-Dorg.lwjgl.util.DebugLoader=true",
                // GLFW Stub width height
                "-Dglfwstub.initEgl=false",
                "-Dext.net.resolvPath=" +resolvFile,
                "-Dlog4j2.formatMsgNoLookups=true", //Log4j RCE mitigation
                "-Dfml.earlyprogresswindow=false", //Forge 1.14+ workaround
                "-Dloader.disable_forked_guis=true",
                "-Dsodium.checks.issue2561=false",
                "-Djdk.lang.Process.launchMechanism=FORK" // Default is POSIX_SPAWN which requires starting jspawnhelper, which doesn't work on Android
        ));
        List<String> additionalArguments = new ArrayList<>();
        for(String arg : overridableArguments) {
            String strippedArg = arg.substring(0,arg.indexOf('='));
            boolean add = true;
            for(String uarg : userArguments) {
                if(uarg.startsWith(strippedArg)) {
                    add = false;
                    break;
                }
            }
            if(add)
                additionalArguments.add(arg);
            else
                Log.i("ArgProcessor","Arg skipped: "+arg);
        }

        //Add all the arguments
        userArguments.addAll(additionalArguments);
        return userArguments;
    }

    private static File getVmPath(File runtimeHomeDir, String arch, String flavor) {
        if(arch != null) return new File(runtimeHomeDir, "lib/"+arch+"/"+flavor+"/libjvm.so");
        else return new File(runtimeHomeDir, "lib/"+flavor+"/libjvm.so");
    }

    private static File findVmForArch(File runtimeHomeDir, String arch) {
        File finalPath;
        if((finalPath = getVmPath(runtimeHomeDir, arch, "server")).exists()) return finalPath;
        if((finalPath = getVmPath(runtimeHomeDir, arch, "client")).exists()) return finalPath;
        return null;
    }

    private static File findVmPath(File runtimeHomeDir, String runtimeArch) {
        File finalPath;
        if((finalPath = findVmForArch(runtimeHomeDir, null)) != null) return finalPath;
        switch (runtimeArch) {
            case "i386": case "i486": case "i586":
                if((finalPath = findVmForArch(runtimeHomeDir, "i386")) != null) return finalPath;
                if((finalPath = findVmForArch(runtimeHomeDir, "i486")) != null) return finalPath;
                if((finalPath = findVmForArch(runtimeHomeDir, "i586")) != null) return finalPath;
                break;
            default:
                if((finalPath = findVmForArch(runtimeHomeDir, runtimeArch)) != null) return finalPath;
        }
        return null;
    }

    private static void relocateLdLibPath(File vmPath, List<String> extraDirs) {
        // Java directory layout:
        // .../server/libjvm.so
        // .../libjava.so
        // and so on. Hotspot itself relies on this we also rely on this.
        File vmDir = Objects.requireNonNull(vmPath.getParentFile());
        File libsDir = Objects.requireNonNull(vmDir.getParentFile());
        StringBuilder libPathBuilder =  new StringBuilder()
                .append(libsDir.getAbsolutePath()).append(":")
                .append(NATIVE_LIB_DIR).append(':')
                .append(vmDir.getAbsolutePath()).append(':')
                .append(new File(libsDir, "jli").getAbsolutePath());

        if(extraDirs != null) for(String path : extraDirs) {
            libPathBuilder.append(':').append(path);
        }

        String ldLibPath = libPathBuilder.toString();
        try {
            Os.setenv("LD_LIBRARY_PATH", ldLibPath, true);
        }catch (ErrnoException e) {
            throw new RuntimeException(e);
        }
        JREUtils.setLdLibraryPath(ldLibPath);
    }

    private static void setImmutableEnvVars(File jreHome) {
        try {
            Os.setenv("POJAV_NATIVEDIR", NATIVE_LIB_DIR, true);
            Os.setenv("JAVA_HOME", jreHome.getAbsolutePath(), true);
            Os.setenv("HOME", Tools.DIR_GAME_HOME, true);
            Os.setenv("TMPDIR", Tools.DIR_CACHE.getAbsolutePath(), true);
        }catch (ErrnoException e) {
            throw new RuntimeException(e);
        }
    }

    private static boolean preprocessUserArgs(List<String> args) {
        ListIterator<String> iterator = args.listIterator();
        boolean hasJavaAgent = false;
        while(iterator.hasNext()) {
            String arg = iterator.next();
            switch (arg) {
                case "-p":
                    arg = "--module-path";
                case "--add-reads":
                case "--add-exports":
                case "--add-opens":
                case "--add-modules":
                case "--limit-modules":
                case "--module-path":
                case "--patch-module":
                case "--upgrade-module-path":
                    iterator.remove();
                    String argValue = iterator.next();
                    iterator.remove();
                    iterator.add(arg+"="+argValue);
                    break;
                case "-d32":
                case "-d64":
                case "-Xint":
                case "-XX:+UseTransparentHugePages":
                case "-XX:+UseLargePagesInMetaspace":
                case "-XX:+UseLargePages":
                    iterator.remove();
                    break;
                default:
                    if(arg.startsWith("-Xms") || arg.startsWith("-Xmx") || arg.startsWith("-XX:ActiveProcessorCount")) iterator.remove();
                    if(!hasJavaAgent && arg.startsWith("-javaagent:")) hasJavaAgent = true;
            }
        }
        return hasJavaAgent;
    }

    /**
     * Start the Java(tm) Virtual Machine.
     * @param runtime the Runtime that we're starting.
     * @param vmArgs the command line parameters for the virtual machine
     * @param classpathEntries the absolute path for each classpath entry
     * @param mainClass the application main class
     * @param applicationArgs the application arguments
     * @throws VMLoadException if an error occurred during VM loading
     */
    public static void startJvm(Runtime runtime, List<String> vmArgs, List<String> classpathEntries, String mainClass, List<String> applicationArgs) throws VMLoadException{
        File runtimeHomeDir = MultiRTUtils.getRuntimeHome(runtime.name);
        File vmPath = findVmPath(runtimeHomeDir, runtime.arch);
        if(vmPath == null) {
            throw new VMLoadException("Unable to find the Java VM", 0, -1);
        }

        boolean hasJavaAgent = preprocessUserArgs(vmArgs);
        List<String> runtimeArgs = new ArrayList<>();
        if(getCacioJavaArgs(runtimeArgs,runtime.javaVersion == 8)) hasJavaAgent = true;
        runtimeArgs.addAll(getJavaArgs(runtimeHomeDir.getAbsolutePath(), vmArgs));


        runtimeArgs.add("-XX:ActiveProcessorCount=" + java.lang.Runtime.getRuntime().availableProcessors());
        StringBuilder classpathBuilder = new StringBuilder().append("-Djava.class.path=");
        boolean first = true;
        for(String entry : classpathEntries) {
            if(first) first = false;
            else classpathBuilder.append(':');
            classpathBuilder.append(entry);
        }
        runtimeArgs.add(classpathBuilder.toString());

        JREUtils.initializeHooks();

        setImmutableEnvVars(runtimeHomeDir);
        relocateLdLibPath(vmPath, null);

        nativeLoadJVM(vmPath.getAbsolutePath(), runtimeArgs.toArray(new String[0]), mainClass, applicationArgs.toArray(new String[0]), hasJavaAgent);
    }

    public static native boolean nativeLoadJVM(String vmPath, String[] javaArgs, String mainClass, String[] appArgs, boolean hasJavaAgents) throws VMLoadException;
    public static native void nativeSetupExit(Context context);
}
