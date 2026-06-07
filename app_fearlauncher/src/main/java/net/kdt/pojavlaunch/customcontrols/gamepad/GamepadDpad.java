package net.kdt.pojavlaunch.customcontrols.gamepad;

import static android.view.InputDevice.KEYBOARD_TYPE_ALPHABETIC;
import static android.view.InputDevice.SOURCE_GAMEPAD;

import android.view.KeyEvent;


public class GamepadDpad {
    public static boolean isDpadEvent(KeyEvent event) {
        return event.isFromSource(SOURCE_GAMEPAD) && (event.getDevice() == null || event.getDevice().getKeyboardType() != KEYBOARD_TYPE_ALPHABETIC);
    }
}