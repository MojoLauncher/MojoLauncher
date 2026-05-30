package net.kdt.pojavlaunch

/*
     * Copyright (c) 1996, 2009, Oracle and/or its affiliates. All rights reserved.
     * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
     *
     * This code is free software; you can redistribute it and/or modify it
     * under the terms of the GNU General Public License version 2 only, as
     * published by the Free Software Foundation.  Oracle designates this
     * particular file as subject to the "Classpath" exception as provided
     * by Oracle in the LICENSE file that accompanied this code.
     *
     * This code is distributed in the hope that it will be useful, but WITHOUT
     * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
     * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
     * version 2 for more details (a copy is included in the LICENSE file that
     * accompanied this code).
     *
     * You should have received a copy of the GNU General Public License version
     * 2 along with this work; if not, write to the Free Software Foundation,
     * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
     *
     * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
     * or visit www.oracle.com if you need additional information or have any
     * questions.
     */
@Suppress("unused")
object AWTInputEvent {
    // InputEvent
    /**
     * This flag indicates that the Shift key was down when the event
     * occurred.
     */
    const val SHIFT_MASK: Int = 1 //first bit

    /**
     * This flag indicates that the Control key was down when the event
     * occurred.
     */
    val CTRL_MASK: Int = 1 shl 1

    /**
     * This flag indicates that the Meta key was down when the event
     * occurred. For mouse events, this flag indicates that the right
     * button was pressed or released.
     */
    val META_MASK: Int = 1 shl 2

    /**
     * This flag indicates that the Alt key was down when
     * the event occurred. For mouse events, this flag indicates that the
     * middle mouse button was pressed or released.
     */
    val ALT_MASK: Int = 1 shl 3

    /**
     * The AltGraph key modifier constant.
     */
    val ALT_GRAPH_MASK: Int = 1 shl 5

    /**
     * The Mouse Button1 modifier constant.
     * It is recommended that BUTTON1_DOWN_MASK be used instead.
     */
    val BUTTON1_MASK: Int = 1 shl 4

    /**
     * The Mouse Button2 modifier constant.
     * It is recommended that BUTTON2_DOWN_MASK be used instead.
     * Note that BUTTON2_MASK has the same value as ALT_MASK.
     */
    val BUTTON2_MASK: Int = ALT_MASK

    /**
     * The Mouse Button3 modifier constant.
     * It is recommended that BUTTON3_DOWN_MASK be used instead.
     * Note that BUTTON3_MASK has the same value as META_MASK.
     */
    val BUTTON3_MASK: Int = META_MASK

    /**
     * The Shift key extended modifier constant.
     * @since 1.4
     */
    val SHIFT_DOWN_MASK: Int = 1 shl 6

    /**
     * The Control key extended modifier constant.
     * @since 1.4
     */
    val CTRL_DOWN_MASK: Int = 1 shl 7

    /**
     * The Meta key extended modifier constant.
     * @since 1.4
     */
    val META_DOWN_MASK: Int = 1 shl 8

    /**
     * The Alt key extended modifier constant.
     * @since 1.4
     */
    val ALT_DOWN_MASK: Int = 1 shl 9

    /**
     * The Mouse Button1 extended modifier constant.
     * @since 1.4
     */
    val BUTTON1_DOWN_MASK: Int = 1 shl 10

    /**
     * The Mouse Button2 extended modifier constant.
     * @since 1.4
     */
    val BUTTON2_DOWN_MASK: Int = 1 shl 11

    /**
     * The Mouse Button3 extended modifier constant.
     * @since 1.4
     */
    val BUTTON3_DOWN_MASK: Int = 1 shl 12

    /**
     * The AltGraph key extended modifier constant.
     * @since 1.4
     */
    val ALT_GRAPH_DOWN_MASK: Int = 1 shl 13


    // KeyEvent
    /**
     * The first number in the range of ids used for key events.
     */
    const val KEY_FIRST: Int = 400

    /**
     * The last number in the range of ids used for key events.
     */
    const val KEY_LAST: Int = 402

