package net.kdt.pojavlaunch.utils

import java.io.File
import java.io.IOException

object FileUtils {
    /**
     * Check if a file denoted by a String path exists.
     * @param filePath the path to check
     * @return whether it exists (same as File.exists()
     */
    @JvmStatic
    fun exists(filePath: String): Boolean {
        return File(filePath).exists()
    }

    /**
     * Get the file name from a path/URL string.
     * @param pathOrUrl the path or the URL of the file
     * @return the file's name
     */
    @JvmStatic
    fun getFileName(pathOrUrl: String): String? {
        val lastSlashIndex = pathOrUrl.lastIndexOf('/')
        if (lastSlashIndex == -1) return null
        return pathOrUrl.substring(lastSlashIndex)
    }

    /**
     * Remove the extension (all text after the last dot) from a path/URL string.
     * @param pathOrUrl the path or the URL of the file
     * @return the input with the extension removed
     */
    @JvmStatic
    fun removeExtension(pathOrUrl: String): String {
        val lastDotIndex = pathOrUrl.lastIndexOf('.')
        if (lastDotIndex == -1) return pathOrUrl
        return pathOrUrl.substring(0, lastDotIndex)
    }

    /**
     * Ensure that a directory exists, is a directory and is writable.
     * @param targetFile the directory to check
     * @return if the check has succeeded
     */
    @JvmStatic
    fun ensureDirectorySilently(targetFile: File): Boolean {
        if (targetFile.isFile()) return false
        if (targetFile.exists()) return targetFile.canWrite()
        else return targetFile.mkdirs()
    }

    /**
     * Ensure that the parent directory of a file exists and is writable
     * @param targetFile the File whose parent should be checked
     * @return if the check as succeeded
     */
    fun ensureParentDirectorySilently(targetFile: File): Boolean {
        val parentFile = targetFile.getParentFile()
        if (parentFile == null) return false
        return ensureDirectorySilently(parentFile)
    }

    /**
     * Same as ensureDirectorySilently(), but throws an IOException telling why the check failed.
     * @param targetFile the directory to check
     * @throws IOException when the checks fail
     */
    @JvmStatic
    @Throws(IOException::class)
    fun ensureDirectory(targetFile: File) {
        if (targetFile.isFile()) throw IOException("Target directory is a file")
        if (targetFile.exists()) {
            if (!targetFile.canWrite()) throw IOException("Target directory is not writable")
        } else if (!targetFile.mkdirs()) {
            // check again just in case (???)
            if (!targetFile.isDirectory()) throw IOException("Unable to create target directory")
        }
    }

    /**
     * Same as ensureParentDirectorySilently(), but throws an IOException telling why the check failed.
     * @param targetFile the File whose parent should be checked
     * @throws IOException when the checks fail
     */
    @JvmStatic
    @Throws(IOException::class)
    fun ensureParentDirectory(targetFile: File) {
        val parentFile = targetFile.getParentFile()
        if (parentFile == null) throw IOException("targetFile does not have a parent")
        ensureDirectory(parentFile)
    }
}