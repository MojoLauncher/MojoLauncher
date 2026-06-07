package net.kdt.pojavlaunch.customcontrols.mouse;

import android.view.MotionEvent;
import android.view.View;

import net.kdt.pojavlaunch.prefs.LauncherPreferences;

import git.artdeell.dnbootstrap.glfw.GLFW;

public abstract class TouchEventProcessor {
    private final View mHostView;
    public TouchEventProcessor(View hostView) {
        mHostView = hostView;
    }

    protected void sendTouchCoordinates(float x, float y) {
        GLFW.cursorX = x / mHostView.getWidth();
        GLFW.cursorY = y / mHostView.getHeight();
        GLFW.sendMousePos();
    }

    protected void applyMoveVector(float[] vector) {
        applyMoveVector(vector[0], vector[1]);
    }

    protected void applyMoveVector(float x, float y) {
        GLFW.cursorX += x * LauncherPreferences.PREF_MOUSESPEED / mHostView.getWidth();
        GLFW.cursorY += y * LauncherPreferences.PREF_MOUSESPEED / mHostView.getHeight();
        GLFW.sendMousePos();
    }

    abstract public boolean processTouchEvent(MotionEvent motionEvent);
    abstract public void cancelPendingActions();
}
