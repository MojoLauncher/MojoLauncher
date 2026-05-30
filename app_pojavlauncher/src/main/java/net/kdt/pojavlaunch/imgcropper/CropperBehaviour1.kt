package net.kdt.pojavlaunch.imgcropper

import android.graphics.Bitmap
import android.graphics.Canvas

interface CropperBehaviour {
    /**
     * Get the largest side of the image currently loaded into this CropperBehaviour.
     * @return the largest side of the loaded image
     */
    fun getLargestImageSide(): Int

    /**
     * This method is called by CropperView for the CropperBehaviour to draw its image with all
     * the transforms applied, It is called before the selection rectangle is drawn.
     * @param canvas the canvas to draw the image on
     */
    fun drawPreHighlight(canvas: Canvas?)

    /**
     * This method is called by CropperView to let the behaviour know that the selection rect
     * dimensions were updated.
     */
    fun onSelectionRectUpdated()

    /**
     * This method is called by CropperView or by the programmer to reset all current transforms
     * applied to the image loaded within this CropperBehaviour
     */
    fun resetTransforms()

    /**
     * Prepares this behaviour for being rendered in CropperView.
     */
    fun applyImage()

    /**
     * This method is called by CropperView to pan the image
     * @param dx pan delta-X
     * @param dy pan delta-Y
     */
    fun pan(dx: Float, dy: Float)

    /**
     * This method is called by CropperView to zoom the image
     * @param dz zoom delta-Z
     * @param originX the X coordinate of a point at which the image should be zoomed
     * @param originY the Y coordinate of a point at which the image should be zoomed
     */
    fun zoom(dz: Float, originX: Float, originY: Float)

    /**
     * Crop the image according to current transforms, with the targetMaxSide specifying the
     * maximum side of the resulting 1:1 bitmap.
     * @param targetMaxSide the maximum side of the 1:1 bitmap
     * @return the crop of the behaviour's image
     */
    fun crop(targetMaxSide: Int): Bitmap?

    companion object {
        val DUMMY: CropperBehaviour = object : CropperBehaviour {
            override fun getLargestImageSide(): Int {
                return 0
            }

            override fun drawPreHighlight(canvas: Canvas?) {
            }

            override fun onSelectionRectUpdated() {
            }

            override fun resetTransforms() {
            }

            override fun applyImage() {
            }

            override fun pan(dx: Float, dy: Float) {
            }

            override fun zoom(dz: Float, originX: Float, originY: Float) {
            }

            override fun crop(targetMaxSide: Int): Bitmap? {
                return null
            }
        }
    }
}
