package net.kdt.pojavlaunch.customcontrols.mouse

import android.graphics.Canvas
import android.graphics.drawable.Drawable

/**
 * Contains cursor data and the draw method
 */
class CursorContainer(val drawable: Drawable, val xHotspot: Int, val yHotspot: Int) {
    fun draw(canvas: Canvas) {
        canvas.translate(-xHotspot.toFloat(), -yHotspot.toFloat())

        drawable.draw(canvas)
    }
}
