package net.kdt.pojavlaunch.customcontrols.mouse

import android.view.MotionEvent

interface TouchEventProcessor {
    fun processTouchEvent(motionEvent: MotionEvent?): Boolean
    fun cancelPendingActions()
}
