package net.kdt.pojavlaunch.platform;

import android.view.Surface;

import git.artdeell.dnbootstrap.glfw.GamepadEnableHandler;

public class NullImpl extends PlatformLibrary {

    @Override
    public void surfaceCreated(Surface surface) {

    }

    @Override
    public void surfaceUpdated() {

    }

    @Override
    public void surfaceDestroyed() {

    }

    @Override
    public void sendMousePosition() {

    }

    @Override
    public void sendMouseEvent(int key, int state, int mods) {

    }

    @Override
    public void sendKeyEvent(int key, int state, int mods, char codepoint) {

    }

    @Override
    public void sendKeyEvent(int key, int state, int mods) {

    }

    @Override
    public void sendKeyEvent(int key, boolean state, int mods) {

    }

    @Override
    public void sendScrollEvent(double x, double y) {

    }

    @Override
    public void sendBulkUnicodeEvent(String text, int mods) {

    }

    @Override
    public void setGamepadEnableHandler(GamepadEnableHandler handler) {

    }
}
