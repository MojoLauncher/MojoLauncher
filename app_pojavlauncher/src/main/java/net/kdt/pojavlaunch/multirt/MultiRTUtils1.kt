package net.kdt.pojavlaunch.multirt

import android.system.Os
import android.util.Log
import com.kdt.mcgui.ProgressLayout
import net.ashmeet.hyperlauncher.R
import net.kdt.pojavlaunch.Tools
import net.kdt.pojavlaunch.utils.MathUtils
import org.apache.commons.compress.archivers.tar.TarArchiveEntry
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream
import org.apache.commons.compress.compressors.xz.XZCompressorInputStream
import org.apache.commons.io.FileUtils
import org.apache.commons.io.IOUtils
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.nio.charset.StandardCharsets

object MultiRTUtils {
    private val sCache = HashMap<String?, Runtime?>()

    private val RUNTIME_FOLDER = File(Tools.MULTIRT_HOME!!)
    private const val JAVA_VERSION_STR = "JAVA_VERSION=\""
    private const val OS_ARCH_STR = "OS_ARCH=\""

    @JvmStatic
    val runtimes: MutableList<Runtime>
        get() {
            if (!RUNTIME_FOLDER.exists() && !RUNTIME_FOLDER.mkdirs()) {
                throw RuntimeException("Failed to create runtime directory")
            }

            val runtimesList = ArrayList<Runtime>()
            val files = RUNTIME_FOLDER.listFiles()
            if (files != null) {
                for (f in files) {
                    runtimesList.add(read(f.name))
                }
            } else {
                throw RuntimeException("The runtime directory does not exist")
            }

            return runtimesList
        }

    @JvmStatic
    fun getExactJreName(majorVersion: Int): String? {
        val runtimesList = runtimes
        for (r in runtimesList) {
            if (r.javaVersion == majorVersion) return r.name
        }

        return null
    }

    @JvmStatic
    fun getNearestJreName(majorVersion: Int): String? {
        val runtimesList = runtimes
        @Suppress("UNCHECKED_CAST")
        val nearestRankedRuntime = MathUtils.findNearestPositive(
            majorVersion,
            runtimesList as MutableList<Runtime?>,
            { runtime: Runtime? -> runtime!!.javaVersion })
        return nearestRankedRuntime?.value?.name
    }

    @JvmStatic
    @Throws(IOException::class)
    fun installRuntimeNamed(nativeLibDir: String, runtimeInputStream: InputStream, name: String?) {
        val dest = File(RUNTIME_FOLDER, "/$name")
        if (dest.exists()) FileUtils.deleteDirectory(dest)
        try {
            uncompressTarXZ(runtimeInputStream, dest)
            runtimeInputStream.close()
            unpack200(nativeLibDir, "${RUNTIME_FOLDER.absolutePath}/$name")
            read(name)
        } finally {
            ProgressLayout.clearProgress(ProgressLayout.UNPACK_RUNTIME)
        }
    }

    @JvmStatic
    @Throws(IOException::class)
    fun postPrepare(name: String?) {
        val dest = File(RUNTIME_FOLDER, "/$name")
        if (!dest.exists()) return
        val runtime = read(name)
        var libFolder = "lib"
        if (File(dest, "$libFolder/${runtime.arch}").exists()) {
            libFolder = "$libFolder/${runtime.arch}"
        }
        val ftIn = File(dest, "$libFolder/libfreetype.so.6")
        val ftOut = File(dest, "$libFolder/libfreetype.so")
        if (ftIn.exists() && (!ftOut.exists() || ftIn.length() != ftOut.length())) {
            if (!ftIn.renameTo(ftOut)) throw IOException("Failed to rename freetype")
        }

        // Refresh libraries
        copyDummyNativeLib("libawt_xawt.so", dest, libFolder)
    }

    @JvmStatic
    @Throws(IOException::class)
    fun installRuntimeNamedBinpack(
        universalFileInputStream: InputStream,
        platformBinsInputStream: InputStream,
        name: String?,
        binpackVersion: String?
    ) {
        val dest = File(RUNTIME_FOLDER, "/$name")
        if (dest.exists()) FileUtils.deleteDirectory(dest)
        try {
            installRuntimeNamedNoRemove(universalFileInputStream, dest)
            installRuntimeNamedNoRemove(platformBinsInputStream, dest)

            unpack200(Tools.NATIVE_LIB_DIR!!, "${RUNTIME_FOLDER.absolutePath}/$name")

            val binpackVerfile = File(RUNTIME_FOLDER, "/$name/pojav_version")
            FileUtils.write(binpackVerfile, binpackVersion, StandardCharsets.UTF_8)
            forceReread(name)
        } finally {
            ProgressLayout.clearProgress(ProgressLayout.UNPACK_RUNTIME)
        }
    }


    @JvmStatic
    fun readInternalRuntimeVersion(name: String?): String? {
        val versionFile = File(RUNTIME_FOLDER, "/$name/pojav_version")
        return try {
            if (versionFile.exists()) {
                Tools.read(versionFile.absolutePath)
            } else {
                null
            }
        } catch (e: IOException) {
            Log.e("MultiRT", "Failed to read internal runtime version", e)
            null
        }
    }

    @JvmStatic
    fun readLastUpdateTime(name: String?): Long {
        val lastUpdateTimeFile = File(RUNTIME_FOLDER, "$name/last_check_time")
        if (!lastUpdateTimeFile.exists()) return -1
        return try {
            Tools.read(lastUpdateTimeFile)?.trim { it <= ' ' }?.toLong() ?: -1
        } catch (_: IOException) {
            -1
        } catch (_: NumberFormatException) {
            -1
        }
    }

