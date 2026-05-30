package net.kdt.pojavlaunch.customcontrols.gamepad

import net.kdt.pojavlaunch.LwjglGlfwKeycode

class GamepadMap {
    /*
       This class is just here to store the mapping
       can be modified to create re-mappable controls I guess
   
       Be warned, you should define ALL keys if you want to avoid a non defined exception
      */
    var BUTTON_A: GamepadButton? = null
    var BUTTON_B: GamepadButton? = null
    var BUTTON_X: GamepadButton? = null
    var BUTTON_Y: GamepadButton? = null
    var BUTTON_START: GamepadButton? = null
    var BUTTON_SELECT: GamepadButton? = null
    var TRIGGER_RIGHT: GamepadButton? = null
    var TRIGGER_LEFT: GamepadButton? = null
    var SHOULDER_RIGHT: GamepadButton? = null
    var SHOULDER_LEFT: GamepadButton? = null
    var THUMBSTICK_RIGHT: GamepadButton? = null
    var THUMBSTICK_LEFT: GamepadButton? = null
    var DPAD_UP: GamepadButton? = null
    var DPAD_DOWN: GamepadButton? = null
    var DPAD_RIGHT: GamepadButton? = null
    var DPAD_LEFT: GamepadButton? = null

    var DIRECTION_FORWARD: GamepadEmulatedButton? = null
    var DIRECTION_BACKWARD: GamepadEmulatedButton? = null
    var DIRECTION_RIGHT: GamepadEmulatedButton? = null
    var DIRECTION_LEFT: GamepadEmulatedButton? = null

    /*
     * Sets all buttons to a not pressed state, sending an input if needed
     */
    fun resetPressedState() {
        BUTTON_A!!.resetButtonState()
        BUTTON_B!!.resetButtonState()
        BUTTON_X!!.resetButtonState()
        BUTTON_Y!!.resetButtonState()

        BUTTON_START!!.resetButtonState()
        BUTTON_SELECT!!.resetButtonState()

        TRIGGER_LEFT!!.resetButtonState()
        TRIGGER_RIGHT!!.resetButtonState()

        SHOULDER_LEFT!!.resetButtonState()
        SHOULDER_RIGHT!!.resetButtonState()

        THUMBSTICK_LEFT!!.resetButtonState()
        THUMBSTICK_RIGHT!!.resetButtonState()

        DPAD_UP!!.resetButtonState()
        DPAD_RIGHT!!.resetButtonState()
        DPAD_DOWN!!.resetButtonState()
        DPAD_LEFT!!.resetButtonState()
    }

    val buttons: Array<GamepadEmulatedButton>
        /*
             * Returns all GamepadEmulatedButtons of the controller key map.
             */
        get() = arrayOf<GamepadEmulatedButton>(
            BUTTON_A!!, BUTTON_B!!, BUTTON_X!!, BUTTON_Y!!,
            BUTTON_SELECT!!, BUTTON_START!!,
            TRIGGER_LEFT!!, TRIGGER_RIGHT!!,
            SHOULDER_LEFT!!, SHOULDER_RIGHT!!,
            THUMBSTICK_LEFT!!, THUMBSTICK_RIGHT!!,
            DPAD_UP!!, DPAD_RIGHT!!, DPAD_DOWN!!, DPAD_LEFT!!,
            DIRECTION_FORWARD!!, DIRECTION_BACKWARD!!,
            DIRECTION_LEFT!!, DIRECTION_RIGHT!!
        )

