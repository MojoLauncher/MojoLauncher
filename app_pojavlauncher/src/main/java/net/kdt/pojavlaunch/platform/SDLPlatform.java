package net.kdt.pojavlaunch.platform;

import android.view.Surface;

import git.mojo.sdl.SDL;
import git.mojo.sdl.SDLActivity;

import git.artdeell.dnbootstrap.glfw.GamepadEnableHandler;

public class SDLPlatform implements Platform {
    @Override
    public void initialize() {
        SDL.initialize();
        SDL.setupJNI();
    }

    @Override
    public void surfaceCreated(Surface surface) {
        SDLActivity.setNativeSurface(surface);
        SDLActivity.onNativeSurfaceCreated();
    }

    @Override
    public void surfaceUpdated() {
        SDLActivity.onNativeSurfaceChanged();
    }

    @Override
    public void surfaceDestroyed() {
        SDLActivity.onNativeSurfaceDestroyed();
    }

    @Override
    public double getCursorX() {
        return 0;
    }

    @Override
    public double getCursorY() {
        return 0;
    }

    @Override
    public void setCursorX(double x) {

    }

    @Override
    public void setCursorY(double y) {

    }

    @Override
    public void sendMousePos() {

    }

    @Override
    public void sendMouseEvent(int key) {

    }

    @Override
    public boolean isGrabbing() {
        return false;
    }

    @Override
    public void setGamepadEnableHandler(GamepadEnableHandler handler) {

    }
}