    /**
     * The "key typed" event.  This event is generated when a character is
     * entered.  In the simplest case, it is produced by a single key press.
     * Often, however, characters are produced by series of key presses, and
     * the mapping from key pressed events to key typed events may be
     * many-to-one or many-to-many.
     */
    val KEY_TYPED: Int = KEY_FIRST

    /**
     * The "key pressed" event. This event is generated when a key
     * is pushed down.
     */
    val KEY_PRESSED: Int = 1 + KEY_FIRST //Event.KEY_PRESS

    /**
     * The "key released" event. This event is generated when a key
     * is let up.
     */
    val KEY_RELEASED: Int = 2 + KEY_FIRST //Event.KEY_RELEASE

    /* Virtual key codes. */
    @JvmField
    val VK_ENTER: Int = '\n'.code
    @JvmField
    val VK_BACK_SPACE: Int = '\b'.code
    val VK_TAB: Int = '\t'.code
    const val VK_CANCEL: Int = 0x03
    const val VK_CLEAR: Int = 0x0C
    const val VK_SHIFT: Int = 0x10
    const val VK_CONTROL: Int = 0x11
    const val VK_ALT: Int = 0x12
    const val VK_PAUSE: Int = 0x13
    const val VK_CAPS_LOCK: Int = 0x14
    const val VK_ESCAPE: Int = 0x1B
    const val VK_SPACE: Int = 0x20
    const val VK_PAGE_UP: Int = 0x21
    const val VK_PAGE_DOWN: Int = 0x22
    const val VK_END: Int = 0x23
    const val VK_HOME: Int = 0x24

    /**
     * Constant for the non-numpad **left** arrow key.
     * @see .VK_KP_LEFT
     */
    const val VK_LEFT: Int = 0x25

    /**
     * Constant for the non-numpad **up** arrow key.
     * @see .VK_KP_UP
     */
    const val VK_UP: Int = 0x26

    /**
     * Constant for the non-numpad **right** arrow key.
     * @see .VK_KP_RIGHT
     */
    const val VK_RIGHT: Int = 0x27

    /**
     * Constant for the non-numpad **down** arrow key.
     * @see .VK_KP_DOWN
     */
    const val VK_DOWN: Int = 0x28

    /**
     * Constant for the comma key, ","
     */
    const val VK_COMMA: Int = 0x2C

    /**
     * Constant for the minus key, "-"
     * @since 1.2
     */
    const val VK_MINUS: Int = 0x2D

    /**
     * Constant for the period key, "."
     */
    const val VK_PERIOD: Int = 0x2E

    /**
     * Constant for the forward slash key, "/"
     */
    const val VK_SLASH: Int = 0x2F

    /** VK_0 thru VK_9 are the same as ASCII '0' thru '9' (0x30 - 0x39)  */
    const val VK_0: Int = 0x30
    const val VK_1: Int = 0x31
    const val VK_2: Int = 0x32
    const val VK_3: Int = 0x33
    const val VK_4: Int = 0x34
    const val VK_5: Int = 0x35
    const val VK_6: Int = 0x36
    const val VK_7: Int = 0x37
    const val VK_8: Int = 0x38
    const val VK_9: Int = 0x39

    /**
     * Constant for the semicolon key, ";"
     */
    const val VK_SEMICOLON: Int = 0x3B

    /**
     * Constant for the equals key, "="
     */
    const val VK_EQUALS: Int = 0x3D

    /** VK_A thru VK_Z are the same as ASCII 'A' thru 'Z' (0x41 - 0x5A)  */
    const val VK_A: Int = 0x41
    const val VK_B: Int = 0x42
    const val VK_C: Int = 0x43
    const val VK_D: Int = 0x44
    const val VK_E: Int = 0x45
    const val VK_F: Int = 0x46
    const val VK_G: Int = 0x47
    const val VK_H: Int = 0x48
    const val VK_I: Int = 0x49
    const val VK_J: Int = 0x4A
    const val VK_K: Int = 0x4B
    const val VK_L: Int = 0x4C
    const val VK_M: Int = 0x4D
    const val VK_N: Int = 0x4E
    const val VK_O: Int = 0x4F
    const val VK_P: Int = 0x50
    const val VK_Q: Int = 0x51
    const val VK_R: Int = 0x52
    const val VK_S: Int = 0x53
    const val VK_T: Int = 0x54
    const val VK_U: Int = 0x55
    const val VK_V: Int = 0x56
    const val VK_W: Int = 0x57
    const val VK_X: Int = 0x58
    const val VK_Y: Int = 0x59
    const val VK_Z: Int = 0x5A

