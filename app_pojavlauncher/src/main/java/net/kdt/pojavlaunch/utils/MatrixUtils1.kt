package net.kdt.pojavlaunch.utils

import android.graphics.Matrix
import android.graphics.Rect
import android.graphics.RectF

@Suppress("unused")
object MatrixUtils {
    /**
     * Transform the coordinates of the RectF using the supplied Matrix, and write the result back into
     * the RectF
     * @param inOutRect the RectF for this operation
     * @param transformMatrix the Matrix for transforming the Rect.
     */
    @JvmStatic
    fun transformRect(inOutRect: Rect, transformMatrix: Matrix) {
        transformRect(inOutRect, inOutRect, transformMatrix)
    }

    /**
     * Transform the coordinates of the RectF using the supplied Matrix, and write the result back into
     * the RectF
     * @param inOutRect the RectF for this operation
     * @param transformMatrix the Matrix for transforming the Rect.
     */
    @JvmStatic
    fun transformRect(inOutRect: RectF, transformMatrix: Matrix) {
        transformRect(inOutRect, inOutRect, transformMatrix)
    }

    /**
     * Transform the coordinates of the input RectF using the supplied Matrix, and write the result
     * into the output Rect
     * @param inRect the input RectF for this operation
     * @param outRect the output Rect for this operation
     * @param transformMatrix the Matrix for transforming the Rect.
     */
    fun transformRect(inRect: RectF, outRect: Rect, transformMatrix: Matrix) {
        val inOutDecodeRect = createInOutDecodeRect(transformMatrix)
        if (inOutDecodeRect == null) return
        writeInputRect(inOutDecodeRect, inRect)
        transformPoints(inOutDecodeRect, transformMatrix)
        readOutputRect(inOutDecodeRect, outRect)
    }

    /**
     * Transform the coordinates of the input Rect using the supplied Matrix, and write the result
     * into the output RectF
     * @param inRect the input Rect for this operation
     * @param outRect the output RectF for this operation
     * @param transformMatrix the Matrix for transforming the Rect.
     */
    fun transformRect(inRect: Rect, outRect: RectF, transformMatrix: Matrix) {
        val inOutDecodeRect = createInOutDecodeRect(transformMatrix)
        if (inOutDecodeRect == null) return
        writeInputRect(inOutDecodeRect, inRect)
        transformPoints(inOutDecodeRect, transformMatrix)
        readOutputRect(inOutDecodeRect, outRect)
    }

    /**
     * Transform the coordinates of the input Rect using the supplied Matrix, and write the result
     * into the output Rect
     * @param inRect the input Rect for this operation
     * @param outRect the output Rect for this operation
     * @param transformMatrix the Matrix for transforming the Rect.
     */
    @JvmStatic
    fun transformRect(inRect: Rect, outRect: Rect, transformMatrix: Matrix) {
        val inOutDecodeRect = createInOutDecodeRect(transformMatrix)
        if (inOutDecodeRect == null) return
        writeInputRect(inOutDecodeRect, inRect)
        transformPoints(inOutDecodeRect, transformMatrix)
        readOutputRect(inOutDecodeRect, outRect)
    }

    /**
     * Transform the coordinates of the input RectF using the supplied Matrix, and write the result
     * into the output RectF
     * @param inRect the input RectF for this operation
     * @param outRect the output RectF for this operation
     * @param transformMatrix the Matrix for transforming the Rect.
     */
    fun transformRect(inRect: RectF, outRect: RectF, transformMatrix: Matrix) {
        val inOutDecodeRect = createInOutDecodeRect(transformMatrix)
        if (inOutDecodeRect == null) return
        writeInputRect(inOutDecodeRect, inRect)
        transformPoints(inOutDecodeRect, transformMatrix)
        readOutputRect(inOutDecodeRect, outRect)
    }

    // The group of functions below are used as building blocks of the transformRect() functions
    // in order to not repeat the same exact code a lot of times.
    private fun writeInputRect(inOutDecodeRect: FloatArray, inRect: RectF) {
        inOutDecodeRect[0] = inRect.left
        inOutDecodeRect[1] = inRect.top
        inOutDecodeRect[2] = inRect.right
        inOutDecodeRect[3] = inRect.bottom
    }

