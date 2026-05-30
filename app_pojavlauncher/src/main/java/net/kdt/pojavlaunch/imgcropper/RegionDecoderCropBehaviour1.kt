package net.kdt.pojavlaunch.imgcropper

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.BitmapRegionDecoder
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Rect
import android.graphics.RectF
import android.os.Handler
import android.os.Looper
import net.kdt.pojavlaunch.PojavApplication
import net.kdt.pojavlaunch.modloaders.modpacks.SelfReferencingFuture
import net.kdt.pojavlaunch.modloaders.modpacks.SelfReferencingFuture.FutureInterface
import net.kdt.pojavlaunch.utils.MatrixUtils.inverse
import net.kdt.pojavlaunch.utils.MatrixUtils.transformRect
import java.util.concurrent.Future
import kotlin.math.floor
import kotlin.math.max

class RegionDecoderCropBehaviour(hostView: CropperView) : BitmapCropBehaviour(hostView) {
    private var mBitmapDecoder: BitmapRegionDecoder? = null
    private var mOverlayBitmap: Bitmap? = null
    private val mOverlayDst = RectF(0f, 0f, 0f, 0f)
    private var mRequiresOverlayBitmap = false
    private val mDecoderPrescaleMatrix = Matrix()
    private val mHiresLoadHandler = Handler(Looper.getMainLooper())
    private var mDecodeFuture: Future<*>? = null
    private val mHiresLoadRunnable = Runnable {
        val subsectionRect =
            RectF(0f, 0f, mHostView.width.toFloat(), mHostView.height.toFloat())
        val overlayDst = RectF()
        discardDecodeFuture()
        mDecodeFuture = SelfReferencingFuture(object : FutureInterface {
            override fun run(myFuture: Future<*>?) {
                val overlayBitmap = decodeRegionBitmap(overlayDst, subsectionRect)
                mHiresLoadHandler.post {
                    if (myFuture?.isCancelled == true) return@post
                    mOverlayBitmap = overlayBitmap
                    mOverlayDst.set(overlayDst)
                    mHostView.invalidate()
                }
            }
        }).startOnExecutor(PojavApplication.sExecutorService)
    }

    /**
     * Decode a region from this Bitmap based on a subsection in the View coordinate space.
     * @param targetDrawRect an output Rect. This Rect is the position at which the region must
     * be rendered within subsectionRect.
     * @param subsectionRect the subsection in View coordinate space. Note that this Rect is modified
     * by this function and shouldn't be re-used.
     * @return null if the resulting region is bigger than the original image
     * null if the resulting region is completely out of the original image bounds
     * null if the resulting region is smaller than 16x16 pixels
     * null if a region decoding error has occurred
     * the resulting Bitmap region otherwise.
     */
    private fun decodeRegionBitmap(targetDrawRect: RectF, subsectionRect: RectF): Bitmap? {
        val decoder = mBitmapDecoder ?: return null
        val decoderRect = RectF(
            0f,
            0f,
            decoder.width.toFloat(),
            decoder.height.toFloat()
        )
        val matrix = createDecoderImageMatrix()
        val inverse = Matrix()
        inverse(matrix, inverse)
        transformRect(subsectionRect, inverse)
        // If our current sub-section is bigger than the decoder rect, skip.
        // We do this to avoid unnecessarily loading the image at full resolution.
        if (subsectionRect.width() > decoderRect.width()
            || subsectionRect.height() > decoderRect.height()
        ) return null
        // If our current sub-section doesn't even intersect the decoder rect, we won't even
        // be able to create an overlay. So, skip.
        if (!subsectionRect.setIntersect(decoderRect, subsectionRect)) return null
        // In my testing, decoding a region smaller than that breaks the current region decoder instance.
        // So, if it is smaller, skip.
        if (subsectionRect.width() < 16 || subsectionRect.height() < 16) return null
        // We can't really create a floating-point subsection from a bitmap, so convert the intersected
        // rectangle that we want to get from the decoder into an integer Rect.
        val bitmapRegionRect = Rect(
            subsectionRect.left.toInt(),
            subsectionRect.top.toInt(),
            subsectionRect.right.toInt(),
            subsectionRect.bottom.toInt()
        )
        transformRect(subsectionRect, matrix)
        targetDrawRect.set(subsectionRect)
        return decoder.decodeRegion(bitmapRegionRect, null)
    }

    private fun discardDecodeFuture() {
        if (mDecodeFuture != null) {
            // Putting false here as I don't know how BitmapRegionDecoder will behave when interrupted
            mDecodeFuture!!.cancel(false)
        }
    }

    fun setRegionDecoder(bitmapRegionDecoder: BitmapRegionDecoder?) {
        mBitmapDecoder = bitmapRegionDecoder
    }

