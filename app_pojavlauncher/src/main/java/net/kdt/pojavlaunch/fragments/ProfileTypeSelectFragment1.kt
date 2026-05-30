package net.kdt.pojavlaunch.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.platform.ComposeView
import androidx.fragment.app.Fragment
import net.kdt.pojavlaunch.Tools.showError
import net.kdt.pojavlaunch.Tools.swapFragment
import net.kdt.pojavlaunch.instances.Instances.Companion.createDefaultInstance
import net.kdt.pojavlaunch.instances.Instances.Companion.setSelectedInstance
import net.kdt.pojavlaunch.ui.screens.ProfileTypeSelectScreen
import net.kdt.pojavlaunch.ui.theme.PojavTheme
import java.io.IOException

class ProfileTypeSelectFragment : Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setContent {
                // ✅ Enabled dynamic color
                PojavTheme(dynamicColor = true) {
                    ProfileTypeSelectScreen(
                        onBack = { parentFragmentManager.popBackStack() },
                        onVanillaClick = {
                            try {
                                val instance = createDefaultInstance()
                                setSelectedInstance(instance)
                                swapFragment(
                                    requireActivity(), InstanceEditorFragment::class.java,
                                    InstanceEditorFragment.TAG, Bundle(1)
                                )
                            } catch (e: IOException) {
                                showError(requireContext(), e)
                            }
                        },
                        onOptifineClick = {
                            swapFragment(requireActivity(), OptiFineInstallFragment::class.java, OptiFineInstallFragment.TAG, null)
                        },
                        onFabricClick = {
                            swapFragment(requireActivity(), FabricInstallFragment::class.java, FabricInstallFragment.TAG, null)
                        },
                        onForgeClick = {
                            swapFragment(requireActivity(), ForgeInstallFragment::class.java, ForgeInstallFragment.TAG, null)
                        },
                        onQuiltClick = {
                            swapFragment(requireActivity(), QuiltInstallFragment::class.java, QuiltInstallFragment.TAG, null)
                        },
                        onNeoForgeClick = {
                            swapFragment(requireActivity(), NeoforgeInstallFragment::class.java, NeoforgeInstallFragment.TAG, null)
                        },
                        onLegacyFabricClick = {
                            swapFragment(requireActivity(), LegacyFabricInstallFragment::class.java, LegacyFabricInstallFragment.TAG, null)
                        },
                        onModpackClick = {
                            swapFragment(requireActivity(), SearchModFragment::class.java, SearchModFragment.TAG, null)
                        },
                        onBTAClick = {
                            swapFragment(requireActivity(), BTAInstallFragment::class.java, BTAInstallFragment.TAG, null)
                        }
                    )
                }
            }
        }
    }

    companion object {
        const val TAG: String = "ProfileTypeSelectFragment"
    }
}
