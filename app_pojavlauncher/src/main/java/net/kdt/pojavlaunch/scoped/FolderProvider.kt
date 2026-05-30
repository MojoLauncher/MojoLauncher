package net.kdt.pojavlaunch.scoped

import android.annotation.TargetApi
import android.content.ContentResolver
import android.content.res.AssetFileDescriptor
import android.database.Cursor
import android.database.MatrixCursor
import android.graphics.Point
import android.net.Uri
import android.os.CancellationSignal
import android.os.ParcelFileDescriptor
import android.provider.DocumentsContract
import android.provider.DocumentsContract.Root
import android.provider.DocumentsProvider
import android.util.Log
import android.webkit.MimeTypeMap
import net.ashmeet.hyperlauncher.BuildConfig
import net.ashmeet.hyperlauncher.R
import net.kdt.pojavlaunch.Tools
import org.apache.commons.io.FileUtils
import java.io.File
import java.io.FileNotFoundException
import java.io.IOException
import java.util.Collections
import java.util.LinkedList
import java.util.Locale

/**
 * A document provider for the Storage Access Framework which exposes the files in the
 * $HOME/ directory to other apps.
 */
class FolderProvider : DocumentsProvider() {
    private var BASE_DIR: File? = null

    private var mContentResolver: ContentResolver? = null

    private var mStorageProviderAuthortiy: String? = null

    override fun queryRoots(projection: Array<String?>?): Cursor {
        val result = MatrixCursor(projection ?: DEFAULT_ROOT_PROJECTION)
        val applicationName = context?.getString(R.string.app_short_name)

        var summary: String? = BuildConfig.VERSION_NAME
        if (BuildConfig.DEBUG) {
            summary = "(" + context?.getString(R.string.generic_debug) + ") " + summary
        }

        val row = result.newRow()
        row.add(Root.COLUMN_ROOT_ID, getDocIdForFile(BASE_DIR!!))
        row.add(Root.COLUMN_DOCUMENT_ID, getDocIdForFile(BASE_DIR!!))
        row.add(Root.COLUMN_SUMMARY, summary)
        row.add(
            Root.COLUMN_FLAGS,
            Root.FLAG_SUPPORTS_CREATE or Root.FLAG_SUPPORTS_SEARCH or Root.FLAG_SUPPORTS_IS_CHILD
        )
        row.add(Root.COLUMN_TITLE, applicationName)
        row.add(Root.COLUMN_MIME_TYPES, ALL_MIME_TYPES)
        row.add(Root.COLUMN_AVAILABLE_BYTES, BASE_DIR!!.freeSpace)
        row.add(Root.COLUMN_ICON, R.mipmap.ic_launcher)
        return result
    }

    @Throws(FileNotFoundException::class)
    override fun queryDocument(documentId: String, projection: Array<String?>?): Cursor {
        val result = MatrixCursor(projection ?: DEFAULT_DOCUMENT_PROJECTION)
        result.setNotificationUri(mContentResolver, createUriForDocId(documentId))
        includeFile(result, documentId, null)
        return result
    }

    @Throws(FileNotFoundException::class)
    override fun queryChildDocuments(
        parentDocumentId: String,
        projection: Array<String?>?,
        sortOrder: String?
    ): Cursor {
        val result = MatrixCursor(projection ?: DEFAULT_DOCUMENT_PROJECTION)
        val parent: File = getFileForDocId(parentDocumentId)
        val children = parent.listFiles()
        if (children == null) throw FileNotFoundException("Unable to list files in " + parent.absolutePath)
        for (file in children) {
            includeFile(result, null, file)
        }
        result.setNotificationUri(mContentResolver, createUriForDocId(parentDocumentId))
        return result
    }

    @Throws(FileNotFoundException::class)
    override fun openDocument(
        documentId: String,
        mode: String?,
        signal: CancellationSignal?
    ): ParcelFileDescriptor? {
        val file: File = getFileForDocId(documentId)
        val accessMode: Int = ParcelFileDescriptor.parseMode(mode)
        return ParcelFileDescriptor.open(file, accessMode)
    }

    @Throws(FileNotFoundException::class)
    override fun openDocumentThumbnail(
        documentId: String,
        sizeHint: Point?,
        signal: CancellationSignal?
    ): AssetFileDescriptor? {
        val file: File = getFileForDocId(documentId)
        val pfd: ParcelFileDescriptor =
            ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
        return AssetFileDescriptor(pfd, 0, file.length())
    }

