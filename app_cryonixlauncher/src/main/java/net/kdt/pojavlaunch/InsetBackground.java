package net.kdt.pojavlaunch;

import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Insets;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;

@RequiresApi(29)
public class InsetBackground extends Drawable {
    private final Rect mLeftRect = new Rect();
    private final Rect mTopRect = new Rect();
    private final Rect mRightRect = new Rect();
    private final Rect mBottomRect = new Rect();
    private final Paint mRectPaint = new Paint();
    private final Insets mInsets;

    public InsetBackground(Insets insets, int bgColor) {
        Log.i("InsetBackground", insets.toString());
        mInsets = insets;
        mRectPaint.setColor(bgColor);
    }

    private void computeRects(int width, int height) {
        mLeftRect.left = 0;
        mLeftRect.right = mInsets.left;
        mLeftRect.top = 0;
        mLeftRect.bottom = height;

        mTopRect.left = mInsets.left;
        mTopRect.right = width - mInsets.right;
        mTopRect.top = 0;
        mTopRect.bottom = mInsets.top;

        mRightRect.left = width - mInsets.right;
        mRightRect.right = width;
        mRightRect.top = 0;
        mRightRect.bottom = height;

        mBottomRect.left = 0;
        mBottomRect.right = width;
        mBottomRect.top = height - mInsets.bottom;
        mBottomRect.bottom = height;
    }

    @Override
    protected void onBoundsChange(@NonNull Rect bounds) {
        computeRects(bounds.width(), bounds.height());
        invalidateSelf();
    }

    @Override
    public void draw(@NonNull Canvas canvas) {
        canvas.drawRect(mLeftRect, mRectPaint);
        canvas.drawRect(mRightRect, mRectPaint);
        canvas.drawRect(mTopRect, mRectPaint);
        canvas.drawRect(mBottomRect, mRectPaint);
    }

    @Override
    public void setAlpha(int alpha) {

    }

    @Override
    public void setColorFilter(@Nullable ColorFilter colorFilter) {

    }

    @Override
    public int getOpacity() {
        return PixelFormat.TRANSPARENT;
    }
}
