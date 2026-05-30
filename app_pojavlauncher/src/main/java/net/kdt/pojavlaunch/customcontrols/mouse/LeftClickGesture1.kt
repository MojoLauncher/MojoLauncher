package net.kdt.pojavlaunch.customcontrols.mouse

import android.os.Handler
import net.kdt.pojavlaunch.LwjglGlfwKeycode
import net.kdt.pojavlaunch.Tools.dpToPx
import net.kdt.pojavlaunch.prefs.LauncherPreferences
import net.kdt.pojavlaunch.utils.MathUtils.dist
import org.lwjgl.glfw.CallbackBridge
import org.lwjgl.glfw.CallbackBridge.sendMouseButton

class LeftClickGesture(handler: Handler) : ValidatorGesture(handler) {
    private var mGestureStartX = 0f
    private var mGestureStartY = 0f
    private var mGestureEndX = 0f
    private var mGestureEndY = 0f
    private var mMouseActivated = false

    fun inputEvent() {
        if (submit()) {
            mGestureEndX = CallbackBridge.mouseX
            mGestureStartX = mGestureEndX
            mGestureEndY = CallbackBridge.mouseY
            mGestureStartY = mGestureEndY
        }
    }

    override val gestureDelay: Int
        get() = LauncherPreferences.PREF_LONGPRESS_TRIGGER

    override fun checkAndTrigger(): Boolean {
        val fingerStill: Boolean = isFingerStill(
            mGestureStartX,
            mGestureStartY,
            mGestureEndX,
            mGestureEndY,
            FINGER_STILL_THRESHOLD.toFloat()
        )
        // If the finger is still, fire the gesture.
        if (fingerStill) {
            sendMouseButton(LwjglGlfwKeycode.GLFW_MOUSE_BUTTON_LEFT.toInt(), true)
            mMouseActivated = true
        }
        // Otherwise, don't click but still keep it active
        return true
    }

    override fun onGestureCancelled(isSwitching: Boolean) {
        if (mMouseActivated) {
            sendMouseButton(LwjglGlfwKeycode.GLFW_MOUSE_BUTTON_LEFT.toInt(), false)
            mMouseActivated = false
        }
    }

    fun setMotion(deltaX: Float, deltaY: Float) {
        mGestureEndX += deltaX
        mGestureEndY += deltaY
    }

    companion object {
        val FINGER_STILL_THRESHOLD: Int = dpToPx(9f).toInt()

        /**
         * Check if the finger is still when compared to mouseX/mouseY in CallbackBridge.
         * @param startX the starting X of the gesture
         * @param startY the starting Y of the gesture
         * @return whether the finger's position counts as "still" or not
         */
        fun isFingerStill(startX: Float, startY: Float, threshold: Float): Boolean {
            return dist(
                CallbackBridge.mouseX,
                CallbackBridge.mouseY,
                startX,
                startY
            ) <= threshold
        }

        fun isFingerStill(
            startX: Float,
            startY: Float,
            endX: Float,
            endY: Float,
            threshold: Float
        ): Boolean {
            return dist(
                endX,
                endY,
                startX,
                startY
            ) <= threshold
        }
    }
}
