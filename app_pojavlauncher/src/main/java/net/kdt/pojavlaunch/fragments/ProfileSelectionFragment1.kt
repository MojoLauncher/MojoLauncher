package net.kdt.pojavlaunch.fragments

import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.ActivityResultCallback
import androidx.activity.result.contract.ActivityResultContracts.OpenDocumentTree
import androidx.compose.ui.platform.ComposeView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import net.ashmeet.hyperlauncher.R
import net.kdt.pojavlaunch.Tools
import net.kdt.pojavlaunch.extra.ExtraConstants
import net.kdt.pojavlaunch.extra.ExtraCore
import net.kdt.pojavlaunch.instances.Instances.Companion.setSelectedInstance
import net.kdt.pojavlaunch.ui.screens.ProfileSelectionScreen
import net.kdt.pojavlaunch.ui.screens.ProfileSelectionViewModel
import net.kdt.pojavlaunch.ui.theme.PojavTheme

class ProfileSelectionFragment : Fragment() {
    private lateinit var mViewModel: ProfileSelectionViewModel

    private val mDirPickerLauncher = registerForActivityResult(
        OpenDocumentTree(),
        ActivityResultCallback { uri: Uri? ->
            if (uri != null) {
                Toast.makeText(requireContext(), "Selected: " + uri.path, Toast.LENGTH_LONG)
                    .show()
            }
        }
    )

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        mViewModel = ViewModelProvider(this)[ProfileSelectionViewModel::class.java]
        mViewModel.loadProfiles()

        return ComposeView(requireContext()).apply {
            setContent {
                PojavTheme(dynamicColor = true) {
                    ProfileSelectionScreen(
                        onAddClick = { openTypeSelect() },
                        onImportClick = { openTypeSelect() },
                        onCreateClick = { openTypeSelect() },
                        onSelectDirClick = { mDirPickerLauncher.launch(null) },
                        onEditClick = { instance ->
                            setSelectedInstance(instance)
                            Tools.swapFragment(
                                requireActivity(),
                                InstanceEditorFragment::class.java,
                                InstanceEditorFragment.TAG,
                                null
                            )
                        },
                        onDeleteClick = { instance ->
                            MaterialAlertDialogBuilder(requireContext(), R.style.AppAlertDialogTheme)
                                .setTitle(R.string.instance_delete)
                                .setMessage(R.string.instance_delete_confirmation)
                                .setPositiveButton(R.string.global_yes) { _, _ ->
                                    mViewModel.deleteInstance(instance) {
                                        Toast.makeText(requireContext(), "Deleted", Toast.LENGTH_SHORT).show()
                                    }
                                }
                                .setNegativeButton(R.string.global_no, null)
                                .show()
                        },
                        onSearch = { mViewModel.updateSearchQuery(it) },
                        onSelect = { instance ->
                            mViewModel.selectInstance(instance)
                            ExtraCore.setValue(ExtraConstants.REFRESH_VERSION_SPINNER, null)
                        },
                        onFilterChange = { r, s, m -> mViewModel.updateFilters(r, s, m) },
                        profiles = mViewModel.filteredList,
                        selectedPathName = mViewModel.selectedInstancePathName,
                        searchQuery = mViewModel.searchQuery,
                        showReleases = mViewModel.showReleases,
                        showSnapshots = mViewModel.showSnapshots,
                        showModded = mViewModel.showModded,
                        isLoading = mViewModel.isLoading
                    )
                }
            }
        }
    }

    private fun openTypeSelect() {
        Tools.swapFragment(
            requireActivity(),
            ProfileTypeSelectFragment::class.java,
            ProfileTypeSelectFragment.TAG,
            null
        )
    }

    override fun onResume() {
        super.onResume()
        ExtraCore.setValue(ExtraConstants.REFRESH_ACCOUNT_SPINNER, true)
        mViewModel.loadProfiles()
    }

    companion object {
        const val TAG: String = "ProfileSelectionFragment"
    }
}
