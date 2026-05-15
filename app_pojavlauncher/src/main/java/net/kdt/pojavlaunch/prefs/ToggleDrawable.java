package net.kdt.pojavlaunch.prefs;

import android.animation.ValueAnimator;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * Fully custom toggle drawable.
 * - Sliding knob between left (OFF) and right (ON)
 * - Tick mark (✓) on knob when ON, cross (✕) when OFF
 * - Smooth color and position animation
 */
public class ToggleDrawable extends Drawable {

    private static final int TRACK_ON  = Color.parseColor("#FFFFFF");
    private static final int TRACK_OFF = Color.parseColor("#616161");
    private static final int KNOB      = Color.BLACK;

    private static final int ICON_ON   = Color.WHITE;
    private static final int ICON_OFF  = Color.parseColor("#9E9E9E");

    private final Paint trackPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint knobPaint  = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint iconPaint  = new Paint(Paint.ANTI_ALIAS_FLAG);

    private final RectF trackRect = new RectF();
    private final RectF knobRect  = new RectF();

    private float progress = 0f;
    private boolean checked = false;

    private ValueAnimator animator;

    public ToggleDrawable(boolean initial) {
        checked = initial;
        progress = initial ? 1f : 0f;

        trackPaint.setStyle(Paint.Style.FILL);
        knobPaint.setStyle(Paint.Style.FILL);
        knobPaint.setColor(KNOB);

        iconPaint.setStyle(Paint.Style.STROKE);
        iconPaint.setStrokeCap(Paint.Cap.ROUND);
        iconPaint.setStrokeJoin(Paint.Join.ROUND);
    }

    public void setChecked(boolean value, boolean animate) {
        if (checked == value && animator == null) return;
        checked = value;

        if (animator != null) animator.cancel();

        float start = progress;
        float end = value ? 1f : 0f;

        if (!animate) {
            progress = end;
            invalidateSelf();
            return;
        }

        animator = ValueAnimator.ofFloat(start, end);
        animator.setDuration(240);
        animator.setInterpolator(new android.view.animation.OvershootInterpolator(1.15f));

        animator.addUpdateListener(a -> {
            progress = (float) a.getAnimatedValue();
            invalidateSelf();
        });

        animator.start();
    }

    @Override
    public void draw(@NonNull Canvas canvas) {
        int w = getBounds().width();
        int h = getBounds().height();

        // Track (pill)
        float trackHeight = h * 0.55f;
        float top = (h - trackHeight) / 2f;
        float left = w * 0.04f;
        float right = w * 0.96f;

        trackRect.set(left, top, right, top + trackHeight);

        trackPaint.setColor(interpolate(TRACK_OFF, TRACK_ON, progress));
        canvas.drawRoundRect(trackRect, trackHeight / 2f, trackHeight / 2f, trackPaint);

        // Knob with consistent gap to track (top/bottom + left/right)
        float knobRadius = trackHeight * 0.38f;
        float gap = Math.max(2f, (trackHeight / 2f) - knobRadius);

        float startX = left + knobRadius + gap;
        float endX = right - knobRadius - gap;

        float cx = startX + (endX - startX) * progress;
        float cy = h / 2f;

        knobRect.set(cx - knobRadius, cy - knobRadius, cx + knobRadius, cy + knobRadius);
        canvas.drawOval(knobRect, knobPaint);

        // Icon bounce + fade
        iconPaint.setStrokeWidth(knobRadius * 0.18f);

        float fade = (progress > 0.5f ? progress : 1f - progress);
        float alpha = 255f * fade;
        iconPaint.setAlpha((int) alpha);
        iconPaint.setColor(progress > 0.5f ? ICON_ON : ICON_OFF);

        float s = knobRadius * 0.45f;
        float scale = 0.85f + (0.25f * fade);

        canvas.save();
        canvas.scale(scale, scale, cx, cy);

        if (progress > 0.5f) {
            drawTick(canvas, cx, cy, s);
        } else {
            drawCross(canvas, cx, cy, s);
        }

        canvas.restore();
        iconPaint.setAlpha(255);
    }

    private void drawTick(Canvas canvas, float cx, float cy, float size) {
        float s = size;

        float x1 = cx - s * 0.6f;
        float y1 = cy + s * 0.1f;

        float x2 = cx - s * 0.1f;
        float y2 = cy + s * 0.6f;

        float x3 = cx + s * 0.7f;
        float y3 = cy - s * 0.6f;

        iconPaint.setStrokeWidth(size * 0.22f);

        canvas.drawLine(x1, y1, x2, y2, iconPaint);
        canvas.drawLine(x2, y2, x3, y3, iconPaint);
    }

    private void drawCross(Canvas canvas, float cx, float cy, float size) {
        float s = size * 0.85f;

        iconPaint.setStrokeWidth(size * 0.22f);

        canvas.drawLine(cx - s, cy - s, cx + s, cy + s, iconPaint);
        canvas.drawLine(cx + s, cy - s, cx - s, cy + s, iconPaint);
    }

    private int interpolate(int a, int b, float t) {
        return Color.rgb(
                (int)(Color.red(a) + (Color.red(b) - Color.red(a)) * t),
                (int)(Color.green(a) + (Color.green(b) - Color.green(a)) * t),
                (int)(Color.blue(a) + (Color.blue(b) - Color.blue(a)) * t)
        );
    }

    @Override public void setAlpha(int alpha) {}
    @Override public void setColorFilter(@Nullable ColorFilter colorFilter) {}
    @Override public int getOpacity() { return PixelFormat.TRANSLUCENT; }
}
