package net.kdt.pojavlaunch.customcontrols.keyboard;

import android.view.KeyEvent;

import net.kdt.pojavlaunch.LwjglGlfwKeycode;

import net.kdt.pojavlaunch.CallbackBridge;
import net.kdt.pojavlaunch.platform.PlatformLibrary;

import git.artdeell.dnbootstrap.glfw.GLFW;

/** Sends keys via the CallBackBridge */
public class LwjglCharSender implements CharacterSenderStrategy {
    @Override
    public void sendBackspace() {
        CallbackBridge.sendKeyPress(KeyEvent.KEYCODE_DEL);
    }

    @Override
    public void sendEnter() {
        CallbackBridge.sendKeyPress(KeyEvent.KEYCODE_ENTER);
    }

    @Override
    public void sendChars(CharSequence chars) {
        PlatformLibrary.PLATFORM.sendBulkUnicodeEvent(chars.toString(), CallbackBridge.getCurrentMods());
    }
}
