package git.artdeell.dnbootstrap.glfw;

import androidx.annotation.NonNull;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.View;

import git.artdeell.dnbglfw.R;


public class GLFWCursorView extends View implements CursorImplementor {
    private Drawable cursorDrawable;
    private final Paint customCursorPaint = new Paint();
    private boolean noDraw = false;
    private float mouseScale = 1f;

    public GLFWCursorView(Context context, AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, 0);
    }

    public GLFWCursorView(Context context) {
        this(context, null);
    }

    public GLFWCursorView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public GLFWCursorView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        GLFW.setCursorImpl(this);
        if(attrs != null) {
            try(TypedArray arr = context.obtainStyledAttributes(attrs,R.styleable.GLFWCursorView)) {
                cursorDrawable = arr.getDrawable(R.styleable.GLFWCursorView_defaultCursorDrawable);
            }
        }
        if(cursorDrawable == null) cursorDrawable = new FallbackCursorDrawable();
        cursorDrawable.setBounds(0, 0, 36, 54);
    }

    @Override
    protected void onDraw(@NonNull Canvas canvas) {
        if(noDraw) return;
        canvas.translate((int)(GLFW.cursorX * getWidth()), (int)(GLFW.cursorY * getHeight()));
        GLFWCursor cursor = GLFW.getCursor();
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
