package net.kdt.pojavlaunch.platform;

import android.view.MotionEvent;
import android.view.Surface;

import net.kdt.pojavlaunch.LauncherGLSurface;
import net.kdt.pojavlaunch.Tools;

import git.mojo.sdl.SDL;
import git.mojo.sdl.SDLActivity;

import git.artdeell.dnbootstrap.glfw.GamepadEnableHandler;
import git.mojo.sdl.SDLInputConnection;

public class SDLImpl extends PlatformLibrary {
    public static void initialize() {
        SDL.initialize();
        SDL.setupJNI();
        SDLActivity.addGrabListener(isGrabbing -> {
            Tools.runOnUiThread(() -> PlatformLibrary.executeGrabbingListeners(isGrabbing));
        });
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
        // SDL expects normalized coords, not pixel ones like we use
        // Or pixel ones? Whatever
        float x = (float) (PlatformLibrary.cursorX * LauncherGLSurface.getWindowWidth());
        float y = (float) (PlatformLibrary.cursorY * LauncherGLSurface.getWindowHeight());
        SDLActivity.onNativeMouse(0, MotionEvent.ACTION_MOVE, x, y, isGrabbing());
        if(isGrabbing()){
            // SDL in relative mode expects these to be reset to 0 or it will freak out (classic:tm: way)
            PlatformLibrary.cursorX = 0;
            PlatformLibrary.cursorY = 0;
        }
    }


    @Override
    public void sendMouseEvent(int key, int state, int mods) {
        // SDL expects normalized coords, not pixel ones like we use
        // Or pixel ones? Whatever
        float x = (float) (PlatformLibrary.cursorX * LauncherGLSurface.getWindowWidth());
        float y = (float) (PlatformLibrary.cursorY * LauncherGLSurface.getWindowHeight());
        SDLActivity.onNativeMouseButton(key, state, x, y, isGrabbing());
    }

    @Override
    public void sendKeyEvent(int key, int state, int mods, char codepoint) {
        if(state == 1) SDLActivity.onNativeKeyDown(key);
        else SDLActivity.onNativeKeyUp(key);
    }

    @Override
    public void sendKeyEvent(int key, int state, int mods) {
        if(state == 1) SDLActivity.onNativeKeyDown(key);
        else SDLActivity.onNativeKeyUp(key);
    }

    @Override
    public void sendKeyEvent(int key, boolean state, int mods) {
        if(state) SDLActivity.onNativeKeyDown(key);
        else SDLActivity.onNativeKeyUp(key);
    }

    @Override
    public void sendScrollEvent(double x, double y) {
        SDLActivity.onNativeMouse(0, MotionEvent.ACTION_SCROLL, (float) x, (float) y, false);
    }

    @Override
    public void sendBulkUnicodeEvent(String text, int mods) {
        SDLInputConnection.nativeCommitText(text, mods);
    }

    @Override
    public void setGamepadEnableHandler(GamepadEnableHandler handler) {

    }
}
