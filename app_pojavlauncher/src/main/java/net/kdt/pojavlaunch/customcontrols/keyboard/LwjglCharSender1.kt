package net.kdt.pojavlaunch.customcontrols.keyboard

import net.kdt.pojavlaunch.LwjglGlfwKeycode
import org.lwjgl.glfw.CallbackBridge.sendChar
import org.lwjgl.glfw.CallbackBridge.sendKeyPress
import org.lwjgl.glfw.CallbackBridge.sendKeycode

/** Sends keys via the CallBackBridge  */
class LwjglCharSender : CharacterSenderStrategy {
    override fun sendBackspace() {
        sendKeycode(LwjglGlfwKeycode.GLFW_KEY_BACKSPACE.toInt(), '\u0008', 0, 0, true)
        sendKeycode(LwjglGlfwKeycode.GLFW_KEY_BACKSPACE.toInt(), '\u0008', 0, 0, false)
    }

    override fun sendEnter() {
        sendKeyPress(LwjglGlfwKeycode.GLFW_KEY_ENTER.toInt())
    }

    override fun sendChar(character: Char) {
        sendChar(character, 0)
    }
}
