package net.kdt.pojavlaunch.platform;

import android.view.Surface;

import git.artdeell.dnbootstrap.glfw.GamepadEnableHandler;

public interface Platform {
    void surfaceCreated(Surface surface);
    void surfaceUpdated();
    void surfaceDestroyed();

    double getCursorX();
    double getCursorY();
    void setCursorX(double x);
    void setCursorY(double y);
    void sendMousePos();
    void sendMouseEvent(int key);
    boolean isGrabbing();
    void setGamepadEnableHandler(GamepadEnableHandler handler);

}
