package net.kdt.pojavlaunch.colorselector

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import net.kdt.pojavlaunch.Tools.dpToPx

class HueView(context: Context?, attrs: AttributeSet?) : View(context, attrs) {
    private val blackPaint = Paint()
    private var mGamma: Bitmap? = null
    private var mHueSelectionListener: HueSelectionListener? = null
    private var mSelectionHue = 0f
    private var mHeightHueRatio = 0f
    private var mHueHeightRatio = 0f
    private var mWidth = 0f
    private var mHeight = 0f
    private var mWidthThird = 0f

    init {
        blackPaint.color = Color.BLACK
        blackPaint.strokeWidth = dpToPx(3f)
    }

    fun setHueSelectionListener(listener: HueSelectionListener?) {
        mHueSelectionListener = listener
    }

    fun setHue(hue: Float) {
        mSelectionHue = hue
        invalidate()
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (event.action == MotionEvent.ACTION_DOWN || event.action == MotionEvent.ACTION_MOVE) {
            parent.requestDisallowInterceptTouchEvent(true)
        }

        mSelectionHue = event.y * mHeightHueRatio
        invalidate()
        if (mHueSelectionListener != null) mHueSelectionListener!!.onHueSelected(mSelectionHue)
        return true
    }

    override fun onDraw(canvas: Canvas) {
        mGamma?.let { canvas.drawBitmap(it, 0f, 0f, null) }
        val linePos = mSelectionHue * mHueHeightRatio
        canvas.drawLine(0f, linePos, mWidthThird, linePos, blackPaint)
        canvas.drawLine(mWidthThird * 2, linePos, mWidth, linePos, blackPaint)
    }

    override fun onSizeChanged(w: Int, h: Int, old_w: Int, old_h: Int) {
        mWidth = w.toFloat()
        mHeight = h.toFloat()
        mWidthThird = mWidth / 3
        regenerateGammaBitmap()
    }

    protected fun regenerateGammaBitmap() {
        if (mGamma != null) mGamma!!.recycle()
        mGamma = Bitmap.createBitmap(width.coerceAtLeast(1), height.coerceAtLeast(1), Bitmap.Config.ARGB_8888)
        val paint = Paint()
        val canvas = Canvas(mGamma!!)
        mHeightHueRatio = 360 / mHeight
        mHueHeightRatio = mHeight / 360
        val hsvFiller = floatArrayOf(0f, 1f, 1f)
        var i = 0f
        while (i < mHeight) {
            hsvFiller[0] = i * mHeightHueRatio
            paint.color = Color.HSVToColor(hsvFiller)
            canvas.drawLine(0f, i, mWidth, i, paint)
            i++
        }
    }
}
