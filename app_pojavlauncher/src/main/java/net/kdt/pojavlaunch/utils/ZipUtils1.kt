package net.kdt.pojavlaunch.utils

import org.apache.commons.io.IOUtils
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipFile

object ZipUtils {
    /**
     * Gets an InputStream for a given ZIP entry, throwing an IOException if the ZIP entry does not
     * exist.
     * @param zipFile The ZipFile to get the entry from
     * @param entryPath The full path inside of the ZipFile
     * @return The InputStream provided by the ZipFile
     * @throws IOException if the entry was not found
     */
    @Throws(IOException::class)
    fun getEntryStream(zipFile: ZipFile, entryPath: String?): InputStream {
        val entry = zipFile.getEntry(entryPath)
        if (entry == null) throw IOException("No entry in ZIP file: " + entryPath)
        return zipFile.getInputStream(entry)
    }

    /**
     * Extracts all files in a ZipFile inside of a given directory to a given destination directory
     * How to specify dirName:
     * If you want to extract all files in the ZipFile, specify ""
     * If you want to extract a single directory, specify its full path followed by a trailing /
     * @param zipFile The ZipFile to extract files from
     * @param dirName The directory to extract the files from
     * @param destination The destination directory to extract the files into
     * @throws IOException if it was not possible to create a directory or file extraction failed
     */
    @Throws(IOException::class)
    fun zipExtract(zipFile: ZipFile, dirName: String, destination: File?) {
        val zipEntries = zipFile.entries()

        val dirNameLen = dirName.length
        while (zipEntries.hasMoreElements()) {
            val zipEntry: ZipEntry = zipEntries.nextElement()
            val entryName = zipEntry.getName()
            if (!entryName.startsWith(dirName) || zipEntry.isDirectory()) continue
            val zipDestination = File(destination, entryName.substring(dirNameLen))
            FileUtils.ensureParentDirectory(zipDestination)
            zipFile.getInputStream(zipEntry).use { inputStream ->
                FileOutputStream(zipDestination).use { outputStream ->
                    IOUtils.copy(inputStream, outputStream)
                }
            }
        }
    }
}
