package net.kdt.pojavlaunch

import android.view.GestureDetector.SimpleOnGestureListener
import android.view.MotionEvent

class SingleTapConfirm : SimpleOnGestureListener() {
    override fun onSingleTapUp(e: MotionEvent): Boolean {
        return true
    }
}
