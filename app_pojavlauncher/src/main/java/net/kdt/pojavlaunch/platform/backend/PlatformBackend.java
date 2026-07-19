package net.kdt.pojavlaunch.platform.backend;

import android.view.Surface;

public interface PlatformBackend {
    void surfaceCreated(Surface surface);
    void surfaceUpdated();
    void surfaceDestroyed();

    void sendMousePosition();
    void sendMouseEvent(int key, int state, int mods);
    void sendKeyEvent(int key, int state, int mods, char codepoint);
    void sendKeyEvent(int key, int state, int mods);
    void sendKeyEvent(int key, boolean state, int mods);
    void sendScrollEvent(double x, double y);
    void sendBulkUnicodeEvent(String text, int mods);
}
