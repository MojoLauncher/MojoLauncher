package net.kdt.pojavlaunch

import android.Manifest
import android.app.NotificationManager
import android.content.DialogInterface
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Build.VERSION
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts.RequestPermission
import androidx.compose.runtime.*
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import net.ashmeet.hyperlauncher.R
import net.kdt.pojavlaunch.authenticator.accounts.Accounts
import net.kdt.pojavlaunch.extra.ExtraConstants
import net.kdt.pojavlaunch.extra.ExtraCore
import net.kdt.pojavlaunch.extra.ExtraListener
import net.kdt.pojavlaunch.fragments.ContentInstallerFragment
import net.kdt.pojavlaunch.fragments.DirectoryManagerFragment
import net.kdt.pojavlaunch.fragments.MainMenuFragment
import net.kdt.pojavlaunch.fragments.MicrosoftLoginFragment
import net.kdt.pojavlaunch.fragments.SelectAuthFragment
import net.kdt.pojavlaunch.fragments.SettingsFragment
import net.kdt.pojavlaunch.instances.InstanceInstaller.Companion.postInstallCheck
import net.kdt.pojavlaunch.instances.Instances.Companion.loadSelectedInstance
import net.kdt.pojavlaunch.lifecycle.ContextAwareDoneListener
import net.kdt.pojavlaunch.lifecycle.ContextExecutor.clearActivity
import net.kdt.pojavlaunch.lifecycle.ContextExecutor.setActivity
import net.kdt.pojavlaunch.modloaders.modpacks.imagecache.IconCacheJanitor.Companion.runJanitor
import net.kdt.pojavlaunch.prefs.LauncherPreferences
import net.kdt.pojavlaunch.progresskeeper.ProgressKeeper.addTaskCountListener
import net.kdt.pojavlaunch.progresskeeper.ProgressKeeper.removeTaskCountListener
import net.kdt.pojavlaunch.progresskeeper.TaskCountListener
import net.kdt.pojavlaunch.services.ProgressServiceKeeper
import net.kdt.pojavlaunch.tasks.AsyncMinecraftDownloader.getListedVersion
import net.kdt.pojavlaunch.tasks.AsyncMinecraftDownloader.normalizeVersionId
import net.kdt.pojavlaunch.tasks.AsyncVersionList
import net.kdt.pojavlaunch.tasks.AsyncVersionList.VersionDoneListener
import net.kdt.pojavlaunch.tasks.MinecraftDownloader
import net.kdt.pojavlaunch.ui.screens.LauncherScreen
import net.kdt.pojavlaunch.ui.theme.PojavTheme
import net.kdt.pojavlaunch.utils.NotificationUtils
import net.kdt.pojavlaunch.utils.RendererCompatUtil.checkRendererCompatible
import net.kdt.pojavlaunch.utils.RendererCompatUtil.getCompatibleRenderers
import net.kdt.pojavlaunch.utils.jre.GameRunner.pickRuntime
import java.lang.ref.WeakReference

class LauncherActivity : BaseActivity() {
    private var mProgressServiceKeeper: ProgressServiceKeeper? = null
    private var mNotificationManager: NotificationManager? = null

    // Compose State
    private var taskCountState = mutableIntStateOf(0)
    private var isProgressVisibleState = mutableStateOf(false)
    private var isFragmentOpenState = mutableStateOf(false)

    /* Allows to switch from one button "type" to another */
    private val mFragmentCallbackListener: FragmentManager.FragmentLifecycleCallbacks =
        object : FragmentManager.FragmentLifecycleCallbacks() {
            override fun onFragmentResumed(fm: FragmentManager, f: Fragment) {
                isFragmentOpenState.value = f !is MainMenuFragment
            }
        }

    /* Listener for the back button in settings */
    private val mBackPreferenceListener = ExtraListener { _: String?, value: String? ->
        if (value == "true") onBackPressed()
        false
    }

    /* Listener for the auth method selection screen */
    private val mSelectAuthMethod = ExtraListener { _: String?, value: Boolean? ->
        val manager = supportFragmentManager
        if (value != true || manager.isStateSaved) return@ExtraListener false
        val fragment = manager.findFragmentById(R.id.container_fragment)
        if (fragment !is MainMenuFragment) return@ExtraListener false

        Tools.swapFragment(this, SelectAuthFragment::class.java, SelectAuthFragment.TAG, null)
        false
    }