    @JvmStatic
    fun writeLastUpdateTime(name: String?, time: Long) {
        val lastUpdateTimeFile = File(RUNTIME_FOLDER, "$name/last_check_time")
        try {
            Tools.write(lastUpdateTimeFile, time.toString())
        } catch (_: IOException) {
        }
    }

    @Throws(IOException::class)
    fun removeRuntimeNamed(name: String?) {
        val dest = File(RUNTIME_FOLDER, "/$name")
        if (dest.exists()) {
            FileUtils.deleteDirectory(dest)
            sCache.remove(name)
        }
    }

    @JvmStatic
    fun getRuntimeHome(name: String): File {
        val dest = File(RUNTIME_FOLDER, name)
        Log.i("MultiRTUtils", "Dest exists? ${dest.exists()}")
        if (!dest.exists() || forceReread(name).versionString == null) {
            throw RuntimeException("Selected runtime is broken!")
        }
        return dest
    }

    @JvmStatic
    fun forceReread(name: String?): Runtime {
        sCache.remove(name)
        return read(name)
    }

    @JvmStatic
    fun read(name: String?): Runtime {
        if (name == null) throw IllegalArgumentException("name cannot be null")
        var returnRuntime = sCache[name]
        if (returnRuntime != null) return returnRuntime
        val release = File(RUNTIME_FOLDER, "$name/release")
        if (!release.exists()) {
            return Runtime(name)
        }
        try {
            val content = Tools.read(release.absolutePath)
            if (content != null) {
                val javaVersion = Tools.extractUntilCharacter(content, JAVA_VERSION_STR, '"')
                val osArch = Tools.extractUntilCharacter(content, OS_ARCH_STR, '"')
                if (javaVersion != null && osArch != null) {
                    val javaVersionSplit = javaVersion.split("\\.".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
                    val javaVersionInt = if (javaVersionSplit[0] == "1") {
                        javaVersionSplit[1]!!.toInt()
                    } else {
                        javaVersionSplit[0]!!.toInt()
                    }
                    returnRuntime = Runtime(name, javaVersion, osArch, javaVersionInt)
                } else {
                    returnRuntime = Runtime(name)
                }
            } else {
                returnRuntime = Runtime(name)
            }
        } catch (e: IOException) {
            returnRuntime = Runtime(name)
        }
        sCache[name] = returnRuntime
        return returnRuntime!!
    }

    /**
     * Unpacks all .pack files into .jar Serves only for java 8, as java 9 brought project jigsaw
     * @param nativeLibraryDir The native lib path, required to execute the unpack200 binary
     * @param runtimePath The path to the runtime to walk into
     */
    private fun unpack200(nativeLibraryDir: String, runtimePath: String) {
        val basePath = File(runtimePath)
        val files = FileUtils.listFiles(basePath, arrayOf("pack"), true)

        val workdir = File(nativeLibraryDir)

        val processBuilder = ProcessBuilder().directory(workdir)
        for (jarFile in files) {
            try {
                val process = processBuilder.command(
                    "./libunpack200.so",
                    "-r",
                    jarFile.absolutePath,
                    jarFile.absolutePath.replace(".pack", "")
                ).start()
                process.waitFor()
            } catch (e: InterruptedException) {
                Log.e("MULTIRT", "Failed to unpack the runtime!", e)
            } catch (e: IOException) {
                Log.e("MULTIRT", "Failed to unpack the runtime!", e)
            }
        }
    }

    @Throws(IOException::class)
    private fun copyDummyNativeLib(name: String, dest: File?, libFolder: String?) {
        val fileLib = File(dest, "/$libFolder/$name")
        val `is` = FileInputStream(File(Tools.NATIVE_LIB_DIR!!, name))
        val os = FileOutputStream(fileLib)
        IOUtils.copy(`is`, os)
        `is`.close()
        os.close()
    }

    @Throws(IOException::class)
    private fun installRuntimeNamedNoRemove(runtimeInputStream: InputStream, dest: File) {
        uncompressTarXZ(runtimeInputStream, dest)
        runtimeInputStream.close()
    }

    @Throws(IOException::class)
    private fun uncompressTarXZ(tarFileInputStream: InputStream?, dest: File) {
        net.kdt.pojavlaunch.utils.FileUtils.ensureDirectory(dest)

        val buffer = ByteArray(8192)
        TarArchiveInputStream(XZCompressorInputStream(tarFileInputStream)).use { tarIn ->
            var tarEntry: TarArchiveEntry?
            while (tarIn.nextTarEntry.also { tarEntry = it } != null) {
                val entry = tarEntry!!
                val tarEntryName = entry.name
                ProgressLayout.setProgress(
                    ProgressLayout.UNPACK_RUNTIME,
                    100,
                    R.string.global_unpacking,
                    tarEntryName
                )

                val destPath = File(dest, tarEntryName)
                net.kdt.pojavlaunch.utils.FileUtils.ensureParentDirectory(destPath)
                if (entry.isSymbolicLink) {
                    try {
                        Os.symlink(entry.linkName, destPath.absolutePath)
                    } catch (e: Throwable) {
                        Log.e("MultiRT", "Failed to create symlink: ${entry.linkName} -> ${destPath.absolutePath}", e)
                    }
                } else if (entry.isDirectory) {
                    net.kdt.pojavlaunch.utils.FileUtils.ensureDirectory(destPath)
                } else if (!destPath.exists() || destPath.length() != entry.size) {
                    FileOutputStream(destPath).use { os ->
                        IOUtils.copyLarge(tarIn, os, buffer)
                    }
                }
            }
        }
    }
}