    override fun getLargestImageSide(): Int {
        val decoder = mBitmapDecoder ?: return 0
        return max(decoder.width, decoder.height)
    }

    override fun drawPreHighlight(canvas: Canvas?) {
        val overlayBitmap = mOverlayBitmap
        if (canvas != null && overlayBitmap != null) {
            canvas.drawBitmap(overlayBitmap, null, mOverlayDst, null)
        } else {
            super.drawPreHighlight(canvas)
        }
    }

    override fun refresh() {
        mOverlayBitmap?.recycle()
        mOverlayBitmap = null
        
        mHiresLoadHandler.removeCallbacks(mHiresLoadRunnable)
        discardDecodeFuture()
        if (mRequiresOverlayBitmap) {
            mHiresLoadHandler.postDelayed(mHiresLoadRunnable, 200)
        }
        super.refresh()
    }

    override fun applyImage() {
        createScaledSourceBitmap()
        computeDecoderPrescaleMatrix()
        super.applyImage()
    }

    override fun onSelectionRectUpdated() {
        createScaledSourceBitmap()
        computeDecoderPrescaleMatrix()
        super.onSelectionRectUpdated()
    }

    /**
     * Load a scaled down version of the Bitmap that will be used for zooming and panning in the view.
     * BitmapCropBehaviour will base its prescale matrix off of this Bitmap.
     */
    private fun createScaledSourceBitmap() {
        val decoder = mBitmapDecoder ?: return
        val width = mHostView.width
        val height = mHostView.height
        val imageWidth = decoder.width
        val imageHeight = decoder.height
        val hRatio = width.toFloat() / imageWidth
        val vRatio = height.toFloat() / imageHeight
        var ratio = max(hRatio, vRatio)
        val options = BitmapFactory.Options()
        if (ratio < 1 && ratio != 0f) {
            ratio = 1 / ratio
            options.inSampleSize = floor(ratio.toDouble()).toInt()
            mRequiresOverlayBitmap = true
        } else {
            mRequiresOverlayBitmap = false
        }
        mOriginalBitmap = decoder.decodeRegion(
            Rect(0, 0, imageWidth, imageHeight),
            options
        )
    }

    /**
     * Compute the prescale matrix for the image bounds of the BitmapRegionDecoder. Used to
     * align the transforms done on the scaled source bitmap with the bitmap region decoder.
     */
    private fun computeDecoderPrescaleMatrix() {
        val decoder = mBitmapDecoder ?: return
        computePrescaleMatrix(
            mDecoderPrescaleMatrix,
            decoder.width,
            decoder.height
        )
    }

    /**
     * Create a Matrix that can be used to transform points from the bitmap coordinate space into the
     * View coordinate space.
     */
    private fun createDecoderImageMatrix(): Matrix {
        val decoderImageMatrix = Matrix(mDecoderPrescaleMatrix)
        decoderImageMatrix.postConcat(mZoomMatrix)
        decoderImageMatrix.postConcat(mTranslateMatrix)
        return decoderImageMatrix
    }

    override fun crop(targetMaxSide: Int): Bitmap? {
        val hostSelection = mHostView.mSelectionRect
        val drawRect = RectF()
        val regionBitmap = decodeRegionBitmap(drawRect, RectF(hostSelection))
        if (regionBitmap == null) {
            // If we can't decode a hi-res region, just crop out of the low-res preview. Yes, this will in fact
            // cause the image to be low res, but we can't really avoid that in this case.
            return super.crop(targetMaxSide)
        }
        // Offset the drawRect by the host selection's top-right corner, to properly position it within the resulting bitmap
        drawRect.offset(-hostSelection.left.toFloat(), -hostSelection.top.toFloat())
        val selectionDims = Rect(mHostView.mSelectionRect)
        selectionDims.offsetTo(0, 0)

        val maxSide = max(selectionDims.width(), selectionDims.height())
        val scaleFactor = targetMaxSide.toFloat() / maxSide

        val drawRectScaleMatrix = Matrix()
        drawRectScaleMatrix.setScale(scaleFactor, scaleFactor)

        transformRect(drawRect, drawRectScaleMatrix)
        transformRect(selectionDims, drawRectScaleMatrix)

        val config = regionBitmap.config ?: Bitmap.Config.ARGB_8888
        val returnBitmap = Bitmap.createBitmap(
            selectionDims.width(),
            selectionDims.height(),
            config
        )
        val canvas = Canvas(returnBitmap)
        canvas.drawBitmap(regionBitmap, null, drawRect, null)
        regionBitmap.recycle()
        return returnBitmap
    }
}
