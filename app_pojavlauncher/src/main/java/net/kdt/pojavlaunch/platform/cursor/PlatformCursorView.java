package net.kdt.pojavlaunch.platform.cursor;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.NonNull;

import net.kdt.pojavlaunch.platform.Platform;

import git.artdeell.dnbootstrap.glfw.FallbackCursorDrawable;
import git.artdeell.mojo.R;

public class PlatformCursorView extends View implements PlatformCursorImplementor {
    private Drawable cursorDrawable;
    private final Paint customCursorPaint = new Paint();
    private boolean noDraw = false;
    private float mouseScale = 1f;

    public PlatformCursorView(Context context, AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, 0);
    }

    public PlatformCursorView(Context context) {
        this(context, null);
    }

    public PlatformCursorView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public PlatformCursorView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        if(attrs != null) {
            try(TypedArray arr = context.obtainStyledAttributes(attrs, R.styleable.PlatformCursorView)) {
                cursorDrawable = arr.getDrawable(R.styleable.PlatformCursorView_defaultCursorDrawable);
            }
        }
        if(cursorDrawable == null) cursorDrawable = new FallbackCursorDrawable();
        cursorDrawable.setBounds(0, 0, 36, 54);
    }

    @Override
    protected void onDraw(@NonNull Canvas canvas) {
        if(noDraw) return;
        canvas.translate((int)(Platform.cursorX * getWidth()), (int)(Platform.cursorY * getHeight()));
        PlatformCursor cursor = Platform.getCursor();
        canvas.scale(mouseScale, mouseScale);
        if(cursor == null) {
            cursorDrawable.draw(canvas);
        }else {
            canvas.drawBitmap(cursor.bitmap, -cursor.hotX, -cursor.hotY, customCursorPaint);
        }
    }

    @Override
    public void onCursorPosition() {
        if(!noDraw) post(this::invalidate);
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

    public void setCursorScale(float scale){
        this.mouseScale = scale;
    }
}
