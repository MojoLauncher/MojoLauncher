package net.kdt.pojavlaunch.colorselector

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import top.defaults.checkerboarddrawable.CheckerboardDrawable

class ColorSideBySideView(context: Context?, attrs: AttributeSet?) : View(context, attrs) {
    private val mPaint: Paint
    private val mCheckerboardDrawable: CheckerboardDrawable = CheckerboardDrawable.create()
    private var mColor = 0
    private var mAlphaColor = 0
    private var mWidth = 0f
    private var mHeight = 0f
    private var mHalfHeight = 0f

    init {
        mPaint = Paint()
    }

    fun setColor(color: Int) {
        mColor = ColorSelector.Companion.setAlpha(color, 0xff)
        mAlphaColor = color
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        mCheckerboardDrawable.draw(canvas)
        mPaint.setColor(mColor)
        canvas.drawRect(0f, 0f, mWidth, mHalfHeight, mPaint)
        mPaint.setColor(mAlphaColor)
        canvas.drawRect(0f, mHalfHeight, mWidth, mHeight, mPaint)
    }

    override fun onSizeChanged(w: Int, h: Int, old_w: Int, old_h: Int) {
        mHalfHeight = h / 2f
        mWidth = w.toFloat()
        mHeight = h.toFloat()
    }
}
