package net.kdt.pojavlaunch.customcontrols.gamepad

import android.view.KeyEvent

/**
 * This class corresponds to a button that does not physically exist on the gamepad, but is
 * emulated from other inputs on it (like WASD directional keys)
 */
open class GamepadEmulatedButton {
    var keycodes: ShortArray = shortArrayOf()
    protected var mIsDown: Boolean = false

    fun update(event: KeyEvent) {
        val isKeyDown = (event.getAction() == KeyEvent.ACTION_DOWN)
        update(isKeyDown)
    }

    fun update(isKeyDown: Boolean) {
        if (isKeyDown != mIsDown) {
            mIsDown = isKeyDown
            onDownStateChanged(mIsDown)
        }
    }

    open fun resetButtonState() {
        if (mIsDown) Gamepad.Companion.sendInput(keycodes, false)
        mIsDown = false
    }

    protected open fun onDownStateChanged(isDown: Boolean) {
        Gamepad.Companion.sendInput(keycodes, mIsDown)
    }
}
