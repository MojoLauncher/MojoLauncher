package net.kdt.pojavlaunch.customcontrols.mouse

import android.os.Handler
import net.kdt.pojavlaunch.LwjglGlfwKeycode
import org.lwjgl.glfw.CallbackBridge
import org.lwjgl.glfw.CallbackBridge.sendMouseButton

class RightClickGesture(mHandler: Handler) : ValidatorGesture(mHandler) {
    private var mGestureEnabled = true
    private var mGestureValid = true
    private var mGestureStartX = 0f
    private var mGestureStartY = 0f
    private var mGestureEndX = 0f
    private var mGestureEndY = 0f
    fun inputEvent() {
        if (!mGestureEnabled) return
        if (submit()) {
            mGestureEndX = CallbackBridge.mouseX
            mGestureStartX = mGestureEndX
            mGestureEndY = CallbackBridge.mouseY
            mGestureStartY = mGestureEndY
            mGestureEnabled = false
            mGestureValid = true
        }
    }

    fun setMotion(deltaX: Float, deltaY: Float) {
        mGestureEndX += deltaX
        mGestureEndY += deltaY
    }

    override val gestureDelay: Int
        get() = 150

    override fun checkAndTrigger(): Boolean {
        // If the validate() method was called, it means that the user held on for too long. The cancellation should be ignored.
        mGestureValid = false
        // Never call onGestureCancelled. This way we will be able to reserve that only for when
        // the gesture is stopped in the code (when the user lets go of the screen or the tap was
        // cancelled by turning on the grab)
        return true
    }

    override fun onGestureCancelled(isSwitching: Boolean) {
        mGestureEnabled = true
        if (!mGestureValid || isSwitching) return
        val fingerStill: Boolean = LeftClickGesture.Companion.isFingerStill(
            mGestureStartX,
            mGestureStartY,
            mGestureEndX,
            mGestureEndY,
            LeftClickGesture.Companion.FINGER_STILL_THRESHOLD.toFloat()
        )
        println("Right click: " + fingerStill)
        if (!fingerStill) return
        sendMouseButton(LwjglGlfwKeycode.GLFW_MOUSE_BUTTON_RIGHT.toInt(), true)
        sendMouseButton(LwjglGlfwKeycode.GLFW_MOUSE_BUTTON_RIGHT.toInt(), false)
    }
}