    /**
     * Constant for the open bracket key, "["
     */
    const val VK_OPEN_BRACKET: Int = 0x5B

    /**
     * Constant for the back slash key, "\"
     */
    const val VK_BACK_SLASH: Int = 0x5C

    /**
     * Constant for the close bracket key, "]"
     */
    const val VK_CLOSE_BRACKET: Int = 0x5D

    const val VK_NUMPAD0: Int = 0x60
    const val VK_NUMPAD1: Int = 0x61
    const val VK_NUMPAD2: Int = 0x62
    const val VK_NUMPAD3: Int = 0x63
    const val VK_NUMPAD4: Int = 0x64
    const val VK_NUMPAD5: Int = 0x65
    const val VK_NUMPAD6: Int = 0x66
    const val VK_NUMPAD7: Int = 0x67
    const val VK_NUMPAD8: Int = 0x68
    const val VK_NUMPAD9: Int = 0x69
    const val VK_MULTIPLY: Int = 0x6A
    const val VK_ADD: Int = 0x6B

    /**
     * This constant is obsolete, and is included only for backwards
     * compatibility.
     * @see .VK_SEPARATOR
     */
    const val VK_SEPARATER: Int = 0x6C

    /**
     * Constant for the Numpad Separator key.
     * @since 1.4
     */
    val VK_SEPARATOR: Int = VK_SEPARATER

    const val VK_SUBTRACT: Int = 0x6D
    const val VK_DECIMAL: Int = 0x6E
    const val VK_DIVIDE: Int = 0x6F
    const val VK_DELETE: Int = 0x7F /* ASCII DEL */
    const val VK_NUM_LOCK: Int = 0x90
    const val VK_SCROLL_LOCK: Int = 0x91

    /** Constant for the F1 function key.  */
    const val VK_F1: Int = 0x70

    /** Constant for the F2 function key.  */
    const val VK_F2: Int = 0x71

    /** Constant for the F3 function key.  */
    const val VK_F3: Int = 0x72

    /** Constant for the F4 function key.  */
    const val VK_F4: Int = 0x73

    /** Constant for the F5 function key.  */
    const val VK_F5: Int = 0x74

    /** Constant for the F6 function key.  */
    const val VK_F6: Int = 0x75

    /** Constant for the F7 function key.  */
    const val VK_F7: Int = 0x76

    /** Constant for the F8 function key.  */
    const val VK_F8: Int = 0x77

    /** Constant for the F9 function key.  */
    const val VK_F9: Int = 0x78

    /** Constant for the F10 function key.  */
    const val VK_F10: Int = 0x79

    /** Constant for the F11 function key.  */
    const val VK_F11: Int = 0x7A

    /** Constant for the F12 function key.  */
    const val VK_F12: Int = 0x7B

    /**
     * Constant for the F13 function key.
     * @since 1.2
     */
    /* F13 - F24 are used on IBM 3270 keyboard; use random range for constants. */
    const val VK_F13: Int = 0xF000

    /**
     * Constant for the F14 function key.
     * @since 1.2
     */
    const val VK_F14: Int = 0xF001

    /**
     * Constant for the F15 function key.
     * @since 1.2
     */
    const val VK_F15: Int = 0xF002

    /**
     * Constant for the F16 function key.
     * @since 1.2
     */
    const val VK_F16: Int = 0xF003

    /**
     * Constant for the F17 function key.
     * @since 1.2
     */
    const val VK_F17: Int = 0xF004

    /**
     * Constant for the F18 function key.
     * @since 1.2
     */
    const val VK_F18: Int = 0xF005