    companion object {
        val MOUSE_SCROLL_DOWN: Short = -1
        val MOUSE_SCROLL_UP: Short = -2

        // Made mouse keycodes their own specials because managing special keycodes above 0
        // proved to be complicated
        val MOUSE_LEFT: Short = -3
        val MOUSE_MIDDLE: Short = -4
        val MOUSE_RIGHT: Short = -5

        // Workaround, because GLFW_KEY_UNKNOWN and GLFW_MOUSE_BUTTON_LEFT are both 0
        val UNSPECIFIED: Short = -6

        private fun createAndInitializeButtons(): GamepadMap {
            val gamepadMap = GamepadMap()
            gamepadMap.BUTTON_A = GamepadButton()
            gamepadMap.BUTTON_B = GamepadButton()
            gamepadMap.BUTTON_X = GamepadButton()
            gamepadMap.BUTTON_Y = GamepadButton()

            gamepadMap.BUTTON_START = GamepadButton()
            gamepadMap.BUTTON_SELECT = GamepadButton()

            gamepadMap.TRIGGER_RIGHT = GamepadButton()
            gamepadMap.TRIGGER_LEFT = GamepadButton()

            gamepadMap.SHOULDER_RIGHT = GamepadButton()
            gamepadMap.SHOULDER_LEFT = GamepadButton()

            gamepadMap.DIRECTION_FORWARD = GamepadEmulatedButton()
            gamepadMap.DIRECTION_BACKWARD = GamepadEmulatedButton()
            gamepadMap.DIRECTION_RIGHT = GamepadEmulatedButton()
            gamepadMap.DIRECTION_LEFT = GamepadEmulatedButton()

            gamepadMap.THUMBSTICK_RIGHT = GamepadButton()
            gamepadMap.THUMBSTICK_LEFT = GamepadButton()

            gamepadMap.DPAD_UP = GamepadButton()
            gamepadMap.DPAD_RIGHT = GamepadButton()
            gamepadMap.DPAD_DOWN = GamepadButton()
            gamepadMap.DPAD_LEFT = GamepadButton()
            return gamepadMap
        }

        val defaultGameMap: GamepadMap
            /*
                 * Returns a pre-done mapping used when the mouse is grabbed by the game.
                 */
            get() {
                val gameMap: GamepadMap = createEmptyMap()

                gameMap.BUTTON_A!!.keycodes[0] = LwjglGlfwKeycode.GLFW_KEY_SPACE
                gameMap.BUTTON_B!!.keycodes[0] = LwjglGlfwKeycode.GLFW_KEY_Q
                gameMap.BUTTON_X!!.keycodes[0] = LwjglGlfwKeycode.GLFW_KEY_E
                gameMap.BUTTON_Y!!.keycodes[0] = LwjglGlfwKeycode.GLFW_KEY_F

                gameMap.DIRECTION_FORWARD!!.keycodes[0] = LwjglGlfwKeycode.GLFW_KEY_W
                gameMap.DIRECTION_BACKWARD!!.keycodes[0] = LwjglGlfwKeycode.GLFW_KEY_S
                gameMap.DIRECTION_RIGHT!!.keycodes[0] = LwjglGlfwKeycode.GLFW_KEY_D
                gameMap.DIRECTION_LEFT!!.keycodes[0] = LwjglGlfwKeycode.GLFW_KEY_A

                gameMap.DPAD_UP!!.keycodes[0] = LwjglGlfwKeycode.GLFW_KEY_LEFT_SHIFT
                gameMap.DPAD_DOWN!!.keycodes[0] = LwjglGlfwKeycode.GLFW_KEY_O //For mods ?
                gameMap.DPAD_RIGHT!!.keycodes[0] = LwjglGlfwKeycode.GLFW_KEY_K //For mods ?
                gameMap.DPAD_LEFT!!.keycodes[0] = LwjglGlfwKeycode.GLFW_KEY_J //For mods ?

                gameMap.SHOULDER_LEFT!!.keycodes[0] = MOUSE_SCROLL_UP
                gameMap.SHOULDER_RIGHT!!.keycodes[0] = MOUSE_SCROLL_DOWN

                gameMap.TRIGGER_LEFT!!.keycodes[0] = MOUSE_RIGHT
                gameMap.TRIGGER_RIGHT!!.keycodes[0] = MOUSE_LEFT

                gameMap.THUMBSTICK_LEFT!!.keycodes[0] = LwjglGlfwKeycode.GLFW_KEY_LEFT_CONTROL
                gameMap.THUMBSTICK_RIGHT!!.keycodes[0] = LwjglGlfwKeycode.GLFW_KEY_LEFT_SHIFT
                gameMap.THUMBSTICK_RIGHT!!.isToggleable = true

                gameMap.BUTTON_START!!.keycodes[0] = LwjglGlfwKeycode.GLFW_KEY_ESCAPE
                gameMap.BUTTON_SELECT!!.keycodes[0] = LwjglGlfwKeycode.GLFW_KEY_TAB

                return gameMap
            }

        val defaultMenuMap: GamepadMap
            /*
                 * Returns a pre-done mapping used when the mouse is NOT grabbed by the game.
                 */
            get() {
                val menuMap: GamepadMap = createEmptyMap()

                menuMap.BUTTON_A!!.keycodes[0] = MOUSE_LEFT
                menuMap.BUTTON_B!!.keycodes[0] = LwjglGlfwKeycode.GLFW_KEY_ESCAPE
                menuMap.BUTTON_X!!.keycodes[0] = MOUSE_RIGHT
                run {
                    val keycodes = menuMap.BUTTON_Y!!.keycodes
                    keycodes[0] = LwjglGlfwKeycode.GLFW_KEY_LEFT_SHIFT
                    keycodes[1] = Companion.MOUSE_RIGHT
                }

                run {
                    val keycodes = menuMap.DIRECTION_FORWARD!!.keycodes
                    keycodes[3] = Companion.MOUSE_SCROLL_UP
                    keycodes[2] = keycodes[3]
                    keycodes[1] = keycodes[2]
                    keycodes[0] = keycodes[1]
                }
                run {
                    val keycodes = menuMap.DIRECTION_BACKWARD!!.keycodes
                    keycodes[3] = Companion.MOUSE_SCROLL_DOWN
                    keycodes[2] = keycodes[3]
                    keycodes[1] = keycodes[2]
                    keycodes[0] = keycodes[1]
                }

                menuMap.DPAD_DOWN!!.keycodes[0] = LwjglGlfwKeycode.GLFW_KEY_O //For mods ?
                menuMap.DPAD_RIGHT!!.keycodes[0] = LwjglGlfwKeycode.GLFW_KEY_K //For mods ?
                menuMap.DPAD_LEFT!!.keycodes[0] = LwjglGlfwKeycode.GLFW_KEY_J //For mods ?

                menuMap.SHOULDER_LEFT!!.keycodes[0] = MOUSE_SCROLL_UP
                menuMap.SHOULDER_RIGHT!!.keycodes[0] = MOUSE_SCROLL_DOWN

                menuMap.BUTTON_SELECT!!.keycodes[0] = LwjglGlfwKeycode.GLFW_KEY_ESCAPE

                return menuMap
            }

        /*
    * Returns an pre-initialized GamepadMap with only empty keycodes
    */
        @Suppress("unused")
        fun createEmptyMap(): GamepadMap {
            val emptyMap: GamepadMap = createAndInitializeButtons()
            for (button in emptyMap.buttons) button.keycodes =
                shortArrayOf(UNSPECIFIED, UNSPECIFIED, UNSPECIFIED, UNSPECIFIED)
            return emptyMap
        }

        val specialKeycodeNames: Array<String>
            get() = arrayOf<String>(
                "UNSPECIFIED",
                "MOUSE_RIGHT",
                "MOUSE_MIDDLE",
                "MOUSE_LEFT",
                "SCROLL_UP",
                "SCROLL_DOWN"
            )
    }
}
