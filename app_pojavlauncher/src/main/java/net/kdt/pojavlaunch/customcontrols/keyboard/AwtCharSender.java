package net.kdt.pojavlaunch.customcontrols.keyboard;

import net.kdt.pojavlaunch.awt.AWTInput;
import net.kdt.pojavlaunch.awt.AWTKeycode;

/** Send chars via the AWT Bridgee */
public class AwtCharSender implements CharacterSenderStrategy {
    @Override
    public void sendBackspace() {
        AWTInput.sendKey(' ', AWTKeycode.VK_BACK_SPACE);
    }

    @Override
    public void sendEnter() {
        AWTInput.sendKey(' ', AWTKeycode.VK_ENTER);
    }

    @Override
    public void sendChars(CharSequence chars) {
        for(int i = 0; i < chars.length(); i++) AWTInput.sendChar(chars.charAt(i));
    }

}