    /**
     * Constant for the F19 function key.
     * @since 1.2
     */
    const val VK_F19: Int = 0xF006

    /**
     * Constant for the F20 function key.
     * @since 1.2
     */
    const val VK_F20: Int = 0xF007

    /**
     * Constant for the F21 function key.
     * @since 1.2
     */
    const val VK_F21: Int = 0xF008

    /**
     * Constant for the F22 function key.
     * @since 1.2
     */
    const val VK_F22: Int = 0xF009

    /**
     * Constant for the F23 function key.
     * @since 1.2
     */
    const val VK_F23: Int = 0xF00A

    /**
     * Constant for the F24 function key.
     * @since 1.2
     */
    const val VK_F24: Int = 0xF00B

    const val VK_PRINTSCREEN: Int = 0x9A
    const val VK_INSERT: Int = 0x9B
    const val VK_HELP: Int = 0x9C
    const val VK_META: Int = 0x9D

    const val VK_BACK_QUOTE: Int = 0xC0
    const val VK_QUOTE: Int = 0xDE

    /**
     * Constant for the numeric keypad **up** arrow key.
     * @see .VK_UP
     * 
     * @since 1.2
     */
    const val VK_KP_UP: Int = 0xE0

    /**
     * Constant for the numeric keypad **down** arrow key.
     * @see .VK_DOWN
     * 
     * @since 1.2
     */
    const val VK_KP_DOWN: Int = 0xE1

    /**
     * Constant for the numeric keypad **left** arrow key.
     * @see .VK_LEFT
     * 
     * @since 1.2
     */
    const val VK_KP_LEFT: Int = 0xE2

    /**
     * Constant for the numeric keypad **right** arrow key.
     * @see .VK_RIGHT
     * 
     * @since 1.2
     */
    const val VK_KP_RIGHT: Int = 0xE3

    /* For European keyboards */
    /** @since 1.2
     */
    const val VK_DEAD_GRAVE: Int = 0x80

    /** @since 1.2
     */
    const val VK_DEAD_ACUTE: Int = 0x81

    /** @since 1.2
     */
    const val VK_DEAD_CIRCUMFLEX: Int = 0x82

    /** @since 1.2
     */
    const val VK_DEAD_TILDE: Int = 0x83

    /** @since 1.2
     */
    const val VK_DEAD_MACRON: Int = 0x84

    /** @since 1.2
     */
    const val VK_DEAD_BREVE: Int = 0x85

    /** @since 1.2
     */
    const val VK_DEAD_ABOVEDOT: Int = 0x86

    /** @since 1.2
     */
    const val VK_DEAD_DIAERESIS: Int = 0x87

    /** @since 1.2
     */
    const val VK_DEAD_ABOVERING: Int = 0x88

    /** @since 1.2
     */
    const val VK_DEAD_DOUBLEACUTE: Int = 0x89

    /** @since 1.2
     */
    const val VK_DEAD_CARON: Int = 0x8a

    /** @since 1.2
     */
    const val VK_DEAD_CEDILLA: Int = 0x8b

    /** @since 1.2
     */
    const val VK_DEAD_OGONEK: Int = 0x8c

    /** @since 1.2
     */
    const val VK_DEAD_IOTA: Int = 0x8d

    /** @since 1.2
     */
    const val VK_DEAD_VOICED_SOUND: Int = 0x8e

    /** @since 1.2
     */
    const val VK_DEAD_SEMIVOICED_SOUND: Int = 0x8f

    /** @since 1.2
     */
    const val VK_AMPERSAND: Int = 0x96

    /** @since 1.2
     */
    const val VK_ASTERISK: Int = 0x97

    /** @since 1.2
     */
    const val VK_QUOTEDBL: Int = 0x98

    /** @since 1.2
     */
    const val VK_LESS: Int = 0x99

    /** @since 1.2
     */
    const val VK_GREATER: Int = 0xa0

    /** @since 1.2
     */
    const val VK_BRACELEFT: Int = 0xa1

    /** @since 1.2
     */
    const val VK_BRACERIGHT: Int = 0xa2

