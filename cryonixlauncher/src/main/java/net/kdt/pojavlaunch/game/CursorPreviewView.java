package net.kdt.pojavlaunch.game;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import androidx.annotation.Nullable;
import androidx.preference.PreferenceManager;

import net.kdt.pojavlaunch.utils.CursorStyleUtils;

public class CursorPreviewView extends View implements SharedPreferences.OnSharedPreferenceChangeListener {
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private SharedPreferences prefs;
    private float targetX = 120f;
    private float targetY = 48f;
    private float renderedX = Float.NaN;
    private float renderedY = Float.NaN;
    private final float[] trailX = new float[7];
    private final float[] trailY = new float[7];
    private Bitmap customBitmap;
    private String cachedPath = "";

    public CursorPreviewView(Context context) { this(context, null); }

    public CursorPreviewView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        setFocusable(true);
    }

    @Override protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        prefs = PreferenceManager.getDefaultSharedPreferences(getContext());
        prefs.registerOnSharedPreferenceChangeListener(this);
        postInvalidateOnAnimation();
    }

    @Override protected void onDetachedFromWindow() {
        if (prefs != null) prefs.unregisterOnSharedPreferenceChangeListener(this);
        super.onDetachedFromWindow();
    }

    @Override public boolean onTouchEvent(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_DOWN || event.getAction() == MotionEvent.ACTION_MOVE) {
            targetX = event.getX();
            targetY = event.getY();
            postInvalidateOnAnimation();
            return true;
        }
        return true;
    }

    @Override protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (prefs == null) prefs = PreferenceManager.getDefaultSharedPreferences(getContext());

        float scale = Math.max(0.5f, Math.min(2f, prefs.getFloat(CursorStyleUtils.SCALE_KEY, 1f))) * 1.75f;
        boolean motion = prefs.getBoolean(CursorStyleUtils.MOTION_KEY, true);
        boolean outline = prefs.getBoolean(CursorStyleUtils.OUTLINE_KEY, false);
        boolean trail = prefs.getBoolean(CursorStyleUtils.TRAIL_KEY, false);
        boolean shadow = prefs.getBoolean(CursorStyleUtils.SHADOW_KEY, true);
        int opacity = Math.max(20, Math.min(100, prefs.getInt(CursorStyleUtils.OPACITY_KEY, 100)));
        float rotation = prefs.getInt(CursorStyleUtils.ROTATION_KEY, 0);
        String template = prefs.getString(CursorStyleUtils.TEMPLATE_KEY, "Classic");
        int color = prefs.getInt(CursorStyleUtils.COLOR_KEY, 0xFFFFFFFF);
        String path = prefs.getString(CursorStyleUtils.CUSTOM_PATH_KEY, "");

        if (Float.isNaN(renderedX)) {
            renderedX = targetX;
            renderedY = targetY;
            fillTrail(renderedX, renderedY);
        } else if (!motion) {
            renderedX = targetX;
            renderedY = targetY;
            fillTrail(renderedX, renderedY);
        } else {
            renderedX += (targetX - renderedX) * 0.24f;
            renderedY += (targetY - renderedY) * 0.24f;
        }

        if (!path.equals(cachedPath)) {
            cachedPath = path;
            customBitmap = path.isEmpty() ? null : BitmapFactory.decodeFile(path);
        }

        if (trail) pushTrail(renderedX, renderedY);

        paint.clearShadowLayer();
        paint.setAlpha(255);
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(0xFF071225);
        canvas.drawRoundRect(0, 0, getWidth(), getHeight(), 16f, 16f, paint);

        if (trail) {
            for (int i = 6; i >= 0; i--) {
                float alpha = (7 - i) / 7f * (opacity / 100f) * 0.22f;
                drawCursor(canvas, trailX[i], trailY[i], scale, rotation, template, color,
                        (int) (255 * alpha), outline, false, shadow, customBitmap);
            }
        }

        drawCursor(canvas, renderedX, renderedY, scale, rotation, template, color,
                (int) (255 * opacity / 100f), outline, true, shadow, customBitmap);

        if (motion && (Math.abs(targetX - renderedX) > 0.5f || Math.abs(targetY - renderedY) > 0.5f)) {
            postInvalidateOnAnimation();
        }
    }

    private void drawCursor(Canvas canvas, float x, float y, float scale, float rotation,
                            String template, int color, int alpha, boolean outline,
                            boolean main, boolean shadow, Bitmap bitmap) {
        canvas.save();
        canvas.translate(x, y);
        canvas.rotate(rotation, 9f, 9f);
        canvas.scale(scale, scale);
        paint.setAlpha(Math.max(0, Math.min(255, alpha)));

        if (shadow && main) {
            paint.setShadowLayer(5f, 2f, 3f, 0x99000000);
        } else {
            paint.clearShadowLayer();
        }

        if (bitmap != null && !bitmap.isRecycled() && main) {
            float maxSize = 78f;
            float fit = Math.min(maxSize / Math.max(1f, bitmap.getWidth()),
                    maxSize / Math.max(1f, bitmap.getHeight()));
            canvas.drawBitmap(bitmap, null,
                    new RectF(0, 0, bitmap.getWidth() * fit, bitmap.getHeight() * fit), paint);
        } else {
            CursorStyleUtils.drawTemplate(canvas, paint, template, color, outline);
        }
        paint.clearShadowLayer();
        canvas.restore();
    }

    private void fillTrail(float x, float y) {
        for (int i = 0; i < trailX.length; i++) {
            trailX[i] = x;
            trailY[i] = y;
        }
    }

    private void pushTrail(float x, float y) {
        for (int i = trailX.length - 1; i > 0; i--) {
            trailX[i] = trailX[i - 1];
            trailY[i] = trailY[i - 1];
        }
        trailX[0] = x;
        trailY[0] = y;
    }

    @Override public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        postInvalidateOnAnimation();
    }
}
