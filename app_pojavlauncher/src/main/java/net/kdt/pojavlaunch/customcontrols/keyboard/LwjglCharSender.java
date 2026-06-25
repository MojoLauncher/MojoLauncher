package net.kdt.pojavlaunch.customcontrols.keyboard;

import net.kdt.pojavlaunch.LwjglGlfwKeycode;

import net.kdt.pojavlaunch.CallbackBridge;

import git.artdeell.dnbootstrap.glfw.GLFW;

/** Sends keys via the CallBackBridge */
public class LwjglCharSender implements CharacterSenderStrategy {
    @Override
    public void sendBackspace() {
        CallbackBridge.sendKeyPress(LwjglGlfwKeycode.GLFW_KEY_BACKSPACE);
    }

    @Override
    public void sendEnter() {
        CallbackBridge.sendKeyPress(LwjglGlfwKeycode.GLFW_KEY_ENTER);
    }

    @Override
    public void sendChars(CharSequence chars) {
        GLFW.sendBulkUnicodeEvent(chars.toString(), CallbackBridge.getCurrentMods());
    }
}