    /**
     * Constant for the "@" key.
     * @since 1.2
     */
    const val VK_AT: Int = 0x0200

    /**
     * Constant for the ":" key.
     * @since 1.2
     */
    const val VK_COLON: Int = 0x0201

    /**
     * Constant for the "^" key.
     * @since 1.2
     */
    const val VK_CIRCUMFLEX: Int = 0x0202

    /**
     * Constant for the "$" key.
     * @since 1.2
     */
    const val VK_DOLLAR: Int = 0x0203

    /**
     * Constant for the Euro currency sign key.
     * @since 1.2
     */
    const val VK_EURO_SIGN: Int = 0x0204

    /**
     * Constant for the "!" key.
     * @since 1.2
     */
    const val VK_EXCLAMATION_MARK: Int = 0x0205

    /**
     * Constant for the inverted exclamation mark key.
     * @since 1.2
     */
    const val VK_INVERTED_EXCLAMATION_MARK: Int = 0x0206

    /**
     * Constant for the "(" key.
     * @since 1.2
     */
    const val VK_LEFT_PARENTHESIS: Int = 0x0207

    /**
     * Constant for the "#" key.
     * @since 1.2
     */
    const val VK_NUMBER_SIGN: Int = 0x0208

    /**
     * Constant for the "+" key.
     * @since 1.2
     */
    const val VK_PLUS: Int = 0x0209

    /**
     * Constant for the ")" key.
     * @since 1.2
     */
    const val VK_RIGHT_PARENTHESIS: Int = 0x020A

    /**
     * Constant for the "_" key.
     * @since 1.2
     */
    const val VK_UNDERSCORE: Int = 0x020B

    /**
     * Constant for the Microsoft Windows "Windows" key.
     * It is used for both the left and right version of the key.
     * see getKeyLocation
     * @since 1.5
     */
    const val VK_WINDOWS: Int = 0x020C

    /**
     * Constant for the Microsoft Windows Context Menu key.
     * @since 1.5
     */
    const val VK_CONTEXT_MENU: Int = 0x020D

    /* for input method support on Asian Keyboards */ /* not clear what this means - listed in Microsoft Windows API */
    const val VK_FINAL: Int = 0x0018

    /** Constant for the Convert function key.  */ /* Japanese PC 106 keyboard, Japanese Solaris keyboard: henkan */
    const val VK_CONVERT: Int = 0x001C

    /** Constant for the Don't Convert function key.  */ /* Japanese PC 106 keyboard: muhenkan */
    const val VK_NONCONVERT: Int = 0x001D

    /** Constant for the Accept or Commit function key.  */ /* Japanese Solaris keyboard: kakutei */
    const val VK_ACCEPT: Int = 0x001E

    /* not clear what this means - listed in Microsoft Windows API */
    const val VK_MODECHANGE: Int = 0x001F

    /* replaced by VK_KANA_LOCK for Microsoft Windows and Solaris;
     might still be used on other platforms */
    const val VK_KANA: Int = 0x0015

    /* replaced by VK_INPUT_METHOD_ON_OFF for Microsoft Windows and Solaris;
     might still be used for other platforms */
    const val VK_KANJI: Int = 0x0019

    /**
     * Constant for the Alphanumeric function key.
     * @since 1.2
     */
    /* Japanese PC 106 keyboard: eisuu */
    const val VK_ALPHANUMERIC: Int = 0x00F0

    /**
     * Constant for the Katakana function key.
     * @since 1.2
     */
    /* Japanese PC 106 keyboard: katakana */
    const val VK_KATAKANA: Int = 0x00F1

    /**
     * Constant for the Hiragana function key.
     * @since 1.2
     */
    /* Japanese PC 106 keyboard: hiragana */
    const val VK_HIRAGANA: Int = 0x00F2

    /**
     * Constant for the Full-Width Characters function key.
     * @since 1.2
     */
    /* Japanese PC 106 keyboard: zenkaku */
    const val VK_FULL_WIDTH: Int = 0x00F3

