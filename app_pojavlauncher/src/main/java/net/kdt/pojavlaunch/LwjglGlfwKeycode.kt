// Keycodes from https://github.com/glfw/glfw/blob/master/include/GLFW/glfw3.h
/*-************************************************************************
 * GLFW 3.4 - www.glfw.org
 * A library for OpenGL, window and input
 *------------------------------------------------------------------------
 * Copyright (c) 2002-2006 Marcus Geelnard
 * Copyright (c) 2006-2019 Camilla Löwy <elmindreda@glfw.org>
 *
 * This software is provided 'as-is', without any express or implied
 * warranty. In no event will the authors be held liable for any damages
 * arising from the use of this software.
 *
 * Permission is granted to anyone to use this software for any purpose,
 * including commercial applications, and to alter it and redistribute it
 * freely, subject to the following restrictions:
 *
 * 1. The origin of this software must not be misrepresented; you must not
 *    claim that you wrote the original software. If you use this software
 *    in a product, an acknowledgment in the product documentation would
 *    be appreciated but is not required.
 *
 * 2. Altered source versions must be plainly marked as such, and must not
 *    be misrepresented as being the original software.
 *
 * 3. This notice may not be removed or altered from any source
 *    distribution.
 *
 *************************************************************************/
package net.kdt.pojavlaunch

@Suppress("unused")
object LwjglGlfwKeycode {
    /** The unknown key.  */
    const val GLFW_KEY_UNKNOWN: Short = 0 // should be -1

    /** Printable keys.  */
    const val GLFW_KEY_SPACE: Short = 32
    const val GLFW_KEY_APOSTROPHE: Short = 39
    const val GLFW_KEY_COMMA: Short = 44
    const val GLFW_KEY_MINUS: Short = 45
    const val GLFW_KEY_PERIOD: Short = 46
    const val GLFW_KEY_SLASH: Short = 47
    const val GLFW_KEY_0: Short = 48
    const val GLFW_KEY_1: Short = 49
    const val GLFW_KEY_2: Short = 50
    const val GLFW_KEY_3: Short = 51
    const val GLFW_KEY_4: Short = 52
    const val GLFW_KEY_5: Short = 53
    const val GLFW_KEY_6: Short = 54
    const val GLFW_KEY_7: Short = 55
    const val GLFW_KEY_8: Short = 56
    const val GLFW_KEY_9: Short = 57
    const val GLFW_KEY_SEMICOLON: Short = 59
    const val GLFW_KEY_EQUAL: Short = 61
    const val GLFW_KEY_A: Short = 65
    const val GLFW_KEY_B: Short = 66
    const val GLFW_KEY_C: Short = 67
    const val GLFW_KEY_D: Short = 68
    const val GLFW_KEY_E: Short = 69
    const val GLFW_KEY_F: Short = 70
    const val GLFW_KEY_G: Short = 71
    const val GLFW_KEY_H: Short = 72
    const val GLFW_KEY_I: Short = 73
    const val GLFW_KEY_J: Short = 74
    const val GLFW_KEY_K: Short = 75
    const val GLFW_KEY_L: Short = 76
    const val GLFW_KEY_M: Short = 77
    const val GLFW_KEY_N: Short = 78
    const val GLFW_KEY_O: Short = 79
    const val GLFW_KEY_P: Short = 80
    const val GLFW_KEY_Q: Short = 81
    const val GLFW_KEY_R: Short = 82
    const val GLFW_KEY_S: Short = 83
    const val GLFW_KEY_T: Short = 84
    const val GLFW_KEY_U: Short = 85
    const val GLFW_KEY_V: Short = 86
    const val GLFW_KEY_W: Short = 87
    const val GLFW_KEY_X: Short = 88
    const val GLFW_KEY_Y: Short = 89
    const val GLFW_KEY_Z: Short = 90
    const val GLFW_KEY_LEFT_BRACKET: Short = 91
    const val GLFW_KEY_BACKSLASH: Short = 92
    const val GLFW_KEY_RIGHT_BRACKET: Short = 93
    const val GLFW_KEY_GRAVE_ACCENT: Short = 96
    const val GLFW_KEY_WORLD_1: Short = 161
    const val GLFW_KEY_WORLD_2: Short = 162

