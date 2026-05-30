package com.kdt.pickafile

import android.content.Context
import android.os.Environment
import android.util.AttributeSet
import android.view.View
import android.widget.AdapterView
import android.widget.AdapterView.OnItemClickListener
import android.widget.AdapterView.OnItemLongClickListener
import android.widget.LinearLayout
import android.widget.ListView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import com.ipaulpro.afilechooser.FileListAdapter
import net.kdt.pojavlaunch.Tools
import java.io.File
import java.util.Arrays

class FileListView(
    context: Context, attrs: AttributeSet?, defStyle: Int, //For filtering by file types:
    private val fileSuffixes: Array<String?>
) : LinearLayout(context, attrs, defStyle) {
    //For list view:
    private var fullPath: File? = null
    private var mainLv: ListView? = null
    private var context: Context? = null

    //For File selected listener:
    private var fileSelectedListener: FileSelectedListener? = null
    private var dialogTitleListener: DialogTitleListener? = null
    private var lockPath = File("/")

    private var showFiles = true
    private var showFolders = true

    constructor(build: AlertDialog, fileSuffix: String?) : this(
        build.context,
        null,
        arrayOf<String?>(fileSuffix)
    ) {
        dialogToTitleListener(build)
    }

    @JvmOverloads
    constructor(
        context: Context,
        attrs: AttributeSet? = null,
        fileSuffixes: Array<String?> = arrayOfNulls<String>(0)
    ) : this(context, attrs, 0, fileSuffixes)

    init {
        init(context)
    }

    private fun dialogToTitleListener(dialog: AlertDialog?) {
        if (dialog != null) dialogTitleListener =
            DialogTitleListener { title: String? -> dialog.setTitle(title) }
    }

    fun init(context: Context) {
        //Main setup:
        this.context = context

        val layParam = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)

        orientation = VERTICAL

        mainLv = ListView(context)

        mainLv!!.setOnItemClickListener(OnItemClickListener { p1: AdapterView<*>?, p2: View?, p3: Int, p4: Long ->
            // TODO: Implement this method
            val mainFile = File(p1!!.getItemAtPosition(p3).toString())
            if (p3 == 0 && lockPath != fullPath) {
                parentDir()
            } else {
                listFileAt(mainFile)
            }
        })

        mainLv!!.setOnItemLongClickListener(OnItemLongClickListener { p1: AdapterView<*>?, p2: View?, p3: Int, p4: Long ->
            // TODO: Implement this method
            val mainFile = File(p1!!.getItemAtPosition(p3).toString())
            if (mainFile.isFile) {
                fileSelectedListener!!.onFileLongClick(mainFile, mainFile.absolutePath)
                return@OnItemLongClickListener true
            }
            false
        })
        addView(mainLv, layParam)

        try {
            listFileAt(Environment.getExternalStorageDirectory())
        } catch (e: NullPointerException) {
        } // Android 10+ disallows access to sdcard
    }

    fun setFileSelectedListener(listener: FileSelectedListener) {
        this.fileSelectedListener = listener
    }

    fun setDialogTitleListener(listener: DialogTitleListener?) {
        this.dialogTitleListener = listener
    }

    fun listFileAt(path: File) {
        try {
            if (path.exists()) {
                if (path.isDirectory) {
                    fullPath = path

                    val listFile = path.listFiles()
                    val fileAdapter = FileListAdapter(context)
                    if (path != lockPath) {
                        fileAdapter.add(File(path, ".."))
                    }

                    if (listFile != null && listFile.size != 0) {
                        Arrays.sort<File?>(listFile, SortFileName())

                        for (file in listFile) {
                            if (file.isDirectory) {
                                if (showFolders && ((!file.name
                                        .startsWith(".")) || file.name == ".minecraft")
                                ) fileAdapter.add(file)
                                continue
                            }

                            if (showFiles) {
                                if (fileSuffixes.isNotEmpty()) {
                                    for (suffix in fileSuffixes) {
                                        if (file.name.endsWith("." + suffix)) {
                                            fileAdapter.add(file)
                                            break
                                        }
                                    }
                                } else {
                                    fileAdapter.add(file)
                                }
                            }
                        }
                    }
                    mainLv!!.adapter = fileAdapter
                    if (dialogTitleListener != null) dialogTitleListener!!.onChangeDialogTitle(path.absolutePath)
                } else {
                    fileSelectedListener!!.onFileSelected(path, path.absolutePath)
                }
            } else {
                Toast.makeText(context, "This folder (or file) doesn't exist", Toast.LENGTH_SHORT)
                    .show()
                refreshPath()
            }
        } catch (e: Exception) {
            Tools.showError(context!!, e)
        }
    }

    fun getFullPath(): File {
        return fullPath!!
    }

    fun refreshPath() {
        listFileAt(getFullPath())
    }

    fun parentDir() {
        if (fullPath!!.absolutePath != "/") {
            listFileAt(fullPath!!.parentFile!!)
        }
    }

    fun lockPathAt(path: File) {
        lockPath = path
        listFileAt(path)
    }

    fun setShowFiles(showFiles: Boolean) {
        this.showFiles = showFiles
    }

    fun setShowFolders(showFolders: Boolean) {
        this.showFolders = showFolders
    }
}
