package net.kdt.pojavlaunch.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.platform.ComposeView
import androidx.fragment.app.Fragment
import net.kdt.pojavlaunch.Tools
import net.kdt.pojavlaunch.ui.screens.SettingsScreen
import net.kdt.pojavlaunch.ui.theme.PojavTheme

class SettingsFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setContent {
                PojavTheme(dynamicColor = true) {
                    SettingsScreen(
                        onBack = {
                            Tools.backToMainMenu(requireActivity())
                        }
                    )
                }
            }
        }
    }

    companion object {
        const val TAG = "SettingsFragment"
    }
}
