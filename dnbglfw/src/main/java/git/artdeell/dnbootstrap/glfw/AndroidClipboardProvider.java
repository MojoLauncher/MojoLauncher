package git.artdeell.dnbootstrap.glfw;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;

public class AndroidClipboardProvider implements ClipboardProvider {
    private final ClipboardManager clipboardManager;
    public AndroidClipboardProvider(Context context) {
        clipboardManager = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
    }

    @Override
    public String getClipboardString() {
        if(!clipboardManager.hasPrimaryClip()) return null;
        ClipData clipData = clipboardManager.getPrimaryClip();
        if(clipData == null) return null;
        if(clipData.getItemCount() < 1) return null;
        CharSequence text = clipData.getItemAt(0).getText();
        if(text == null) return null;
        return text.toString();
    }

    @Override
    public void setClipboardString(String str) {
        clipboardManager.setPrimaryClip(ClipData.newPlainText("GLFW Paste", str));
    }
}
