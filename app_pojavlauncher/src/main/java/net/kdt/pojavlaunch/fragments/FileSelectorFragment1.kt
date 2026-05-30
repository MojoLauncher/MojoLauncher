package net.kdt.pojavlaunch.fragments

import android.content.DialogInterface
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.kdt.pickafile.FileListView
import com.kdt.pickafile.FileSelectedListener
import net.ashmeet.hyperlauncher.R
import net.kdt.pojavlaunch.Tools
import net.kdt.pojavlaunch.Tools.removeCurrentFragment
import net.kdt.pojavlaunch.extra.ExtraConstants
import net.kdt.pojavlaunch.extra.ExtraCore
import java.io.File

class FileSelectorFragment : Fragment(R.layout.fragment_file_selector) {
    private var mSelectFolderButton: Button? = null
    private var mCreateFolderButton: Button? = null
    private var mFileListView: FileListView? = null
    private var mFilePathView: TextView? = null

    private var mSelectFolder = true
    private var mShowFiles = true
    private var mShowFolders = true
    private var mRootPath = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P)
        Tools.DIR_GAME_NEW
    else
        Environment.getExternalStorageDirectory().absolutePath


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        bindViews(view)
        parseBundle()
        if (!mSelectFolder) mSelectFolderButton!!.visibility = View.GONE
        else mSelectFolderButton!!.visibility = View.VISIBLE

        mFileListView!!.setShowFiles(mShowFiles)
        mFileListView!!.setShowFolders(mShowFolders)
        mFileListView!!.lockPathAt(File(mRootPath))
        mFileListView!!.setDialogTitleListener { title: String? ->
            mFilePathView!!.text = if (title != null) removeLockPath(title) else ""
        }
        mFileListView!!.refreshPath()

        mCreateFolderButton!!.setOnClickListener { v: View? ->
            val editText = EditText(context)
            MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.folder_dialog_insert_name)
                .setView(editText)
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton(
                    R.string.folder_dialog_create
                ) { dialog: DialogInterface?, which: Int ->
                    val folder =
                        File(mFileListView!!.getFullPath(), editText.text.toString())
                    val success = folder.mkdir()
                    if (success) {
                        mFileListView!!.listFileAt(
                            File(
                                mFileListView!!.getFullPath(),
                                editText.text.toString()
                            )
                        )
                    } else {
                        mFileListView!!.refreshPath()
                    }
                }.show()
        }

        mSelectFolderButton!!.setOnClickListener { v: View? ->
            ExtraCore.setValue(
                ExtraConstants.FILE_SELECTOR,
                removeLockPath(mFileListView!!.getFullPath().absolutePath)
            )
            removeCurrentFragment(requireActivity())
        }

        mFileListView!!.setFileSelectedListener(object : FileSelectedListener() {
            override fun onFileSelected(file: File?, path: String?) {
                if (path != null) {
                    ExtraCore.setValue(ExtraConstants.FILE_SELECTOR, removeLockPath(path))
                }
                removeCurrentFragment(requireActivity())
            }
        })
    }

    private fun removeLockPath(path: String): String {
        return mRootPath?.let { path.replace(it, ".") } ?: path
    }

    private fun parseBundle() {
        val bundle = arguments
        if (bundle == null) return
        mSelectFolder = bundle.getBoolean(BUNDLE_SELECT_FOLDER, mSelectFolder)
        mShowFiles = bundle.getBoolean(BUNDLE_SHOW_FILE, mShowFiles)
        mShowFolders = bundle.getBoolean(BUNDLE_SHOW_FOLDER, mShowFolders)
        mRootPath = bundle.getString(BUNDLE_ROOT_PATH, mRootPath)
    }

    private fun bindViews(view: View) {
        mSelectFolderButton = view.findViewById(R.id.file_selector_select_folder)
        mCreateFolderButton = view.findViewById(R.id.file_selector_create_folder)
        mFileListView = view.findViewById(R.id.file_selector)
        mFilePathView = view.findViewById(R.id.file_selector_current_path)
    }

    companion object {
        const val TAG: String = "FileSelectorFragment"
        const val BUNDLE_SELECT_FOLDER: String = "select_folder"
        const val BUNDLE_SHOW_FILE: String = "show_file"
        const val BUNDLE_SHOW_FOLDER: String = "show_folder"
        const val BUNDLE_ROOT_PATH: String = "root_path"
    }
}
