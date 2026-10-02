package net.kdt.pojavlaunch.customcontrols.mouse;

import android.graphics.Canvas;
import android.graphics.drawable.Drawable;

import androidx.annotation.NonNull;

/**
 * Contains cursor data and the draw method
 */
public class CursorContainer {
    private final Drawable drawable;
    private final int xHotspot;
    private final int yHotspot;

    public CursorContainer(Drawable drawable, int xHotspot, int yHotspot) {
        this.drawable = drawable;
        this.xHotspot = xHotspot;
        this.yHotspot = yHotspot;
    }

    public void draw(@NonNull Canvas canvas) {
        canvas.translate(-xHotspot, -yHotspot);

        drawable.draw(canvas);
    }

    public Drawable getDrawable() {
        return drawable;
    }

    public int getXHotspot() {
        return xHotspot;
    }

    public int getYHotspot() {
        return yHotspot;
    }
}
