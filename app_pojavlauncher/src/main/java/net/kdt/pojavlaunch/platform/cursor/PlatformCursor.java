package net.kdt.pojavlaunch.platform.cursor;

import android.graphics.Bitmap;

public class PlatformCursor {
    public final Bitmap bitmap;
    public final int hotX, hotY;

    public PlatformCursor(Bitmap bitmap, int hotX, int hotY) {
        this.bitmap = bitmap;
        this.hotX = hotX;
        this.hotY = hotY;
    }
}
