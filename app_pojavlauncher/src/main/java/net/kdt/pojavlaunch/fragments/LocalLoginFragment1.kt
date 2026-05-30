package net.kdt.pojavlaunch.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.platform.ComposeView
import androidx.fragment.app.Fragment
import net.ashmeet.hyperlauncher.R
import net.kdt.pojavlaunch.Tools.dialog
import net.kdt.pojavlaunch.Tools.swapFragment
import net.kdt.pojavlaunch.extra.ExtraConstants
import net.kdt.pojavlaunch.extra.ExtraCore
import net.kdt.pojavlaunch.ui.screens.LocalLoginScreen
import net.kdt.pojavlaunch.ui.theme.PojavTheme
import java.util.regex.Pattern

class LocalLoginFragment : Fragment() {
    private val mUsernameValidationPattern: Pattern = Pattern.compile("^[a-zA-Z0-9_]*$")

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setContent {
                // ✅ Enabled dynamic color
                PojavTheme(dynamicColor = true) {
                    LocalLoginScreen(
                        onBack = { parentFragmentManager.popBackStack() },
                        onLoginClick = { username ->
                            if (!checkUsername(username)) {
                                dialog(
                                    requireContext(),
                                    requireContext().getString(R.string.local_login_bad_username_title),
                                    requireContext().getString(R.string.local_login_bad_username_text)
                                )
                            } else {
                                ExtraCore.setValue(
                                    ExtraConstants.MOJANG_LOGIN_TODO,
                                    arrayOf(username, "")
                                )
                                swapFragment(
                                    requireActivity(),
                                    MainMenuFragment::class.java,
                                    MainMenuFragment.TAG,
                                    null
                                )
                            }
                        }
                    )
                }
            }
        }
    }

    private fun checkUsername(username: String): Boolean {
        val matcher = mUsernameValidationPattern.matcher(username)
        return !(username.isEmpty()
                || username.length < 3 || username.length > 16 || !matcher.find()
                )
    }

    companion object {
        const val TAG: String = "LOCAL_LOGIN_FRAGMENT"
    }
}
