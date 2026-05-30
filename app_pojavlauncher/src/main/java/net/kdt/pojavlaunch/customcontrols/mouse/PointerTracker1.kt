package net.kdt.pojavlaunch.customcontrols.mouse

import android.view.MotionEvent

class PointerTracker {
    private var mColdStart = true
    private var mTrackedPointerId = 0
    private var mPointerCount = 0
    private var mLastX = 0f
    private var mLastY = 0f
    val motionVector: FloatArray = FloatArray(2)

    fun startTracking(motionEvent: MotionEvent) {
        mColdStart = false
        mTrackedPointerId = motionEvent.getPointerId(0)
        mPointerCount = motionEvent.getPointerCount()
        mLastX = motionEvent.getX()
        mLastY = motionEvent.getY()
    }

    fun cancelTracking() {
        mColdStart = true
    }

    fun trackEvent(motionEvent: MotionEvent): Int {
        var trackedPointerIndex = motionEvent.findPointerIndex(mTrackedPointerId)
        val pointerCount = motionEvent.getPointerCount()
        if (trackedPointerIndex == -1 || mPointerCount != pointerCount || mColdStart) {
            startTracking(motionEvent)
            trackedPointerIndex = 0
        }
        val trackedX = motionEvent.getX(trackedPointerIndex)
        val trackedY = motionEvent.getY(trackedPointerIndex)
        this.motionVector[0] = trackedX - mLastX
        this.motionVector[1] = trackedY - mLastY
        mLastX = trackedX
        mLastY = trackedY
        return trackedPointerIndex
    }
}
