package net.kdt.pojavlaunch.game;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
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
    private Bitmap customBitmap;
    private String cachedPath = "";

    public CursorPreviewView(Context context) {
        this(context, null);
    }

    public CursorPreviewView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        setFocusable(true);
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        prefs = PreferenceManager.getDefaultSharedPreferences(getContext());
        prefs.registerOnSharedPreferenceChangeListener(this);
        postInvalidateOnAnimation();
    }

    @Override
    protected void onDetachedFromWindow() {
        if (prefs != null) prefs.unregisterOnSharedPreferenceChangeListener(this);
        super.onDetachedFromWindow();
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_DOWN
                || event.getAction() == MotionEvent.ACTION_MOVE) {
            targetX = event.getX();
            targetY = event.getY();
            postInvalidateOnAnimation();
            return true;
        }
        return true;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (prefs == null) prefs = PreferenceManager.getDefaultSharedPreferences(getContext());

        float scale = Math.max(0.5f, Math.min(2f, prefs.getFloat(CursorStyleUtils.SCALE_KEY, 1f)));
        boolean motion = prefs.getBoolean(CursorStyleUtils.MOTION_KEY, true);
        String template = prefs.getString(CursorStyleUtils.TEMPLATE_KEY, "Classic");
        int color = prefs.getInt(CursorStyleUtils.COLOR_KEY, 0xFFFFFFFF);
        String path = prefs.getString(CursorStyleUtils.CUSTOM_PATH_KEY, "");

        if (Float.isNaN(renderedX)) {
            renderedX = targetX;
            renderedY = targetY;
        } else if (!motion) {
            renderedX = targetX;
            renderedY = targetY;
        } else {
            renderedX += (targetX - renderedX) * 0.24f;
            renderedY += (targetY - renderedY) * 0.24f;
        }

        if (!path.equals(cachedPath)) {
            cachedPath = path;
            customBitmap = path.isEmpty() ? null : BitmapFactory.decodeFile(path);
        }

        paint.setStyle(Paint.Style.FILL);
        paint.setColor(0xFF0A1424);
        canvas.drawRoundRect(0, 0, getWidth(), getHeight(), 14f, 14f, paint);

        canvas.save();
        canvas.translate(renderedX, renderedY);
        canvas.scale(scale, scale);

        if (customBitmap != null && !customBitmap.isRecycled()) {
            canvas.drawBitmap(customBitmap, 0, 0, paint);
        } else {
            CursorStyleUtils.drawTemplate(canvas, paint, template, color);
        }

        canvas.restore();

        if (motion && (Math.abs(targetX - renderedX) > 0.5f || Math.abs(targetY - renderedY) > 0.5f)) {
            postInvalidateOnAnimation();
        }
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        postInvalidateOnAnimation();
    }
}