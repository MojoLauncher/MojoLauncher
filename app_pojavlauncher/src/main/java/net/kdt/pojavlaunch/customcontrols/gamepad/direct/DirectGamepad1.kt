package net.kdt.pojavlaunch.customcontrols.gamepad.direct

import android.view.KeyEvent
import android.view.MotionEvent
import fr.spse.gamepad_remapper.GamepadHandler
import org.lwjgl.glfw.CallbackBridge.sGamepadAxisBuffer
import org.lwjgl.glfw.CallbackBridge.sGamepadButtonBuffer

class DirectGamepad : GamepadHandler {
    override fun handleGamepadInput(keycode: Int, value: Float) {
        var value = value
        var gKeycode = -1
        var gAxis = -1
        var normalize = false
        when (keycode) {
            KeyEvent.KEYCODE_BUTTON_A -> gKeycode = GamepadKeycodes.GLFW_GAMEPAD_BUTTON_A.toInt()
            KeyEvent.KEYCODE_BUTTON_B -> gKeycode = GamepadKeycodes.GLFW_GAMEPAD_BUTTON_B.toInt()
            KeyEvent.KEYCODE_BUTTON_X -> gKeycode = GamepadKeycodes.GLFW_GAMEPAD_BUTTON_X.toInt()
            KeyEvent.KEYCODE_BUTTON_Y -> gKeycode = GamepadKeycodes.GLFW_GAMEPAD_BUTTON_Y.toInt()
            KeyEvent.KEYCODE_BUTTON_L1 -> gKeycode =
                GamepadKeycodes.GLFW_GAMEPAD_BUTTON_LEFT_BUMPER.toInt()

            KeyEvent.KEYCODE_BUTTON_R1 -> gKeycode =
                GamepadKeycodes.GLFW_GAMEPAD_BUTTON_RIGHT_BUMPER.toInt()

            KeyEvent.KEYCODE_BUTTON_L2, MotionEvent.AXIS_LTRIGGER -> {
                gAxis = GamepadKeycodes.GLFW_GAMEPAD_AXIS_LEFT_TRIGGER.toInt()
                normalize = true
            }

            KeyEvent.KEYCODE_BUTTON_R2, MotionEvent.AXIS_RTRIGGER -> {
                gAxis = GamepadKeycodes.GLFW_GAMEPAD_AXIS_RIGHT_TRIGGER.toInt()
                normalize = true
            }

            KeyEvent.KEYCODE_BUTTON_THUMBL -> gKeycode =
                GamepadKeycodes.GLFW_GAMEPAD_BUTTON_LEFT_THUMB.toInt()

            KeyEvent.KEYCODE_BUTTON_THUMBR -> gKeycode =
                GamepadKeycodes.GLFW_GAMEPAD_BUTTON_RIGHT_THUMB.toInt()

            KeyEvent.KEYCODE_BUTTON_START -> gKeycode =
                GamepadKeycodes.GLFW_GAMEPAD_BUTTON_START.toInt()

            KeyEvent.KEYCODE_BUTTON_SELECT -> gKeycode =
                GamepadKeycodes.GLFW_GAMEPAD_BUTTON_BACK.toInt()

            KeyEvent.KEYCODE_DPAD_UP -> gKeycode =
                GamepadKeycodes.GLFW_GAMEPAD_BUTTON_DPAD_UP.toInt()

            KeyEvent.KEYCODE_DPAD_DOWN -> gKeycode =
                GamepadKeycodes.GLFW_GAMEPAD_BUTTON_DPAD_DOWN.toInt()

            KeyEvent.KEYCODE_DPAD_LEFT -> gKeycode =
                GamepadKeycodes.GLFW_GAMEPAD_BUTTON_DPAD_LEFT.toInt()

            KeyEvent.KEYCODE_DPAD_RIGHT -> gKeycode =
                GamepadKeycodes.GLFW_GAMEPAD_BUTTON_DPAD_RIGHT.toInt()

            KeyEvent.KEYCODE_DPAD_CENTER -> {
                // Behave the same way as the Gamepad here, as GLFW doesn't have a keycode
                // for the dpad center.
                sGamepadButtonBuffer?.put(
                    GamepadKeycodes.GLFW_GAMEPAD_BUTTON_DPAD_UP.toInt(),
                    GamepadKeycodes.GLFW_RELEASE
                )
                sGamepadButtonBuffer?.put(
                    GamepadKeycodes.GLFW_GAMEPAD_BUTTON_DPAD_DOWN.toInt(),
                    GamepadKeycodes.GLFW_RELEASE
                )
                sGamepadButtonBuffer?.put(
                    GamepadKeycodes.GLFW_GAMEPAD_BUTTON_DPAD_LEFT.toInt(),
                    GamepadKeycodes.GLFW_RELEASE
                )
                sGamepadButtonBuffer?.put(
                    GamepadKeycodes.GLFW_GAMEPAD_BUTTON_DPAD_RIGHT.toInt(),
                    GamepadKeycodes.GLFW_RELEASE
                )
                return
            }

            MotionEvent.AXIS_X -> gAxis = GamepadKeycodes.GLFW_GAMEPAD_AXIS_LEFT_X.toInt()
            MotionEvent.AXIS_Y -> gAxis = GamepadKeycodes.GLFW_GAMEPAD_AXIS_LEFT_Y.toInt()
            MotionEvent.AXIS_Z -> gAxis = GamepadKeycodes.GLFW_GAMEPAD_AXIS_RIGHT_X.toInt()
            MotionEvent.AXIS_RZ -> gAxis = GamepadKeycodes.GLFW_GAMEPAD_AXIS_RIGHT_Y.toInt()
            MotionEvent.AXIS_HAT_X -> {
                sGamepadButtonBuffer?.put(
                    GamepadKeycodes.GLFW_GAMEPAD_BUTTON_DPAD_LEFT.toInt(),
                    if (value < -0.85) GamepadKeycodes.GLFW_PRESS else GamepadKeycodes.GLFW_RELEASE
                )
                sGamepadButtonBuffer?.put(
                    GamepadKeycodes.GLFW_GAMEPAD_BUTTON_DPAD_RIGHT.toInt(),
                    if (value > 0.85) GamepadKeycodes.GLFW_PRESS else GamepadKeycodes.GLFW_RELEASE
                )
                return
            }

            MotionEvent.AXIS_HAT_Y -> {
                sGamepadButtonBuffer?.put(
                    GamepadKeycodes.GLFW_GAMEPAD_BUTTON_DPAD_UP.toInt(),
                    if (value < -0.85) GamepadKeycodes.GLFW_PRESS else GamepadKeycodes.GLFW_RELEASE
                )
                sGamepadButtonBuffer?.put(
                    GamepadKeycodes.GLFW_GAMEPAD_BUTTON_DPAD_DOWN.toInt(),
                    if (value > 0.85) GamepadKeycodes.GLFW_PRESS else GamepadKeycodes.GLFW_RELEASE
                )
                return
            }
        }
        if (gKeycode != -1) {
            sGamepadButtonBuffer?.put(
                gKeycode,
                if (value > 0.85) GamepadKeycodes.GLFW_PRESS else GamepadKeycodes.GLFW_RELEASE
            )
        }
        if (gAxis != -1) {
            if (normalize) value = value * 2 - 1
            sGamepadAxisBuffer.put(gAxis, value)
        }
    }
}
