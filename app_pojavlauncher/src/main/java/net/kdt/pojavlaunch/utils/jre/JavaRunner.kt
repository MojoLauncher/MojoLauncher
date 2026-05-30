package net.kdt.pojavlaunch.utils.jre

import android.content.Context
import android.os.Build.VERSION
import android.system.ErrnoException
import android.system.Os
import android.util.Log
import net.kdt.pojavlaunch.AWTCanvasView
import net.kdt.pojavlaunch.Tools
import net.kdt.pojavlaunch.multirt.MultiRTUtils.getRuntimeHome
import net.kdt.pojavlaunch.multirt.Runtime
import net.kdt.pojavlaunch.prefs.LauncherPreferences
import net.kdt.pojavlaunch.utils.JREUtils
import java.io.File
import java.io.FilenameFilter
import java.util.Arrays
import java.util.Objects
import java.util.TimeZone

object JavaRunner {
    private fun getCacioJavaArgs(javaArgList: MutableList<String?>, isJava8: Boolean): Boolean {
        // Caciocavallo config AWT-enabled version
        javaArgList.add("-Djava.awt.headless=false")
        javaArgList.add("-Dcacio.managed.screensize=" + AWTCanvasView.AWT_CANVAS_WIDTH + "x" + AWTCanvasView.AWT_CANVAS_HEIGHT)
        javaArgList.add("-Dcacio.font.fontmanager=sun.awt.X11FontManager")
        javaArgList.add("-Dcacio.font.fontscaler=sun.font.FreetypeFontScaler")
        javaArgList.add("-Dswing.defaultlaf=javax.swing.plaf.metal.MetalLookAndFeel")
        if (isJava8) {
            javaArgList.add("-Dawt.toolkit=net.java.openjdk.cacio.ctc.CTCToolkit")
            javaArgList.add("-Djava.awt.graphicsenv=net.java.openjdk.cacio.ctc.CTCGraphicsEnvironment")
            val cacioClasspath = createCacioClasspath()
            javaArgList.add(cacioClasspath.toString())
            return false
        } else {
            val caciocavallo17AgentDir = File(Tools.DIR_GAME_HOME, "caciocavallo17")
            val cacioJars =
                caciocavallo17AgentDir.listFiles(FilenameFilter { file: File?, s: String? ->
                    s!!.endsWith(".jar")
                })
            if (cacioJars == null || cacioJars.size < 1) {
                return false
            }

            val bootCp = StringBuilder("-Xbootclasspath/a:")
            for (i in cacioJars.indices) {
                bootCp.append(cacioJars[i]!!.absolutePath)
                if (i < cacioJars.size - 1) bootCp.append(":")
            }
            javaArgList.add(bootCp.toString())

            javaArgList.add("-Dawt.toolkit=com.github.caciocavallosilano.cacio.ctc.CTCToolkit")
            javaArgList.add("-Djava.awt.graphicsenv=com.github.caciocavallosilano.cacio.ctc.CTCGraphicsEnvironment")

            javaArgList.add("--add-exports=java.desktop/java.awt=ALL-UNNAMED")
            javaArgList.add("--add-exports=java.desktop/java.awt.peer=ALL-UNNAMED")
            javaArgList.add("--add-exports=java.desktop/sun.awt.image=ALL-UNNAMED")
            javaArgList.add("--add-exports=java.desktop/sun.java2d=ALL-UNNAMED")
            javaArgList.add("--add-exports=java.desktop/java.awt.dnd.peer=ALL-UNNAMED")
            javaArgList.add("--add-exports=java.desktop/sun.awt=ALL-UNNAMED")
            javaArgList.add("--add-exports=java.desktop/sun.awt.event=ALL-UNNAMED")
            javaArgList.add("--add-exports=java.desktop/sun.awt.datatransfer=ALL-UNNAMED")
            javaArgList.add("--add-exports=java.desktop/sun.font=ALL-UNNAMED")
            javaArgList.add("--add-exports=java.base/sun.security.action=ALL-UNNAMED")
            javaArgList.add("--add-opens=java.base/java.util=ALL-UNNAMED")
            javaArgList.add("--add-opens=java.desktop/java.awt=ALL-UNNAMED")
            javaArgList.add("--add-opens=java.desktop/sun.font=ALL-UNNAMED")
            javaArgList.add("--add-opens=java.desktop/sun.java2d=ALL-UNNAMED")
            javaArgList.add("--add-opens=java.base/java.lang.reflect=ALL-UNNAMED")
            return false
        }
    }

    private fun createCacioClasspath(): StringBuilder {
        val cacioClasspath = StringBuilder()
        cacioClasspath.append("-Xbootclasspath/p")
        val cacioDir = File(Tools.DIR_GAME_HOME, "caciocavallo")
        val cacioFiles = cacioDir.listFiles()
        if (cacioFiles != null) {
            for (file in cacioFiles) {
                if (file.getName().endsWith(".jar")) {
                    cacioClasspath.append(":").append(file.getAbsolutePath())
                }
            }
        }
        return cacioClasspath
    }

