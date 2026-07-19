package net.kdt.pojavlaunch.customcontrols.mouse;

import static net.kdt.pojavlaunch.platform.Platform.PLATFORM;

import android.view.MotionEvent;
import android.view.View;

import net.kdt.pojavlaunch.platform.Platform;
import net.kdt.pojavlaunch.prefs.LauncherPreferences;

public abstract class TouchEventProcessor {
    private final View mHostView;
    public TouchEventProcessor(View hostView) {
        mHostView = hostView;
    }

    protected void sendTouchCoordinates(float x, float y) {
        Platform.cursorX = x / mHostView.getWidth();
        Platform.cursorY = y / mHostView.getHeight();
        PLATFORM.sendMousePosition();
    }

    protected void applyMoveVector(float[] vector) {
        applyMoveVector(vector[0], vector[1]);
    }

    protected void applyMoveVector(float x, float y) {
        Platform.cursorX += x * LauncherPreferences.PREF_MOUSESPEED / mHostView.getWidth();
        Platform.cursorY += y * LauncherPreferences.PREF_MOUSESPEED / mHostView.getHeight();
        PLATFORM.sendMousePosition();
    }

    abstract public boolean processTouchEvent(MotionEvent motionEvent);
    abstract public void cancelPendingActions();
}
