package net.kdt.pojavlaunch.game.platform.clipboard;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;

import git.artdeell.dnbootstrap.glfw.GLFWClipboard;
import git.mojo.sdl.SDLClipboard;

/**
 * Android clipboard implementation for GLFW/SDL
 */
public class AndroidClipboard implements GLFWClipboard, SDLClipboard {
    private final ClipboardManager mClipboardManager;

    public AndroidClipboard(Context context) {
        mClipboardManager = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
    }

    /**
     * Get clipboard contents
     *
     * @return content String
     */
    @Override
    public String getClipboardString() {
        if (!mClipboardManager.hasPrimaryClip()) return null;
        ClipData clipData = mClipboardManager.getPrimaryClip();
        if (clipData == null) return null;
        if (clipData.getItemCount() < 1) return null;
        CharSequence text = clipData.getItemAt(0).getText();
        if (text == null) return null;
        return text.toString();
    }

    /**
     * Set clipboard contents
     *
     * @param content content String
     */
    @Override
    public void setClipboardString(String content) {
        mClipboardManager.setPrimaryClip(ClipData.newPlainText("MJ Paste", content));
    }
}