    /**
     * Constant for the Half-Width Characters function key.
     * @since 1.2
     */
    /* Japanese PC 106 keyboard: hankaku */
    const val VK_HALF_WIDTH: Int = 0x00F4

    /**
     * Constant for the Roman Characters function key.
     * @since 1.2
     */
    /* Japanese PC 106 keyboard: roumaji */
    const val VK_ROMAN_CHARACTERS: Int = 0x00F5

    /**
     * Constant for the All Candidates function key.
     * @since 1.2
     */
    /* Japanese PC 106 keyboard - VK_CONVERT + ALT: zenkouho */
    const val VK_ALL_CANDIDATES: Int = 0x0100

    /**
     * Constant for the Previous Candidate function key.
     * @since 1.2
     */
    /* Japanese PC 106 keyboard - VK_CONVERT + SHIFT: maekouho */
    const val VK_PREVIOUS_CANDIDATE: Int = 0x0101

    /**
     * Constant for the Code Input function key.
     * @since 1.2
     */
    /* Japanese PC 106 keyboard - VK_ALPHANUMERIC + ALT: kanji bangou */
    const val VK_CODE_INPUT: Int = 0x0102

    /**
     * Constant for the Japanese-Katakana function key.
     * This key switches to a Japanese input method and selects its Katakana input mode.
     * @since 1.2
     */
    /* Japanese Macintosh keyboard - VK_JAPANESE_HIRAGANA + SHIFT */
    const val VK_JAPANESE_KATAKANA: Int = 0x0103

    /**
     * Constant for the Japanese-Hiragana function key.
     * This key switches to a Japanese input method and selects its Hiragana input mode.
     * @since 1.2
     */
    /* Japanese Macintosh keyboard */
    const val VK_JAPANESE_HIRAGANA: Int = 0x0104

    /**
     * Constant for the Japanese-Roman function key.
     * This key switches to a Japanese input method and selects its Roman-Direct input mode.
     * @since 1.2
     */
    /* Japanese Macintosh keyboard */
    const val VK_JAPANESE_ROMAN: Int = 0x0105

    /**
     * Constant for the locking Kana function key.
     * This key locks the keyboard into a Kana layout.
     * @since 1.3
     */
    /* Japanese PC 106 keyboard with special Windows driver - eisuu + Control; Japanese Solaris keyboard: kana */
    const val VK_KANA_LOCK: Int = 0x0106

    /**
     * Constant for the input method on/off key.
     * @since 1.3
     */
    /* Japanese PC 106 keyboard: kanji. Japanese Solaris keyboard: nihongo */
    const val VK_INPUT_METHOD_ON_OFF: Int = 0x0107

    /* for Sun keyboards */
    /** @since 1.2
     */
    const val VK_CUT: Int = 0xFFD1

    /** @since 1.2
     */
    const val VK_COPY: Int = 0xFFCD

    /** @since 1.2
     */
    const val VK_PASTE: Int = 0xFFCF

    /** @since 1.2
     */
    const val VK_UNDO: Int = 0xFFCB

    /** @since 1.2
     */
    const val VK_AGAIN: Int = 0xFFC9

    /** @since 1.2
     */
    const val VK_FIND: Int = 0xFFD0

    /** @since 1.2
     */
    const val VK_PROPS: Int = 0xFFCA

    /** @since 1.2
     */
    const val VK_STOP: Int = 0xFFC8

    /**
     * Constant for the Compose function key.
     * @since 1.2
     */
    const val VK_COMPOSE: Int = 0xFF20

    /**
     * Constant for the AltGraph function key.
     * @since 1.2
     */
    const val VK_ALT_GRAPH: Int = 0xFF7E

    /**
     * Constant for the Begin key.
     * @since 1.5
     */
    const val VK_BEGIN: Int = 0xFF58

    /**
     * This value is used to indicate that the keyCode is unknown.
     * KEY_TYPED events do not have a keyCode value; this value
     * is used instead.
     */
    const val VK_UNDEFINED: Int = 0x0

    /**
     * KEY_PRESSED and KEY_RELEASED events which do not map to a
     * valid Unicode character use this for the keyChar value.
     */
    val CHAR_UNDEFINED: Char = 0xFFFF.toChar()

