package net.kdt.pojavlaunch.customcontrols.mouse

import android.view.MotionEvent
import net.kdt.pojavlaunch.LwjglGlfwKeycode
import net.kdt.pojavlaunch.Tools.dpToPx
import net.kdt.pojavlaunch.prefs.LauncherPreferences
import org.lwjgl.glfw.CallbackBridge
import org.lwjgl.glfw.CallbackBridge.putMouseEventWithCoords
import org.lwjgl.glfw.CallbackBridge.sendCursorPos
import org.lwjgl.glfw.CallbackBridge.sendMouseButton

class InGUIEventProcessor : TouchEventProcessor {
    private val mTracker = PointerTracker()
    private val mSingleTapDetector: TapDetector
    private var mTouchpad: AbstractTouchpad? = null
    private var mIsMouseDown = false
    private var mStartX = 0f
    private var mStartY = 0f
    private val mScroller = Scroller(FINGER_SCROLL_THRESHOLD)

    init {
        mSingleTapDetector = TapDetector(1, TapDetector.Companion.DETECTION_METHOD_BOTH)
    }

    override fun processTouchEvent(motionEvent: MotionEvent?): Boolean {
        if (motionEvent == null) return false
        val singleTap = mSingleTapDetector.onTouchEvent(motionEvent)

        when (motionEvent.getActionMasked()) {
            MotionEvent.ACTION_DOWN -> {
                mTracker.startTracking(motionEvent)
                if (!touchpadDisplayed()) {
                    sendTouchCoordinates(motionEvent.getX(), motionEvent.getY())

                    // disabled gestures means no scrolling possible, send gesture early
                    if (LauncherPreferences.PREF_DISABLE_GESTURES) enableMouse()
                    else setGestureStart(motionEvent)
                }
            }

            MotionEvent.ACTION_MOVE -> {
                val pointerCount = motionEvent.getPointerCount()
                val pointerIndex = mTracker.trackEvent(motionEvent)
                if (pointerCount == 1 || LauncherPreferences.PREF_DISABLE_GESTURES) {
                    if (touchpadDisplayed()) {
                        mTouchpad!!.applyMotionVector(mTracker.motionVector)
                    } else {
                        val mainPointerX = motionEvent.getX(pointerIndex)
                        val mainPointerY = motionEvent.getY(pointerIndex)
                        sendTouchCoordinates(mainPointerX, mainPointerY)

                        if (!mIsMouseDown) {
                            if (!hasGestureStarted()) setGestureStart(motionEvent)
                            if (!LeftClickGesture.Companion.isFingerStill(
                                    mStartX,
                                    mStartY,
                                    FINGER_STILL_THRESHOLD
                                )
                            ) enableMouse()
                        }
                    }
                } else mScroller.performScroll(mTracker.motionVector)
            }

            MotionEvent.ACTION_CANCEL, MotionEvent.ACTION_UP -> {
                mScroller.resetScrollOvershoot()
                mTracker.cancelTracking()

                // Handle single tap on gestures
                if ((!LauncherPreferences.PREF_DISABLE_GESTURES || touchpadDisplayed()) && !mIsMouseDown && singleTap) {
                    putMouseEventWithCoords(
                        LwjglGlfwKeycode.GLFW_MOUSE_BUTTON_LEFT.toInt(),
                        CallbackBridge.mouseX,
                        CallbackBridge.mouseY
                    )
                }

                if (mIsMouseDown) disableMouse()
                resetGesture()
            }
        }


        return true
    }

    private fun touchpadDisplayed(): Boolean {
        return mTouchpad != null && mTouchpad!!.getDisplayState()
    }

    fun setAbstractTouchpad(touchpad: AbstractTouchpad?) {
        mTouchpad = touchpad
    }

    private fun sendTouchCoordinates(x: Float, y: Float) {
        sendCursorPos(
            x * LauncherPreferences.PREF_SCALE_FACTOR,
            y * LauncherPreferences.PREF_SCALE_FACTOR
        )
    }

    private fun enableMouse() {
        sendMouseButton(LwjglGlfwKeycode.GLFW_MOUSE_BUTTON_LEFT.toInt(), true)
        mIsMouseDown = true
    }

    private fun disableMouse() {
        sendMouseButton(LwjglGlfwKeycode.GLFW_MOUSE_BUTTON_LEFT.toInt(), false)
        mIsMouseDown = false
    }

    private fun setGestureStart(event: MotionEvent) {
        mStartX = event.getX() * LauncherPreferences.PREF_SCALE_FACTOR
        mStartY = event.getY() * LauncherPreferences.PREF_SCALE_FACTOR
    }

    private fun resetGesture() {
        mStartY = -1f
        mStartX = mStartY
    }

    private fun hasGestureStarted(): Boolean {
        return mStartX != -1f || mStartY != -1f
    }

    override fun cancelPendingActions() {
        mScroller.resetScrollOvershoot()
        disableMouse()
    }

    companion object {
        val FINGER_SCROLL_THRESHOLD: Float = dpToPx(6f)
        val FINGER_STILL_THRESHOLD: Float = dpToPx(5f)
    }
}
