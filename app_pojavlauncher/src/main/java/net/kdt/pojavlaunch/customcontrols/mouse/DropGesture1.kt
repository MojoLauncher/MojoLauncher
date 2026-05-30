package net.kdt.pojavlaunch.customcontrols.mouse

import android.os.Handler
import net.kdt.pojavlaunch.LwjglGlfwKeycode
import net.kdt.pojavlaunch.prefs.LauncherPreferences
import org.lwjgl.glfw.CallbackBridge.sendKeyPress

class DropGesture(private val mHandler: Handler) : Runnable {
    private var mActive = false

    fun submit() {
        if (!mActive) {
            mActive = true
            mHandler.postDelayed(this, LauncherPreferences.PREF_LONGPRESS_TRIGGER.toLong())
        }
    }

    fun cancel() {
        mActive = false
        mHandler.removeCallbacks(this)
    }

    override fun run() {
        if (!mActive) return
        sendKeyPress(LwjglGlfwKeycode.GLFW_KEY_Q.toInt())
        mHandler.postDelayed(this, 250)
    }
}
