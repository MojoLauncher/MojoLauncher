package net.kdt.pojavlaunch.fragments

import android.graphics.Bitmap
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.platform.ComposeView
import androidx.core.graphics.drawable.toDrawable
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import net.ashmeet.hyperlauncher.R
import net.kdt.pojavlaunch.Tools
import net.kdt.pojavlaunch.Tools.backToMainMenu
import net.kdt.pojavlaunch.Tools.swapFragment
import net.kdt.pojavlaunch.extra.ExtraConstants
import net.kdt.pojavlaunch.extra.ExtraCore
import net.kdt.pojavlaunch.instances.InstanceIconProvider
import net.kdt.pojavlaunch.profiles.VersionSelectorDialog
import net.kdt.pojavlaunch.profiles.VersionSelectorListener
import net.kdt.pojavlaunch.ui.screens.InstanceEditorScreen
import net.kdt.pojavlaunch.ui.screens.InstanceEditorViewModel
import net.kdt.pojavlaunch.ui.theme.PojavTheme
import net.kdt.pojavlaunch.utils.CropperUtils
import net.kdt.pojavlaunch.utils.CropperUtils.CropperReceiver

class InstanceEditorFragment : Fragment(), CropperReceiver {
    private lateinit var mViewModel: InstanceEditorViewModel
    private val mCropperLauncher = CropperUtils.registerCropper(this, this)

    override var targetMaxSide: Int = 512
        private set

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        mViewModel = ViewModelProvider(this)[InstanceEditorViewModel::class.java]
        mViewModel.init(requireContext())

        // Paths, which can be changed via FileSelectorFragment
        val value = ExtraCore.consumeValue(ExtraConstants.FILE_SELECTOR) as String?
        if (value != null) {
            mViewModel.controlLayout = value
        }

        return ComposeView(requireContext()).apply {
            setContent {
                // ✅ Enabled dynamic color
                PojavTheme(dynamicColor = true) {
                    InstanceEditorScreen(
                        onBack = { backToMainMenu(requireActivity()) },
                        onSave = {
                            mViewModel.instance?.let { InstanceIconProvider.dropIcon(it) }
                            mViewModel.save()
                            backToMainMenu(requireActivity())
                        },
                        onDelete = {
                            DeleteConfirmDialogFragment().show(childFragmentManager, "delete_dialog_confirm")
                        },
                        onIconClick = {
                            CropperUtils.startCropper(mCropperLauncher)
                        },
                        onVersionClick = {
                            VersionSelectorDialog.open(
                                requireContext(),
                                false,
                                object : VersionSelectorListener {
                                    override fun onVersionSelected(versionId: String?, isSnapshot: Boolean) {
                                        mViewModel.versionId = versionId ?: ""
                                    }
                                })
                        },
                        onControlClick = {
                            val bundle = Bundle(3)
                            bundle.putBoolean(FileSelectorFragment.BUNDLE_SELECT_FOLDER, false)
                            bundle.putString(FileSelectorFragment.BUNDLE_ROOT_PATH, Tools.CTRLMAP_PATH)
                            swapFragment(
                                requireActivity(),
                                FileSelectorFragment::class.java, FileSelectorFragment.TAG, bundle
                            )
                        },
                        instanceIcon = mViewModel.instanceIcon,
                        name = mViewModel.name,
                        onNameChange = { mViewModel.name = it },
                        versionId = mViewModel.versionId,
                        controlLayout = mViewModel.controlLayout,
                        jvmArgs = mViewModel.jvmArgs,
                        onJvmArgsChange = { mViewModel.jvmArgs = it },
                        sharedData = mViewModel.sharedData,
                        onSharedDataChange = { mViewModel.sharedData = it },
                        availableRuntimes = mViewModel.availableRuntimes,
                        selectedRuntime = mViewModel.selectedRuntime,
                        onRuntimeSelected = { mViewModel.selectedRuntime = it },
                        rendererDisplayNames = mViewModel.rendererDisplayNames,
                        selectedRendererIndex = mViewModel.selectedRendererIndex,
                        onRendererSelected = { mViewModel.selectedRendererIndex = it }
                    )
                }
            }
        }
    }

    override val aspectRatio: Float
        get() = 1f

    override fun onCropped(contentBitmap: Bitmap?) {
        if (contentBitmap == null) return
        mViewModel.updateIcon(contentBitmap)
        // Refresh drawable in VM
        mViewModel.instanceIcon = contentBitmap.toDrawable(resources)
    }

    override fun onFailed(exception: Exception?) {
        Tools.showErrorRemote(exception)
    }

    companion object {
        const val TAG: String = "InstanceEditorFragment"
    }
}
