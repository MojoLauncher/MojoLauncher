package net.kdt.pojavlaunch.customcontrols.buttons

import android.annotation.SuppressLint
import android.view.View
import io.github.controlwear.virtual.joystick.android.JoystickView
import net.kdt.pojavlaunch.LwjglGlfwKeycode
import net.kdt.pojavlaunch.Tools.dpToPx
import net.kdt.pojavlaunch.customcontrols.ControlData
import net.kdt.pojavlaunch.customcontrols.ControlJoystickData
import net.kdt.pojavlaunch.customcontrols.ControlLayout
import net.kdt.pojavlaunch.customcontrols.gamepad.GamepadJoystick
import net.kdt.pojavlaunch.customcontrols.handleview.EditControlSideDialog
import org.lwjgl.glfw.CallbackBridge.currentMods
import org.lwjgl.glfw.CallbackBridge.sendKeyPress

@SuppressLint("ViewConstructor")
class ControlJoystick(parent: ControlLayout, data: ControlJoystickData) :
    JoystickView(parent.context), ControlInterface {
    // Directions keycode
    private val mDirectionForwardLock = intArrayOf(LwjglGlfwKeycode.GLFW_KEY_LEFT_CONTROL.toInt())
    private val mDirectionForward = intArrayOf(LwjglGlfwKeycode.GLFW_KEY_W.toInt())
    private val mDirectionRight = intArrayOf(LwjglGlfwKeycode.GLFW_KEY_D.toInt())
    private val mDirectionBackward = intArrayOf(LwjglGlfwKeycode.GLFW_KEY_S.toInt())
    private val mDirectionLeft = intArrayOf(LwjglGlfwKeycode.GLFW_KEY_A.toInt())
    private var mControlData: ControlJoystickData? = null
    private var mLastDirectionInt: Int = GamepadJoystick.DIRECTION_NONE
    private var mCurrentDirectionInt: Int = GamepadJoystick.DIRECTION_NONE

    init {
        init(data, parent)
    }

    private fun init(data: ControlJoystickData, layout: ControlLayout) {
        mControlData = data
        setProperties(preProcessProperties(data, layout), true)
        setDeadzone(35)
        setFixedCenter(data.absolute)
        setAutoReCenterButton(true)

        injectBehaviors()
        injectLayoutParamBehavior()

        setOnMoveListener(object : OnMoveListener {
            override fun onMove(angle: Int, strength: Int) {
                mLastDirectionInt = mCurrentDirectionInt
                mCurrentDirectionInt = getDirectionInt(angle, strength)

                if (mLastDirectionInt != mCurrentDirectionInt) {
                    sendDirectionalKeycode(mLastDirectionInt, false)
                    sendDirectionalKeycode(mCurrentDirectionInt, true)
                }
            }

            override fun onForwardLock(isLocked: Boolean) {
                sendInput(mDirectionForwardLock, isLocked)
            }
        })
    }

    override val controlView: View
        get() = this

    override val properties: ControlData
        get() = mControlData!!

    override fun setProperties(properties: ControlData?, changePos: Boolean) {
        mControlData = properties as? ControlJoystickData
        mControlData?.isHideable = true
        super.setProperties(properties, changePos)
        postDelayed({
            if (mControlData != null) {
                setForwardLockDistance(if (mControlData!!.forwardLock) dpToPx(60f).toInt() else 0)
                setFixedCenter(mControlData!!.absolute)
            }
        }, 10)
    }

    override fun removeButton() {
        controlLayoutParent?.layout?.mJoystickDataList?.remove(mControlData)
        controlLayoutParent?.removeView(this)
    }

    override fun cloneButton() {
        if (mControlData != null) {
            val data = ControlJoystickData(mControlData!!)
            controlLayoutParent?.addJoystickButton(data)
        }
    }

    override fun handlePressed() { /*STUB since non swipeable*/
    }

    override fun handleReleased() { /*STUB since non swipeable*/
    }


    override fun setBackground() {
        val parent = controlLayoutParent ?: return
        setBorderWidth(dpToPx(properties.strokeWidth * (parent.layoutScale / 100f)).toInt())
        setBorderColor(properties.strokeColor)
        setBackgroundColor(properties.bgColor)
    }

    override fun loadEditValues(editControlDialog: EditControlSideDialog?) {
        mControlData?.let { editControlDialog?.loadJoystickValues(it) }
    }

    private fun getDirectionInt(angle: Int, intensity: Int): Int {
        if (intensity == 0) return GamepadJoystick.DIRECTION_NONE
        return (((angle + 22.5) / 45) % 8).toInt()
    }

    private fun sendDirectionalKeycode(direction: Int, isDown: Boolean) {
        when (direction) {
            GamepadJoystick.DIRECTION_NORTH -> sendInput(mDirectionForward, isDown)
            GamepadJoystick.DIRECTION_NORTH_EAST -> {
                sendInput(mDirectionForward, isDown)
                sendInput(mDirectionRight, isDown)
            }

            GamepadJoystick.DIRECTION_EAST -> sendInput(mDirectionRight, isDown)
            GamepadJoystick.DIRECTION_SOUTH_EAST -> {
                sendInput(mDirectionRight, isDown)
                sendInput(mDirectionBackward, isDown)
            }

            GamepadJoystick.DIRECTION_SOUTH -> sendInput(mDirectionBackward, isDown)
            GamepadJoystick.DIRECTION_SOUTH_WEST -> {
                sendInput(mDirectionBackward, isDown)
                sendInput(mDirectionLeft, isDown)
            }

            GamepadJoystick.DIRECTION_WEST -> sendInput(mDirectionLeft, isDown)
            GamepadJoystick.DIRECTION_NORTH_WEST -> {
                sendInput(mDirectionForward, isDown)
                sendInput(mDirectionLeft, isDown)
            }

            DIRECTION_FORWARD_LOCK -> sendInput(mDirectionForwardLock, isDown)
        }
    }

    companion object {
        const val DIRECTION_FORWARD_LOCK: Int = 8
        private fun sendInput(keys: IntArray, isDown: Boolean) {
            for (key in keys) {
                sendKeyPress(key, currentMods, isDown)
            }
        }
    }
}
