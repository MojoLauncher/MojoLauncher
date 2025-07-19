package net.kdt.pojavlaunch.customcontrols.mouse;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.drawable.Drawable;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.util.Consumer;

import java.util.HashSet;
import java.util.Set;

public class CursorDrawable extends Drawable {
    private Bitmap bitmap;
    private int fallbackWidth;
    private int fallbackHeight;
    private int xHotspot;
    private int yHotspot;
    private Set<Consumer<CursorDrawable>> listeners = new HashSet<>();
    private final int xHotspotFallback;
    private final int yHotspotFallback;
    private final Drawable fallback;
    private final Paint paint = new Paint();

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

    public void setXHotspot(int xHotspot) {
        this.xHotspot = xHotspot;
    }

    public void setYHotspot(int yHotspot) {
        this.yHotspot = yHotspot;
    }

    public void markDirty() {
        for (Consumer<CursorDrawable> listener : this.listeners) {
            listener.accept(this);
        }
    }

    public void onChange(Consumer<CursorDrawable> consumer) {
        this.listeners.add(consumer);
    }

    public void removeChangeListener(Consumer<CursorDrawable> onCursorChange) {
        this.listeners.remove(onCursorChange);
    }

    @Override
    public void draw(@NonNull Canvas canvas) {
        if(this.bitmap == null) {
            fallback.draw(canvas);
        } else {
            canvas.drawBitmap(bitmap, 0, 0, paint);
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
            return PixelFormat.UNKNOWN;
        }
        return fallback.getOpacity();
    }

    @Override
    public void setBounds(int left, int top, int right, int bottom) {
        super.setBounds(left, top, right, bottom);
        fallback.setBounds(left, top, right, bottom);
    }
}
