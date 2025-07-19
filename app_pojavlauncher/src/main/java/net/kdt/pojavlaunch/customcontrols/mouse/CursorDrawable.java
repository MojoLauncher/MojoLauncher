package net.kdt.pojavlaunch.customcontrols.mouse;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class CursorDrawable extends Drawable {
    private Bitmap bitmap;
    private Paint paint;
    private int fallbackWidth;
    private int fallbackHeight;
    private int xHotspot;
    private int yHotspot;
    private final int xHotspotFallback;
    private final int yHotspotFallback;
    private final Drawable fallback;

    public CursorDrawable(Bitmap bitmap, Drawable fallback, int fallbackWidth, int fallbackHeight, int xHotspot, int yHotspot,
                          int xHotspotFallback, int yHotspotFallback) {
        this.bitmap = bitmap;
        this.fallback = fallback;
        this.fallbackWidth = fallbackWidth;
        this.fallbackHeight = fallbackHeight;
        this.xHotspot = xHotspot;
        this.yHotspot = yHotspot;
        this.xHotspotFallback = xHotspotFallback;
        this.yHotspotFallback = yHotspotFallback;
    }

    public Bitmap getBitmap() {
        return bitmap;
    }

    public void setBitmap(Bitmap bitmap) {
        this.bitmap = bitmap;
    }

    public Drawable getFallback() {
        return fallback;
    }

    public int getWidth() {
        if(bitmap == null) return fallbackWidth;
        return bitmap.getWidth();
    }

    public int getHeight() {
        if(bitmap == null) return fallbackHeight;
        return bitmap.getHeight();
    }

    public int getXHotspot() {
        if(bitmap == null) return xHotspotFallback;
        return xHotspot;
    }

    public int getYHotspot() {
        if(bitmap == null) return yHotspotFallback;
        return yHotspot;
    }

    @Override
    public void draw(@NonNull Canvas canvas) {
        if(this.bitmap == null) {
            fallback.draw(canvas);
        } else {
            canvas.drawBitmap(bitmap, null, new Rect(), paint);
        }
    }

    @Override
    public void setAlpha(int alpha) {
        paint.setAlpha(alpha);
        fallback.setAlpha(alpha);
    }

    @Override
    public void setColorFilter(@Nullable ColorFilter colorFilter) {
        paint.setColorFilter(colorFilter);
        fallback.setColorFilter(colorFilter);
    }

    @Override
    public int getOpacity() {
        if(this.bitmap != null) {
            return PixelFormat.TRANSPARENT;
        }
        return fallback.getOpacity();
    }
}
