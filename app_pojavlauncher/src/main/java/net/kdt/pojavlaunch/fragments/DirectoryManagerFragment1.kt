package net.kdt.pojavlaunch.fragments

import android.os.Bundle
import android.text.InputType
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts.GetContent
import androidx.compose.ui.platform.ComposeView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputLayout
import net.ashmeet.hyperlauncher.R
import net.kdt.pojavlaunch.Tools
import net.kdt.pojavlaunch.Tools.openPath
import net.kdt.pojavlaunch.ui.screens.DirectoryManagerScreen
import net.kdt.pojavlaunch.ui.screens.DirectoryManagerViewModel
import net.kdt.pojavlaunch.ui.theme.PojavTheme

class DirectoryManagerFragment : Fragment() {
    private lateinit var mViewModel: DirectoryManagerViewModel

    private val mPickUpload: ActivityResultLauncher<String> =
        registerForActivityResult(GetContent()) { uri ->
            mViewModel.uploadPicked(requireContext(), uri)
        }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        mViewModel = ViewModelProvider(this)[DirectoryManagerViewModel::class.java]
        
        val args = arguments
        mViewModel.init(
            args?.getString(ARG_TITLE),
            args?.getString(ARG_ROOT_PATH)
        )

        return ComposeView(requireContext()).apply {
            setContent {
                // ✅ Enabled dynamic color
                PojavTheme(dynamicColor = true) {
                    DirectoryManagerScreen(
                        onBack = { Tools.backToMainMenu(requireActivity()) },
                        title = mViewModel.title,
                        breadcrumbs = mViewModel.getBreadcrumbs(),
                        entries = mViewModel.entries,
                        selectedFile = mViewModel.selectedFile,
                        statusText = mViewModel.statusText,
                        onEntryClick = { file ->
                            if (file.isDirectory) {
                                mViewModel.openDir(file)
                            } else {
                                openPath(requireContext(), file, true)
                            }
                        },
                        onEntryLongClick = { file ->
                            mViewModel.selectedFile = if (mViewModel.selectedFile == file) null else file
                        },
                        onCrumbClick = { file -> mViewModel.openDir(file) },
                        onUpClick = { mViewModel.goUp() },
                        onUploadClick = { mPickUpload.launch("*/*") },
                        onNewFolderClick = { promptNewFolder() },
                        onRenameClick = { promptRename() },
                        onDeleteClick = { deleteSelected() }
                    )
                }
            }
        }
    }

    private fun promptNewFolder() {
        val view = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_material_edit_text, null)
        val input = view.findViewById<EditText>(R.id.edit_text)
        val inputLayout = view.findViewById<TextInputLayout>(R.id.edit_text_layout)

        inputLayout.setHint("Folder name")
        input.setInputType(InputType.TYPE_CLASS_TEXT)

        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Create folder")
            .setView(view)
            .setNegativeButton(android.R.string.cancel, null)
            .setPositiveButton(android.R.string.ok) { _, _ ->
                val name = input.text.toString().trim()
                if (name.isNotEmpty()) {
                    mViewModel.createFolder(name)
                }
            }
            .show()
    }

    private fun promptRename() {
        val selected = mViewModel.selectedFile ?: return
        val view = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_material_edit_text, null)
        val input = view.findViewById<EditText>(R.id.edit_text)
        val inputLayout = view.findViewById<TextInputLayout>(R.id.edit_text_layout)

        inputLayout.setHint("Name")
        input.setText(selected.name)
        input.setInputType(InputType.TYPE_CLASS_TEXT)
        input.setSelectAllOnFocus(true)

        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Rename")
            .setView(view)
            .setNegativeButton(android.R.string.cancel, null)
            .setPositiveButton(android.R.string.ok) { _, _ ->
                val name = input.text.toString().trim()
                if (name.isNotEmpty()) {
                    mViewModel.renameSelected(name)
                }
            }
            .show()
    }

    private fun deleteSelected() {
        val selected = mViewModel.selectedFile ?: return
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Delete?")
            .setMessage(selected.name)
            .setNegativeButton(android.R.string.cancel, null)
            .setPositiveButton(android.R.string.ok) { _, _ ->
                mViewModel.deleteSelected()
            }
            .show()
    }

    companion object {
        const val TAG: String = "DirectoryManagerFragment"
        private const val ARG_ROOT_PATH = "root_path"
        private const val ARG_TITLE = "title"

        fun argsForRoot(rootDir: java.io.File, title: String?): Bundle {
            val args = Bundle()
            args.putString(ARG_ROOT_PATH, rootDir.absolutePath)
            args.putString(ARG_TITLE, title)
            return args
        }
    }
}
