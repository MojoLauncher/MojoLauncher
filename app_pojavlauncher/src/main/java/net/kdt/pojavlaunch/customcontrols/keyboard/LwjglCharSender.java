package net.kdt.pojavlaunch.customcontrols.keyboard;

import static net.kdt.pojavlaunch.game.platform.Platform.PLATFORM;

import android.view.KeyEvent;

import net.kdt.pojavlaunch.CallbackBridge;

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
        PLATFORM.sendBulkUnicodeEvent(chars.toString(), CallbackBridge.getCurrentMods());
    }
}
