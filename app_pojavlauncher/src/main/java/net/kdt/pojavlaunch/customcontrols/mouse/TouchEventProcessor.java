package net.kdt.pojavlaunch.customcontrols.mouse;

import static net.kdt.pojavlaunch.platform.PlatformLibrary.PLATFORM;

import android.view.MotionEvent;
import android.view.View;

import net.kdt.pojavlaunch.platform.PlatformLibrary;
import net.kdt.pojavlaunch.prefs.LauncherPreferences;

import git.artdeell.dnbootstrap.glfw.GLFW;

public abstract class TouchEventProcessor {
    private final View mHostView;
    public TouchEventProcessor(View hostView) {
        mHostView = hostView;
    }

    protected void sendTouchCoordinates(float x, float y) {
        PlatformLibrary.cursorX = x / mHostView.getWidth();
        PlatformLibrary.cursorY = y / mHostView.getHeight();
        PLATFORM.sendMousePosition();
    }

    protected void applyMoveVector(float[] vector) {
        applyMoveVector(vector[0], vector[1]);
    }

    protected void applyMoveVector(float x, float y) {
        PlatformLibrary.cursorX += x * LauncherPreferences.PREF_MOUSESPEED / mHostView.getWidth();
        PlatformLibrary.cursorY += y * LauncherPreferences.PREF_MOUSESPEED / mHostView.getHeight();
        PLATFORM.sendMousePosition();
    }

    abstract public boolean processTouchEvent(MotionEvent motionEvent);
    abstract public void cancelPendingActions();
}
