package net.kdt.pojavlaunch.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.compose.ui.platform.ComposeView
import androidx.fragment.app.Fragment
import com.kdt.mcgui.ProgressLayout
import net.ashmeet.hyperlauncher.R
import net.kdt.pojavlaunch.Tools.swapFragment
import net.kdt.pojavlaunch.progresskeeper.ProgressKeeper.hasProgressKey
import net.kdt.pojavlaunch.ui.screens.SelectAuthScreen
import net.kdt.pojavlaunch.ui.theme.PojavTheme

class SelectAuthFragment : Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setContent {
                // ✅ Enabled dynamic color
                PojavTheme(dynamicColor = true) {
                    SelectAuthScreen(
                        onBack = { parentFragmentManager.popBackStack() },
                        onMicrosoftClick = {
                            launchAuthFragment(MicrosoftLoginFragment::class.java, MicrosoftLoginFragment.TAG)
                        },
                        onLocalClick = {
                            launchAuthFragment(LocalLoginFragment::class.java, LocalLoginFragment.TAG)
                        },
                        onElyByClick = {
                            launchAuthFragment(ElyByLoginFragment::class.java, ElyByLoginFragment.TAG)
                        }
                    )
                }
            }
        }
    }

    private fun launchAuthFragment(fragmentClass: Class<out Fragment>, fragmentTag: String?) {
        if (hasProgressKey(ProgressLayout.AUTHENTICATE)) {
            Toast.makeText(requireContext(), R.string.tasks_ongoing, Toast.LENGTH_SHORT).show()
            return
        }
        swapFragment(requireActivity(), fragmentClass, fragmentTag, null)
    }

    companion object {
        const val TAG: String = "AUTH_SELECT_FRAGMENT"
    }
}
