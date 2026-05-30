package net.kdt.pojavlaunch.customcontrols.gamepad

import android.view.InputDevice
import android.view.KeyEvent

object GamepadDpad {
    fun isDpadEvent(event: KeyEvent): Boolean {
        return event.isFromSource(InputDevice.SOURCE_GAMEPAD) && (event.getDevice() == null || event.getDevice()
            .getKeyboardType() != InputDevice.KEYBOARD_TYPE_ALPHABETIC)
    }
}