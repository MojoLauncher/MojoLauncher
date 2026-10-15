package git.artdeell.mojo.utils;

import android.view.KeyCharacterMap;
import android.view.KeyEvent;

import git.artdeell.mojo.LwjglGlfwKeycode;

public class KeyboardUtils {
    private static final KeyCharacterMap KEY_CHARACTER_MAP = KeyCharacterMap.load(KeyCharacterMap.VIRTUAL_KEYBOARD);

    public static char getCodepoint(int androidKeyCode, int callbackBridgeMods) {
        int convertedMods = 0;
        if((callbackBridgeMods & LwjglGlfwKeycode.GLFW_MOD_ALT) != 0) {
            convertedMods |= KeyEvent.META_ALT_ON;
        } if ((callbackBridgeMods & LwjglGlfwKeycode.GLFW_MOD_CAPS_LOCK) != 0) {
            convertedMods |= KeyEvent.META_CAPS_LOCK_ON;
        } if ((callbackBridgeMods & LwjglGlfwKeycode.GLFW_MOD_CONTROL) != 0) {
            convertedMods |= KeyEvent.META_CTRL_ON;
        } if ((callbackBridgeMods & LwjglGlfwKeycode.GLFW_MOD_NUM_LOCK) != 0) {
            convertedMods |= KeyEvent.META_NUM_LOCK_ON;
        } if ((callbackBridgeMods & LwjglGlfwKeycode.GLFW_MOD_SHIFT) != 0) {
            convertedMods |= KeyEvent.META_SHIFT_ON;
        }

        // casting key from KeyCharacterMap to char can possibly be unsafe?
        return (char) KEY_CHARACTER_MAP.get(androidKeyCode, convertedMods);
    }
}
