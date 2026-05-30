package net.kdt.pojavlaunch.customcontrols.keyboard

import net.kdt.pojavlaunch.AWTInputBridge
import net.kdt.pojavlaunch.AWTInputBridge.sendKey
import net.kdt.pojavlaunch.AWTInputEvent

/** Send chars via the AWT Bridgee  */
class AwtCharSender : CharacterSenderStrategy {
    override fun sendBackspace() {
        sendKey(' ', AWTInputEvent.VK_BACK_SPACE)
    }

    override fun sendEnter() {
        sendKey(' ', AWTInputEvent.VK_ENTER)
    }

    override fun sendChar(character: Char) {
        AWTInputBridge.sendChar(character)
    }
}