    override fun onCreate(): Boolean {
        val ctx = context ?: return false
        if (Tools.checkStorageRoot(ctx)) {
            Tools.initStorageConstants(ctx)
        } else {
            return false
        }
        BASE_DIR = File(Tools.DIR_GAME_HOME)
        mContentResolver = ctx.contentResolver
        mStorageProviderAuthortiy = ctx.getString(R.string.storageProviderAuthorities)
        return true
    }

    @Throws(FileNotFoundException::class)
    override fun createDocument(
        parentDocumentId: String,
        mimeType: String?,
        displayName: String
    ): String {
        var newFile = File(parentDocumentId, displayName)
        var noConflictId = 2
        while (newFile.exists()) {
            newFile = File(parentDocumentId, "$displayName ($noConflictId)")
            noConflictId++
        }
        try {
            val succeeded: Boolean = if (DocumentsContract.Document.MIME_TYPE_DIR == mimeType) {
                newFile.mkdir()
            } else {
                newFile.createNewFile()
            }
            if (!succeeded) {
                throw FileNotFoundException("Failed to create document with id " + newFile.path)
            }
        } catch (e: IOException) {
            throw FileNotFoundException("Failed to create document with id " + newFile.path)
        }
        notifyChange(createUriForDocId(parentDocumentId)!!)
        return newFile.path
    }

    @Throws(FileNotFoundException::class)
    override fun renameDocument(documentId: String, displayName: String?): String {
        val sourceFile: File = getFileForDocId(documentId)
        val sourceParent = sourceFile.parentFile ?: throw FileNotFoundException("Cannot rename root")
        val targetFile = File(getDocIdForFile(sourceParent) + "/" + displayName)
        if (!sourceFile.renameTo(targetFile)) {
            throw FileNotFoundException("Couldn't rename the document with id $documentId")
        }
        return getDocIdForFile(targetFile)
    }

    @Throws(FileNotFoundException::class)
    override fun moveDocument(
        sourceDocumentId: String?,
        sourceParentDocumentId: String?,
        targetParentDocumentId: String?
    ): String {
        val sourceFile: File = getFileForDocId(sourceParentDocumentId + sourceDocumentId)
        val targetFile = File(targetParentDocumentId + sourceDocumentId)
        if (!sourceFile.renameTo(targetFile)) {
            throw FileNotFoundException("Failed to move the document with id " + sourceFile.path)
        }
        return getDocIdForFile(targetFile)
    }

    @Throws(FileNotFoundException::class)
    override fun removeDocument(documentId: String?, parentDocumentId: String?) {
        deleteDocument("$parentDocumentId/$documentId")
    }

    @Throws(FileNotFoundException::class)
    override fun deleteDocument(documentId: String) {
        val file: File = getFileForDocId(documentId)
        if (file.isDirectory) {
            try {
                FileUtils.deleteDirectory(file)
            } catch (e: IOException) {
                throw FileNotFoundException("Failed to delete document with id $documentId")
            }
        } else {
            if (!file.delete()) {
                throw FileNotFoundException("Failed to delete document with id $documentId")
            }
        }
        val parent = file.parentFile
        if (parent != null) {
            notifyChange(createUriForFile(parent)!!)
        }
    }

    @Throws(FileNotFoundException::class)
    override fun getDocumentType(documentId: String): String {
        val file: File = getFileForDocId(documentId)
        return getMimeType(file)
    }

    @Throws(FileNotFoundException::class)
    override fun querySearchDocuments(
        rootId: String,
        query: String,
        projection: Array<String?>?
    ): Cursor {
        val result = MatrixCursor(projection ?: DEFAULT_DOCUMENT_PROJECTION)
        val parent: File = getFileForDocId(rootId)

        val pending = LinkedList<File>()
        pending.add(parent)

        val MAX_SEARCH_RESULTS = 50
        while (pending.isNotEmpty() && result.count < MAX_SEARCH_RESULTS) {
            val file = pending.removeFirst()
            var isInsideHome: Boolean
            try {
                isInsideHome = file.canonicalPath.startsWith(Tools.DIR_GAME_HOME)
            } catch (e: IOException) {
                isInsideHome = true
            }
            if (isInsideHome) {
                if (file.isDirectory) {
                    val listing = file.listFiles()
                    if (listing != null) Collections.addAll(pending, *listing)
                } else {
                    if (file.name.lowercase(Locale.getDefault()).contains(query)) {
                        includeFile(result, null, file)
                    }
                }
            }
        }

        return result
    }

    override fun isChildDocument(parentDocumentId: String, documentId: String): Boolean {
        return documentId.startsWith(parentDocumentId)
    }