    /** Function keys.  */
    const val GLFW_KEY_ESCAPE: Short = 256
    const val GLFW_KEY_ENTER: Short = 257
    const val GLFW_KEY_TAB: Short = 258
    const val GLFW_KEY_BACKSPACE: Short = 259
    const val GLFW_KEY_INSERT: Short = 260
    const val GLFW_KEY_DELETE: Short = 261
    const val GLFW_KEY_RIGHT: Short = 262
    const val GLFW_KEY_LEFT: Short = 263
    const val GLFW_KEY_DOWN: Short = 264
    const val GLFW_KEY_UP: Short = 265
    const val GLFW_KEY_PAGE_UP: Short = 266
    const val GLFW_KEY_PAGE_DOWN: Short = 267
    const val GLFW_KEY_HOME: Short = 268
    const val GLFW_KEY_END: Short = 269
    const val GLFW_KEY_CAPS_LOCK: Short = 280
    const val GLFW_KEY_SCROLL_LOCK: Short = 281
    const val GLFW_KEY_NUM_LOCK: Short = 282
    const val GLFW_KEY_PRINT_SCREEN: Short = 283
    const val GLFW_KEY_PAUSE: Short = 284
    const val GLFW_KEY_F1: Short = 290
    const val GLFW_KEY_F2: Short = 291
    const val GLFW_KEY_F3: Short = 292
    const val GLFW_KEY_F4: Short = 293
    const val GLFW_KEY_F5: Short = 294
    const val GLFW_KEY_F6: Short = 295
    const val GLFW_KEY_F7: Short = 296
    const val GLFW_KEY_F8: Short = 297
    const val GLFW_KEY_F9: Short = 298
    const val GLFW_KEY_F10: Short = 299
    const val GLFW_KEY_F11: Short = 300
    const val GLFW_KEY_F12: Short = 301
    const val GLFW_KEY_F13: Short = 302
    const val GLFW_KEY_F14: Short = 303
    const val GLFW_KEY_F15: Short = 304
    const val GLFW_KEY_F16: Short = 305
    const val GLFW_KEY_F17: Short = 306
    const val GLFW_KEY_F18: Short = 307
    const val GLFW_KEY_F19: Short = 308
    const val GLFW_KEY_F20: Short = 309
    const val GLFW_KEY_F21: Short = 310
    const val GLFW_KEY_F22: Short = 311
    const val GLFW_KEY_F23: Short = 312
    const val GLFW_KEY_F24: Short = 313
    const val GLFW_KEY_F25: Short = 314
    const val GLFW_KEY_KP_0: Short = 320
    const val GLFW_KEY_KP_1: Short = 321
    const val GLFW_KEY_KP_2: Short = 322
    const val GLFW_KEY_KP_3: Short = 323
    const val GLFW_KEY_KP_4: Short = 324
    const val GLFW_KEY_KP_5: Short = 325
    const val GLFW_KEY_KP_6: Short = 326
    const val GLFW_KEY_KP_7: Short = 327
    const val GLFW_KEY_KP_8: Short = 328
    const val GLFW_KEY_KP_9: Short = 329
    const val GLFW_KEY_KP_DECIMAL: Short = 330
    const val GLFW_KEY_KP_DIVIDE: Short = 331
    const val GLFW_KEY_KP_MULTIPLY: Short = 332
    const val GLFW_KEY_KP_SUBTRACT: Short = 333
    const val GLFW_KEY_KP_ADD: Short = 334
    const val GLFW_KEY_KP_ENTER: Short = 335
    const val GLFW_KEY_KP_EQUAL: Short = 336
    const val GLFW_KEY_LEFT_SHIFT: Short = 340
    const val GLFW_KEY_LEFT_CONTROL: Short = 341
    const val GLFW_KEY_LEFT_ALT: Short = 342
    const val GLFW_KEY_LEFT_SUPER: Short = 343
    const val GLFW_KEY_RIGHT_SHIFT: Short = 344
    const val GLFW_KEY_RIGHT_CONTROL: Short = 345
    const val GLFW_KEY_RIGHT_ALT: Short = 346
    const val GLFW_KEY_RIGHT_SUPER: Short = 347
    const val GLFW_KEY_MENU: Short = 348
    val GLFW_KEY_LAST: Short = GLFW_KEY_MENU

    /** If this bit is set one or more Shift keys were held down.  */
    const val GLFW_MOD_SHIFT: Int = 0x1

    /** If this bit is set one or more Control keys were held down.  */
    const val GLFW_MOD_CONTROL: Int = 0x2

    /** If this bit is set one or more Alt keys were held down.  */
    const val GLFW_MOD_ALT: Int = 0x4

    /** If this bit is set one or more Super keys were held down.  */
    const val GLFW_MOD_SUPER: Int = 0x8

    /** If this bit is set the Caps Lock key is enabled and the LOCK_KEY_MODS input mode is set.  */
    const val GLFW_MOD_CAPS_LOCK: Int = 0x10

    /** If this bit is set the Num Lock key is enabled and the LOCK_KEY_MODS input mode is set.  */
    const val GLFW_MOD_NUM_LOCK: Int = 0x20


    /** Mouse buttons. See [mouse button input](http://www.glfw.org/docs/latest/input.html#input_mouse_button) for how these are used.  */
    const val GLFW_MOUSE_BUTTON_1: Short = 0
    const val GLFW_MOUSE_BUTTON_2: Short = 1
    const val GLFW_MOUSE_BUTTON_3: Short = 2
    const val GLFW_MOUSE_BUTTON_4: Short = 3
    const val GLFW_MOUSE_BUTTON_5: Short = 4
    const val GLFW_MOUSE_BUTTON_6: Short = 5
    const val GLFW_MOUSE_BUTTON_7: Short = 6
    const val GLFW_MOUSE_BUTTON_8: Short = 7
    val GLFW_MOUSE_BUTTON_LAST: Short = GLFW_MOUSE_BUTTON_8
    @JvmField
    val GLFW_MOUSE_BUTTON_LEFT: Short = GLFW_MOUSE_BUTTON_1
    @JvmField
    val GLFW_MOUSE_BUTTON_RIGHT: Short = GLFW_MOUSE_BUTTON_2
    @JvmField
    val GLFW_MOUSE_BUTTON_MIDDLE: Short = GLFW_MOUSE_BUTTON_3

    const val GLFW_VISIBLE: Int = 0x20004
    const val GLFW_HOVERED: Int = 0x2000B
}
