package net.kdt.pojavlaunch

import android.graphics.Canvas
import android.graphics.ColorFilter
import android.graphics.Insets
import android.graphics.Paint
import android.graphics.PixelFormat
import android.graphics.Rect
import android.graphics.drawable.Drawable
import android.util.Log
import androidx.annotation.RequiresApi

@RequiresApi(29)
class InsetBackground(insets: Insets, bgColor: Int) : Drawable() {
    private val mLeftRect = Rect()
    private val mTopRect = Rect()
    private val mRightRect = Rect()
    private val mBottomRect = Rect()
    private val mRectPaint = Paint()
    private val mInsets: Insets

    init {
        Log.i("InsetBackground", insets.toString())
        mInsets = insets
        mRectPaint.setColor(bgColor)
    }

    private fun computeRects(width: Int, height: Int) {
        mLeftRect.left = 0
        mLeftRect.right = mInsets.left
        mLeftRect.top = 0
        mLeftRect.bottom = height

        mTopRect.left = mInsets.left
        mTopRect.right = width - mInsets.right
        mTopRect.top = 0
        mTopRect.bottom = mInsets.top

        mRightRect.left = width - mInsets.right
        mRightRect.right = width
        mRightRect.top = 0
        mRightRect.bottom = height

        mBottomRect.left = 0
        mBottomRect.right = width
        mBottomRect.top = height - mInsets.bottom
        mBottomRect.bottom = height
    }

    override fun onBoundsChange(bounds: Rect) {
        computeRects(bounds.width(), bounds.height())
        invalidateSelf()
    }

    override fun draw(canvas: Canvas) {
        canvas.drawRect(mLeftRect, mRectPaint)
        canvas.drawRect(mRightRect, mRectPaint)
        canvas.drawRect(mTopRect, mRectPaint)
        canvas.drawRect(mBottomRect, mRectPaint)
    }

    override fun setAlpha(alpha: Int) {
    }

    override fun setColorFilter(colorFilter: ColorFilter?) {
    }

    override fun getOpacity(): Int {
        return PixelFormat.TRANSPARENT
    }
}