    private val mLaunchGameListener = ExtraListener { _: String?, _: Boolean? ->
        // Check if tasks are ongoing (could use taskCountState.intValue)
        val selectedInstance = loadSelectedInstance()

        if (selectedInstance == null) {
            Toast.makeText(this, R.string.no_instance, Toast.LENGTH_LONG).show()
            return@ExtraListener false
        }

        if (selectedInstance.installer != null) {
            selectedInstance.installer!!.start()
            return@ExtraListener false
        }

        if (!Tools.isValidString(selectedInstance.versionId)) {
            Toast.makeText(this, R.string.error_no_version, Toast.LENGTH_LONG).show()
            return@ExtraListener false
        }

        if (Accounts.current == null) {
            Toast.makeText(this, R.string.no_saved_accounts, Toast.LENGTH_LONG).show()
            ExtraCore.setValue(ExtraConstants.SELECT_AUTH_METHOD, true)
            return@ExtraListener false
        }

        val normalizedVersionId = normalizeVersionId(selectedInstance.versionId)
        val mcVersion = getListedVersion(normalizedVersionId)

        try {
            var requiredJavaVersion = 8
            val javaVersionInfo = mcVersion?.javaVersion
            if (javaVersionInfo != null) {
                requiredJavaVersion = javaVersionInfo.majorVersion
            }
            pickRuntime(selectedInstance, requiredJavaVersion)

            val renderer = selectedInstance.launchRenderer
            if (!checkRendererCompatible(this, renderer)) {
                val renderersList = getCompatibleRenderers(this)
                if (renderersList.rendererIds.isNotEmpty()) {
                    selectedInstance.renderer = renderersList.rendererIds[0]
                    selectedInstance.maybeWrite()
                }
            }
        } catch (e: Throwable) {
            Log.e("LauncherActivity", "Pre-launch preparation failed", e)
        }

        MinecraftDownloader().start(
            assets,
            mcVersion,
            normalizedVersionId!!,
            ContextAwareDoneListener(this, normalizedVersionId)
        )
        false
    }

    private val mDoubleLaunchPreventionListener: TaskCountListener =
        object : TaskCountListener {
            override fun onUpdateTaskCount(taskCount: Int): Boolean {
                if (taskCount > 0) {
                    runOnUiThread { mNotificationManager!!.cancel(NotificationUtils.NOTIFICATION_ID_GAME_START) }
                }
                return false
            }
        }

    private val mTopProgressBarListener: TaskCountListener = object : TaskCountListener {
        override fun onUpdateTaskCount(taskCount: Int): Boolean {
            runOnUiThread {
                taskCountState.intValue = taskCount
                if (taskCount == 0) isProgressVisibleState.value = false
            }
            return false
        }
    }

    private var mRequestNotificationPermissionLauncher: ActivityResultLauncher<String?>? = null
    private var mRequestNotificationPermissionRunnable: WeakReference<Runnable?>? = null

