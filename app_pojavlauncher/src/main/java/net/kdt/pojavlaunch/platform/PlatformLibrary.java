package net.kdt.pojavlaunch.platform;

import android.view.Surface;

import git.artdeell.dnbootstrap.glfw.GamepadEnableHandler;

public abstract class PlatformLibrary {

    public static PlatformLibrary PLATFORM = null;
    public static double cursorX;
    public static double cursorY;


    public abstract void surfaceCreated(Surface surface);
    public abstract void surfaceUpdated();
    public abstract void surfaceDestroyed();

    public abstract void sendMousePosition();
    public abstract void sendMouseEvent(int key, int state, int mods);
    public abstract void sendKeyEvent(int key, int state, int mods, char codepoint);
    public abstract void sendScrollEvent(double x, double y);
    public abstract boolean isGrabbing();
    public abstract void setGamepadEnableHandler(GamepadEnableHandler handler);

    public static void setPlatformLibrary(PlatformLibrary library){
        PLATFORM = library;
    }

}