    private fun writeInputRect(inOutDecodeRect: FloatArray, inRect: Rect) {
        inOutDecodeRect[0] = inRect.left.toFloat()
        inOutDecodeRect[1] = inRect.top.toFloat()
        inOutDecodeRect[2] = inRect.right.toFloat()
        inOutDecodeRect[3] = inRect.bottom.toFloat()
    }

    private fun readOutputRect(inOutDecodeRect: FloatArray, outRect: RectF) {
        outRect.left = inOutDecodeRect[4]
        outRect.top = inOutDecodeRect[5]
        outRect.right = inOutDecodeRect[6]
        outRect.bottom = inOutDecodeRect[7]
    }

    private fun readOutputRect(inOutDecodeRect: FloatArray, outRect: Rect) {
        outRect.left = inOutDecodeRect[4].toInt()
        outRect.top = inOutDecodeRect[5].toInt()
        outRect.right = inOutDecodeRect[6].toInt()
        outRect.bottom = inOutDecodeRect[7].toInt()
    }

    private fun createInOutDecodeRect(transformMatrix: Matrix): FloatArray? {
        if (transformMatrix.isIdentity()) return null
        // We need an array of 8 floats because each point is two floats,
        // we need to transform two points and we need to have a separated input and output
        return FloatArray(8)
    }

    private fun transformPoints(inOutDecodeRect: FloatArray?, transformMatrix: Matrix) {
        transformMatrix.mapPoints(inOutDecodeRect, 4, inOutDecodeRect, 0, 2)
    }

    /**
     * Invert the source matrix, and write the result into the destination matrix.
     * Android's integrated Matrix.invert() has some unexpected conditions when the matrix
     * can't be inverted, and in that case the method inverts the matrix by hand.
     * @param source Source matrix
     * @param destination The inverse of the source matrix
     * @throws IllegalArgumentException when the matrix is not invertible
     */
    @JvmStatic
    @Throws(IllegalArgumentException::class)
    fun inverse(source: Matrix, destination: Matrix) {
        if (source.invert(destination)) return
        val matrix = FloatArray(9)
        source.getValues(matrix)
        inverseMatrix(matrix)
        destination.setValues(matrix)
    }

    // This was made by ChatGPT and i have no clue what's happening here, but it works so eh
    private fun inverseMatrix(matrix: FloatArray) {
        val determinant = (matrix[0] * (matrix[4] * matrix[8] - matrix[5] * matrix[7])
                - matrix[1] * (matrix[3] * matrix[8] - matrix[5] * matrix[6])
                + matrix[2] * (matrix[3] * matrix[7] - matrix[4] * matrix[6]))

        require(determinant != 0f) { "Matrix is not invertible" }

        val invDet = 1 / determinant

        val temp0 = (matrix[4] * matrix[8] - matrix[5] * matrix[7])
        val temp1 = (matrix[2] * matrix[7] - matrix[1] * matrix[8])
        val temp2 = (matrix[1] * matrix[5] - matrix[2] * matrix[4])
        val temp3 = (matrix[5] * matrix[6] - matrix[3] * matrix[8])
        val temp4 = (matrix[0] * matrix[8] - matrix[2] * matrix[6])
        val temp5 = (matrix[2] * matrix[3] - matrix[0] * matrix[5])
        val temp6 = (matrix[3] * matrix[7] - matrix[4] * matrix[6])
        val temp7 = (matrix[1] * matrix[6] - matrix[0] * matrix[7])
        val temp8 = (matrix[0] * matrix[4] - matrix[1] * matrix[3])
        matrix[0] = temp0 * invDet
        matrix[1] = temp1 * invDet
        matrix[2] = temp2 * invDet
        matrix[3] = temp3 * invDet
        matrix[4] = temp4 * invDet
        matrix[5] = temp5 * invDet
        matrix[6] = temp6 * invDet
        matrix[7] = temp7 * invDet
        matrix[8] = temp8 * invDet
    }
}