    /**
     * A constant indicating that the keyLocation is indeterminate
     * or not relevant.
     * `KEY_TYPED` events do not have a keyLocation; this value
     * is used instead.
     * @since 1.4
     */
    const val KEY_LOCATION_UNKNOWN: Int = 0

    /**
     * A constant indicating that the key pressed or released
     * is not distinguished as the left or right version of a key,
     * and did not originate on the numeric keypad (or did not
     * originate with a virtual key corresponding to the numeric
     * keypad).
     * @since 1.4
     */
    const val KEY_LOCATION_STANDARD: Int = 1

    /**
     * A constant indicating that the key pressed or released is in
     * the left key location (there is more than one possible location
     * for this key).  Example: the left shift key.
     * @since 1.4
     */
    const val KEY_LOCATION_LEFT: Int = 2

    /**
     * A constant indicating that the key pressed or released is in
     * the right key location (there is more than one possible location
     * for this key).  Example: the right shift key.
     * @since 1.4
     */
    const val KEY_LOCATION_RIGHT: Int = 3

    /**
     * A constant indicating that the key event originated on the
     * numeric keypad or with a virtual key corresponding to the
     * numeric keypad.
     * @since 1.4
     */
    const val KEY_LOCATION_NUMPAD: Int = 4


    // MOUSE
    /**
     * The first number in the range of ids used for mouse events.
     */
    const val MOUSE_FIRST: Int = 500

    /**
     * The last number in the range of ids used for mouse events.
     */
    const val MOUSE_LAST: Int = 507

    /**
     * The "mouse clicked" event. This `MouseEvent`
     * occurs when a mouse button is pressed and released.
     */
    val MOUSE_CLICKED: Int = MOUSE_FIRST

    /**
     * The "mouse pressed" event. This `MouseEvent`
     * occurs when a mouse button is pushed down.
     */
    val MOUSE_PRESSED: Int = 1 + MOUSE_FIRST //Event.MOUSE_DOWN

    /**
     * The "mouse released" event. This `MouseEvent`
     * occurs when a mouse button is let up.
     */
    val MOUSE_RELEASED: Int = 2 + MOUSE_FIRST //Event.MOUSE_UP

    /**
     * The "mouse moved" event. This `MouseEvent`
     * occurs when the mouse position changes.
     */
    val MOUSE_MOVED: Int = 3 + MOUSE_FIRST //Event.MOUSE_MOVE

    /**
     * The "mouse entered" event. This `MouseEvent`
     * occurs when the mouse cursor enters the unobscured part of component's
     * geometry.
     */
    val MOUSE_ENTERED: Int = 4 + MOUSE_FIRST //Event.MOUSE_ENTER

    /**
     * The "mouse exited" event. This `MouseEvent`
     * occurs when the mouse cursor exits the unobscured part of component's
     * geometry.
     */
    val MOUSE_EXITED: Int = 5 + MOUSE_FIRST //Event.MOUSE_EXIT

    /**
     * The "mouse dragged" event. This `MouseEvent`
     * occurs when the mouse position changes while a mouse button is pressed.
     */
    val MOUSE_DRAGGED: Int = 6 + MOUSE_FIRST //Event.MOUSE_DRAG

    /**
     * The "mouse wheel" event.  This is the only `MouseWheelEvent`.
     * It occurs when a mouse equipped with a wheel has its wheel rotated.
     * @since 1.4
     */
    val MOUSE_WHEEL: Int = 7 + MOUSE_FIRST

    /**
     * Indicates no mouse buttons; used by getButton.
     * @since 1.4
     */
    const val NOBUTTON: Int = 0

    /**
     * Indicates mouse button #1; used by getButton.
     * @since 1.4
     */
    const val BUTTON1: Int = 1

    /**
     * Indicates mouse button #2; used by getButton.
     * @since 1.4
     */
    const val BUTTON2: Int = 2

    /**
     * Indicates mouse button #3; used by getButton.
     * @since 1.4
     */
    const val BUTTON3: Int = 3
}
