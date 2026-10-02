package net.kdt.pojavlaunch.game.platform.backend;

import android.view.Surface;


/**
 * Null (dummy) Platform implementation. Use when none of other platforms are available
 */
public class DummyBackend implements PlatformBackend {

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
    public void sendMouseEvent(int button, int state, int mods) {

    }

    @Override
    public boolean sendKeyEvent(int key, int state, int mods, char codepoint) {
        return true;
    }

    @Override
    public boolean sendKeyEvent(int key, int state, int mods) {
        return true;
    }

    @Override
    public boolean sendKeyEvent(int key, boolean state, int mods) {
        return true;
    }

    @Override
    public void sendScrollEvent(double x, double y) {

    }

    @Override
    public void sendBulkUnicodeEvent(String text, int mods) {

    }

    @Override
    public String backendName() {
        return "You must not see this!";
    }

    @Override
    public void setHovered(boolean hovered) {

    }

    @Override
    public void setVisible(boolean visible) {

    }
}
