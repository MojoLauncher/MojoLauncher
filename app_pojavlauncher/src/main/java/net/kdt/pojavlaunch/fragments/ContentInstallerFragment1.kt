package net.kdt.pojavlaunch.fragments

import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.ActivityResultCallback
import androidx.activity.result.contract.ActivityResultContracts.GetContent
import androidx.compose.ui.platform.ComposeView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import net.ashmeet.hyperlauncher.R
import net.kdt.pojavlaunch.Tools
import net.kdt.pojavlaunch.instances.Instances.Companion.loadSelectedInstance
import net.kdt.pojavlaunch.ui.screens.ContentInstallerScreen
import net.kdt.pojavlaunch.ui.screens.ContentInstallerViewModel
import net.kdt.pojavlaunch.ui.screens.ContentInstallerType
import net.kdt.pojavlaunch.ui.theme.PojavTheme

class ContentInstallerFragment : Fragment() {
    private lateinit var mViewModel: ContentInstallerViewModel

    private val mPickLocalContent = registerForActivityResult(
        GetContent(),
        ActivityResultCallback { uri: Uri? -> this.installLocalUriToInstance(uri) })

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        mViewModel = ViewModelProvider(this)[ContentInstallerViewModel::class.java]
        mViewModel.init(requireContext())

        return ComposeView(requireContext()).apply {
            setContent {
                PojavTheme(dynamicColor = true) {
                    ContentInstallerScreen(
                        onBack = { Tools.backToMainMenu(requireActivity()) },
                        onOpenDownloads = {
                            parentFragmentManager.beginTransaction()
                                .replace(R.id.container_fragment, DirectoryManagerFragment())
                                .addToBackStack(null)
                                .commit()
                        },
                        onInstallLocal = { mPickLocalContent.launch("*/*") },
                        onSearch = { query, type, version, loader ->
                            mViewModel.triggerSearch(query, type)
                        },
                        onProjectClick = { project ->
                            mViewModel.loadVersions(project)
                        },
                        onVersionClick = { version ->
                            mViewModel.downloadVersion(requireContext(), version, mViewModel.selectedType)
                        },
                        projects = mViewModel.projects,
                        isLoading = mViewModel.isLoading,
                        statusText = mViewModel.statusText,
                        selectedVersion = mViewModel.versionFilter,
                        selectedLoader = mViewModel.loaderFilter,
                        onVersionFilterChange = { mViewModel.versionFilter = it },
                        onLoaderFilterChange = { mViewModel.loaderFilter = it },
                        instanceVersion = mViewModel.instanceVersion,
                        instanceLoader = mViewModel.instanceLoader,
                        viewingProject = mViewModel.viewingProject,
                        projectVersions = mViewModel.projectVersions,
                        availableProjectMCVersions = mViewModel.availableProjectMCVersions,
                        selectedProjectMCVersion = mViewModel.selectedProjectMCVersion,
                        onProjectMCVersionClick = { mcVersion ->
                            mViewModel.selectedProjectMCVersion = if (mcVersion.isEmpty()) null else mcVersion
                        },
                        onBackToProjects = { mViewModel.viewingProject = null }
                    )
                }
            }
        }
    }

    private fun installLocalUriToInstance(uri: Uri?) {
        if (uri == null) return
        if (loadSelectedInstance() == null) {
            Toast.makeText(requireContext(), R.string.no_instance, Toast.LENGTH_LONG).show()
            return
        }
        Toast.makeText(requireContext(), "Local install not supported yet", Toast.LENGTH_SHORT).show()
    }

    companion object {
        const val TAG: String = "ContentInstallerFrag"
    }
}
