package net.kdt.pojavlaunch.game.platform.backend;


import android.view.Surface;

import net.kdt.pojavlaunch.CallbackBridge;
import net.kdt.pojavlaunch.awt.AWTBridge;
import net.kdt.pojavlaunch.game.platform.Platform;

public class AWTBackend implements PlatformBackend {
    static {
        System.loadLibrary("pojavexec_awt");
    }

    @Override
    public void surfaceCreated(Surface surface) {
        Platform.grabStateChanged(false);
        // AWT requires us to manually draw on the screen
        AWTBridge.nativeBeginRendering(surface, CallbackBridge.windowWidth, CallbackBridge.windowHeight);
    }

    @Override
    public void surfaceUpdated() {
        AWTBridge.nativeResize(CallbackBridge.windowWidth, CallbackBridge.windowHeight);
        // There's no need of updating AWT Surface... for now
    }

    @Override
    public void surfaceDestroyed() {
        AWTBridge.nativeEndRendering();
    }

    @Override
    public void sendMousePosition() {
        AWTBridge.nativeSendCursorPos((int) Platform.cursorX, (int) Platform.cursorY);
    }

    @Override
    public void sendMouseEvent(int button, int state, int mods) {
        AWTBridge.nativeSendMouseEvent(button, state, mods);
    }

    @Override
    public boolean sendKeyEvent(int key, int state, int mods, char codepoint) {
        return AWTBridge.nativeSendKeyEvent(key, state, mods, codepoint);
    }

    @Override
    public boolean sendKeyEvent(int key, int state, int mods) {
        return AWTBridge.nativeSendKeyEvent(key, state, mods, 0);
    }

    @Override
    public boolean sendKeyEvent(int key, boolean state, int mods) {
        return AWTBridge.nativeSendKeyEvent(key, state ? 1 : 0, mods, 0);
    }

    @Override
    public void sendScrollEvent(double x, double y) {
        // Unsupported
    }

    @Override
    public void sendBulkUnicodeEvent(String text, int mods) {
        AWTBridge.nativeTypeChars(text);
    }

    @Override
    public String backendName() {
        return "AWT";
    }

    @Override
    public void setHovered(boolean hovered) {
        // Unsupported
    }

    @Override
    public void setVisible(boolean visible) {
        // Unsupported
    }
}
