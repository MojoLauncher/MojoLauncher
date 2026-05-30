package net.kdt.pojavlaunch.imgcropper

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import androidx.annotation.CallSuper
import net.kdt.pojavlaunch.Tools.dpToPx
import top.defaults.checkerboarddrawable.CheckerboardDrawable
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

class CropperView : View {
    private val mSelectionHighlight = RectF()
    val mSelectionRect: Rect = Rect()
    var horizontalLock: Boolean = false
    var verticalLock: Boolean = false
    private var mLastTouchX = 0f
    private var mLastTouchY = 0f
    private var mHighlightThickness = 0f
    private var mLastDistance = -1f
    private var mSelectionPadding = 0f
    private var mAspectRatio = 1f // w/h
    private var mLastTrackedPointer = 0
    private var mSelectionPaint: Paint? = null
    private var mCropperBehaviour: CropperBehaviour = CropperBehaviour.Companion.DUMMY

    constructor(context: Context?) : super(context) {
        init()
    }

    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs) {
        init()
    }

    constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    ) {
        init()
    }

    fun setAspectRatio(ratio: Float) {
        mAspectRatio = ratio
    }

    protected fun init() {
        setBackground(CheckerboardDrawable.Builder().build())
        mSelectionPadding = dpToPx(24f)
        mHighlightThickness = dpToPx(3f)
        mSelectionPaint = Paint()
        mSelectionPaint!!.setColor(Color.DKGRAY)
        mSelectionPaint!!.setStrokeWidth(mHighlightThickness)
        // Divide the thickness by 2 since we will be needing only half of it for
        // rect highlight correction.
        mHighlightThickness /= 2f
        mSelectionPaint!!.setStyle(Paint.Style.STROKE)
    }

    override fun dispatchGenericMotionEvent(event: MotionEvent): Boolean {
        var x1 = event.getX(0)
        var y1 = event.getY(0)
        if (event.getPointerCount() > 1) {
            // More than 1 pointer = pinching
            // Compute the distance and zoom the image with it
            var x2 = event.getX(1)
            var y2 = event.getY(1)
            val deltaXSquared = (x2 - x1) * (x2 - x1)
            val deltaYSquared = (y2 - y1) * (y2 - y1)
            val distance = sqrt((deltaXSquared + deltaYSquared).toDouble()).toFloat()
            if (mLastDistance != -1f) {
                val distanceDelta = distance - mLastDistance
                val multiplier = 0.005f
                if (horizontalLock) {
                    x1 = mSelectionRect.left.toFloat()
                    x2 = mSelectionRect.right.toFloat()
                }
                if (verticalLock) {
                    y1 = mSelectionRect.top.toFloat()
                    y2 = mSelectionRect.bottom.toFloat()
                }
                val midpointX = (x1 + x2) / 2
                val midpointY = (y1 + y2) / 2
                mCropperBehaviour.zoom(1 + distanceDelta * multiplier, midpointX, midpointY)
            }
            mLastDistance = distance
            return true
        } else {
            // Reset lastDistance as it's fairly reliable to assume that when
            // there's less than 2 pointers on the screen, the zoom gesture is over
            mLastDistance = -1f
        }

        // When not pinching, pan around. Simultaneous panning and zooming proved to be confusing in my testing.
        // Lots of code there to allow seamless finger changing while panning.
        when (event.getActionMasked()) {
            MotionEvent.ACTION_DOWN -> {
                mLastTouchX = x1
                mLastTouchY = y1
                // Remember the pointer index from the start of the gesture.
                // We will be tracking it for the rest of the gesture unless it gets released.
                mLastTrackedPointer = event.getPointerId(0)
            }

            MotionEvent.ACTION_MOVE -> {
                // Fond the pointer we should be tracking
                val trackedIndex = findPointerIndex(event, mLastTrackedPointer)
                // By default, we query the X/Y coordinates of pointer index 0. If our tracked
                // pointer is no longer at index 0 and is still tracked, overwrite the coordinates
                // with the expected ones
                if (trackedIndex > 0) {
                    x1 = event.getX(trackedIndex)
                    y1 = event.getY(trackedIndex)
                }
                if (trackedIndex != -1) {
                    // If we still track out current pointer, pan the image by the movement delta
                    mCropperBehaviour.pan(x1 - mLastTouchX, y1 - mLastTouchY)
                } else {
                    // Otherwise, mark the new tracked pointer without panning.
                    mLastTrackedPointer = event.getPointerId(0)
                }
                mLastTouchX = x1
                mLastTouchY = y1
            }
        }
        return true
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.save()
        mCropperBehaviour.drawPreHighlight(canvas)
        canvas.restore()
        canvas.drawRect(mSelectionHighlight, mSelectionPaint!!)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        return dispatchGenericMotionEvent(event)
    }

    private fun findPointerIndex(event: MotionEvent, id: Int): Int {
        for (i in 0..<event.getPointerCount()) {
            if (event.getPointerId(i) == id) return i
        }
        return -1
    }

    override fun onSizeChanged(w: Int, h: Int, oldW: Int, oldH: Int) {
        super.onSizeChanged(w, h, oldW, oldH)
        val lesserDimension = (min(w, h) - mSelectionPadding).toInt()
        // Calculate the corners of the new selection frame. It should always appear at the center of the view.
        // Accounts for the aspect ratio.
        var targetWidth = lesserDimension
        var centerShiftX = (w - lesserDimension) / 2
        var targetHeight = lesserDimension
        var centerShiftY = (h - lesserDimension) / 2

        val safeAspectRatio = if (mAspectRatio <= 0) 1f else mAspectRatio

        if (safeAspectRatio < 1) {
            targetWidth = (lesserDimension * safeAspectRatio).toInt()
            centerShiftX = (w - targetWidth) / 2
        } else if (safeAspectRatio > 1) {
            targetHeight = (lesserDimension * (1f / safeAspectRatio)).toInt()
            centerShiftY = (h - targetHeight) / 2
        }

        targetWidth = max(1, targetWidth)
        targetHeight = max(1, targetHeight)

        mSelectionRect.left = centerShiftX
        mSelectionRect.top = centerShiftY
        mSelectionRect.right = centerShiftX + targetWidth
        mSelectionRect.bottom = centerShiftY + targetHeight
        mCropperBehaviour.onSelectionRectUpdated()
        // Adjust the selection highlight rectangle to be bigger than the selection area
        // by the highlight thickness, to make sure that the entire inside of the selection highlight
        // will fit into the image
        mSelectionHighlight.left = mSelectionRect.left - mHighlightThickness
        mSelectionHighlight.top = mSelectionRect.top - mHighlightThickness // fixed: was +
        mSelectionHighlight.right = mSelectionRect.right + mHighlightThickness
        mSelectionHighlight.bottom = mSelectionRect.bottom + mHighlightThickness // fixed: was -
    }

    override fun onMeasure(widthSpec: Int, heightSpec: Int) {
        val widthMode = MeasureSpec.getMode(widthSpec)
        val widthSize = MeasureSpec.getSize(widthSpec)
        val heightMode = MeasureSpec.getMode(heightSpec)
        val heightSize = MeasureSpec.getSize(heightSpec)
        if (widthMode == MeasureSpec.EXACTLY && heightMode == MeasureSpec.EXACTLY) {
            // No leeway. Size to spec.
            setMeasuredDimension(widthSize, heightSize)
            return
        }
        var biggestAllowedDimension = mCropperBehaviour.getLargestImageSide()
        if (widthMode == MeasureSpec.EXACTLY) biggestAllowedDimension = widthSize
        if (heightMode == MeasureSpec.EXACTLY) biggestAllowedDimension = heightSize
        setMeasuredDimension(
            pickDesiredDimension(widthMode, widthSize, biggestAllowedDimension),
            pickDesiredDimension(heightMode, heightSize, biggestAllowedDimension)
        )
    }

    private fun pickDesiredDimension(mode: Int, size: Int, desired: Int): Int {
        when (mode) {
            MeasureSpec.EXACTLY -> return size
            MeasureSpec.AT_MOST -> return min(size, desired)
            MeasureSpec.UNSPECIFIED -> return desired
        }
        return desired
    }

    fun setCropperBehaviour(cropperBehaviour: CropperBehaviour) {
        this.mCropperBehaviour = cropperBehaviour
        cropperBehaviour.onSelectionRectUpdated()
    }

    fun resetTransforms() {
        mCropperBehaviour.resetTransforms()
    }


    @CallSuper
    fun reset() {
        mLastDistance = -1f
    }

    fun crop(targetMaxSide: Int): Bitmap? {
        return mCropperBehaviour.crop(targetMaxSide)
    }
}
