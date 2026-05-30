package net.kdt.pojavlaunch

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.os.Build
import android.util.AttributeSet
import android.util.Log
import android.view.InputDevice
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.Surface
import android.view.View
import android.view.ViewGroup
import androidx.annotation.RequiresApi
import fr.spse.gamepad_remapper.GamepadHandler
import fr.spse.gamepad_remapper.RemapperManager
import fr.spse.gamepad_remapper.RemapperView
import net.kdt.pojavlaunch.customcontrols.ControlLayout
import net.kdt.pojavlaunch.customcontrols.gamepad.DefaultDataProvider
import net.kdt.pojavlaunch.customcontrols.gamepad.Gamepad
import net.kdt.pojavlaunch.customcontrols.gamepad.direct.DirectGamepad
import net.kdt.pojavlaunch.customcontrols.gamepad.direct.DirectGamepadEnableHandler
import net.kdt.pojavlaunch.customcontrols.mouse.AbstractTouchpad
import net.kdt.pojavlaunch.customcontrols.mouse.AndroidPointerCapture
import net.kdt.pojavlaunch.customcontrols.mouse.InGUIEventProcessor
import net.kdt.pojavlaunch.customcontrols.mouse.InGameEventProcessor
import net.kdt.pojavlaunch.customcontrols.mouse.TouchEventProcessor
import net.kdt.pojavlaunch.prefs.LauncherPreferences
import net.kdt.pojavlaunch.render.SurfaceProvider.SurfaceCallback
import net.kdt.pojavlaunch.render.SurfaceViewSurfaceProvider
import net.kdt.pojavlaunch.render.TextureViewSurfaceProvider
import net.kdt.pojavlaunch.utils.JREUtils.applyWindowSize
import net.kdt.pojavlaunch.utils.JREUtils.releaseBridgeWindow
import net.kdt.pojavlaunch.utils.JREUtils.setupBridgeWindow
import net.kdt.pojavlaunch.utils.MCOptionUtils
import org.lwjgl.glfw.CallbackBridge
import org.lwjgl.glfw.CallbackBridge.isGrabbing
import org.lwjgl.glfw.CallbackBridge.sendCursorPos
import org.lwjgl.glfw.CallbackBridge.sendMouseButton
import org.lwjgl.glfw.CallbackBridge.sendScroll
import org.lwjgl.glfw.CallbackBridge.sendUpdateWindowSize
import org.lwjgl.glfw.CallbackBridge.setDirectGamepadEnableHandler

/**
 * Class dealing with showing minecraft surface and taking inputs to dispatch them to minecraft
 */
