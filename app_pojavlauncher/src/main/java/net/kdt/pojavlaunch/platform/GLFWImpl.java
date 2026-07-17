package net.kdt.pojavlaunch.platform;


import android.view.Surface;

import git.artdeell.dnbootstrap.glfw.GLFW;
import git.artdeell.dnbootstrap.glfw.GamepadEnableHandler;

/*
Static provider for GLFW
 */
public class GLFWImpl extends PlatformLibrary{

    @Override
    public void surfaceCreated(Surface surface) {
        GLFW.nativeSurfaceCreated(surface);
    }

    @Override
    public void surfaceUpdated() {
        GLFW.nativeSurfaceUpdated();
    }

    @Override
    public void surfaceDestroyed() {
        GLFW.nativeSurfaceDestroyed();
    }

    @Override
    public void sendMousePosition() {
        GLFW.cursorX = PlatformLibrary.cursorX;
        GLFW.cursorY = PlatformLibrary.cursorY;
        GLFW.sendMousePos();
    }

    @Override
    public void sendMouseEvent(int key, int state, int mods) {
        GLFW.sendMouseEvent(key, state, mods);
    }

    @Override
    public void sendKeyEvent(int key, int state, int mods, char codepoint) {
        GLFW.sendRawKeyEvent(key, state, mods, codepoint);
    }
    @Override
    public void sendKeyEvent(int key, int state, int mods) {
        GLFW.sendRawKeyEvent(key, state, mods, (char)0);
    }
    @Override
    public void sendKeyEvent(int key, boolean state, int mods) {
        GLFW.sendRawKeyEvent(key, state ? 1 : 0, mods, (char)0);
    }

    @Override
    public void sendScrollEvent(double x, double y) {
        GLFW.sendScrollEvent(x, y);
    }

    @Override
    public boolean isGrabbing() {
        return GLFW.isGrabbing();
    }

    @Override
    public void setGamepadEnableHandler(GamepadEnableHandler handler) {
        GLFW.setGamepadEnableHandler(handler);
    }
}
