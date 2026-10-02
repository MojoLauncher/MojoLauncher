package net.kdt.pojavlaunch.game.platform.input;

import android.view.KeyEvent;
import android.view.MotionEvent;

/**
 * Platform gamepad event consumer. Use this to override emulated gamepad input
 */
public interface PlatformGamepad {
    /**
     * Send gamepad key event directly
     *
     * @param event Android KeyEvent
     */
    void sendKeyEvent(KeyEvent event);

    /**
     * Send gamepad motion event directly
     *
     * @param event Android MotionEvent
     */
    void sendMotionEvent(MotionEvent event);

    /**
     * Destroy gamepad instance
     */
    void onDestroy();
}
