package net.kdt.pojavlaunch.customcontrols.mouse;

import android.view.MotionEvent;
import android.view.View;

import net.kdt.pojavlaunch.LwjglGlfwKeycode;
import net.kdt.pojavlaunch.Tools;
import net.kdt.pojavlaunch.prefs.LauncherPreferences;

import net.kdt.pojavlaunch.CallbackBridge;

public class InGUIEventProcessor extends TouchEventProcessor {
    public static final float FINGER_SCROLL_THRESHOLD = Tools.dpToPx(6);
    public static final float FINGER_STILL_THRESHOLD = Tools.dpToPx(5);

    private final PointerTracker mTracker = new PointerTracker();
    private final TapDetector mSingleTapDetector;
    private View mTouchpad;
    private boolean mIsMouseDown = false;
    private float mStartX, mStartY;
    private final Scroller mScroller = new Scroller(FINGER_SCROLL_THRESHOLD);

    public InGUIEventProcessor(View hostView) {
        super(hostView);
        mSingleTapDetector = new TapDetector(1, TapDetector.DETECTION_METHOD_BOTH);
    }

    @Override
    public boolean processTouchEvent(MotionEvent motionEvent) {
        boolean singleTap = mSingleTapDetector.onTouchEvent(motionEvent);

        switch (motionEvent.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                mTracker.startTracking(motionEvent);
                if(!touchpadDisplayed()) {
                    sendTouchCoordinates(motionEvent.getX(), motionEvent.getY());

                    // disabled gestures means no scrolling possible, send gesture early
                    if (LauncherPreferences.PREF_DISABLE_GESTURES) enableMouse();
                    else setGestureStart(motionEvent);
                }
                break;

            case MotionEvent.ACTION_MOVE:
                int pointerCount = motionEvent.getPointerCount();
                int pointerIndex = mTracker.trackEvent(motionEvent);
                if(pointerCount == 1 || LauncherPreferences.PREF_DISABLE_GESTURES) {
                    if(touchpadDisplayed()) {
                        applyMoveVector(mTracker.getMotionVector());
                    } else {
                        float mainPointerX = motionEvent.getX(pointerIndex);
                        float mainPointerY = motionEvent.getY(pointerIndex);
                        sendTouchCoordinates(mainPointerX, mainPointerY);

                        if(!mIsMouseDown) {
                            if(!hasGestureStarted()) setGestureStart(motionEvent);
                            if(!LeftClickGesture.isFingerStill(mStartX, mStartY, mainPointerX, mainPointerY, FINGER_STILL_THRESHOLD))
                                enableMouse();
                        }

                    }
                } else mScroller.performScroll(mTracker.getMotionVector());
                break;

            case MotionEvent.ACTION_CANCEL:
            case MotionEvent.ACTION_UP:
                mScroller.resetScrollOvershoot();
                mTracker.cancelTracking();

                // Handle single tap on gestures
                if((!LauncherPreferences.PREF_DISABLE_GESTURES || touchpadDisplayed()) && !mIsMouseDown && singleTap) {
                    CallbackBridge.performClick(LwjglGlfwKeycode.GLFW_MOUSE_BUTTON_LEFT);
                }

                if(mIsMouseDown) disableMouse();
                resetGesture();
        }


        return true;
    }

    private boolean touchpadDisplayed() {
        return mTouchpad != null && mTouchpad.getVisibility() == View.VISIBLE;
    }

    public void setAbstractTouchpad(View touchpad) {
        mTouchpad = touchpad;
    }

    private void enableMouse() {
        CallbackBridge.sendMouseButton(LwjglGlfwKeycode.GLFW_MOUSE_BUTTON_LEFT, true);
        mIsMouseDown = true;
    }

    private void disableMouse() {
        CallbackBridge.sendMouseButton(LwjglGlfwKeycode.GLFW_MOUSE_BUTTON_LEFT, false);
        mIsMouseDown = false;
    }

    private void setGestureStart(MotionEvent event) {
        mStartX = event.getX();
        mStartY = event.getY();
    }

    private void resetGesture() {
        mStartX = mStartY = -1;
    }

    private boolean hasGestureStarted() {
        return mStartX != -1 || mStartY != -1;
    }

    @Override
    public void cancelPendingActions() {
        mScroller.resetScrollOvershoot();
        if(mIsMouseDown) disableMouse();
    }
}
