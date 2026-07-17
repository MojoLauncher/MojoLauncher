package net.kdt.pojavlaunch.platform;

import android.view.Surface;

import git.mojo.sdl.SDL;
import git.mojo.sdl.SDLActivity;

import git.artdeell.dnbootstrap.glfw.GamepadEnableHandler;

public class SDLImpl extends PlatformLibrary {
    static {
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
    public void sendMousePosition() {

    }


    @Override
    public void sendMouseEvent(int key, int state, int mods) {

    }

    @Override
    public boolean isGrabbing() {
        return false;
    }

    @Override
    public void setGamepadEnableHandler(GamepadEnableHandler handler) {

    }
}