    /**
     * Gives an argument list filled with both the user args
     * and the auto-generated ones (eg. the window resolution).
     * @return A list filled with args.
     */
    private fun getJavaArgs(
        runtimeHome: String?,
        userArguments: MutableList<String>
    ): MutableList<String> {
        val resolvFile: String?
        resolvFile = File(Tools.DIR_DATA, "resolv.conf").getAbsolutePath()

        userArguments.add(0, "-Xms" + LauncherPreferences.PREF_RAM_ALLOCATION + "M")
        userArguments.add(0, "-Xmx" + LauncherPreferences.PREF_RAM_ALLOCATION + "M")

        val overridableArguments = ArrayList<String>(
            Arrays.asList<String?>(
                "-Djava.home=" + runtimeHome,
                "-Djava.io.tmpdir=" + Tools.DIR_CACHE!!.getAbsolutePath(),
                "-Djna.boot.library.path=" + Tools.NATIVE_LIB_DIR,
                "-Duser.home=" + Tools.DIR_GAME_HOME,
                "-Duser.language=" + System.getProperty("user.language"),
                "-Dos.name=Linux",
                "-Dos.version=Android-" + VERSION.RELEASE,
                "-Dpojav.path.minecraft=" + Tools.DIR_GAME_NEW,
                "-Dpojav.path.private.account=" + Tools.DIR_ACCOUNT_NEW,
                "-Duser.timezone=" + TimeZone.getDefault().getID(),

                "-Dorg.lwjgl.vulkan.libname=libvulkan.so",  //LWJGL 3 DEBUG FLAGS
                //"-Dorg.lwjgl.util.Debug=true",
                //"-Dorg.lwjgl.util.DebugFunctions=true",
                //"-Dorg.lwjgl.util.DebugLoader=true",
                // GLFW Stub width height
                "-Dglfwstub.initEgl=false",
                "-Dext.net.resolvPath=" + resolvFile,
                "-Dlog4j2.formatMsgNoLookups=true",  //Log4j RCE mitigation
                "-Dfml.earlyprogresswindow=false",  //Forge 1.14+ workaround
                "-Dloader.disable_forked_guis=true",
                "-Dsodium.checks.issue2561=false",
                "-Djdk.lang.Process.launchMechanism=FORK" // Default is POSIX_SPAWN which requires starting jspawnhelper, which doesn't work on Android
            )
        )
        val additionalArguments: MutableList<String> = ArrayList<String>()
        for (arg in overridableArguments) {
            val strippedArg = arg.substring(0, arg.indexOf('='))
            var add = true
            for (uarg in userArguments) {
                if (uarg.startsWith(strippedArg)) {
                    add = false
                    break
                }
            }
            if (add) additionalArguments.add(arg)
            else Log.i("ArgProcessor", "Arg skipped: " + arg)
        }

        //Add all the arguments
        userArguments.addAll(additionalArguments)
        return userArguments
    }

    private fun getVmPath(runtimeHomeDir: File?, arch: String?, flavor: String?): File {
        if (arch != null) return File(runtimeHomeDir, "lib/" + arch + "/" + flavor + "/libjvm.so")
        else return File(runtimeHomeDir, "lib/" + flavor + "/libjvm.so")
    }

    private fun findVmForArch(runtimeHomeDir: File?, arch: String?): File? {
        var finalPath: File?
        if ((getVmPath(runtimeHomeDir, arch, "server").also {
                finalPath = it
            }).exists()) return finalPath
        if ((getVmPath(runtimeHomeDir, arch, "client").also {
                finalPath = it
            }).exists()) return finalPath
        return null
    }

    private fun findVmPath(runtimeHomeDir: File?, runtimeArch: String): File? {
        var finalPath: File?
        if ((findVmForArch(runtimeHomeDir, null).also { finalPath = it }) != null) return finalPath
        when (runtimeArch) {
            "i386", "i486", "i586" -> {
                if ((findVmForArch(runtimeHomeDir, "i386").also {
                        finalPath = it
                    }) != null) return finalPath
                if ((findVmForArch(runtimeHomeDir, "i486").also {
                        finalPath = it
                    }) != null) return finalPath
                if ((findVmForArch(runtimeHomeDir, "i586").also {
                        finalPath = it
                    }) != null) return finalPath
            }

            else -> if ((findVmForArch(runtimeHomeDir, runtimeArch).also {
                    finalPath = it
                }) != null) return finalPath
        }
        return null
    }