class MinecraftGLSurface @JvmOverloads constructor(
    context: Context?,
    attributeSet: AttributeSet? = null
) : View(context, attributeSet), GrabListener, DirectGamepadEnableHandler, SurfaceCallback {
    /* Gamepad object for gamepad inputs, instantiated on need */
    private var mGamepadHandler: GamepadHandler? = null

    /* The RemapperView.Builder object allows you to set which buttons to remap */
    private val mInputManager = RemapperManager(
        getContext(), RemapperView.Builder(null)
            .remapA(true)
            .remapB(true)
            .remapX(true)
            .remapY(true)

            .remapLeftJoystick(true)
            .remapRightJoystick(true)
            .remapStart(true)
            .remapSelect(true)
            .remapLeftShoulder(true)
            .remapRightShoulder(true)
            .remapLeftTrigger(true)
            .remapRightTrigger(true)
            .remapDpad(true)
    )

    /* Sensitivity, adjusted according to screen size */
    private val mSensitivityFactor: Double =
        (1.4 * (1080f / (Tools.getDisplayMetrics(getContext() as Activity).heightPixels.toFloat())))

    private val mSurfaceProvider =
        if (LauncherPreferences.PREF_USE_ALTERNATE_SURFACE) SurfaceViewSurfaceProvider() else TextureViewSurfaceProvider()
    private var mRefreshOnly = true

    /* Surface ready listener, used by the activity to launch minecraft */
    var mSurfaceReadyListener: SurfaceReadyListener? = null
    val mSurfaceReadyListenerLock: Any = Any()

    /* View holding the surface, either a SurfaceView or a TextureView */
    var mSurface: View? = null

    private val mIngameProcessor = InGameEventProcessor(mSensitivityFactor)
    private val mInGUIProcessor = InGUIEventProcessor()
    private var mCurrentTouchProcessor: TouchEventProcessor = mInGUIProcessor
    private var mPointerCapture: AndroidPointerCapture? = null
    private var mLastGrabState = false

    init {
        isFocusable = true
        setDirectGamepadEnableHandler(this)
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private fun setUpPointerCapture(touchpad: AbstractTouchpad?) {
        if (mPointerCapture != null) mPointerCapture!!.detach()
        if (touchpad != null) {
            mPointerCapture = AndroidPointerCapture(touchpad, this)
        }
    }

    /** Initialize the view and all its settings
     * @param isAlreadyRunning set to true to tell the view that the game is already running
     * (only updates the window without calling the start listener)
     * @param touchpad the optional cursor-emulating touchpad, used for touch event processing
     * when the cursor is not grabbed
     */
    fun start(isAlreadyRunning: Boolean, touchpad: AbstractTouchpad?) {
        if (Tools.isAndroid8OrHigher) setUpPointerCapture(touchpad)
        mInGUIProcessor.setAbstractTouchpad(touchpad)
        mRefreshOnly = isAlreadyRunning
        mSurface = mSurfaceProvider.create(getContext(), this)
        (parent as ViewGroup).addView(mSurface)
    }

    /**
     * The touch event for both grabbed an non-grabbed mouse state on the touch screen
     * Does not cover the virtual mouse touchpad
     */
    override fun onTouchEvent(e: MotionEvent): Boolean {
        // Kinda need to send this back to the layout
        if ((parent as ControlLayout).modifiable) return false

        // Looking for a mouse to handle, won't have an effect if no mouse exists.
        for (i in 0..<e.pointerCount) {
            val toolType = e.getToolType(i)
            if (toolType == MotionEvent.TOOL_TYPE_MOUSE) {
                if (Tools.isAndroid8OrHigher &&
                    mPointerCapture != null
                ) {
                    mPointerCapture!!.handleAutomaticCapture()
                    return true
                }
            } else if (toolType != MotionEvent.TOOL_TYPE_STYLUS) continue

            // Mouse found
            if (isGrabbing) return false
            sendCursorPos(
                e.getX(i) * LauncherPreferences.PREF_SCALE_FACTOR,
                e.getY(i) * LauncherPreferences.PREF_SCALE_FACTOR
            )
            return true //mouse event handled successfully
        }
        return mCurrentTouchProcessor.processTouchEvent(e)
    }

    private fun createGamepad(contextView: View, inputDevice: InputDevice?) {
        if (CallbackBridge.sGamepadDirectInput) {
            mGamepadHandler = DirectGamepad()
        } else {
            mGamepadHandler = Gamepad(contextView, inputDevice, DefaultDataProvider.INSTANCE, true)
        }
    }

    /**
     * The event for mouse/joystick movements
     */
    @SuppressLint("NewApi")
    override fun dispatchGenericMotionEvent(event: MotionEvent): Boolean {
        var mouseCursorIndex = -1

        if (Gamepad.isGamepadEvent(event)) {
            if (mGamepadHandler == null) createGamepad(this, event.device)

            mInputManager.handleMotionEventInput(getContext(), event, mGamepadHandler)
            return true
        }

        for (i in 0..<event.pointerCount) {
            if (event.getToolType(i) != MotionEvent.TOOL_TYPE_MOUSE && event.getToolType(i) != MotionEvent.TOOL_TYPE_STYLUS) continue
            // Mouse found
            mouseCursorIndex = i
            break
        }
        if (mouseCursorIndex == -1) return false // we cant consoom that, theres no mice!


        // Make sure we grabbed the mouse if necessary
        updateGrabState(isGrabbing)

        when (event.actionMasked) {
            MotionEvent.ACTION_HOVER_MOVE -> {
                CallbackBridge.mouseX =
                    (event.getX(mouseCursorIndex) * LauncherPreferences.PREF_SCALE_FACTOR)
                CallbackBridge.mouseY =
                    (event.getY(mouseCursorIndex) * LauncherPreferences.PREF_SCALE_FACTOR)
                sendCursorPos(CallbackBridge.mouseX, CallbackBridge.mouseY)
                return true
            }

            MotionEvent.ACTION_SCROLL -> {
                sendScroll(
                    event.getAxisValue(MotionEvent.AXIS_HSCROLL).toDouble(),
                    event.getAxisValue(MotionEvent.AXIS_VSCROLL).toDouble()
                )
                return true
            }

            MotionEvent.ACTION_BUTTON_PRESS -> return sendMouseButtonUnconverted(
                event.actionButton,
                true
            )

            MotionEvent.ACTION_BUTTON_RELEASE -> return sendMouseButtonUnconverted(
                event.actionButton,
                false
            )

            else -> return false
        }
    }

    /** The event for keyboard/ gamepad button inputs  */
    fun processKeyEvent(event: KeyEvent): Boolean {
        val eventKeycode = event.keyCode
        if (eventKeycode == KeyEvent.KEYCODE_UNKNOWN) return true
        if (eventKeycode == KeyEvent.KEYCODE_VOLUME_DOWN) return false
        if (eventKeycode == KeyEvent.KEYCODE_VOLUME_UP) return false
        if (event.repeatCount != 0) return true
        val action = event.action
        if (action == KeyEvent.ACTION_MULTIPLE) return true
        if (action == KeyEvent.ACTION_UP &&
            (event.flags and KeyEvent.FLAG_CANCELED) != 0
        ) return true

        if ((event.flags and KeyEvent.FLAG_SOFT_KEYBOARD) == KeyEvent.FLAG_SOFT_KEYBOARD) {
            if (eventKeycode == KeyEvent.KEYCODE_ENTER) return true

            MainActivity.touchCharInput?.dispatchKeyEvent(event)
            return true
        }

        if (event.device != null
            && ((event.source and InputDevice.SOURCE_MOUSE_RELATIVE) == InputDevice.SOURCE_MOUSE_RELATIVE
                    || (event.source and InputDevice.SOURCE_MOUSE) == InputDevice.SOURCE_MOUSE)
        ) {
            if (eventKeycode == KeyEvent.KEYCODE_BACK) {
                CallbackBridge.sendMouseButton(
                    LwjglGlfwKeycode.GLFW_MOUSE_BUTTON_RIGHT.toInt(),
                    event.action == KeyEvent.ACTION_DOWN
                )
                return true
            }
        }

        if (Gamepad.isGamepadEvent(event)) {
            if (mGamepadHandler == null) createGamepad(this, event.device)

            mInputManager.handleKeyEventInput(getContext(), event, mGamepadHandler)
            return true
        }

        val index = EfficientAndroidLWJGLKeycode.getIndexByKey(eventKeycode)
        if (EfficientAndroidLWJGLKeycode.containsIndex(index)) {
            EfficientAndroidLWJGLKeycode.execKey(event, index)
            return true
        }

        return (event.flags and KeyEvent.FLAG_FALLBACK) == KeyEvent.FLAG_FALLBACK
    }

    @JvmOverloads
    fun refreshSize(immediate: Boolean = false) {
        if (isInLayout && !immediate) {
            post { this.refreshSize() }
            return
        }
        val newWidth =
            Tools.getDisplayFriendlyRes(width, LauncherPreferences.PREF_SCALE_FACTOR)
        val newHeight =
            Tools.getDisplayFriendlyRes(height, LauncherPreferences.PREF_SCALE_FACTOR)
        if (newHeight < 1 || newWidth < 1) {
            Log.e("MGLSurface", String.format("Impossible resolution : %dx%d", newWidth, newHeight))
            return
        }
        CallbackBridge.windowWidth = newWidth
        CallbackBridge.windowHeight = newHeight
        if (mSurface == null) {
            Log.w("MGLSurface", "Attempt to refresh size on null surface")
            return
        }
        sendUpdateWindowSize(CallbackBridge.windowWidth, CallbackBridge.windowHeight)
        applyWindowSize()
    }

    private fun realStart() {
        refreshSize(true)

        MCOptionUtils.set("fullscreen", "off")
        MCOptionUtils.set("overrideWidth", CallbackBridge.windowWidth.toString())
        MCOptionUtils.set("overrideHeight", CallbackBridge.windowHeight.toString())
        MCOptionUtils.save()
        MCOptionUtils.mcScale()

        Thread({
            try {
                synchronized(mSurfaceReadyListenerLock) {
                    if (mSurfaceReadyListener == null) (mSurfaceReadyListenerLock as java.lang.Object).wait()
                }

                mSurfaceReadyListener!!.onSurfaceReady()
            } catch (e: Throwable) {
                Tools.showError(getContext(), e, true)
            }
        }, "JVM Main thread").start()
    }

    override fun onGrabState(isGrabbing: Boolean) {
        post { updateGrabState(isGrabbing) }
    }

    private fun pickEventProcessor(isGrabbing: Boolean): TouchEventProcessor {
        return if (isGrabbing) mIngameProcessor else mInGUIProcessor
    }

    private fun updateGrabState(isGrabbing: Boolean) {
        if (mLastGrabState != isGrabbing) {
            mCurrentTouchProcessor.cancelPendingActions()
            mCurrentTouchProcessor = pickEventProcessor(isGrabbing)
            mLastGrabState = isGrabbing
        }
    }

    override fun onDirectGamepadEnabled() {
        post {
            if (mGamepadHandler != null && mGamepadHandler is Gamepad) {
                (mGamepadHandler as Gamepad).removeSelf()
            }
            mGamepadHandler = null
        }
    }

    override fun onSurfaceAvailable(surface: Surface?) {
        setupBridgeWindow(surface)
        if (mRefreshOnly) return
        realStart()
        mRefreshOnly = true
    }

    override fun onSurfaceResized() {
    }

    override fun onSurfaceDestroyed() {
        releaseBridgeWindow()
    }

    fun interface SurfaceReadyListener {
        fun onSurfaceReady()
    }

    fun setSurfaceReadyListener(listener: SurfaceReadyListener?) {
        synchronized(mSurfaceReadyListenerLock) {
            mSurfaceReadyListener = listener
            (mSurfaceReadyListenerLock as java.lang.Object).notifyAll()
        }
    }

    companion object {
        @JvmStatic
        fun sendMouseButtonUnconverted(button: Int, status: Boolean): Boolean {
            var glfwButton = -256
            when (button) {
                MotionEvent.BUTTON_PRIMARY -> glfwButton =
                    LwjglGlfwKeycode.GLFW_MOUSE_BUTTON_LEFT.toInt()

                MotionEvent.BUTTON_TERTIARY -> glfwButton =
                    LwjglGlfwKeycode.GLFW_MOUSE_BUTTON_MIDDLE.toInt()

                MotionEvent.BUTTON_SECONDARY -> glfwButton =
                    LwjglGlfwKeycode.GLFW_MOUSE_BUTTON_RIGHT.toInt()
            }
            if (glfwButton == -256) return false
            sendMouseButton(glfwButton, status)
            return true
        }
    }
}
