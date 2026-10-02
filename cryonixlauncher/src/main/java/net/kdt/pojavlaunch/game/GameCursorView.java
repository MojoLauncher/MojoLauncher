package net.kdt.pojavlaunch.game;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.preference.PreferenceManager;

import net.kdt.pojavlaunch.game.platform.Platform;
import net.kdt.pojavlaunch.game.platform.cursor.PlatformCursor;
import net.kdt.pojavlaunch.game.platform.cursor.PlatformCursorImplementor;
import net.kdt.pojavlaunch.utils.CursorStyleUtils;

import git.artdeell.mojo.R;

/**
 * A view that draws the platform cursor on the screen.
 */
public class GameCursorView extends View implements PlatformCursorImplementor {
    private final Paint customCursorPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Drawable cursorDrawable;
    private boolean noDraw = false;
    private float mouseScale = 1f;
    private float renderedX = Float.NaN;
    private float renderedY = Float.NaN;
    private final float[] trailX = new float[7];
    private final float[] trailY = new float[7];
    private Bitmap customCursorBitmap;
    private String cachedCustomPath = "";

    public GameCursorView(Context context, AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, 0);
    }

    public GameCursorView(Context context) { this(context, null); }
    public GameCursorView(Context context, AttributeSet attrs) { this(context, attrs, 0); }

    public GameCursorView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        cursorDrawable = ContextCompat.getDrawable(context, R.drawable.ic_mouse_pointer);
        assert cursorDrawable != null;
        cursorDrawable.setBounds(0, 0, 36, 54);
    }

    @Override protected void onDraw(@NonNull Canvas canvas) {
        if (noDraw) return;

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getContext());
        mouseScale = Math.max(0.5f, Math.min(2.0f, prefs.getFloat(CursorStyleUtils.SCALE_KEY, 1.0f)));
        boolean motionJelly = prefs.getBoolean(CursorStyleUtils.MOTION_KEY, true);
        boolean outline = prefs.getBoolean(CursorStyleUtils.OUTLINE_KEY, false);
        boolean trail = prefs.getBoolean(CursorStyleUtils.TRAIL_KEY, false);
        boolean shadow = prefs.getBoolean(CursorStyleUtils.SHADOW_KEY, true);
        int opacity = Math.max(20, Math.min(100, prefs.getInt(CursorStyleUtils.OPACITY_KEY, 100)));
        float rotation = prefs.getInt(CursorStyleUtils.ROTATION_KEY, 0);
        String template = prefs.getString(CursorStyleUtils.TEMPLATE_KEY, "Classic");
        int color = prefs.getInt(CursorStyleUtils.COLOR_KEY, CursorStyleUtils.COLORS[0]);
        String customPath = prefs.getString(CursorStyleUtils.CUSTOM_PATH_KEY, "");

        float targetX = (float) (Platform.cursorX * ((GameView) getParent()).cursorRatioX);
        float targetY = (float) (Platform.cursorY * ((GameView) getParent()).cursorRatioY);

        if (Float.isNaN(renderedX) || !motionJelly) {
            renderedX = targetX;
            renderedY = targetY;
            fillTrail(renderedX, renderedY);
        } else {
            renderedX += (targetX - renderedX) * 0.34f;
            renderedY += (targetY - renderedY) * 0.34f;
        }

        if (!customPath.equals(cachedCustomPath)) {
            cachedCustomPath = customPath;
            customCursorBitmap = customPath.isEmpty() ? null : BitmapFactory.decodeFile(customPath);
        }

        if (trail) pushTrail(renderedX, renderedY);

        if (trail) {
            for (int i = 6; i >= 0; i--) {
                int alpha = (int) (255f * (opacity / 100f) * ((7 - i) / 7f) * 0.22f);
                drawCursor(canvas, trailX[i], trailY[i], mouseScale, rotation, template, color,
                        alpha, outline, false, shadow, customCursorBitmap);
            }
        }

        drawCursor(canvas, renderedX, renderedY, mouseScale, rotation, template, color,
                (int) (255f * opacity / 100f), outline, true, shadow, customCursorBitmap);
    }

    private void drawCursor(Canvas canvas, float x, float y, float scale, float rotation,
                            String template, int color, int alpha, boolean outline,
                            boolean main, boolean shadow, Bitmap bitmap) {
        canvas.save();
        canvas.translate(x, y);
        canvas.rotate(rotation, 9f, 9f);
        canvas.scale(scale, scale);
        customCursorPaint.setAlpha(Math.max(0, Math.min(255, alpha)));

        if (shadow && main) {
            customCursorPaint.setShadowLayer(5f, 2f, 3f, 0xAA000000);
        } else {
            customCursorPaint.clearShadowLayer();
        }

        if (bitmap != null && !bitmap.isRecycled() && main) {
            float maxSize = 52f;
            float fit = Math.min(maxSize / Math.max(1f, bitmap.getWidth()),
                    maxSize / Math.max(1f, bitmap.getHeight()));
            float drawW = bitmap.getWidth() * fit;
            float drawH = bitmap.getHeight() * fit;
            canvas.drawBitmap(bitmap, null, new RectF(0, 0, drawW, drawH), customCursorPaint);
        } else {
            CursorStyleUtils.drawTemplate(canvas, customCursorPaint, template, color, outline);
        }

        customCursorPaint.clearShadowLayer();
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

    @Override public void onCursorPosition() {
        if (!noDraw) postInvalidateOnAnimation();
    }

    @Override public void onCursorChanged() {
        post(this::invalidate);
    }

    @Override public void onGrabState(boolean isGrabbing) {
        noDraw = isGrabbing;
        invalidate();
    }

    public void setCursorScale(float scale) {
        this.mouseScale = Math.max(0.5f, Math.min(2.0f, scale));
        invalidate();
    }
}
