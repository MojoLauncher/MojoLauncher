package net.kdt.pojavlaunch.customcontrols.mouse

import android.os.Handler
import android.os.Looper
import android.view.MotionEvent
import net.kdt.pojavlaunch.prefs.LauncherPreferences
import org.lwjgl.glfw.CallbackBridge
import org.lwjgl.glfw.CallbackBridge.sendCursorPos

class InGameEventProcessor(private val mSensitivity: Double) : TouchEventProcessor {
    private val mGestureHandler = Handler(Looper.getMainLooper())
    private var mEventTransitioned = true
    private val mTracker = PointerTracker()
    private val mLeftClickGesture = LeftClickGesture(mGestureHandler)
    private val mRightClickGesture = RightClickGesture(mGestureHandler)

    override fun processTouchEvent(motionEvent: MotionEvent?): Boolean {
        if (motionEvent == null) return false
        when (motionEvent.getActionMasked()) {
            MotionEvent.ACTION_DOWN -> {
                mTracker.startTracking(motionEvent)
                if (LauncherPreferences.PREF_DISABLE_GESTURES) return true
                mEventTransitioned = false
                checkGestures()
            }

            MotionEvent.ACTION_MOVE -> {
                mTracker.trackEvent(motionEvent)
                val motionVector = mTracker.motionVector
                val deltaX = (motionVector[0] * mSensitivity).toFloat()
                val deltaY = (motionVector[1] * mSensitivity).toFloat()
                mLeftClickGesture.setMotion(deltaX, deltaY)
                mRightClickGesture.setMotion(deltaX, deltaY)
                CallbackBridge.mouseX += deltaX
                CallbackBridge.mouseY += deltaY
                sendCursorPos(CallbackBridge.mouseX, CallbackBridge.mouseY)
                if (LauncherPreferences.PREF_DISABLE_GESTURES) return true
                checkGestures()
            }

            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                mTracker.cancelTracking()
                cancelGestures(false)
            }
        }
        return true
    }

    override fun cancelPendingActions() {
        cancelGestures(true)
    }

    private fun checkGestures() {
        mLeftClickGesture.inputEvent()
        // Only register right click events if it's a fresh event stream, not one after a transition.
        // This is done to avoid problems when people hold the button for just a bit too long after
        // exiting a menu for example.
        if (!mEventTransitioned) mRightClickGesture.inputEvent()
    }

    private fun cancelGestures(isSwitching: Boolean) {
        mEventTransitioned = true
        mLeftClickGesture.cancel(isSwitching)
        mRightClickGesture.cancel(isSwitching)
    }
}