    @Throws(FileNotFoundException::class)
    private fun includeFile(result: MatrixCursor, docId: String?, file: File?) {
        var finalDocId = docId
        var finalFile = file
        if (finalDocId == null) {
            finalDocId = getDocIdForFile(finalFile!!)
        } else {
            finalFile = getFileForDocId(finalDocId)
        }

        var flags = 0
        if (finalFile.isDirectory) {
            if (finalFile.canWrite()) flags =
                flags or DocumentsContract.Document.FLAG_DIR_SUPPORTS_CREATE
        } else if (finalFile.canWrite()) {
            flags = flags or DocumentsContract.Document.FLAG_SUPPORTS_WRITE
        }
        val parent = finalFile.parentFile
        if (parent != null) {
            if (parent.canWrite()) flags = flags or DocumentsContract.Document.FLAG_SUPPORTS_DELETE
        }

        val displayName = finalFile.name
        val mimeType: String = getMimeType(finalFile)
        if (mimeType.startsWith("image/")) flags =
            flags or DocumentsContract.Document.FLAG_SUPPORTS_THUMBNAIL

        val row = result.newRow()
        row.add(DocumentsContract.Document.COLUMN_DOCUMENT_ID, finalDocId)
        row.add(DocumentsContract.Document.COLUMN_DISPLAY_NAME, displayName)
        row.add(DocumentsContract.Document.COLUMN_SIZE, finalFile.length())
        row.add(DocumentsContract.Document.COLUMN_MIME_TYPE, mimeType)
        row.add(DocumentsContract.Document.COLUMN_LAST_MODIFIED, finalFile.lastModified())
        row.add(DocumentsContract.Document.COLUMN_FLAGS, flags)
        row.add(DocumentsContract.Document.COLUMN_ICON, R.mipmap.ic_launcher)
    }

    @TargetApi(26)
    @Throws(FileNotFoundException::class)
    override fun findDocumentPath(
        parentDocumentId: String?,
        childDocumentId: String
    ): DocumentsContract.Path {
        var source = BASE_DIR!!
        if (parentDocumentId != null) source = getFileForDocId(parentDocumentId)
        var destination: File? = getFileForDocId(childDocumentId)
        val pathIds = mutableListOf<String?>()
        while (source != destination && destination != null) {
            pathIds.add(getDocIdForFile(destination))
            destination = destination.parentFile
        }
        pathIds.add(getDocIdForFile(source))
        pathIds.reverse()
        return DocumentsContract.Path(getDocIdForFile(source), pathIds)
    }

    @Throws(FileNotFoundException::class)
    private fun createUriForDocId(documentId: String): Uri? {
        return createUriForFile(getFileForDocId(documentId))
    }

    private fun createUriForFile(file: File): Uri? {
        return DocumentsContract.buildDocumentUri(mStorageProviderAuthortiy, file.absolutePath)
    }

    private fun notifyChange(uri: Uri) {
        mContentResolver?.notifyChange(uri, null)
    }

    companion object {
        private const val ALL_MIME_TYPES = "*/*"

        private val DEFAULT_ROOT_PROJECTION: Array<String?> = arrayOf(
            Root.COLUMN_ROOT_ID,
            Root.COLUMN_MIME_TYPES,
            Root.COLUMN_FLAGS,
            Root.COLUMN_ICON,
            Root.COLUMN_TITLE,
            Root.COLUMN_SUMMARY,
            Root.COLUMN_DOCUMENT_ID,
            Root.COLUMN_AVAILABLE_BYTES
        )

        private val DEFAULT_DOCUMENT_PROJECTION: Array<String?> = arrayOf(
            DocumentsContract.Document.COLUMN_DOCUMENT_ID,
            DocumentsContract.Document.COLUMN_MIME_TYPE,
            DocumentsContract.Document.COLUMN_DISPLAY_NAME,
            DocumentsContract.Document.COLUMN_LAST_MODIFIED,
            DocumentsContract.Document.COLUMN_FLAGS,
            DocumentsContract.Document.COLUMN_SIZE
        )

        private fun getDocIdForFile(file: File): String {
            return file.absolutePath
        }

        @Throws(FileNotFoundException::class)
        private fun getFileForDocId(docId: String): File {
            val f = File(docId)
            if (!f.exists()) throw FileNotFoundException(f.absolutePath + " not found")
            return f
        }

        private fun getMimeType(file: File): String {
            if (file.isDirectory) {
                return DocumentsContract.Document.MIME_TYPE_DIR
            } else {
                val name = file.name
                val lastDot = name.lastIndexOf('.')
                if (lastDot >= 0) {
                    val extension = name.substring(lastDot + 1).lowercase(Locale.getDefault())
                    val mime: String? =
                        MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension)
                    if (mime != null) return mime
                }
                return "application/octet-stream"
            }
        }
    }
}
