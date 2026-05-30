package net.kdt.pojavlaunch.colorselector

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import net.kdt.pojavlaunch.Tools.dpToPx

class SVRectangleView(context: Context?, attrs: AttributeSet?) : View(context, attrs) {
    private val mColorPaint = Paint()
    private val mPointerPaint = Paint()
    private val mPointerSize: Float
    private var mSvRectangle: Bitmap? = null
    private var mViewSize: RectF? = null
    private var mHeightInverted = 0f
    private var mWidthInverted = 0f
    private var mFingerPosX = 0f
    private var mFingerPosY = 0f
    var mRectSelectionListener: RectangleSelectionListener? = null

    init {
        mColorPaint.setColor(Color.BLACK)
        mColorPaint.setStyle(Paint.Style.FILL)
        mPointerSize = dpToPx(6f)
        mPointerPaint.setColor(Color.BLACK)
        mPointerPaint.setStrokeWidth(dpToPx(3f))
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (event.getAction() == MotionEvent.ACTION_DOWN || event.getAction() == MotionEvent.ACTION_MOVE) {
            getParent().requestDisallowInterceptTouchEvent(true)
        }

        val x = event.getX()
        val y = event.getY()
        mFingerPosX = x * mWidthInverted
        mFingerPosY = y * mHeightInverted

        if (mFingerPosX < 0) mFingerPosX = 0f
        else if (mFingerPosX > 1) mFingerPosX = 1f

        if (mFingerPosY < 0) mFingerPosY = 0f
        else if (mFingerPosY > 1) mFingerPosY = 1f

        if (mRectSelectionListener != null) mRectSelectionListener!!.onLuminosityIntensityChanged(
            mFingerPosY,
            mFingerPosX
        )
        invalidate()
        return true
    }

    fun setLuminosityIntensity(luminosity: Float, intensity: Float) {
        mFingerPosX = intensity
        mFingerPosY = luminosity
        invalidate()
    }

    fun setColor(color: Int, invalidate: Boolean) {
        mColorPaint.setColor(color)
        if (invalidate) invalidate()
    }

    fun setRectSelectionListener(listener: RectangleSelectionListener?) {
        mRectSelectionListener = listener
    }

    protected fun drawPointer(canvas: Canvas, x: Float, y: Float) {
        canvas.drawLine(mPointerSize * 2 + x, y, mPointerSize + x, y, mPointerPaint)
        canvas.drawLine(x - mPointerSize * 2, y, x - mPointerSize, y, mPointerPaint)
        canvas.drawLine(x, mPointerSize * 2 + y, x, mPointerSize + y, mPointerPaint)
        canvas.drawLine(x, y - mPointerSize * 2, x, y - mPointerSize, mPointerPaint)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.drawRect(mViewSize!!, mColorPaint)
        canvas.drawBitmap(mSvRectangle!!, 0f, 0f, null)
        drawPointer(canvas, mViewSize!!.right * mFingerPosX, mViewSize!!.bottom * mFingerPosY)
    }

    override fun onSizeChanged(w: Int, h: Int, old_w: Int, old_h: Int) {
        mViewSize = RectF(0f, 0f, w.toFloat(), h.toFloat())
        mWidthInverted = 1 / mViewSize!!.right
        mHeightInverted = 1 / mViewSize!!.bottom
        if (w > 0 && h > 0) regenerateRectangle()
    }

    protected fun regenerateRectangle() {
        val w = getWidth()
        val h = getHeight()
        if (mSvRectangle != null) mSvRectangle!!.recycle()
        mSvRectangle = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val rectPaint = Paint()
        val canvas = Canvas(mSvRectangle!!)
        val h2f = h / 2f
        val w2f = w / 2f
        rectPaint.setShader(
            LinearGradient(
                0f,
                h2f,
                w.toFloat(),
                h2f,
                Color.WHITE,
                0,
                Shader.TileMode.CLAMP
            )
        )
        canvas.drawRect(mViewSize!!, rectPaint)
        rectPaint.setShader(
            LinearGradient(
                w2f,
                0f,
                w2f,
                h.toFloat(),
                Color.BLACK,
                0,
                Shader.TileMode.CLAMP
            )
        )
        canvas.drawRect(mViewSize!!, rectPaint)
    }
}
