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
