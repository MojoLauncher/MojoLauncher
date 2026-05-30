package net.kdt.pojavlaunch

import android.annotation.SuppressLint
import android.app.Activity
import android.content.ClipData
import android.content.ComponentName
import android.content.DialogInterface
import android.content.Intent
import android.content.ServiceConnection
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.view.InputDevice
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.TextView
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.annotation.Keep
import androidx.annotation.RequiresApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.kdt.LoggerView
import net.ashmeet.hyperlauncher.R
import net.kdt.pojavlaunch.LwjglGlfwKeycode.GLFW_KEY_ESCAPE
import net.kdt.pojavlaunch.authenticator.accounts.Accounts
import net.kdt.pojavlaunch.authenticator.accounts.MinecraftAccount
import net.kdt.pojavlaunch.customcontrols.ControlButtonMenuListener
import net.kdt.pojavlaunch.customcontrols.ControlData
import net.kdt.pojavlaunch.customcontrols.ControlDrawerData
import net.kdt.pojavlaunch.customcontrols.ControlJoystickData
import net.kdt.pojavlaunch.customcontrols.ControlLayout
import net.kdt.pojavlaunch.customcontrols.CustomControls
import net.kdt.pojavlaunch.customcontrols.EditorExitable
import net.kdt.pojavlaunch.customcontrols.buttons.ControlInterface
import net.kdt.pojavlaunch.customcontrols.handleview.DrawerPullButton
import net.kdt.pojavlaunch.customcontrols.mouse.GyroControl
import net.kdt.pojavlaunch.customcontrols.mouse.HotbarView
import net.kdt.pojavlaunch.customcontrols.mouse.Touchpad
import net.kdt.pojavlaunch.customcontrols.keyboard.TouchCharInput
import net.kdt.pojavlaunch.customcontrols.keyboard.LwjglCharSender
import net.kdt.pojavlaunch.lifecycle.ContextExecutor
import net.kdt.pojavlaunch.prefs.LauncherPreferences
import net.kdt.pojavlaunch.services.GameService
import net.kdt.pojavlaunch.tasks.AsyncAssetManager
import net.kdt.pojavlaunch.ui.screens.BaseMainScreen
import net.kdt.pojavlaunch.ui.screens.ButtonEditDialog
import net.kdt.pojavlaunch.ui.screens.QuickSettingsDialog
import net.kdt.pojavlaunch.ui.theme.PojavTheme
import net.kdt.pojavlaunch.utils.JREUtils
import net.kdt.pojavlaunch.utils.MCOptionUtils
import net.kdt.pojavlaunch.utils.RendererCompatUtil
import net.kdt.pojavlaunch.utils.jre.GameRunner
import org.lwjgl.glfw.CallbackBridge


