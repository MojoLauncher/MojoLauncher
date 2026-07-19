package net.kdt.pojavlaunch.platform.backend;

import android.app.Activity;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.Surface;

import net.kdt.pojavlaunch.LauncherGLSurface;
import net.kdt.pojavlaunch.Tools;
import net.kdt.pojavlaunch.platform.Platform;

import git.mojo.sdl.SDL;
import git.mojo.sdl.SDLActivity;

import git.mojo.sdl.SDLControllerManager;
import git.mojo.sdl.SDLInputConnection;

public class SDLBackend extends Platform {
    public SDLBackend(){
        SDLActivity.addGrabListener(isGrabbing -> {
            Tools.runOnUiThread(() -> Platform.executeGrabbingListeners(isGrabbing));
        });
    }
    public static void initialize(Activity activity) {
        // TODO: check what can be moved to the initialize point
        // we need to setup enough SDL for the game to not crash to initialize it later
        SDL.initialize();
        SDL.setContext(activity);
        SDL.setupJNI();
        SDLControllerManager.initializeDeviceListener();
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
        float x = (float) (Platform.cursorX * LauncherGLSurface.getWindowWidth());
        float y = (float) (Platform.cursorY * LauncherGLSurface.getWindowHeight());
        SDLActivity.onNativeMouse(0, MotionEvent.ACTION_MOVE, x, y, isGrabbing());
        if(isGrabbing()){
            // SDL in relative mode expects these to be reset to 0 or it will freak out (classic:tm: way)
            Platform.cursorX = 0;
            Platform.cursorY = 0;
        }
        Platform.getCursorImplementor().onCursorPosition();
    }


    @Override
    public void sendMouseEvent(int key, int state, int mods) {
        // SDL expects normalized coords, not pixel ones like we use
        // Or pixel ones? Whatever
        float x = (float) (Platform.cursorX * LauncherGLSurface.getWindowWidth());
        float y = (float) (Platform.cursorY * LauncherGLSurface.getWindowHeight());
        if(state == KeyEvent.ACTION_DOWN)
            SDLActivity.onNativeMouseButton(key, KeyEvent.ACTION_DOWN, x, y, isGrabbing());
        else
            SDLActivity.onNativeMouseButton(key, KeyEvent.ACTION_UP, x, y, isGrabbing());
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
}
