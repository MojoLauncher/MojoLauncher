//
// Created by maks on 22.08.2026.
//

#include <android/input.h>
#include "awt_keycodes.h"

jint translate_awt_mouse(jint android_mousekey) {
    switch (android_mousekey) {
        case AMOTION_EVENT_BUTTON_PRIMARY: return BUTTON1_DOWN_MASK;
        case AMOTION_EVENT_BUTTON_SECONDARY: return BUTTON3_DOWN_MASK;
        case AMOTION_EVENT_BUTTON_TERTIARY: return BUTTON2_DOWN_MASK;
        default:
            return -1;
    }
}

jint translate_awt_keycode(jint android_keycode) {
    switch (android_keycode) {
        // Other keycodes require keyboard mapping and modifier handling

        case AKEYCODE_ENTER: return VK_ENTER;
        case AKEYCODE_DEL: return VK_BACK_SPACE;
        case AKEYCODE_TAB: return VK_TAB;
        case AKEYCODE_CLEAR: return VK_CLEAR;
        case AKEYCODE_SHIFT_LEFT: return VK_SHIFT;
        case AKEYCODE_CTRL_LEFT: return VK_CONTROL;
        case AKEYCODE_ALT_LEFT: return VK_ALT;
        case AKEYCODE_BREAK: return VK_PAUSE;
        case AKEYCODE_CAPS_LOCK: return VK_CAPS_LOCK;
        case AKEYCODE_ESCAPE: return VK_ESCAPE;
        case AKEYCODE_SPACE: return VK_SPACE;
        case AKEYCODE_PAGE_UP: return VK_PAGE_UP;
        case AKEYCODE_PAGE_DOWN: return VK_PAGE_DOWN;
        case AKEYCODE_MOVE_END: return VK_END;
        case AKEYCODE_MOVE_HOME: return VK_HOME;
        case AKEYCODE_DPAD_LEFT: return VK_LEFT;
        case AKEYCODE_DPAD_UP: return VK_UP;
        case AKEYCODE_DPAD_RIGHT: return VK_RIGHT;
        case AKEYCODE_DPAD_DOWN: return VK_DOWN;
        case AKEYCODE_COMMA: return VK_COMMA;
        case AKEYCODE_MINUS: return VK_MINUS;
        case AKEYCODE_PERIOD: return VK_PERIOD;
        case AKEYCODE_SLASH: return VK_SLASH;
        case AKEYCODE_0: return VK_0;
        case AKEYCODE_1: return VK_1;
        case AKEYCODE_2: return VK_2;
        case AKEYCODE_3: return VK_3;
        case AKEYCODE_4: return VK_4;
        case AKEYCODE_5: return VK_5;
        case AKEYCODE_6: return VK_6;
        case AKEYCODE_7: return VK_7;
        case AKEYCODE_8: return VK_8;
        case AKEYCODE_9: return VK_9;
        case AKEYCODE_SEMICOLON: return VK_SEMICOLON;
        case AKEYCODE_EQUALS: return VK_EQUALS;
        case AKEYCODE_A: return VK_A;
        case AKEYCODE_B: return VK_B;
        case AKEYCODE_C: return VK_C;
        case AKEYCODE_D: return VK_D;
        case AKEYCODE_E: return VK_E;
        case AKEYCODE_F: return VK_F;
        case AKEYCODE_G: return VK_G;
        case AKEYCODE_H: return VK_H;
        case AKEYCODE_I: return VK_I;
        case AKEYCODE_J: return VK_J;
        case AKEYCODE_K: return VK_K;
        case AKEYCODE_L: return VK_L;
        case AKEYCODE_M: return VK_M;
        case AKEYCODE_N: return VK_N;
        case AKEYCODE_O: return VK_O;
        case AKEYCODE_P: return VK_P;
        case AKEYCODE_Q: return VK_Q;
        case AKEYCODE_R: return VK_R;
        case AKEYCODE_S: return VK_S;
        case AKEYCODE_T: return VK_T;
        case AKEYCODE_U: return VK_U;
        case AKEYCODE_V: return VK_V;
        case AKEYCODE_W: return VK_W;
        case AKEYCODE_X: return VK_X;
        case AKEYCODE_Y: return VK_Y;
        case AKEYCODE_Z: return VK_Z;
        case AKEYCODE_LEFT_BRACKET: return VK_OPEN_BRACKET;
        case AKEYCODE_BACKSLASH: return VK_BACK_SLASH;
        case AKEYCODE_RIGHT_BRACKET: return VK_CLOSE_BRACKET;
        case AKEYCODE_NUMPAD_0: return VK_NUMPAD0;
        case AKEYCODE_NUMPAD_1: return VK_NUMPAD1;
        case AKEYCODE_NUMPAD_2: return VK_NUMPAD2;
        case AKEYCODE_NUMPAD_3: return VK_NUMPAD3;
        case AKEYCODE_NUMPAD_4: return VK_NUMPAD4;
        case AKEYCODE_NUMPAD_5: return VK_NUMPAD5;
        case AKEYCODE_NUMPAD_6: return VK_NUMPAD6;
        case AKEYCODE_NUMPAD_7: return VK_NUMPAD7;
        case AKEYCODE_NUMPAD_8: return VK_NUMPAD8;
        case AKEYCODE_NUMPAD_9: return VK_NUMPAD9;
        case AKEYCODE_NUMPAD_MULTIPLY: return VK_MULTIPLY;
        case AKEYCODE_NUMPAD_ADD: return VK_ADD;
        case AKEYCODE_NUMPAD_COMMA: return VK_SEPARATOR;
        case AKEYCODE_NUMPAD_SUBTRACT: return VK_SUBTRACT;
        case AKEYCODE_NUMPAD_DOT: return VK_DECIMAL;
        case AKEYCODE_NUMPAD_DIVIDE: return VK_DIVIDE;
        case AKEYCODE_FORWARD_DEL: return VK_DELETE;
        case AKEYCODE_NUM_LOCK: return VK_NUM_LOCK;
        case AKEYCODE_SCROLL_LOCK: return VK_SCROLL_LOCK;
        case AKEYCODE_F1: return VK_F1;
        case AKEYCODE_F2: return VK_F2;
        case AKEYCODE_F3: return VK_F3;
        case AKEYCODE_F4: return VK_F4;
        case AKEYCODE_F5: return VK_F5;
        case AKEYCODE_F6: return VK_F6;
        case AKEYCODE_F7: return VK_F7;
        case AKEYCODE_F8: return VK_F8;
        case AKEYCODE_F9: return VK_F9;
        case AKEYCODE_F10: return VK_F10;
        case AKEYCODE_F11: return VK_F11;
        case AKEYCODE_F12: return VK_F12;

        case AKEYCODE_SYSRQ: return VK_PRINTSCREEN;
        case AKEYCODE_INSERT: return VK_INSERT;
        case AKEYCODE_HELP: return VK_HELP;
        case AKEYCODE_META_LEFT: return VK_META;

        case AKEYCODE_GRAVE: return VK_BACK_QUOTE;
        case AKEYCODE_APOSTROPHE: return VK_QUOTE;

        case AKEYCODE_AT: return VK_AT;

        case AKEYCODE_PLUS: return VK_PLUS;
        case AKEYCODE_KANA: return VK_KANA;

        case AKEYCODE_CUT: return VK_CUT;
        case AKEYCODE_COPY: return VK_COPY;
        case AKEYCODE_PASTE: return VK_PASTE;

        default:
            return VK_UNDEFINED;
    }
}



