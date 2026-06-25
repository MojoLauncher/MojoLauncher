package net.kdt.pojavlaunch.customcontrols.mouse;

import android.os.Handler;

import net.kdt.pojavlaunch.LwjglGlfwKeycode;

import net.kdt.pojavlaunch.CallbackBridge;

public class RightClickGesture extends DistanceGesture {
    private boolean mGestureEnabled = true;
    private boolean mGestureValid = true;

    public RightClickGesture(Handler mHandler) {
        super(mHandler);
    }

    @Override
    void onGestureSubmitted() {
        mGestureEnabled = false;
        mGestureValid = true;
    }

    @Override
    boolean shouldSubmitGesture() {
        return mGestureEnabled;
    }

    @Override
    protected int getGestureDelay() {
        return 150;
    }

    @Override
    public boolean checkAndTrigger() {
        // If the validate() method was called, it means that the user held on for too long. The cancellation should be ignored.
        mGestureValid = false;
        // Never call onGestureCancelled. This way we will be able to reserve that only for when
        // the gesture is stopped in the code (when the user lets go of the screen or the tap was
        // cancelled by turning on the grab)
        return true;
    }

    @Override
    public void onGestureCancelled(boolean isSwitching) {
        mGestureEnabled = true;
        if(!mGestureValid || isSwitching) return;
        boolean fingerStill = travelBelowThreshold(LeftClickGesture.FINGER_STILL_THRESHOLD);
        System.out.println("Right click: " + fingerStill);
        if(!fingerStill) return;
        CallbackBridge.sendMouseButton(LwjglGlfwKeycode.GLFW_MOUSE_BUTTON_RIGHT, true);
        CallbackBridge.sendMouseButton(LwjglGlfwKeycode.GLFW_MOUSE_BUTTON_RIGHT, false);
    }
}
