package net.kdt.pojavlaunch.customcontrols.gamepad.direct;

import static org.lwjgl.glfw.CallbackBridge.sGamepadAxisBuffer;
import static org.lwjgl.glfw.CallbackBridge.sGamepadButtonBuffer;

import android.view.KeyEvent;
import android.view.MotionEvent;

import fr.spse.gamepad_remapper.GamepadHandler;

public class DirectGamepad implements GamepadHandler {
    @Override
    public void handleGamepadInput(int keycode, float value) {
        int gKeycode = -1, gAxis = -1;
        switch (keycode) {
            case KeyEvent.KEYCODE_BUTTON_A: gKeycode = GamepadKeycodes.GLFW_GAMEPAD_BUTTON_A; break;
            case KeyEvent.KEYCODE_BUTTON_B: gKeycode = GamepadKeycodes.GLFW_GAMEPAD_BUTTON_B; break;
            case KeyEvent.KEYCODE_BUTTON_X: gKeycode = GamepadKeycodes.GLFW_GAMEPAD_BUTTON_X; break;
            case KeyEvent.KEYCODE_BUTTON_Y: gKeycode = GamepadKeycodes.GLFW_GAMEPAD_BUTTON_Y; break;
            case KeyEvent.KEYCODE_BUTTON_L1: gKeycode = GamepadKeycodes.GLFW_GAMEPAD_BUTTON_LEFT_BUMPER; break;
            case KeyEvent.KEYCODE_BUTTON_R1: gKeycode = GamepadKeycodes.GLFW_GAMEPAD_BUTTON_RIGHT_BUMPER; break;
            case KeyEvent.KEYCODE_BUTTON_L2:
            case MotionEvent.AXIS_LTRIGGER:
                gAxis = GamepadKeycodes.GLFW_GAMEPAD_AXIS_LEFT_TRIGGER;
                break;
            case KeyEvent.KEYCODE_BUTTON_R2:
            case MotionEvent.AXIS_RTRIGGER:
                gAxis = GamepadKeycodes.GLFW_GAMEPAD_AXIS_RIGHT_TRIGGER;
                break;
            case KeyEvent.KEYCODE_BUTTON_THUMBL: gKeycode = GamepadKeycodes.GLFW_GAMEPAD_BUTTON_LEFT_THUMB; break;
            case KeyEvent.KEYCODE_BUTTON_THUMBR: gKeycode = GamepadKeycodes.GLFW_GAMEPAD_BUTTON_RIGHT_THUMB; break;
            case KeyEvent.KEYCODE_BUTTON_START: gKeycode = GamepadKeycodes.GLFW_GAMEPAD_BUTTON_START; break;
            case KeyEvent.KEYCODE_BUTTON_SELECT: gKeycode = GamepadKeycodes.GLFW_GAMEPAD_BUTTON_BACK; break;
            case KeyEvent.KEYCODE_DPAD_UP: gKeycode = GamepadKeycodes.GLFW_GAMEPAD_BUTTON_DPAD_UP; break;
            case KeyEvent.KEYCODE_DPAD_DOWN: gKeycode = GamepadKeycodes.GLFW_GAMEPAD_BUTTON_DPAD_DOWN; break;
            case KeyEvent.KEYCODE_DPAD_LEFT: gKeycode = GamepadKeycodes.GLFW_GAMEPAD_BUTTON_DPAD_LEFT; break;
            case KeyEvent.KEYCODE_DPAD_RIGHT: gKeycode = GamepadKeycodes.GLFW_GAMEPAD_BUTTON_DPAD_RIGHT; break;
            case KeyEvent.KEYCODE_DPAD_CENTER:
                // Behave the same way as the Gamepad here, as GLFW doesn't have a keycode
                // for the dpad center.
                sGamepadButtonBuffer.put(GamepadKeycodes.GLFW_GAMEPAD_BUTTON_DPAD_UP, GamepadKeycodes.GLFW_RELEASE);
                sGamepadButtonBuffer.put(GamepadKeycodes.GLFW_GAMEPAD_BUTTON_DPAD_DOWN, GamepadKeycodes.GLFW_RELEASE);
                sGamepadButtonBuffer.put(GamepadKeycodes.GLFW_GAMEPAD_BUTTON_DPAD_LEFT, GamepadKeycodes.GLFW_RELEASE);
                sGamepadButtonBuffer.put(GamepadKeycodes.GLFW_GAMEPAD_BUTTON_DPAD_RIGHT, GamepadKeycodes.GLFW_RELEASE);
                return;
            case MotionEvent.AXIS_X: gAxis = GamepadKeycodes.GLFW_GAMEPAD_AXIS_LEFT_X; break;
            case MotionEvent.AXIS_Y: gAxis = GamepadKeycodes.GLFW_GAMEPAD_AXIS_LEFT_Y; break;
            case MotionEvent.AXIS_Z: gAxis = GamepadKeycodes.GLFW_GAMEPAD_AXIS_RIGHT_X; break;
            case MotionEvent.AXIS_RZ: gAxis = GamepadKeycodes.GLFW_GAMEPAD_AXIS_RIGHT_Y; break;
            case MotionEvent.AXIS_HAT_X:
                sGamepadButtonBuffer.put(
                        GamepadKeycodes.GLFW_GAMEPAD_BUTTON_DPAD_LEFT,
                        value < -0.85 ? GamepadKeycodes.GLFW_PRESS : GamepadKeycodes.GLFW_RELEASE
                );
                sGamepadButtonBuffer.put(
                        GamepadKeycodes.GLFW_GAMEPAD_BUTTON_DPAD_RIGHT,
                        value > 0.85 ? GamepadKeycodes.GLFW_PRESS : GamepadKeycodes.GLFW_RELEASE
                );
                return;
            case MotionEvent.AXIS_HAT_Y:
                sGamepadButtonBuffer.put(
                        GamepadKeycodes.GLFW_GAMEPAD_BUTTON_DPAD_UP,
                        value < -0.85 ? GamepadKeycodes.GLFW_PRESS : GamepadKeycodes.GLFW_RELEASE
                );
                sGamepadButtonBuffer.put(
                        GamepadKeycodes.GLFW_GAMEPAD_BUTTON_DPAD_DOWN,
                        value > 0.85 ? GamepadKeycodes.GLFW_PRESS : GamepadKeycodes.GLFW_RELEASE
                );
                return;
        }
        if(gKeycode != -1) {
            sGamepadButtonBuffer.put(gKeycode, value > 0.85 ? GamepadKeycodes.GLFW_PRESS : GamepadKeycodes.GLFW_RELEASE);
        }
        if(gAxis != -1) {
            sGamepadAxisBuffer.put(gAxis, value);
        }
    }
}
