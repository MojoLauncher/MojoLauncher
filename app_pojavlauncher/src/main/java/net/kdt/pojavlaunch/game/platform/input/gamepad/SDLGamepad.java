package net.kdt.pojavlaunch.game.platform.input.gamepad;

import android.view.KeyEvent;
import android.view.MotionEvent;

import net.kdt.pojavlaunch.game.platform.input.PlatformGamepad;

import git.mojo.sdl.SDLControllerManager;


/**
 * SDL3 Gamepad implementation
 */
public class SDLGamepad implements PlatformGamepad {
    @Override
    public void sendKeyEvent(KeyEvent event) {
        if (event.getAction() == KeyEvent.ACTION_DOWN)
            SDLControllerManager.onNativePadDown(event.getDeviceId(), event.getKeyCode(), event.getScanCode());
        else
            SDLControllerManager.onNativePadUp(event.getDeviceId(), event.getKeyCode(), event.getScanCode());
    }

    @Override
    public void sendMotionEvent(MotionEvent event) {
        SDLControllerManager.handleJoystickMotionEvent(event);
    }

    @Override
    public void onDestroy() {

    }
}
