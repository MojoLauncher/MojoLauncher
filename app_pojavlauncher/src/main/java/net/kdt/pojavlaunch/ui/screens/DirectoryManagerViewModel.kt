package net.kdt.pojavlaunch.ui.screens

import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.compose.runtime.*
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.kdt.pojavlaunch.Tools
import net.kdt.pojavlaunch.instances.Instances.Companion.loadSelectedInstance
import net.kdt.pojavlaunch.utils.FileUtils.ensureParentDirectory
import org.apache.commons.io.IOUtils
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.regex.Pattern

class DirectoryManagerViewModel : ViewModel() {
    var rootDir by mutableStateOf<File?>(null)
    var currentDir by mutableStateOf<File?>(null)
    var entries by mutableStateOf<List<File>>(emptyList())
    var selectedFile by mutableStateOf<File?>(null)
    var statusText by mutableStateOf("")
    var title by mutableStateOf("Files")

    fun init(title: String?, rootPath: String?) {
        if (title != null) this.title = title

        if (rootPath != null) {
            rootDir = File(rootPath)
            currentDir = rootDir
        } else {
            val instance = loadSelectedInstance()
            if (instance != null) {
                rootDir = instance.gameDirectory
                currentDir = rootDir
            }
        }
        refresh()
    }

    fun refresh() {
        val dir = currentDir
        if (dir == null) {
            entries = emptyList()
            return
        }

        viewModelScope.launch(Dispatchers.IO) {
            val children = dir.listFiles()
            val list = mutableListOf<File>()
            if (children != null) list.addAll(children)
            list.sortWith(compareBy<File>({ !it.isDirectory }, { it.name.lowercase() }))
            
            withContext(Dispatchers.Main) {
                entries = list
                selectedFile = null
            }
        }
    }

    fun openDir(dir: File) {
        if (!dir.isDirectory || !isWithinRoot(dir)) return
        currentDir = dir
        statusText = ""
        refresh()
    }

    fun goUp() {
        val parent = currentDir?.parentFile ?: return
        if (isWithinRoot(parent)) {
            currentDir = parent
            statusText = ""
            refresh()
        }
    }

    private fun isWithinRoot(dir: File): Boolean {
        val root = rootDir ?: return false
        return try {
            val rootPath = root.canonicalPath
            val targetPath = dir.canonicalPath
            if (!targetPath.startsWith(rootPath)) return false
            if (targetPath.length > rootPath.length) {
                val sep = targetPath[rootPath.length]
                if (sep != File.separatorChar) return false
            }
            true
        } catch (e: Exception) {
            false
        }
    }

    fun createFolder(name: String) {
        val dir = currentDir ?: return
        val newFolder = File(dir, name)
        if (!isWithinRoot(newFolder)) return
        if (newFolder.exists()) {
            statusText = "Already exists"
            return
        }
        if (newFolder.mkdirs()) {
            statusText = "Created: $name"
            refresh()
        } else {
            statusText = "Failed to create folder"
        }
    }

    fun renameSelected(newName: String) {
        val selected = selectedFile ?: return
        if (newName.isEmpty() || newName == selected.name) return

        val target = File(selected.parentFile, newName)
        if (!isWithinRoot(target)) return
        if (target.exists()) {
            statusText = "Name already taken"
            return
        }
        if (selected.renameTo(target)) {
            statusText = "Renamed to $newName"
            refresh()
        } else {
            statusText = "Rename failed"
        }
    }

    fun deleteSelected() {
        val selected = selectedFile ?: return
        if (!isWithinRoot(selected)) return

        viewModelScope.launch(Dispatchers.IO) {
            val ok = deleteRecursively(selected)
            withContext(Dispatchers.Main) {
                statusText = if (ok) "Deleted" else "Delete failed"
                refresh()
            }
        }
    }

    private fun deleteRecursively(file: File): Boolean {
        if (file.isDirectory) {
            file.listFiles()?.forEach { if (!deleteRecursively(it)) return false }
        }
        return file.delete()
    }

    fun uploadPicked(context: Context, uri: Uri?) {
        if (uri == null || currentDir == null) return
        val targetDir = currentDir!!
        if (!isWithinRoot(targetDir)) return

        viewModelScope.launch(Dispatchers.IO) {
            try {
                var name = Tools.getFileName(context, uri) ?: "upload"
                name = name.replace("/", "_").replace("\\", "_")
                var target = File(targetDir, name)
                target = resolveUnique(target)

                ensureParentDirectory(target)
                context.contentResolver.openInputStream(uri)?.use { input ->
                    FileOutputStream(target).use { output ->
                        IOUtils.copy(input, output)
                    }
                }
                withContext(Dispatchers.Main) {
                    statusText = "Uploaded: ${target.name}"
                    Toast.makeText(context, "Uploaded: ${target.name}", Toast.LENGTH_SHORT).show()
                    refresh()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Tools.showError(context, e)
                }
            }
        }
    }

    private fun resolveUnique(target: File): File {
        if (!target.exists()) return target
        val name = target.name
        var base = name
        var ext = ""
        val dot = name.lastIndexOf('.')
        if (dot > 0) {
            base = name.substring(0, dot)
            ext = name.substring(dot)
        }
        for (i in 1..999) {
            val candidate = File(target.parentFile, "$base ($i)$ext")
            if (!candidate.exists()) return candidate
        }
        return target
    }

    fun getBreadcrumbs(): List<Pair<String, File>> {
        val root = rootDir ?: return emptyList()
        val current = currentDir ?: return emptyList()
        val crumbs = mutableListOf<Pair<String, File>>()
        
        var rootName = root.name.ifBlank { "Root" }
        crumbs.add(rootName to root)

        try {
            val rootPath = root.canonicalPath
            val currentPath = current.canonicalPath
            if (currentPath == rootPath) return crumbs
            if (!currentPath.startsWith(rootPath)) return listOf(current.absolutePath to current)

            var rel = currentPath.substring(rootPath.length)
            while (rel.startsWith(File.separator)) rel = rel.substring(1)
            if (rel.isEmpty()) return crumbs

            var acc = root
            val parts = rel.split(Pattern.quote(File.separator).toRegex()).filter { it.isNotEmpty() }
            for (part in parts) {
                acc = File(acc, part)
                crumbs.add(part to acc)
            }
        } catch (e: Exception) {
            return listOf(current.absolutePath to current)
        }
        return crumbs
    }
}
