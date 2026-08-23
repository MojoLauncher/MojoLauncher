package net.kdt.pojavlaunch.customcontrols.mouse;

import static net.kdt.pojavlaunch.CallbackBridge.sendMouseButton;

import android.os.Handler;
import android.view.MotionEvent;

import net.kdt.pojavlaunch.LwjglGlfwKeycode;
import net.kdt.pojavlaunch.Tools;
import net.kdt.pojavlaunch.prefs.LauncherPreferences;
import net.kdt.pojavlaunch.utils.MathUtils;

public class LeftClickGesture extends DistanceGesture {
    public static final int FINGER_STILL_THRESHOLD = (int) Tools.dpToPx(9);

    private boolean mMouseActivated;

    public LeftClickGesture(Handler handler) {
        super(handler);
    }

    @Override
    void onGestureSubmitted() {

    }

    @Override
    boolean shouldSubmitGesture() {
        return true;
    }

    @Override
    protected int getGestureDelay() {
        return LauncherPreferences.PREF_LONGPRESS_TRIGGER;
    }

    @Override
    public boolean checkAndTrigger() {
        boolean fingerStill = travelBelowThreshold(LeftClickGesture.FINGER_STILL_THRESHOLD);
        // If the finger is still, fire the gesture.
        if(fingerStill) {
            sendMouseButton(MotionEvent.BUTTON_PRIMARY, true);
            mMouseActivated = true;
        }
        // Otherwise, don't click but still keep it active
        return true;
    }

    @Override
    public void onGestureCancelled(boolean isSwitching) {
        if(mMouseActivated) {
            sendMouseButton(MotionEvent.BUTTON_PRIMARY, false);
            mMouseActivated = false;
        }
    }

    public static boolean isFingerStill(float startX, float startY, float endX, float endY, float threshold) {
        return MathUtils.dist(
                endX,
                endY,
                startX,
                startY
        ) <= threshold;
    }
}