class MainActivity : BaseActivity(), ControlButtonMenuListener, EditorExitable, ServiceConnection,
    GrabListener {
    private var minecraftGLView: MinecraftGLSurface? = null
    private var loggerView: LoggerView? = null
    private var navDrawer: android.widget.ListView? = null
    private var mDrawerPullButton: DrawerPullButton? = null
    private var mGyroControl: GyroControl? = null
    private var mControlLayout: ControlLayout? = null
    private var mHotbarView: HotbarView? = null

    private val mLoadingVisible = mutableStateOf(true)
    private val mDrawerValue = mutableStateOf(DrawerValue.Closed)
    private val mShowQuickSettings = mutableStateOf(false)
    private val mEditingButton = mutableStateOf<ControlInterface?>(null)

    var instance: net.kdt.pojavlaunch.instances.Instance? = null
    var minecraftAccount: MinecraftAccount? = null

    var isInEditorState = mutableStateOf(false)

    private var mServiceBinder: GameService.LocalBinder? = null

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        instance = net.kdt.pojavlaunch.instances.Instances.loadSelectedInstance()
        minecraftAccount = Accounts.current
        if (instance == null) {
            Toast.makeText(this, R.string.instance_dir_missing, Toast.LENGTH_LONG).show()
            finish()
            return
        }
        AsyncAssetManager.extractDefaultSettings(this, instance!!.gameDirectory!!)
        MCOptionUtils.load(instance!!.gameDirectory!!.absolutePath)

        val gameServiceIntent: Intent = Intent(this, GameService::class.java)
        // Start the service a bit early
        ContextCompat.startForegroundService(this, gameServiceIntent)
        
        // Initialize UI via Compose
        initLayout()

        mGyroControl = GyroControl(this)

        // Handled by BaseActivity now, but ensure we don't break TextureView
        if (LauncherPreferences.PREF_USE_ALTERNATE_SURFACE && LauncherPreferences.PREF_BACKGROUND_PATH == null) {
            getWindow().setBackgroundDrawable(null)
        }

        // Set the sustained performance mode for available APIs
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) getWindow().setSustainedPerformanceMode(
            LauncherPreferences.PREF_SUSTAINED_PERFORMANCE
        )

        // Recompute the gui scale when options are changed
        val optionListener = MCOptionUtils.MCOptionListener { MCOptionUtils.mcScale() }
        MCOptionUtils.addMCOptionListener(optionListener)
        
        // Set the activity for the executor. Must do this here, or else Tools.showErrorRemote() may not
        // execute the correct method
        ContextExecutor.setActivity(this)
        //Now, attach to the service. The game will only start when this happens, to make sure that we know the right state.
        bindService(gameServiceIntent, this, 0)
    }

    @SuppressLint("ClickableViewAccessibility")
    protected fun initLayout() {
        // ✅ Disable drawer button stroke as requested
        LauncherPreferences.PREF_DRAWER_BUTTON_STROKE_ENABLED = false

        setContent {
            // ✅ Enabled dynamic color for the game activity UI
            PojavTheme(dynamicColor = true) {
                // ✅ Fix: Use a standard drawer state and sync it with external state
                val drawerState = rememberDrawerState(DrawerValue.Closed)
                
                // Sync external state (from button click) to internal drawer state
                LaunchedEffect(mDrawerValue.value) {
                    if (mDrawerValue.value == DrawerValue.Open) {
                        drawerState.open()
                    } else {
                        drawerState.close()
                    }
                }
                
                // Sync internal drawer state back to external state (for swipe-to-close)
                LaunchedEffect(drawerState.currentValue) {
                    mDrawerValue.value = drawerState.currentValue
                }

                BaseMainScreen(
                    drawerState = drawerState,
                    loadingVisible = mLoadingVisible.value,
                    onLoadingClick = { hideLoading() },
                    onControlLayoutBound = { 
                        mControlLayout = it 
                        mControlLayout?.setMenuListener(this@MainActivity)
                        mControlLayout?.modifiable = false
                        // Trigger control loading as soon as the layout is bound and metrics are ready
                        mControlLayout?.post {
                             Tools.getDisplayMetrics(this@MainActivity)
                             loadControls()
                        }
                    },
                    onGlSurfaceBound = { 
                        minecraftGLView = it 
                        CallbackBridge.addGrabListener(minecraftGLView)
                        setupSurfaceReadyListener()
                    },
                    onTouchpadBound = { 
                        MainActivity.touchpad = it 
                        CallbackBridge.addGrabListener(MainActivity.touchpad)
                    },
                    onCharInputBound = { 
                        MainActivity.touchCharInput = it 
                        MainActivity.touchCharInput?.setCharacterSender(LwjglCharSender())
                    },
                    onPullButtonBound = { 
                        mDrawerPullButton = it 
                        setupDrawerButton()
                    },
                    onHotbarBound = { mHotbarView = it },
                    onLoggerBound = { loggerView = it },
                    onNavListBound = { navDrawer = it },
                    onDismissMenu = { mDrawerValue.value = DrawerValue.Closed },
                    drawerContent = { isExpanded ->
                        RailMenuContent(isExpanded)
                    }
                )

                // ✅ Material 3 Quick Settings Dialog
                if (mShowQuickSettings.value) {
                    QuickSettingsDialog(
                        onDismissRequest = { mShowQuickSettings.value = false },
                        onResolutionChanged = {
                            minecraftGLView?.refreshSize()
                            mHotbarView?.onResolutionChanged()
                        },
                        onGyroStateChanged = {
                            mGyroControl?.updateOrientation()
                            if (LauncherPreferences.PREF_ENABLE_GYRO) mGyroControl?.enable() else mGyroControl?.disable()
                        }
                    )
                }

                // ✅ Material 3 Button Edit Dialog
                mEditingButton.value?.let { button ->
                    ButtonEditDialog(
                        controlData = button.properties!!,
                        onDismissRequest = { mEditingButton.value = null },
                        onSaveRequest = {
                            button.setProperties(button.properties, false)
                            // ✅ Dialog stays open for real-time editing
                        }
                    )
                }
            }
        }
        
        CallbackBridge.addGrabListener(this)

        try {
            val latestLogFile = java.io.File(Tools.DIR_GAME_HOME, "latestlog.txt")
            if (!latestLogFile.exists() && !latestLogFile.createNewFile()) throw java.io.IOException(
                "Failed to create a new log file"
            )
            net.kdt.pojavlaunch.Logger.begin(latestLogFile.absolutePath)
            MainActivity.GLOBAL_CLIPBOARD =
                getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager

            var version = getIntent().getStringExtra(MainActivity.INTENT_MINECRAFT_VERSION)
            version = if (version == null) instance!!.versionId else version
            setTitle("Minecraft " + version)
        } catch (e: kotlin.Throwable) {
            Tools.showError(this, e, true)
        }
    }

    @Composable
    private fun RailMenuContent(isExpanded: Boolean) {
        val menuIngameStrings = remember { getResources().getStringArray(R.array.menu_ingame) }
        val menuEditorStrings = remember { getResources().getStringArray(R.array.menu_customcontrol) }
        
        if (!isInEditorState.value) {
            // In-game Menu Items
            menuIngameStrings.forEachIndexed { index, label ->
                val icon = when(index) {
                    0 -> Icons.Default.Close
                    1 -> Icons.AutoMirrored.Filled.List
                    2 -> Icons.Default.Edit
                    3 -> Icons.Default.Settings
                    4 -> Icons.Default.Build
                    else -> Icons.Default.Settings
                }
                RailItem(icon, label, isExpanded) {
                    when (index) {
                        0 -> Tools.dialogForceClose(this@MainActivity)
                        1 -> openLogOutput()
                        2 -> dialogSendCustomKey()
                        3 -> openQuickSettings()
                        4 -> openCustomControls()
                    }
                    mDrawerValue.value = DrawerValue.Closed
                }
            }
        } else {
            // Editor Menu Items with unique icons
            menuEditorStrings.forEachIndexed { index, label ->
                val icon = when(index) {
                    0 -> Icons.Default.Add
                    1 -> Icons.Default.Menu
                    2 -> Icons.Default.Settings // Using settings for joystick
                    3 -> Icons.Default.Refresh
                    4 -> Icons.Default.Done
                    5 -> Icons.Default.Star
                    6 -> Icons.AutoMirrored.Filled.ExitToApp
                    else -> Icons.Default.Settings
                }
                RailItem(icon, label, isExpanded) {
                    when (index) {
                        0 -> mControlLayout?.addControlButton(ControlData("New"))
                        1 -> mControlLayout?.addDrawer(ControlDrawerData())
                        2 -> mControlLayout?.addJoystickButton(ControlJoystickData())
                        3 -> mControlLayout?.openLoadDialog()
                        4 -> mControlLayout?.openSaveDialog(this@MainActivity)
                        5 -> mControlLayout?.openSetDefaultDialog()
                        6 -> mControlLayout?.openExitDialog(this@MainActivity)
                    }
                    mDrawerValue.value = DrawerValue.Closed
                }
            }
        }
    }

    @Composable
    private fun RailItem(icon: ImageVector, label: String, isExpanded: Boolean, onClick: () -> Unit) {
        if (isExpanded) {
            // ✅ Text on the right when expanded
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 4.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { onClick() },
                color = Color.Transparent
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(icon, contentDescription = null, modifier = Modifier.size(24.dp))
                    Spacer(Modifier.width(16.dp))
                    Text(label, style = MaterialTheme.typography.bodyMedium, maxLines = 1)
                }
            }
        } else {
            // Standard centered icon when collapsed
            NavigationRailItem(
                icon = { Icon(icon, contentDescription = null, modifier = Modifier.size(24.dp)) },
                label = { Text(label, style = MaterialTheme.typography.labelSmall) },
                selected = false,
                onClick = onClick,
                alwaysShowLabel = false
            )
        }
    }

    private fun setupSurfaceReadyListener() {
        var version = getIntent().getStringExtra(MainActivity.INTENT_MINECRAFT_VERSION)
        version = if (version == null) instance!!.versionId else version
        val finalVersion = version

        minecraftGLView?.setSurfaceReadyListener(object : MinecraftGLSurface.SurfaceReadyListener {
            override fun onSurfaceReady() {
                try {
                    if (LauncherPreferences.PREF_VIRTUAL_MOUSE_START) {
                        touchpad?.post { touchpad?.switchState() }
                    }
                    runCraft(finalVersion)
                } catch (e: Throwable) {
                    Tools.showErrorRemote(e)
                }
            }
        })
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setupDrawerButton() {
        mDrawerPullButton?.setOnClickListener { onClickedMenu() }
        mDrawerPullButton?.setOnTouchListener(object : View.OnTouchListener {
            private var initialX = 0f
            private var initialY = 0f
            private var initialTouchX = 0f
            private var initialTouchY = 0f
            private var isDragging = false

            override fun onTouch(v: android.view.View, event: MotionEvent): kotlin.Boolean {
                if (!LauncherPreferences.PREF_DRAWER_BUTTON_MOVABLE) return false
                when (event.getAction()) {
                    MotionEvent.ACTION_DOWN -> {
                        initialX = v.x
                        initialY = v.y
                        initialTouchX = event.getRawX()
                        initialTouchY = event.getRawY()
                        isDragging = false
                        return false
                    }
                    MotionEvent.ACTION_MOVE -> {
                        val dx = event.getRawX() - initialTouchX
                        val dy = event.getRawY() - initialTouchY
                        if (kotlin.math.abs(dx) > 10 || kotlin.math.abs(dy) > 10) isDragging = true
                        if (isDragging) {
                            var nextX = initialX + dx
                            var nextY = initialY + dy
                            nextX = kotlin.math.max(0f, kotlin.math.min((mControlLayout!!.getWidth() - v.width).toFloat(), nextX))
                            nextY = kotlin.math.max(0f, kotlin.math.min((mControlLayout!!.getHeight() - v.height).toFloat(), nextY))
                            v.x = nextX
                            v.y = nextY
                            val parentRangeX = (mControlLayout!!.width - v.width).toFloat()
                            val parentRangeY = (mControlLayout!!.height - v.height).toFloat()
                            LauncherPreferences.PREF_DRAWER_BUTTON_X = if (parentRangeX > 0) (nextX / parentRangeX) * 100f else 0f
                            LauncherPreferences.PREF_DRAWER_BUTTON_Y = if (parentRangeY > 0) (nextY / parentRangeY) * 100f else 0f
                            return true
                        }
                    }
                    MotionEvent.ACTION_UP -> if (isDragging) {
                        LauncherPreferences.DEFAULT_PREF?.edit()
                            ?.putInt("drawerButtonX", LauncherPreferences.PREF_DRAWER_BUTTON_X.toInt())
                            ?.putInt("drawerButtonY", LauncherPreferences.PREF_DRAWER_BUTTON_Y.toInt())
                            ?.putString("drawerButtonPreset", "custom")
                            ?.commit()
                        return true
                    }
                }
                return false
            }
        })
    }

    private fun loadControls() {
        if (mControlLayout == null || instance == null) return
        try {
            mControlLayout?.loadLayout(instance!!.launchControls!!)
        } catch (e: java.io.IOException) {
            try {
                mControlLayout?.loadLayout(Tools.CTRLDEF_FILE!!)
            } catch (ioException: java.io.IOException) {
                Tools.showError(this, ioException)
            }
        } catch (th: Throwable) {
            Tools.showError(this, th)
        }
        updateDrawerButton()
        mControlLayout?.toggleControlVisible()
    }

    private fun updateDrawerButton() {
        if (mDrawerPullButton == null || mControlLayout == null) return
        mDrawerPullButton!!.setVisibility(if (mControlLayout!!.hasMenuButton()) android.view.View.GONE else android.view.View.VISIBLE)
        mDrawerPullButton!!.updateCustomImage()

        mDrawerPullButton!!.post(java.lang.Runnable {
            val parentWidth = mControlLayout!!.width
            val parentHeight = mControlLayout!!.height
            if (parentWidth == 0 || parentHeight == 0) return@Runnable

            val sizePx = (LauncherPreferences.PREF_DRAWER_BUTTON_SIZE * resources.displayMetrics.density).toInt()
            val lp = mDrawerPullButton!!.layoutParams as? android.widget.FrameLayout.LayoutParams ?: return@Runnable
            lp.width = sizePx
            lp.height = sizePx

            val xPercent: Float = LauncherPreferences.PREF_DRAWER_BUTTON_X / 100f
            val yPercent: Float = LauncherPreferences.PREF_DRAWER_BUTTON_Y / 100f

            lp.gravity = android.view.Gravity.TOP or android.view.Gravity.START
            lp.leftMargin = (xPercent * (parentWidth - sizePx)).toInt()
            lp.topMargin = (yPercent * (parentHeight - sizePx)).toInt()
            mDrawerPullButton!!.layoutParams = lp
        })
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
    }

    private fun hideLoading() {
        mLoadingVisible.value = false
    }

    public override fun onResume() {
        super.onResume()
        if (LauncherPreferences.PREF_ENABLE_GYRO) mGyroControl?.enable()
        CallbackBridge.nativeSetWindowAttrib(LwjglGlfwKeycode.GLFW_HOVERED, 1)
        updateDrawerButton()
    }

    override fun onPause() {
        mGyroControl?.disable()
        if (CallbackBridge.isGrabbing) CallbackBridge.sendKeyPress(GLFW_KEY_ESCAPE.toInt())
        CallbackBridge.nativeSetWindowAttrib(LwjglGlfwKeycode.GLFW_HOVERED, 0)
        super.onPause()
    }

    override fun onStart() {
        super.onStart()
        CallbackBridge.nativeSetWindowAttrib(LwjglGlfwKeycode.GLFW_VISIBLE, 1)
    }

    override fun onStop() {
        CallbackBridge.nativeSetWindowAttrib(LwjglGlfwKeycode.GLFW_VISIBLE, 0)
        super.onStop()
    }

    override fun onDestroy() {
        super.onDestroy()
        CallbackBridge.removeGrabListener(MainActivity.touchpad)
        CallbackBridge.removeGrabListener(minecraftGLView)
        CallbackBridge.removeGrabListener(this)
        ContextExecutor.clearActivity()
    }

    override fun onConfigurationChanged(newConfig: android.content.res.Configuration) {
        super.onConfigurationChanged(newConfig)
        if (mGyroControl != null) mGyroControl!!.updateOrientation()
        if (mControlLayout == null) return
        mControlLayout!!.requestLayout()
        mControlLayout!!.post {
            minecraftGLView?.refreshSize()
            mControlLayout!!.refreshControlButtonPositions()
            updateDrawerButton()
        }
    }

    override fun onPostResume() {
        super.onPostResume()
        if (minecraftGLView != null)
            Tools.MAIN_HANDLER.postDelayed({ minecraftGLView?.refreshSize() }, 500)
    }

    override fun onActivityResult(requestCode: kotlin.Int, resultCode: kotlin.Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 1 && resultCode == Activity.RESULT_OK) {
            if (!Tools.checkStorageRoot(this)) return
            LauncherPreferences.loadPreferences(getApplicationContext())
            try {
                mControlLayout?.loadLayout(LauncherPreferences.PREF_DEFAULTCTRL_PATH!!)
            } catch (e: java.io.IOException) {
                e.printStackTrace()
            }
        }
    }

    @Throws(Throwable::class)
    private fun runCraft(versionId: kotlin.String?) {
        var renderer = instance!!.launchRenderer
        if (!RendererCompatUtil.checkRendererCompatible(this, renderer!!)) {
            val renderersList: RendererCompatUtil.RenderersList = RendererCompatUtil.getCompatibleRenderers(this)
            val firstCompatibleRenderer: kotlin.String? = renderersList.rendererIds.get(0)
            renderer = firstCompatibleRenderer
        }
        JREUtils.redirectAndPrintJRELog()
        GameRunner.launchMinecraft(this, minecraftAccount!!, instance!!, versionId!!, renderer!!)
        Tools.runOnUiThread {
            if (mServiceBinder != null) mServiceBinder!!.isActive = false
            Tools.exitToLauncher(this@MainActivity)
        }
    }

    override fun onGrabState(isGrabbing: kotlin.Boolean) {
        if (isGrabbing) hideLoading()
        mControlLayout?.post {
            mControlLayout?.setControlVisible(mControlLayout?.areControlVisible() ?: false)
        }
    }

    private fun dialogSendCustomKey() {
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.control_customkey)
            .setItems(EfficientAndroidLWJGLKeycode.generateKeyName()) { _, position ->
                EfficientAndroidLWJGLKeycode.execKeyIndex(position)
            }
            .show()
    }

    private fun openCustomControls() {
        mControlLayout?.modifiable = true
        mDrawerPullButton?.visibility = android.view.View.VISIBLE
        isInEditorState.value = true
    }

    private fun openLogOutput() {
        loggerView?.visibility = View.VISIBLE
    }

    private fun openQuickSettings() {
        mShowQuickSettings.value = true
    }

    override fun dispatchKeyEvent(event: android.view.KeyEvent): Boolean {
        if (isInEditorState.value) {
            if (event.getKeyCode() == android.view.KeyEvent.KEYCODE_BACK) {
                if (event.action == android.view.KeyEvent.ACTION_DOWN) mControlLayout?.askToExit(this)
                return true
            }
            return super.dispatchKeyEvent(event)
        }
        var handleEvent = false
        if (!(minecraftGLView?.processKeyEvent(event) ?: false.also { handleEvent = it })) {
            if (event.getKeyCode() == android.view.KeyEvent.KEYCODE_BACK && !(MainActivity.touchCharInput?.isEnabled ?: false)) {
                if (event.action != android.view.KeyEvent.ACTION_UP) return true
                CallbackBridge.sendKeyPress(LwjglGlfwKeycode.GLFW_KEY_ESCAPE.toInt())
                return true
            }
            handleEvent = false
        } else {
            handleEvent = true
        }
        return handleEvent
    }

    override fun onClickedMenu() {
        mDrawerValue.value = DrawerValue.Open
    }

    override fun exitEditor() {
        try {
            mControlLayout?.modifiable = false
            
            // Sync current control layout to instance
            mControlLayout?.mLayoutFileName?.let {
                if (it.isNotEmpty()) {
                    instance?.controlLayout = "$it.json"
                    instance?.maybeWrite()
                }
            }

            // Reload from instance path to ensure clean state and correct file
            val reloadPath = instance!!.launchControls!!
            mControlLayout?.loadLayout(null as CustomControls?)
            java.lang.System.gc()
            mControlLayout?.loadLayout(reloadPath)
            updateDrawerButton()
        } catch (e: java.lang.Exception) {
            Tools.showError(this, e)
        }
        isInEditorState.value = false
    }

    override fun editControlButton(button: ControlInterface) {
        mEditingButton.value = button
    }

    override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
        val localBinder = service as GameService.LocalBinder
        mServiceBinder = localBinder
        minecraftGLView?.start(localBinder.isActive, MainActivity.touchpad)
        localBinder.isActive = true
    }

    override fun onServiceDisconnected(name: ComponentName?) {}

    @RequiresApi(api = Build.VERSION_CODES.O)
    private fun checkCaptureDispatchConditions(event: MotionEvent): kotlin.Boolean {
        val eventSource: kotlin.Int = event.source
        return (eventSource and InputDevice.SOURCE_MOUSE_RELATIVE) != 0 ||
                (eventSource and InputDevice.SOURCE_MOUSE) != 0
    }

    override fun dispatchTrackballEvent(ev: MotionEvent): kotlin.Boolean {
        if (Tools.isAndroid8OrHigher && checkCaptureDispatchConditions(ev)) return minecraftGLView?.dispatchCapturedPointerEvent(ev) ?: false
        else return super.dispatchTrackballEvent(ev)
    }

    companion object {
        @Volatile
        var GLOBAL_CLIPBOARD: android.content.ClipboardManager? = null
        const val INTENT_MINECRAFT_VERSION: kotlin.String = "intent_version"
        var touchCharInput: TouchCharInput? = null
        private var touchpad: Touchpad? = null

        @JvmStatic
        fun toggleMouse(ctx: android.content.Context?) {
            if (CallbackBridge.isGrabbing) return
            Toast.makeText(ctx, if (MainActivity.touchpad?.switchState() ?: false) R.string.control_mouseon else R.string.control_mouseoff, Toast.LENGTH_SHORT).show()
        }

        @JvmStatic
        fun switchKeyboardState() {
            if (MainActivity.touchCharInput != null) MainActivity.touchCharInput!!.switchKeyboardState()
        }

        @Keep
        @JvmStatic
        fun openLink(link: kotlin.String) {
            val ctx = MainActivity.touchpad?.context
            if (ctx is Activity) {
                ctx.runOnUiThread {
                    try {
                        if (link.startsWith("file:")) {
                            var truncLength = 5
                            if (link.startsWith("file://")) truncLength = 7
                            Tools.openPath(ctx, java.io.File(link.substring(truncLength)), false)
                        } else {
                            val intent = Intent(Intent.ACTION_VIEW)
                            intent.setDataAndType(android.net.Uri.parse(link), "*/*")
                            ctx.startActivity(intent)
                        }
                    } catch (th: Throwable) {
                        Tools.showError(ctx, th)
                    }
                }
            }
        }

        @JvmStatic
        fun openPath(path: kotlin.String) {
            val ctx = MainActivity.touchpad?.context
            if (ctx is Activity) {
                ctx.runOnUiThread {
                    try {
                        Tools.openPath(ctx, java.io.File(path), false)
                    } catch (th: Throwable) {
                        Tools.showError(ctx, th)
                    }
                }
            }
        }

        @Keep
        @JvmStatic
        fun querySystemClipboard() {
            Tools.runOnUiThread {
                val clipData = MainActivity.GLOBAL_CLIPBOARD?.primaryClip
                if (clipData == null) {
                    AWTInputBridge.nativeClipboardReceived(null, null)
                    return@runOnUiThread
                }
                val clipItemText = clipData.getItemAt(0).text
                if (clipItemText == null) {
                    AWTInputBridge.nativeClipboardReceived(null, null)
                    return@runOnUiThread
                }
                AWTInputBridge.nativeClipboardReceived(clipItemText.toString(), "plain")
            }
        }

        @Keep
        @JvmStatic
        fun putClipboardData(data: kotlin.String?, mimeType: kotlin.String) {
            Tools.runOnUiThread {
                var clipData: ClipData? = null
                when (mimeType) {
                    "text/plain" -> clipData = ClipData.newPlainText("AWT Paste", data)
                    "text/html" -> clipData = ClipData.newHtmlText("AWT Paste", data, data)
                }
                if (clipData != null) MainActivity.GLOBAL_CLIPBOARD?.setPrimaryClip(clipData)
            }
        }
    }
}
