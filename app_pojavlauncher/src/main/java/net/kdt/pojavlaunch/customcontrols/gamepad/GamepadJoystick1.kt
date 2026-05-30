package net.kdt.pojavlaunch.customcontrols.gamepad

import android.view.InputDevice
import android.view.MotionEvent
import net.kdt.pojavlaunch.utils.MathUtils.dist
import kotlin.math.abs
import kotlin.math.atan2

class GamepadJoystick(
) {
    var verticalAxis: Float = 0f
        private set
    var horizontalAxis: Float = 0f
        private set

    val angleRadian: Double
        get() =//From -PI to PI
            // TODO misuse of the deadzone here !
            -atan2(this.verticalAxis.toDouble(), this.horizontalAxis.toDouble())


    val angleDegree: Double
        get() {
            //From 0 to 360 degrees
            var result = Math.toDegrees(this.angleRadian)
            if (result < 0) result += 360.0

            return result
        }

    val magnitude: Double
        get() {
            val x = abs(this.horizontalAxis)
            val y = abs(this.verticalAxis)

            return dist(0f, 0f, x, y).toDouble()
        }

    val heightDirection: Int
        get() {
            if (this.magnitude == 0.0) return DIRECTION_NONE
            return (((this.angleDegree + 22.5) / 45).toInt()) % 8
        }


    /* Setters */
    fun setXAxisValue(value: Float) {
        this.horizontalAxis = value
    }

    fun setYAxisValue(value: Float) {
        this.verticalAxis = value
    }

    companion object {
        //Directions
        val DIRECTION_NONE: Int = -1 //GamepadJoystick at the center

        const val DIRECTION_EAST: Int = 0
        const val DIRECTION_NORTH_EAST: Int = 1
        const val DIRECTION_NORTH: Int = 2
        const val DIRECTION_NORTH_WEST: Int = 3
        const val DIRECTION_WEST: Int = 4
        const val DIRECTION_SOUTH_WEST: Int = 5
        const val DIRECTION_SOUTH: Int = 6
        const val DIRECTION_SOUTH_EAST: Int = 7

        fun isJoystickEvent(event: MotionEvent): Boolean {
            return (event.getSource() and InputDevice.SOURCE_JOYSTICK) == InputDevice.SOURCE_JOYSTICK
                    && event.getAction() == MotionEvent.ACTION_MOVE
        }
    }
}