    override fun setFullscreen(): Boolean {
        return true
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // ✅ Enable edge-to-edge support AFTER super.onCreate to override any default system bar logic
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT)
        )

        runJanitor()
        mRequestNotificationPermissionLauncher = registerForActivityResult(
            RequestPermission()
        ) { isAllowed: Boolean? ->
            if (isAllowed != true) handleNoNotificationPermission()
            else {
                val runnable = Tools.getWeakReference(mRequestNotificationPermissionRunnable)
                runnable?.run()
            }
        } as ActivityResultLauncher<String?>?

        mNotificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        addTaskCountListener(mDoubleLaunchPreventionListener)
        addTaskCountListener(mTopProgressBarListener)
        addTaskCountListener((ProgressServiceKeeper(this).also { mProgressServiceKeeper = it }))

        ExtraCore.addExtraListener(ExtraConstants.BACK_PREFERENCE, mBackPreferenceListener)
        ExtraCore.addExtraListener(ExtraConstants.SELECT_AUTH_METHOD, mSelectAuthMethod)
        ExtraCore.addExtraListener(ExtraConstants.LAUNCH_GAME, mLaunchGameListener)

        AsyncVersionList().getVersionList(object : VersionDoneListener {
            override fun onVersionDone(versions: JMinecraftVersionList?) {
                ExtraCore.setValue(ExtraConstants.RELEASE_TABLE, versions)
            }
        })

        supportFragmentManager.registerFragmentLifecycleCallbacks(mFragmentCallbackListener, true)

        setContent {
            PojavTheme(dynamicColor = true) {
                LauncherScreen(
                    onHomeClick = {
                        Tools.backToMainMenu(this@LauncherActivity)
                    },
                    onSettingsClick = {
                        val manager = supportFragmentManager
                        if (!manager.isStateSaved) {
                            val fragment = manager.findFragmentById(R.id.container_fragment)
                            if (fragment is SettingsFragment) {
                                Tools.backToMainMenu(this)
                            } else {
                                Tools.swapFragment(
                                    this,
                                    SettingsFragment::class.java,
                                    SettingsFragment.TAG,
                                    null
                                )
                            }
                        }
                    },
                    onInstallerClick = {
                        if (!supportFragmentManager.isStateSaved) {
                            val currentFragment =
                                supportFragmentManager.findFragmentById(R.id.container_fragment)
                            if (currentFragment is ContentInstallerFragment) {
                                Tools.backToMainMenu(this)
                            } else {
                                Tools.swapFragment(
                                    this,
                                    ContentInstallerFragment::class.java,
                                    ContentInstallerFragment.TAG,
                                    null
                                )
                            }
                        }
                    },
                    onFilesClick = {
                        if (!supportFragmentManager.isStateSaved) {
                            val currentFragment =
                                supportFragmentManager.findFragmentById(R.id.container_fragment)
                            if (currentFragment is DirectoryManagerFragment) {
                                Tools.backToMainMenu(this)
                            } else {
                                val gameDir = loadSelectedInstance()?.gameDirectory
                                if (gameDir != null) {
                                    Tools.swapFragment(
                                        this,
                                        DirectoryManagerFragment::class.java,
                                        DirectoryManagerFragment.TAG,
                                        DirectoryManagerFragment.argsForRoot(
                                            gameDir,
                                            getString(R.string.launcher_files_title)
                                        )
                                    )
                                } else {
                                    Toast.makeText(this, R.string.no_instance, Toast.LENGTH_SHORT)
                                        .show()
                                }
                            }
                        }
                    },
                    onProgressClick = {
                        if (taskCountState.intValue > 0) {
                            isProgressVisibleState.value = !isProgressVisibleState.value
                        } else {
                            Toast.makeText(this, "No tasks running", Toast.LENGTH_SHORT).show()
                        }
                    },
                    fragmentManager = supportFragmentManager,
                    isProgressVisible = isProgressVisibleState.value,
                    taskCount = taskCountState.intValue,
                    isFragmentOpen = isFragmentOpenState.value
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        setActivity(this)
        postInstallCheck(this)
    }

    override fun onPause() {
        super.onPause()
        clearActivity()
    }

    override fun onStart() {
        super.onStart()
    }

    override fun onDestroy() {
        super.onDestroy()
        removeTaskCountListener(mTopProgressBarListener)
        removeTaskCountListener(mDoubleLaunchPreventionListener)
        removeTaskCountListener(mProgressServiceKeeper)
        ExtraCore.removeExtraListenerFromValue(
            ExtraConstants.BACK_PREFERENCE,
            mBackPreferenceListener
        )
        ExtraCore.removeExtraListenerFromValue(ExtraConstants.SELECT_AUTH_METHOD, mSelectAuthMethod)
        ExtraCore.removeExtraListenerFromValue(ExtraConstants.LAUNCH_GAME, mLaunchGameListener)

        supportFragmentManager.unregisterFragmentLifecycleCallbacks(mFragmentCallbackListener)
    }

    override fun onBackPressed() {
        if (isProgressVisibleState.value) {
            isProgressVisibleState.value = false
            return
        }

        val fragment = getVisibleFragment(MicrosoftLoginFragment.TAG) as MicrosoftLoginFragment?
        if (fragment != null) {
            if (fragment.canGoBack()) {
                fragment.goBack()
                return
            }
        }

        super.onBackPressed()
    }

    private fun getVisibleFragment(tag: String?): Fragment? {
        val fragment = supportFragmentManager.findFragmentByTag(tag)
        if (fragment != null && fragment.isVisible) {
            return fragment
        }
        return null
    }

    private fun handleNoNotificationPermission() {
        LauncherPreferences.PREF_SKIP_NOTIFICATION_PERMISSION_CHECK = true
        LauncherPreferences.DEFAULT_PREF!!.edit()
            .putBoolean(LauncherPreferences.PREF_KEY_SKIP_NOTIFICATION_CHECK, true)
            .apply()
        Toast.makeText(this, R.string.notification_permission_toast, Toast.LENGTH_LONG).show()
    }

    fun checkForNotificationPermission(): Boolean {
        return VERSION.SDK_INT < 33 || ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.POST_NOTIFICATIONS
        ) != PackageManager.PERMISSION_DENIED
    }

    fun askForNotificationPermission(onSuccessRunnable: Runnable?) {
        if (VERSION.SDK_INT < 33) return
        if (onSuccessRunnable != null) {
            mRequestNotificationPermissionRunnable = WeakReference(onSuccessRunnable)
        }
        mRequestNotificationPermissionLauncher!!.launch(Manifest.permission.POST_NOTIFICATIONS)
    }

    companion object {
        const val SETTING_FRAGMENT_TAG: String = "SETTINGS_FRAGMENT"
    }

}
