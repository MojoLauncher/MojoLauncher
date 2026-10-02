package net.kdt.pojavlaunch.game;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.View;

import android.content.SharedPreferences;

import androidx.preference.PreferenceManager;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;

import net.kdt.pojavlaunch.game.platform.Platform;
import net.kdt.pojavlaunch.game.platform.cursor.PlatformCursor;
import net.kdt.pojavlaunch.game.platform.cursor.PlatformCursorImplementor;

import git.artdeell.mojo.R;

/**
 * A view that draws the platform cursor on the screen
 */
public class GameCursorView extends View implements PlatformCursorImplementor {
    private final Paint customCursorPaint = new Paint();
    private final Drawable cursorDrawable;
    private boolean noDraw = false;
    private float mouseScale = 1f;
    private float renderedX = Float.NaN;
    private float renderedY = Float.NaN;

    public GameCursorView(Context context, AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, 0);
    }

    public GameCursorView(Context context) {
        this(context, null);
    }

    public GameCursorView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public GameCursorView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        cursorDrawable = ContextCompat.getDrawable(context, R.drawable.ic_mouse_pointer);
        assert cursorDrawable != null;
        cursorDrawable.setBounds(0, 0, 36, 54);
    }

    @Override
    protected void onDraw(@NonNull Canvas canvas) {
        if (noDraw) return;
        // Read the persisted Cryonix cursor settings so customization also applies in-game.
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getContext());
        mouseScale = Math.max(0.5f, Math.min(2.0f, prefs.getFloat("cryonix_cursor_scale", 1.0f)));
        boolean motionJelly = prefs.getBoolean("cryonix_cursor_motion_jelly", true);

        float targetX = Platform.cursorX * ((GameView) getParent()).cursorRatioX;
        float targetY = Platform.cursorY * ((GameView) getParent()).cursorRatioY;
        if (Float.isNaN(renderedX) || !motionJelly) {
            renderedX = targetX;
            renderedY = targetY;
        } else {
            // Small critically-damped trailing motion: smooth without making the cursor feel delayed.
            renderedX += (targetX - renderedX) * 0.34f;
            renderedY += (targetY - renderedY) * 0.34f;
            if (Math.abs(targetX - renderedX) > 0.5f || Math.abs(targetY - renderedY) > 0.5f) {
                postInvalidateOnAnimation();
            }
        }
        canvas.translate((int) renderedX, (int) renderedY);
        PlatformCursor cursor = Platform.getCursor();
        canvas.scale(mouseScale, mouseScale);
        if (cursor == null) {
            cursorDrawable.draw(canvas);
        } else {
            canvas.drawBitmap(cursor.bitmap, -cursor.hotX, -cursor.hotY, customCursorPaint);
        }
    }

    @Override
    public void onCursorPosition() {
        if (!noDraw) postInvalidateOnAnimation();
    }

    @Override
    public void onCursorChanged() {
        post(this::invalidate);
    }

    @Override
    public void onGrabState(boolean isGrabbing) {
        noDraw = isGrabbing;
        invalidate();
    }

    public void setCursorScale(float scale) {
        this.mouseScale = Math.max(0.5f, Math.min(2.0f, scale));
        invalidate();
    }
}
