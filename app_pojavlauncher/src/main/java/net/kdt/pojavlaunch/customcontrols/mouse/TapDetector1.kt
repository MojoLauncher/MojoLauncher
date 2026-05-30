package net.kdt.pojavlaunch.customcontrols.mouse

import android.view.MotionEvent
import net.kdt.pojavlaunch.Tools.dpToPx

/**
 * Class aiming at better detecting X-tap events regardless of the POINTERS
 * Only uses the least amount of events possible,
 * since we aren't guaranteed to have all events in order
 */
class TapDetector(tapNumberToDetect: Int, private val mDetectionMethod: Int) {
    private val mTapNumberToDetect: Int
    private var mCurrentTapNumber = 0

    private var mLastEventTime: Long = 0
    private var mLastX = 9999f
    private var mLastY = 9999f

    /**
     * @param tapNumberToDetect How many taps are needed before onTouchEvent returns True.
     * @param mDetectionMethod Method used to detect touches. See DETECTION_METHOD constants above.
     */
    init {
        //We expect both ACTION_DOWN and ACTION_UP for the DETECTION_METHOD_BOTH
        this.mTapNumberToDetect =
            if (detectBothTouch()) 2 * tapNumberToDetect else tapNumberToDetect
    }

    /**
     * A function to call when you have a touch event.
     * @param e The MotionEvent to inspect
     * @return whether or not a X-tap happened for a pointer
     */
    fun onTouchEvent(e: MotionEvent): Boolean {
        val eventAction = e.getActionMasked()
        var pointerIndex = -1

        //Get the event to look forward
        if (detectDownTouch()) {
            if (eventAction == MotionEvent.ACTION_DOWN) pointerIndex = 0
            else if (eventAction == MotionEvent.ACTION_POINTER_DOWN) pointerIndex =
                e.getActionIndex()
        }
        if (detectUpTouch()) {
            if (eventAction == MotionEvent.ACTION_UP) pointerIndex = 0
            else if (eventAction == MotionEvent.ACTION_POINTER_UP) pointerIndex = e.getActionIndex()
        }

        if (pointerIndex == -1) return false // Useless event


        //Store current event info
        val eventX = e.getX(pointerIndex)
        val eventY = e.getY(pointerIndex)
        val eventTime = e.getEventTime()

        //Compute deltas
        val deltaTime = eventTime - mLastEventTime
        val deltaX = mLastX.toInt() - eventX.toInt()
        val deltaY = mLastY.toInt() - eventY.toInt()

        //Store current event info to persist on next event
        mLastEventTime = eventTime
        mLastX = eventX
        mLastY = eventY

        //Check for high enough speed and precision
        if (mCurrentTapNumber > 0) {
            if ((deltaTime < TAP_MIN_DELTA_MS || deltaTime > TAP_MAX_DELTA_MS) ||
                ((deltaX * deltaX + deltaY * deltaY) > TAP_SLOP_SQUARE_PX)
            ) {
                if (mDetectionMethod == DETECTION_METHOD_BOTH && (eventAction == MotionEvent.ACTION_UP || eventAction == MotionEvent.ACTION_POINTER_UP)) {
                    // For the both method, the user is expected to start with a down action.
                    resetTapDetectionState()
                    return false
                } else {
                    // We invalidate previous taps, not this one though
                    mCurrentTapNumber = 0
                }
            }
        }

        //A worthy tap happened
        mCurrentTapNumber += 1
        if (mCurrentTapNumber >= mTapNumberToDetect) {
            resetTapDetectionState()
            return true
        }

        //If not enough taps are reached
        return false
    }

    /**
     * Reset the double tap values.
     */
    private fun resetTapDetectionState() {
        mCurrentTapNumber = 0
        mLastEventTime = 0
        mLastX = 9999f
        mLastY = 9999f
    }

    private fun detectDownTouch(): Boolean {
        return (mDetectionMethod and DETECTION_METHOD_DOWN) == DETECTION_METHOD_DOWN
    }

    private fun detectUpTouch(): Boolean {
        return (mDetectionMethod and DETECTION_METHOD_UP) == DETECTION_METHOD_UP
    }

    private fun detectBothTouch(): Boolean {
        return mDetectionMethod == DETECTION_METHOD_BOTH
    }

    companion object {
        const val DETECTION_METHOD_DOWN: Int = 0x1
        const val DETECTION_METHOD_UP: Int = 0x2
        const val DETECTION_METHOD_BOTH: Int = 0x3 //Unused for now

        private val TAP_MIN_DELTA_MS = -1
        private const val TAP_MAX_DELTA_MS = 300
        private val TAP_SLOP_SQUARE_PX = dpToPx(2500f).toInt()
    }
}
