package net.kdt.pojavlaunch.platform.input;

import android.view.KeyEvent;
import android.view.MotionEvent;

public interface PlatformGamepad {
    void sendKeyEvent(KeyEvent event);
    void sendMotionEvent(MotionEvent event);
    boolean shouldOverride();
}
