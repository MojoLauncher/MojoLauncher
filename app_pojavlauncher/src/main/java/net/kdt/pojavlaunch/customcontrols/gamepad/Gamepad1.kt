package net.kdt.pojavlaunch.customcontrols.gamepad

import android.view.Choreographer
import android.view.Choreographer.FrameCallback
import android.view.InputDevice
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import androidx.core.content.res.ResourcesCompat
import androidx.core.math.MathUtils
import fr.spse.gamepad_remapper.GamepadHandler
import fr.spse.gamepad_remapper.Settings
import net.ashmeet.hyperlauncher.R
import net.kdt.pojavlaunch.GrabListener
import net.kdt.pojavlaunch.LwjglGlfwKeycode
import net.kdt.pojavlaunch.Tools.currentDisplayMetrics
import net.kdt.pojavlaunch.prefs.LauncherPreferences
import net.kdt.pojavlaunch.utils.MCOptionUtils.MCOptionListener
import net.kdt.pojavlaunch.utils.MCOptionUtils.addMCOptionListener
import net.kdt.pojavlaunch.utils.MCOptionUtils.mcScale
import org.lwjgl.glfw.CallbackBridge
import org.lwjgl.glfw.CallbackBridge.currentMods
import org.lwjgl.glfw.CallbackBridge.sendCursorPos
import org.lwjgl.glfw.CallbackBridge.sendKeyPress
import org.lwjgl.glfw.CallbackBridge.sendMouseButton
import org.lwjgl.glfw.CallbackBridge.sendScroll
import org.lwjgl.glfw.CallbackBridge.setModifiers
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin


