package net.kdt.pojavlaunch.platform.clipboard;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;

import git.artdeell.dnbootstrap.glfw.ClipboardProvider;
import git.mojo.sdl.SDLClipboard;

public class AndroidClipboard implements ClipboardProvider, SDLClipboard {
    private final ClipboardManager mClipboardManager;
    public AndroidClipboard(Context context){
        mClipboardManager = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
    }
    @Override
    public void setClipboardString(String content) {
        mClipboardManager.setPrimaryClip(ClipData.newPlainText("MJ Paste", content));
    }

    @Override
    public String getClipboardString() {
        if(!mClipboardManager.hasPrimaryClip()) return null;
        ClipData clipData = mClipboardManager.getPrimaryClip();
        if(clipData == null) return null;
        if(clipData.getItemCount() < 1) return null;
        CharSequence text = clipData.getItemAt(0).getText();
        if(text == null) return null;
        return text.toString();
    }
}
