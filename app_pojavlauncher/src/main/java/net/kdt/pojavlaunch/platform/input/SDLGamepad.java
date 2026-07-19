package net.kdt.pojavlaunch.platform.input;

import android.view.KeyEvent;
import android.view.MotionEvent;

import git.mojo.sdl.SDLControllerManager;

public class SDLGamepad implements PlatformGamepad {
    @Override
    public void sendKeyEvent(KeyEvent event) {
        if(event.getAction() == KeyEvent.ACTION_DOWN)
            SDLControllerManager.onNativePadDown(event.getDeviceId(),event.getKeyCode(), event.getScanCode());
        else
            SDLControllerManager.onNativePadUp(event.getDeviceId(), event.getKeyCode(), event.getScanCode());
    }

    @Override
    public void sendMotionEvent(MotionEvent event) {
        SDLControllerManager.handleJoystickMotionEvent(event);
    }

    @Override
    public boolean shouldOverride() {
        return true;
    }
}
