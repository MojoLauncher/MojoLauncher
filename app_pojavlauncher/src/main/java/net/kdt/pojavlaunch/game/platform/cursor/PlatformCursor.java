package net.kdt.pojavlaunch.game.platform.cursor;

import android.graphics.Bitmap;

/**
 * Platform cursor object. Direct copy of GLFWCursor. Holds hotspot offsets and a bitmap of a custom cursor
 */
public class PlatformCursor {
    public final Bitmap bitmap;
    public final int hotX, hotY;

    public PlatformCursor(Bitmap bitmap, int hotX, int hotY) {
        this.bitmap = bitmap;
        this.hotX = hotX;
        this.hotY = hotY;
    }
}