    private fun relocateLdLibPath(vmPath: File, extraDirs: MutableList<String?>?) {
        // Java directory layout:
        // .../server/libjvm.so
        // .../libjava.so
        // and so on. Hotspot itself relies on this we also rely on this.
        val vmDir = Objects.requireNonNull<File>(vmPath.getParentFile())
        val libsDir = Objects.requireNonNull<File>(vmDir.getParentFile())
        val libPathBuilder = StringBuilder()
            .append(libsDir.getAbsolutePath()).append(":")
            .append(Tools.NATIVE_LIB_DIR).append(':')
            .append(vmDir.getAbsolutePath()).append(':')
            .append(File(libsDir, "jli").getAbsolutePath())

        if (extraDirs != null) for (path in extraDirs) {
            libPathBuilder.append(':').append(path)
        }

        val ldLibPath = libPathBuilder.toString()
        try {
            Os.setenv("LD_LIBRARY_PATH", ldLibPath, true)
        } catch (e: ErrnoException) {
            throw RuntimeException(e)
        }
        JREUtils.setLdLibraryPath(ldLibPath)
    }

    private fun setImmutableEnvVars(jreHome: File) {
        try {
            Os.setenv("POJAV_NATIVEDIR", Tools.NATIVE_LIB_DIR, true)
            Os.setenv("JAVA_HOME", jreHome.getAbsolutePath(), true)
            Os.setenv("HOME", Tools.DIR_GAME_HOME, true)
            Os.setenv("TMPDIR", Tools.DIR_CACHE!!.getAbsolutePath(), true)
        } catch (e: ErrnoException) {
            throw RuntimeException(e)
        }
    }

    private fun preprocessUserArgs(args: MutableList<String>): Boolean {
        val iterator = args.listIterator()
        var hasJavaAgent = false
        while (iterator.hasNext()) {
            var arg = iterator.next()
            when (arg) {
                "-p" -> {
                    arg = "--module-path"
                    iterator.remove()
                    val argValue: String? = iterator.next()
                    iterator.remove()
                    iterator.add(arg + "=" + argValue)
                }

                "--add-reads", "--add-exports", "--add-opens", "--add-modules", "--limit-modules", "--module-path", "--patch-module", "--upgrade-module-path" -> {
                    iterator.remove()
                    val argValue: String? = iterator.next()
                    iterator.remove()
                    iterator.add(arg + "=" + argValue)
                }

                "-d32", "-d64", "-Xint", "-XX:+UseTransparentHugePages", "-XX:+UseLargePagesInMetaspace", "-XX:+UseLargePages" -> iterator.remove()
                else -> {
                    if (arg.startsWith("-Xms") || arg.startsWith("-Xmx") || arg.startsWith("-XX:ActiveProcessorCount")) iterator.remove()
                    if (!hasJavaAgent && arg.startsWith("-javaagent:")) hasJavaAgent = true
                }
            }
        }
        return hasJavaAgent
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
    @Throws(VMLoadException::class)
    fun startJvm(
        runtime: Runtime,
        vmArgs: MutableList<String>,
        classpathEntries: MutableList<String?>,
        mainClass: String?,
        applicationArgs: MutableList<String?>
    ) {
        val normalizedMainClass = mainClass?.trim()
        if (normalizedMainClass.isNullOrEmpty()) {
            throw VMLoadException("Main class is missing from version metadata", 6, -1)
        }

        val runtimeHomeDir = getRuntimeHome(runtime.name)
        val vmPath = JavaRunner.findVmPath(runtimeHomeDir, runtime.arch!!)
        if (vmPath == null) {
            throw VMLoadException("Unable to find the Java VM", 0, -1)
        }

        var hasJavaAgent = preprocessUserArgs(vmArgs)
        val runtimeArgs: MutableList<String?> = ArrayList<String?>()
        if (getCacioJavaArgs(runtimeArgs, runtime.javaVersion == 8)) hasJavaAgent = true
        runtimeArgs.addAll(getJavaArgs(runtimeHomeDir.getAbsolutePath(), vmArgs))


        runtimeArgs.add(
            "-XX:ActiveProcessorCount=" + java.lang.Runtime.getRuntime().availableProcessors()
        )
        val classpathBuilder = StringBuilder().append("-Djava.class.path=")
        var first = true
        for (entry in classpathEntries) {
            if (first) first = false
            else classpathBuilder.append(':')
            classpathBuilder.append(entry)
        }
        runtimeArgs.add(classpathBuilder.toString())

        JREUtils.initializeHooks()

        setImmutableEnvVars(runtimeHomeDir)
        relocateLdLibPath(vmPath, null)

        nativeLoadJVM(
            vmPath.getAbsolutePath(),
            runtimeArgs.toTypedArray<String?>(),
            normalizedMainClass,
            applicationArgs.toTypedArray<String?>(),
            hasJavaAgent
        )
    }

    @Throws(VMLoadException::class)
    external fun nativeLoadJVM(
        vmPath: String?,
        javaArgs: Array<String?>?,
        mainClass: String?,
        appArgs: Array<String?>?,
        hasJavaAgents: Boolean
    ): Boolean

    @JvmStatic
    external fun nativeSetupExit(context: Context?)
}
