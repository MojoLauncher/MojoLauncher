package net.kdt.pojavlaunch.fragments

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.fragment.app.Fragment
import net.ashmeet.hyperlauncher.R
import net.kdt.pojavlaunch.CustomControlsActivity
import net.kdt.pojavlaunch.Tools
import net.kdt.pojavlaunch.Tools.launchModInstaller
import net.kdt.pojavlaunch.Tools.openPath
import net.kdt.pojavlaunch.Tools.openURL
import net.kdt.pojavlaunch.Tools.shareLog
import net.kdt.pojavlaunch.Tools.swapFragment
import net.kdt.pojavlaunch.authenticator.AuthType
import net.kdt.pojavlaunch.authenticator.accounts.Accounts
import net.kdt.pojavlaunch.authenticator.accounts.MinecraftAccount
import net.kdt.pojavlaunch.contracts.OpenDocumentWithExtension
import net.kdt.pojavlaunch.extra.ExtraConstants
import net.kdt.pojavlaunch.extra.ExtraCore
import net.kdt.pojavlaunch.instances.Instances.Companion.loadSelectedInstance
import net.kdt.pojavlaunch.progresskeeper.ProgressKeeper.clearAll
import net.kdt.pojavlaunch.progresskeeper.ProgressKeeper.hasOngoingTasks
import net.kdt.pojavlaunch.progresskeeper.ProgressKeeper.taskCount
import net.kdt.pojavlaunch.ui.screens.MainMenuRevamp
import net.kdt.pojavlaunch.ui.theme.PojavTheme
import net.kdt.pojavlaunch.utils.FileUtils.ensureDirectorySilently
import java.io.File

class MainMenuFragment : Fragment() {

    private val mModInstallerLauncher = registerForActivityResult(
        OpenDocumentWithExtension("jar")
    ) { data: Uri? ->
        if (data != null) launchModInstaller(requireContext(), data)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setContent {
                PojavTheme(dynamicColor = true) {
                    Box(modifier = Modifier.fillMaxSize().background(Color.Transparent)) {
                        MainMenuRevamp(
                            onEditProfileClick = {
                                swapFragment(
                                    requireActivity(),
                                    InstanceEditorFragment::class.java,
                                    InstanceEditorFragment.TAG,
                                    null
                                )
                            },
                            onCustomControlsClick = {
                                startActivity(Intent(requireContext(), CustomControlsActivity::class.java))
                            },
                            onInstallJarClick = { runInstallerWithConfirmation() },
                            onShareLogsClick = { shareLog(requireContext()) },
                            onOpenFilesClick = { openGameDirectory(requireContext()) },
                            onWikiClick = { openURL(requireActivity(), Tools.URL_HOME) },
                            onSocialMediaClick = { openURL(requireActivity(), getString(R.string.social_media_invite)) },
                            onPlayClick = { ExtraCore.setValue(ExtraConstants.LAUNCH_GAME, true) },
                            onTerminateClick = {
                                if (hasOngoingTasks()) {
                                    clearAll()
                                    Toast.makeText(requireContext(), R.string.notification_terminate, Toast.LENGTH_SHORT).show()
                                } else {
                                    Toast.makeText(requireContext(), "No service running", Toast.LENGTH_SHORT).show()
                                }
                            },
                            onInstanceSelect = {
                                swapFragment(
                                    requireActivity(),
                                    ProfileSelectionFragment::class.java,
                                    ProfileSelectionFragment.TAG,
                                    null
                                )
                            }
                        )
                    }
                }
            }
        }
    }

    private fun openGameDirectory(context: Context) {
        val instance = loadSelectedInstance()
        if (instance == null) {
            Toast.makeText(context, R.string.no_instance, Toast.LENGTH_LONG).show()
            return
        }
        val gameDirectory: File? = instance.gameDirectory
        if (gameDirectory != null && ensureDirectorySilently(gameDirectory)) {
            openPath(context, gameDirectory, false)
        } else {
            Toast.makeText(context, R.string.gamedir_open_failed, Toast.LENGTH_LONG).show()
        }
    }

    private fun runInstallerWithConfirmation() {
        if (taskCount == 0) {
            mModInstallerLauncher.launch(null)
        } else Toast.makeText(requireContext(), R.string.tasks_ongoing, Toast.LENGTH_LONG).show()
    }

    companion object {
        const val TAG: String = "MainMenuFragment"
    }
}
