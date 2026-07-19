package net.kdt.pojavlaunch.customcontrols.keyboard;

import android.view.KeyEvent;

import net.kdt.pojavlaunch.CallbackBridge;
import net.kdt.pojavlaunch.platform.Platform;

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
        Platform.PLATFORM.sendBulkUnicodeEvent(chars.toString(), CallbackBridge.getCurrentMods());
    }
}
