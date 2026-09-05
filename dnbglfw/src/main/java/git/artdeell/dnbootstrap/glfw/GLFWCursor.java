package git.artdeell.dnbootstrap.glfw;

import android.graphics.Bitmap;

public class GLFWCursor {
    public final Bitmap bitmap;
    public final int hotX, hotY;

    public GLFWCursor(Bitmap bitmap, int hotX, int hotY) {
        this.bitmap = bitmap;
        this.hotX = hotX;
        this.hotY = hotY;
    }
}
