package net.kdt.pojavlaunch.game;

import android.view.MotionEvent;
import android.view.View;

import net.kdt.pojavlaunch.game.platform.Platform;
import net.kdt.pojavlaunch.prefs.LauncherPreferences;

public abstract class TouchEventProcessor {
    private final GameView mHostView;
    public TouchEventProcessor(GameView hostView) {
        mHostView = hostView;
    }

    protected void sendTouchCoordinates(float x, float y) {
        Platform.cursorX = x / mHostView.cursorRatioX;
        Platform.cursorY = y / mHostView.cursorRatioY;
        Platform.sendCursorPosition();
    }

    protected void applyMoveVector(float[] vector) {
        applyMoveVector(vector[0], vector[1]);
    }

    protected void applyMoveVector(float x, float y) {
        Platform.cursorX += x * LauncherPreferences.PREF_MOUSESPEED;
        Platform.cursorY += y * LauncherPreferences.PREF_MOUSESPEED;
        Platform.sendCursorPosition();
    }

    abstract public boolean processTouchEvent(MotionEvent motionEvent);
    abstract public void cancelPendingActions();
}
