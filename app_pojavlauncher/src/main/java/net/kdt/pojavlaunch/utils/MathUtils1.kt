package net.kdt.pojavlaunch.utils

import kotlin.math.hypot

object MathUtils {
    //Ported from https://www.arduino.cc/reference/en/language/functions/math/map/
    @JvmStatic
    fun map(x: Float, in_min: Float, in_max: Float, out_min: Float, out_max: Float): Float {
        return (x - in_min) * (out_max - out_min) / (in_max - in_min) + out_min
    }

    /** Returns the distance between two points.  */
    @JvmStatic
    fun dist(x1: Float, y1: Float, x2: Float, y2: Float): Float {
        val x = (x2 - x1)
        val y = (y2 - y1)
        return hypot(x.toDouble(), y.toDouble()).toFloat()
    }

    @JvmStatic
    fun calculateConstraint(pos: Float, parentSize: Float, viewSize: Float, isX: Boolean): String {
        val variable = if (isX) "screen_width" else "screen_height"
        return (pos / parentSize).toString() + " * \${" + variable + "}"
    }

    /**
     * Find the object T with the closest (or higher) value compared to targetValue
     */
    fun <T> findNearestPositive(
        targetValue: Int,
        objects: MutableList<T>,
        valueProvider: ValueProvider<T>
    ): RankedValue<T>? {
        var delta = Int.MAX_VALUE
        var selectedObject: T? = null
        for (obj in objects) {
            val objectValue = valueProvider.getValue(obj)
            if (objectValue < targetValue) continue

            val currentDelta = objectValue - targetValue
            if (currentDelta == 0) return RankedValue(obj, 0)
            if (currentDelta >= delta) continue

            selectedObject = obj
            delta = currentDelta
        }
        if (selectedObject == null) return null
        return RankedValue(selectedObject, delta)
    }

    /**
     * Out of two objects, select one with the lowest value.
     */
    fun <T> objectMin(object1: T?, object2: T?, valueProvider: ValueProvider<T>): T? {
        if (object1 == null) return object2
        if (object2 == null) return object1
        val value1 = valueProvider.getValue(object1)
        val value2 = valueProvider.getValue(object2)
        return if (value1 <= value2) {
            object1
        } else {
            object2
        }
    }

    fun interface ValueProvider<T> {
        fun getValue(obj: T): Int
    }

    class RankedValue<T>(@JvmField val value: T, @JvmField val rank: Int)
}
