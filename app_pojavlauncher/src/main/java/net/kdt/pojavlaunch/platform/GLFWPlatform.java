package net.kdt.pojavlaunch.platform;


import android.view.Surface;

import git.artdeell.dnbootstrap.glfw.GLFW;
import git.artdeell.dnbootstrap.glfw.GamepadEnableHandler;

/*
Static provider for GLFW
 */
public class GLFWPlatform implements Platform {


    @Override
    public void initialize() {
        GLFW.initialize();
    }

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
    public double getCursorX() {
        return GLFW.cursorX;
    }

    @Override
    public double getCursorY() {
        return GLFW.cursorY;
    }

    @Override
    public void setCursorX(double x) {
        GLFW.cursorX = x;
    }

    @Override
    public void setCursorY(double y) {
        GLFW.cursorY = y;
    }

    @Override
    public void sendMousePos() {

    }

    @Override
    public void sendMouseEvent(int key) {

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
