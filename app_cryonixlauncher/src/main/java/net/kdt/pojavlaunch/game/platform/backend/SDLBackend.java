package net.kdt.pojavlaunch.game.platform.backend;

import android.app.Activity;
import android.view.MotionEvent;
import android.view.Surface;

import net.kdt.pojavlaunch.game.GameView;
import net.kdt.pojavlaunch.game.platform.Platform;

import git.mojo.sdl.SDL;
import git.mojo.sdl.SDLActivity;
import git.mojo.sdl.SDLControllerManager;
import git.mojo.sdl.SDLInputConnection;

/**
 * SDL3 Platform implementation
 */
public class SDLBackend implements PlatformBackend {

    public SDLBackend() {
        SDLActivity.setGrabListener(SDLBackend::handleGrabStateChange);
        SDLActivity.setCursorCallback(cursor -> {
            if (cursor != null)
                Platform.setCursor(cursor.getBitmap(), cursor.getXhot(), cursor.getYhot());
            else Platform.setCursor(null, 0, 0);
        });
    }

    private static void handleGrabStateChange(boolean isGrabbing) {
        if (isGrabbing) {
            // SDL really expects cursor to be at 0x0 position when relative mode (grabbing = true) is enabled
            // This caused weird jumps when gaining grab because Platform cursor position values contain stale non-zero values at that point.
            // Reset position to 0x0 when gaining grab state
            Platform.cursorX = 0;
            Platform.cursorY = 0;
        }
        Platform.grabStateChanged(isGrabbing);
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
        if (SDLActivity.getNativeSurface() != null) SDLActivity.onNativeSurfaceDestroyed();
        SDLActivity.setNativeSurface(surface);
        SDLActivity.onNativeSurfaceCreated();
        this.surfaceUpdated(); // Update initial size
        SDLActivity.onNativeSurfaceChanged();
    }

    @Override
    public void surfaceUpdated() {
        int w = GameView.getWindowWidth();
        int h = GameView.getWindowHeight();
        float r = GameView.getWindowRate();
        SDLActivity.nativeSetScreenResolution(w, h, w, h, 1.0f, r);
        SDLActivity.onNativeResize();
    }

    @Override
    public void surfaceDestroyed() {
        SDLActivity.onNativeSurfaceDestroyed();
    }

    @Override
    public void sendMousePosition() {
        SDLActivity.onNativeMouse(0, MotionEvent.ACTION_MOVE, (float) Platform.cursorX, (float) Platform.cursorY, Platform.isGrabbing());
        if (Platform.isGrabbing()) {
            // SDL in relative mode expects these to be reset to 0 or it will freak out (classic:tm: way)
            Platform.cursorX = 0;
            Platform.cursorY = 0;
        }
    }


    @Override
    public void sendMouseEvent(int button, int state, int mods) {
        SDLActivity.onNativeMouseButton(button, state, (float) Platform.cursorX, (float) Platform.cursorY, Platform.isGrabbing());
    }

    @Override
    public boolean sendKeyEvent(int key, int state, int mods, char codepoint) {
        if (state == 1) {
            if (codepoint != 0) SDLInputConnection.nativeCommitText(String.valueOf(codepoint), 0);
            return SDLActivity.onNativeKeyDown(key);
        } else return SDLActivity.onNativeKeyUp(key);
    }

    @Override
    public boolean sendKeyEvent(int key, int state, int mods) {
        if (state == 1) return SDLActivity.onNativeKeyDown(key);
        else return SDLActivity.onNativeKeyUp(key);
    }

    @Override
    public boolean sendKeyEvent(int key, boolean state, int mods) {
        if (state) return SDLActivity.onNativeKeyDown(key);
        else return SDLActivity.onNativeKeyUp(key);
    }

    @Override
    public void sendScrollEvent(double x, double y) {
        SDLActivity.onNativeMouse(0, MotionEvent.ACTION_SCROLL, (float) x, (float) y, false);
    }

    @Override
    public void sendBulkUnicodeEvent(String text, int mods) {
        SDLInputConnection.nativeCommitText(text, 0);
    }

    @Override
    public String backendName() {
        return "SDL";
    }

    @Override
    public void setHovered(boolean hovered) {
        SDLActivity.nativeFocusChanged(hovered);
    }

    @Override
    public void setVisible(boolean visible) {
        SDLActivity.nativeVisibilityChanged(visible);
    }
}
