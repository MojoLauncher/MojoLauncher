package net.kdt.pojavlaunch.imgcropper

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Rect
import net.kdt.pojavlaunch.utils.MatrixUtils.inverse
import net.kdt.pojavlaunch.utils.MatrixUtils.transformRect
import kotlin.math.max
import kotlin.math.min

open class BitmapCropBehaviour(protected var mHostView: CropperView) : CropperBehaviour {
    private val mTranslateInverse = Matrix()
    protected val mTranslateMatrix: Matrix = Matrix()
    private val mPrescaleMatrix = Matrix()
    private val mImageMatrix = Matrix()
    protected val mZoomMatrix: Matrix = Matrix()
    private var mTranslateInverseOutdated = true
    protected var mOriginalBitmap: Bitmap? = null
    override fun pan(panX: Float, panY: Float) {
        var currentPanX = panX
        var currentPanY = panY
        if (mHostView.horizontalLock) currentPanX = 0f
        if (mHostView.verticalLock) currentPanY = 0f
        if (currentPanX != 0f || currentPanY != 0f) {
            // Actually translate and refresh only if either of the pan deltas are nonzero
            mTranslateMatrix.postTranslate(currentPanX, currentPanY)
            mTranslateInverseOutdated = true
            refresh()
        }
    }

    override fun zoom(zoomLevel: Float, midpointX: Float, midpointY: Float) {
        // Do this to avoid constantly inverting the same matrix on each touch event.
        if (mTranslateInverseOutdated) {
            inverse(mTranslateMatrix, mTranslateInverse)
            mTranslateInverseOutdated = false
        }
        val zoomCenter = floatArrayOf(
            midpointX,
            midpointY
        )
        val realZoomCenter = FloatArray(2)
        mTranslateInverse.mapPoints(realZoomCenter, 0, zoomCenter, 0, 1)
        mZoomMatrix.postScale(zoomLevel, zoomLevel, realZoomCenter[0], realZoomCenter[1])
        refresh()
    }

    override fun getLargestImageSide(): Int {
        val originalBitmap = mOriginalBitmap
        if (originalBitmap == null) return 0
        return max(originalBitmap.width, originalBitmap.height)
    }

    override fun drawPreHighlight(canvas: Canvas?) {
        val originalBitmap = mOriginalBitmap
        if (canvas != null && originalBitmap != null) {
            canvas.drawBitmap(originalBitmap, mImageMatrix, null)
        }
    }

    override fun onSelectionRectUpdated() {
        computeLocalPrescaleMatrix()
    }

    override fun applyImage() {
        mHostView.reset()
        computeLocalPrescaleMatrix()
        resetTransforms()
        refresh()
    }

    fun setBitmap(bitmap: Bitmap?) {
        mOriginalBitmap = bitmap
    }

    protected open fun refresh() {
        mImageMatrix.set(mPrescaleMatrix)
        mImageMatrix.postConcat(mZoomMatrix)
        mImageMatrix.postConcat(mTranslateMatrix)
        mHostView.invalidate()
    }

    override fun crop(targetMaxSide: Int): Bitmap? {
        val originalBitmap = mOriginalBitmap ?: return null
        val imageInverse = Matrix()
        inverse(mImageMatrix, imageInverse)
        // By inverting the matrix we will effectively "divide" our rectangle by it, thus getting
        // its two points on the surface of the bitmap. Math be cool indeed.
        val targetRect = Rect()
        transformRect(mHostView.mSelectionRect, targetRect, imageInverse)
        // Pick the best dimensions for the crop result, shrinking the target if necessary.
        val targetWidth: Int
        val targetHeight: Int
        val targetMinDimension = min(targetRect.width(), targetRect.height())
        if (targetMaxSide < targetMinDimension) {
            val ratio = targetMaxSide.toFloat() / targetMinDimension
            targetWidth = (targetRect.width() * ratio).toInt()
            targetHeight = (targetRect.height() * ratio).toInt()
        } else {
            targetWidth = targetRect.width()
            targetHeight = targetRect.height()
        }
        val config = originalBitmap.config ?: Bitmap.Config.ARGB_8888
        val croppedBitmap = Bitmap.createBitmap(
            targetWidth, targetHeight,
            config
        )
        // Draw the bitmap on the target. Doing this allows us to not bother with making sure
        // that targetRect is fully contained within image bounds.
        val drawCanvas = Canvas(croppedBitmap)
        drawCanvas.drawBitmap(
            originalBitmap,
            targetRect,
            Rect(0, 0, targetWidth, targetHeight),
            null
        )

        return croppedBitmap
    }

    /**
     * Computes a prescale matrix.
     * This matrix basically centers the source image in the selection rect.
     * Mainly intended for convenience of implementing a "Reset" button.
     */
    protected fun computePrescaleMatrix(inMatrix: Matrix, imageWidth: Int, imageHeight: Int) {
        if (mOriginalBitmap == null) return
        val selectionRectWidth = mHostView.mSelectionRect.width()
        val selectionRectHeight = mHostView.mSelectionRect.height()
        // A basic "scale to fit while preserving aspect ratio" I have taken from
        // https://stackoverflow.com/a/23105310
        val hRatio = selectionRectWidth.toFloat() / imageWidth
        val vRatio = selectionRectHeight.toFloat() / imageHeight
        val ratio = min(hRatio, vRatio)
        var centerShift_x = (selectionRectWidth - imageWidth * ratio) / 2
        var centerShift_y = (selectionRectHeight - imageHeight * ratio) / 2
        centerShift_x += mHostView.mSelectionRect.left.toFloat()
        centerShift_y += mHostView.mSelectionRect.top.toFloat()
        // By doing setScale() we don't have to reset() the matrix beforehand saving us a
        // JNI transition
        inMatrix.setScale(ratio, ratio)
        inMatrix.postTranslate(centerShift_x, centerShift_y)
        refresh()
    }

    private fun computeLocalPrescaleMatrix() {
        val originalBitmap = mOriginalBitmap
        if (originalBitmap != null) {
            computePrescaleMatrix(
                mPrescaleMatrix,
                originalBitmap.width,
                originalBitmap.height
            )
        }
    }

    override fun resetTransforms() {
        // Don't set the mTranslateInverseOutdated flag to true here as
        // the inverse of an identity matrix (aka the matrix we're setting ours to on reset())
        // is an identity matrix, which technically means that mTranslateInverse gets up-to-date there
        mTranslateMatrix.reset()
        mTranslateInverse.reset()
        mZoomMatrix.reset()
        refresh()
    }
}