open class Gamepad(
    contextView: View,
    inputDevice: InputDevice?,
    mapProvider: GamepadDataProvider,
    showCursor: Boolean
) : GrabListener, GamepadHandler {
    /* Sensitivity, adjusted according to screen size */
    private val mSensitivityFactor: Double = (1.4 * (1080f / (currentDisplayMetrics?.heightPixels ?: 1080)))

    private val mPointerImageView: ImageView

    private val mLeftJoystick: GamepadJoystick
    private var mCurrentJoystickDirection: Int = GamepadJoystick.Companion.DIRECTION_NONE

    private val mRightJoystick: GamepadJoystick
    private var mLastHorizontalValue = 0.0f
    private var mLastVerticalValue = 0.0f

    private var mMouseMagnitude = 0.0
    private var mMouseAngle = 0.0
    private var mMouseSensitivity = 19.0

    private var mGameMap: GamepadMap? = null
    private var mMenuMap: GamepadMap? = null
    private var currentMap: GamepadMap? = null

    private var isGrabbing = false


    /* Choreographer with time to compute delta on ticking */
    private val mScreenChoreographer: Choreographer
    private var mLastFrameTime: Long

    /* Listen for change in gui scale */
    //the field is used in a WeakReference
    private val mGuiScaleListener: MCOptionListener =
        MCOptionListener { notifyGUISizeChange(mcScale()) }

    private val mMapProvider: GamepadDataProvider

    private var mRemoved = false

    init {
        Settings.setDeadzoneScale(LauncherPreferences.PREF_DEADZONE_SCALE)

        mScreenChoreographer = Choreographer.getInstance()
        val frameCallback: FrameCallback = object : FrameCallback {
            override fun doFrame(frameTimeNanos: Long) {
                tick(frameTimeNanos)
                if (!mRemoved) mScreenChoreographer.postFrameCallback(this)
            }
        }
        mScreenChoreographer.postFrameCallback(frameCallback)
        mLastFrameTime = System.nanoTime()

        /* Add the listener for the cross hair */
        addMCOptionListener(mGuiScaleListener)

        mLeftJoystick = GamepadJoystick()
        mRightJoystick = GamepadJoystick()


        val ctx = contextView.getContext()
        mPointerImageView = ImageView(contextView.getContext())
        mPointerImageView.setImageDrawable(
            ResourcesCompat.getDrawable(
                ctx.getResources(),
                R.drawable.ic_gamepad_pointer,
                ctx.getTheme()
            )
        )
        mPointerImageView.getDrawable().setFilterBitmap(false)

        val size = ((22 * mcScale()) / LauncherPreferences.PREF_SCALE_FACTOR).toInt()
        mPointerImageView.setLayoutParams(FrameLayout.LayoutParams(size, size))

        mMapProvider = mapProvider

        sendCursorPos(CallbackBridge.windowWidth / 2f, CallbackBridge.windowHeight / 2f)

        if (showCursor) {
            (contextView.getParent() as ViewGroup).addView(mPointerImageView)
            centerPointer()
        }

        reloadGamepadMaps()
        mMapProvider.attachGrabListener(this)
    }


    fun reloadGamepadMaps() {
        if (mGameMap != null) mGameMap!!.resetPressedState()
        if (mMenuMap != null) mMenuMap!!.resetPressedState()
        GamepadMapStore.Companion.load()
        mGameMap = mMapProvider.gameMap
        mMenuMap = mMapProvider.menuMap
        this.currentMap = mGameMap
        // Force state refresh
        val currentGrab = CallbackBridge.isGrabbing
        isGrabbing = !currentGrab
        onGrabState(currentGrab)
    }

    fun updateJoysticks() {
        updateDirectionalJoystick()
        updateMouseJoystick()
    }

    fun notifyGUISizeChange(newSize: Int) {
        //Change the pointer size to match UI
        val size = ((22 * newSize) / LauncherPreferences.PREF_SCALE_FACTOR).toInt()
        mPointerImageView.post(Runnable {
            mPointerImageView.setLayoutParams(
                FrameLayout.LayoutParams(
                    size,
                    size
                )
            )
        })
    }


    /**
     * Send the new mouse position, computing the delta
     * @param frameTimeNanos The time to render the frame, used to compute mouse delta
     */
    private fun tick(frameTimeNanos: Long) {
        //update mouse position
        var newFrameTime = System.nanoTime()
        if (mLastHorizontalValue != 0f || mLastVerticalValue != 0f) {
            var acceleration: Double = mMouseMagnitude.pow(MOUSE_MAX_ACCELERATION)
            if (acceleration > 1) acceleration = 1.0

            // Compute delta since last tick time
            var deltaX = (cos(mMouseAngle) * acceleration * mMouseSensitivity).toFloat()
            var deltaY = (sin(mMouseAngle) * acceleration * mMouseSensitivity).toFloat()
            newFrameTime = System.nanoTime() // More accurate delta
            val deltaTimeScale = ((newFrameTime - mLastFrameTime) / 16666666f) // Scale of 1 = 60Hz
            deltaX *= deltaTimeScale
            deltaY *= deltaTimeScale

            CallbackBridge.mouseX += deltaX
            CallbackBridge.mouseY -= deltaY

            if (!isGrabbing) {
                CallbackBridge.mouseX =
                    MathUtils.clamp(CallbackBridge.mouseX, 0f, CallbackBridge.windowWidth.toFloat())
                CallbackBridge.mouseY = MathUtils.clamp(
                    CallbackBridge.mouseY,
                    0f,
                    CallbackBridge.windowHeight.toFloat()
                )
                placePointerView(
                    (CallbackBridge.mouseX / LauncherPreferences.PREF_SCALE_FACTOR).toInt(),
                    (CallbackBridge.mouseY / LauncherPreferences.PREF_SCALE_FACTOR).toInt()
                )
            }

            //Send the mouse to the game
            sendCursorPos(CallbackBridge.mouseX, CallbackBridge.mouseY)
        }

        // Update last nano time
        mLastFrameTime = newFrameTime
    }

    private fun updateMouseJoystick() {
        val currentJoystick = if (isGrabbing) mRightJoystick else mLeftJoystick
        val horizontalValue = currentJoystick.horizontalAxis
        val verticalValue = currentJoystick.verticalAxis
        if (horizontalValue != mLastHorizontalValue || verticalValue != mLastVerticalValue) {
            mLastHorizontalValue = horizontalValue
            mLastVerticalValue = verticalValue

            mMouseMagnitude = currentJoystick.magnitude
            mMouseAngle = currentJoystick.angleRadian

            tick(System.nanoTime())
            return
        }
        mLastHorizontalValue = horizontalValue
        mLastVerticalValue = verticalValue

        mMouseMagnitude = currentJoystick.magnitude
        mMouseAngle = currentJoystick.angleRadian
    }

    private fun updateDirectionalJoystick() {
        val currentJoystick = if (isGrabbing) mLeftJoystick else mRightJoystick

        val lastJoystickDirection = mCurrentJoystickDirection
        mCurrentJoystickDirection = currentJoystick.heightDirection

        if (mCurrentJoystickDirection == lastJoystickDirection) return

        Companion.sendDirectionalKeycode(lastJoystickDirection, false, this.currentMap!!)
        Companion.sendDirectionalKeycode(mCurrentJoystickDirection, true, this.currentMap!!)
    }


    /** Place the pointer on the screen, offsetting the image size  */
    private fun placePointerView(x: Int, y: Int) {
        mPointerImageView.setX(x - mPointerImageView.getWidth() / 2f)
        mPointerImageView.setY(y - mPointerImageView.getHeight() / 2f)
    }

    /** Update the grabbing state, and change the currentMap, mouse position and sensibility  */
    override fun onGrabState(isGrabbing: Boolean) {
        val lastGrabbingValue = this.isGrabbing
        this.isGrabbing = isGrabbing
        if (lastGrabbingValue == isGrabbing) return

        // Switch grabbing state then
        currentMap!!.resetPressedState()
        if (isGrabbing) {
            this.currentMap = mGameMap
            mPointerImageView.setVisibility(View.INVISIBLE)
            mMouseSensitivity = 18.0
            return
        }

        this.currentMap = mMenuMap
        Companion.sendDirectionalKeycode(
            mCurrentJoystickDirection,
            false,
            mGameMap!!
        ) // removing what we were doing

        sendCursorPos(CallbackBridge.windowWidth / 2f, CallbackBridge.windowHeight / 2f)
        centerPointer()
        mPointerImageView.setVisibility(View.VISIBLE)
        // Sensitivity in menu is MC and HARDWARE resolution dependent
        mMouseSensitivity = 19 * LauncherPreferences.PREF_SCALE_FACTOR / mSensitivityFactor
    }

    override fun handleGamepadInput(keycode: Int, value: Float) {
        val isKeyEventDown = value == 1f
        when (keycode) {
            KeyEvent.KEYCODE_BUTTON_A -> this.currentMap?.BUTTON_A?.update(isKeyEventDown)
            KeyEvent.KEYCODE_BUTTON_B -> this.currentMap?.BUTTON_B?.update(isKeyEventDown)
            KeyEvent.KEYCODE_BUTTON_X -> this.currentMap?.BUTTON_X?.update(isKeyEventDown)
            KeyEvent.KEYCODE_BUTTON_Y -> this.currentMap?.BUTTON_Y?.update(isKeyEventDown)
            KeyEvent.KEYCODE_BUTTON_L1 -> this.currentMap?.SHOULDER_LEFT?.update(isKeyEventDown)
            KeyEvent.KEYCODE_BUTTON_R1 -> this.currentMap?.SHOULDER_RIGHT?.update(isKeyEventDown)
            KeyEvent.KEYCODE_BUTTON_L2 -> this.currentMap?.TRIGGER_LEFT?.update(isKeyEventDown)
            KeyEvent.KEYCODE_BUTTON_R2 -> this.currentMap?.TRIGGER_RIGHT?.update(isKeyEventDown)
            KeyEvent.KEYCODE_BUTTON_THUMBL -> this.currentMap?.THUMBSTICK_LEFT?.update(
                isKeyEventDown
            )

            KeyEvent.KEYCODE_BUTTON_THUMBR -> this.currentMap?.THUMBSTICK_RIGHT?.update(
                isKeyEventDown
            )

            KeyEvent.KEYCODE_DPAD_UP -> this.currentMap?.DPAD_UP?.update(isKeyEventDown)
            KeyEvent.KEYCODE_DPAD_DOWN -> this.currentMap?.DPAD_DOWN?.update(isKeyEventDown)
            KeyEvent.KEYCODE_DPAD_LEFT -> this.currentMap?.DPAD_LEFT?.update(isKeyEventDown)
            KeyEvent.KEYCODE_DPAD_RIGHT -> this.currentMap?.DPAD_RIGHT?.update(isKeyEventDown)
            KeyEvent.KEYCODE_DPAD_CENTER -> {
                this.currentMap?.DPAD_RIGHT?.update(false)
                this.currentMap?.DPAD_LEFT?.update(false)
                this.currentMap?.DPAD_UP?.update(false)
                this.currentMap?.DPAD_DOWN?.update(false)
            }

            KeyEvent.KEYCODE_BUTTON_START -> this.currentMap?.BUTTON_START?.update(isKeyEventDown)
            KeyEvent.KEYCODE_BUTTON_SELECT -> this.currentMap?.BUTTON_SELECT?.update(isKeyEventDown)
            MotionEvent.AXIS_HAT_X -> {
                this.currentMap?.DPAD_RIGHT?.update(value > 0.85)
                this.currentMap?.DPAD_LEFT?.update(value < -0.85)
            }

            MotionEvent.AXIS_HAT_Y -> {
                this.currentMap?.DPAD_DOWN?.update(value > 0.85)
                this.currentMap?.DPAD_UP?.update(value < -0.85)
            }

            MotionEvent.AXIS_X -> {
                mLeftJoystick.setXAxisValue(value)
                updateJoysticks()
            }

            MotionEvent.AXIS_Y -> {
                mLeftJoystick.setYAxisValue(value)
                updateJoysticks()
            }

            MotionEvent.AXIS_Z -> {
                mRightJoystick.setXAxisValue(value)
                updateJoysticks()
            }

            MotionEvent.AXIS_RZ -> {
                mRightJoystick.setYAxisValue(value)
                updateJoysticks()
            }

            MotionEvent.AXIS_RTRIGGER -> this.currentMap?.TRIGGER_RIGHT?.update(value > 0.5)
            MotionEvent.AXIS_LTRIGGER -> this.currentMap?.TRIGGER_LEFT?.update(value > 0.5)
            else -> sendKeyPress(
                LwjglGlfwKeycode.GLFW_KEY_SPACE.toInt(),
                currentMods,
                isKeyEventDown
            )
        }
    }

    private fun centerPointer() {
        val parent = mPointerImageView.getParent() as ViewGroup?
        if (parent == null) return
        placePointerView(parent.getWidth() / 2, parent.getHeight() / 2)
    }

    /**
     * Stops the Gamepad and removes all traces of the Gamepad from the view hierarchy.
     * After this call, the Gamepad is not recoverable and a new one must be made.
     */
    fun removeSelf() {
        mRemoved = true
        mMapProvider.detachGrabListener(this)
        val viewGroup = mPointerImageView.getParent() as ViewGroup?
        if (viewGroup != null) viewGroup.removeView(mPointerImageView)
    }

    companion object {
        private const val MOUSE_MAX_ACCELERATION = 2.0

        fun sendInput(keycodes: ShortArray, isDown: Boolean) {
            for (keycode in keycodes) {
                when (keycode) {
                    GamepadMap.Companion.MOUSE_SCROLL_DOWN -> if (isDown) sendScroll(0.0, -1.0)
                    GamepadMap.Companion.MOUSE_SCROLL_UP -> if (isDown) sendScroll(0.0, 1.0)
                    GamepadMap.Companion.MOUSE_LEFT -> sendMouseButton(
                        LwjglGlfwKeycode.GLFW_MOUSE_BUTTON_LEFT.toInt(),
                        isDown
                    )

                    GamepadMap.Companion.MOUSE_MIDDLE -> sendMouseButton(
                        LwjglGlfwKeycode.GLFW_MOUSE_BUTTON_MIDDLE.toInt(),
                        isDown
                    )

                    GamepadMap.Companion.MOUSE_RIGHT -> sendMouseButton(
                        LwjglGlfwKeycode.GLFW_MOUSE_BUTTON_RIGHT.toInt(),
                        isDown
                    )

                    GamepadMap.Companion.UNSPECIFIED -> {}
                    else -> {
                        sendKeyPress(keycode.toInt(), currentMods, isDown)
                        setModifiers(keycode.toInt(), isDown)
                    }
                }
            }
        }

        fun isGamepadEvent(event: MotionEvent): Boolean {
            return GamepadJoystick.Companion.isJoystickEvent(event)
        }

        fun isGamepadEvent(event: KeyEvent): Boolean {
            val isGamepad =
                ((event.getSource() and InputDevice.SOURCE_GAMEPAD) == InputDevice.SOURCE_GAMEPAD)
                        || ((event.getDevice() != null) && ((event.getDevice()
                    .getSources() and InputDevice.SOURCE_GAMEPAD) == InputDevice.SOURCE_GAMEPAD))

            return isGamepad && GamepadDpad.isDpadEvent(event)
        }

        private fun sendDirectionalKeycode(direction: Int, isDown: Boolean, map: GamepadMap) {
            when (direction) {
                GamepadJoystick.Companion.DIRECTION_NORTH -> map.DIRECTION_FORWARD?.update(isDown)
                GamepadJoystick.Companion.DIRECTION_NORTH_EAST -> {
                    map.DIRECTION_FORWARD?.update(isDown)
                    map.DIRECTION_RIGHT?.update(isDown)
                }

                GamepadJoystick.Companion.DIRECTION_EAST -> map.DIRECTION_RIGHT?.update(isDown)
                GamepadJoystick.Companion.DIRECTION_SOUTH_EAST -> {
                    map.DIRECTION_RIGHT?.update(isDown)
                    map.DIRECTION_BACKWARD?.update(isDown)
                }

                GamepadJoystick.Companion.DIRECTION_SOUTH -> map.DIRECTION_BACKWARD?.update(isDown)
                GamepadJoystick.Companion.DIRECTION_SOUTH_WEST -> {
                    map.DIRECTION_BACKWARD?.update(isDown)
                    map.DIRECTION_LEFT?.update(isDown)
                }

                GamepadJoystick.Companion.DIRECTION_WEST -> map.DIRECTION_LEFT?.update(isDown)
                GamepadJoystick.Companion.DIRECTION_NORTH_WEST -> {
                    map.DIRECTION_FORWARD?.update(isDown)
                    map.DIRECTION_LEFT?.update(isDown)
                }
            }
        }
    }
}
