package net.kdt.pojavlaunch.awt;

import android.telecom.Call;

import net.kdt.pojavlaunch.CallbackBridge;
import net.kdt.pojavlaunch.game.platform.Platform;

public class AWTInput {
    public static final int EVENT_TYPE_CHAR = 1000;
    public static final int EVENT_TYPE_CURSOR_POS = 1003;
    public static final int EVENT_TYPE_KEY = 1005;
    public static final int EVENT_TYPE_MOUSE_BUTTON = 1006;
    
    public static void sendKey(char keychar, int keycode) {
        // TODO: Android -> AWT keycode mapping
        sendKey(keychar, keycode, 1);
        sendKey(keychar, keycode, 0);
    }

    public static void sendKey(char keychar, int keycode, int state) {
        // TODO: Android -> AWT keycode mapping
        Platform.PLATFORM.sendKeyEvent(keycode, state, CallbackBridge.getCurrentMods(), keychar);
    }

    public static void sendChar(char keychar){
        Platform.PLATFORM.sendKeyEvent(0, 0, CallbackBridge.getCurrentMods(), keychar);
    }
    
    public static void sendMousePress(int awtButtons, boolean isDown) {
        Platform.PLATFORM.sendMouseEvent(awtButtons, isDown ? 1 : 0, CallbackBridge.getCurrentMods());
    }
    
    public static void sendMousePress(int awtButtons) {
        sendMousePress(awtButtons, true);
        sendMousePress(awtButtons, false);
    }
    
    public static void sendMousePos(int x, int y) {
        Platform.cursorX = x;
        Platform.cursorY = y;
        Platform.PLATFORM.sendMousePosition();
    }
    
    static {
        System.loadLibrary("pojavexec_awt");
    }
    
    public static native void nativeSendData(int type, int i1, int i2, int i3, int i4);
}
