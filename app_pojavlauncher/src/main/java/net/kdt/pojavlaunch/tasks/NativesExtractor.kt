package net.kdt.pojavlaunch.tasks

import net.kdt.pojavlaunch.Architecture
import net.kdt.pojavlaunch.Tools
import net.kdt.pojavlaunch.utils.FileUtils
import java.io.File
import java.io.FileInputStream
import java.io.FilterInputStream
import java.io.IOException
import java.io.InputStream
import java.util.zip.CRC32
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream

class NativesExtractor(private val mDestinationDir: File?) {
    private val mLibraryLocation: String

    init {
        this.mLibraryLocation = "jni/" + aarArchitectureName + "/"
    }

    @Throws(IOException::class)
    fun extractFromAar(source: File?) {
        val buffer = ByteArray(8192)
        FileInputStream(source).use { fileInputStream ->
            ZipInputStream(fileInputStream).use { zipInputStream ->
                // Wrap the ZIP input stream into a non-closeable stream to
                // avoid it being closed by processEntry()
                val entryCopyStream = NonCloseableInputStream(zipInputStream)
                var entry: ZipEntry?
                while ((zipInputStream.getNextEntry().also { entry = it }) != null) {
                    var entryName = entry!!.getName()
                    if (!entryName!!.startsWith(mLibraryLocation) || entry.isDirectory()) continue
                    // Entry name is actually the full path, so we need to strip the path before extraction
                    entryName = FileUtils.getFileName(entryName)
                    // getFileName may make the file name null, avoid that case.
                    if (entryName == null || LIBRARY_BLACKLIST.contains(entryName)) continue

                    processEntry(entryCopyStream, entry, File(mDestinationDir, entryName), buffer)
                }
            }
        }
    }

    @Throws(IOException::class)
    private fun processEntry(
        sourceStream: InputStream,
        zipEntry: ZipEntry,
        entryDestination: File,
        buffer: ByteArray?
    ) {
        if (entryDestination.exists()) {
            val expectedSize = zipEntry.getSize()
            val expectedCrc32 = zipEntry.getCrc()
            val realSize = entryDestination.length()
            val realCrc32: Long = fileCrc32(entryDestination, buffer)
            // File in archive is the same as the local one, don't extract
            if (realSize == expectedSize && realCrc32 == expectedCrc32) return
        }
        // copyInputStreamToFile copies the stream to a file and then closes it.
        org.apache.commons.io.FileUtils.copyInputStreamToFile(sourceStream, entryDestination)
    }


    private class NonCloseableInputStream(`in`: InputStream?) : FilterInputStream(`in`) {
        override fun close() {
            // Do nothing (the point of this class)
        }
    }

    companion object {
        private val LIBRARY_BLACKLIST: ArrayList<String?> = createLibraryBlacklist()

        /**
         * Create a library blacklist so that downloaded natives are not able to
         * override built-in libraries.
         * @return the resulting blacklist of library file names
         */
        private fun createLibraryBlacklist(): ArrayList<String?> {
            val includedLibraryNames = File(Tools.NATIVE_LIB_DIR).list()
            val blacklist = ArrayList<String?>(includedLibraryNames!!.size)
            for (libraryName in includedLibraryNames) {
                // allow overriding jnidispatch (as the integrated version may be too old)
                if (libraryName == "libjnidispatch.so") continue
                blacklist.add(libraryName)
            }
            blacklist.trimToSize()
            return blacklist
        }

        private val aarArchitectureName: String
            get() {
                val architecture = Architecture.deviceArchitecture
                when (architecture) {
                    Architecture.ARCH_ARM -> return "armeabi-v7a"
                    Architecture.ARCH_ARM64 -> return "arm64-v8a"
                    Architecture.ARCH_X86 -> return "x86"
                    Architecture.ARCH_X86_64 -> return "x86_64"
                }
                throw RuntimeException("Unknown CPU architecture: " + architecture)
            }

        @Throws(IOException::class)
        private fun fileCrc32(target: File?, buffer: ByteArray?): Long {
            FileInputStream(target).use { fileInputStream ->
                val crc32 = CRC32()
                var len: Int
                while ((fileInputStream.read(buffer).also { len = it }) != -1) {
                    crc32.update(buffer, 0, len)
                }
                return crc32.getValue()
            }
        }
    }
}
