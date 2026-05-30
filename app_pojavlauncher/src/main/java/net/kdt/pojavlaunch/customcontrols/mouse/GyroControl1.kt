package net.kdt.pojavlaunch.customcontrols.mouse

import android.app.Activity
import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.view.OrientationEventListener
import android.view.Surface
import android.view.WindowManager
import net.kdt.pojavlaunch.GrabListener
import net.kdt.pojavlaunch.prefs.LauncherPreferences
import org.lwjgl.glfw.CallbackBridge
import org.lwjgl.glfw.CallbackBridge.addGrabListener
import org.lwjgl.glfw.CallbackBridge.isGrabbing
import org.lwjgl.glfw.CallbackBridge.removeGrabListener
import org.lwjgl.glfw.CallbackBridge.sendCursorPos
import java.util.Arrays
import kotlin.math.abs

class GyroControl(activity: Activity) : SensorEventListener, GrabListener {
    private val mWindowManager: WindowManager
    private var mSurfaceRotation: Int
    private val mSensorManager: SensorManager
    private val mSensor: Sensor?
    private val mCorrectionListener: OrientationCorrectionListener
    private var mShouldHandleEvents = false
    private var mWarmup = 0
    private var xFactor = 0f // -1 or 1 depending on device orientation
    private var yFactor = 0f
    private var mSwapXY = false

    private val mPreviousRotation = FloatArray(16)
    private val mCurrentRotation = FloatArray(16)
    private val mAngleDifference = FloatArray(3)


    /* Used to average the last values, if smoothing is enabled */
    private val mAngleBuffer: Array<FloatArray> = Array(
        if (LauncherPreferences.PREF_GYRO_SMOOTHING) 2 else 1
    ) { FloatArray(3) }
    private var xTotal = 0f
    private var yTotal = 0f

    private var xAverage = 0f
    private var yAverage = 0f
    private var mHistoryIndex = -1

    /* Store the gyro movement under the threshold */
    private var mStoredX = 0f
    private var mStoredY = 0f

    init {
        mWindowManager = activity.getWindowManager()
        mSurfaceRotation = -10
        mSensorManager = activity.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        mSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_GAME_ROTATION_VECTOR)
        mCorrectionListener = OrientationCorrectionListener(activity)
        updateOrientation()
    }

    fun enable() {
        if (mSensor == null) return
        mWarmup = ROTATION_VECTOR_WARMUP_PERIOD
        mSensorManager.registerListener(
            this,
            mSensor,
            1000 * LauncherPreferences.PREF_GYRO_SAMPLE_RATE
        )
        mCorrectionListener.enable()
        mShouldHandleEvents = isGrabbing
        addGrabListener(this)
    }

    fun disable() {
        if (mSensor == null) return
        mSensorManager.unregisterListener(this)
        mCorrectionListener.disable()
        mStoredY = 0f
        mStoredX = mStoredY
        resetDamper()
        removeGrabListener(this)
    }

    override fun onSensorChanged(sensorEvent: SensorEvent) {
        if (!mShouldHandleEvents) return
        // Copy the old array content
        System.arraycopy(mCurrentRotation, 0, mPreviousRotation, 0, 16)
        SensorManager.getRotationMatrixFromVector(mCurrentRotation, sensorEvent.values)


        if (mWarmup > 0) {  // Setup initial position
            mWarmup--
            return
        }
        SensorManager.getAngleChange(mAngleDifference, mCurrentRotation, mPreviousRotation)
        damperValue(mAngleDifference)
        mStoredX += xAverage * 1000 * LauncherPreferences.PREF_GYRO_SENSITIVITY
        mStoredY += yAverage * 1000 * LauncherPreferences.PREF_GYRO_SENSITIVITY

        var updatePosition = false
        val absX = abs(mStoredX)
        val absY = abs(mStoredY)

        if (absX + absY > MULTI_AXIS_LOW_PASS_THRESHOLD) {
            CallbackBridge.mouseX -= ((if (mSwapXY) mStoredY else mStoredX) * xFactor)
            CallbackBridge.mouseY += ((if (mSwapXY) mStoredX else mStoredY) * yFactor)
            mStoredX = 0f
            mStoredY = 0f
            updatePosition = true
        } else {
            if (abs(mStoredX) > SINGLE_AXIS_LOW_PASS_THRESHOLD) {
                CallbackBridge.mouseX -= ((if (mSwapXY) mStoredY else mStoredX) * xFactor)
                mStoredX = 0f
                updatePosition = true
            }

            if (abs(mStoredY) > SINGLE_AXIS_LOW_PASS_THRESHOLD) {
                CallbackBridge.mouseY += ((if (mSwapXY) mStoredX else mStoredY) * yFactor)
                mStoredY = 0f
                updatePosition = true
            }
        }

        if (updatePosition) {
            sendCursorPos(CallbackBridge.mouseX, CallbackBridge.mouseY)
        }
    }

    /** Update the axis mapping in accordance to activity rotation, used for initial rotation  */
    fun updateOrientation() {
        val rotation = mWindowManager.getDefaultDisplay().getRotation()
        mSurfaceRotation = rotation
        when (rotation) {
            Surface.ROTATION_0 -> {
                mSwapXY = true
                xFactor = 1f
                yFactor = 1f
            }

            Surface.ROTATION_90 -> {
                mSwapXY = false
                xFactor = -1f
                yFactor = 1f
            }

            Surface.ROTATION_180 -> {
                mSwapXY = true
                xFactor = -1f
                yFactor = -1f
            }

            Surface.ROTATION_270 -> {
                mSwapXY = false
                xFactor = 1f
                yFactor = -1f
            }
        }

        if (LauncherPreferences.PREF_GYRO_INVERT_X) xFactor *= -1f
        if (LauncherPreferences.PREF_GYRO_INVERT_Y) yFactor *= -1f
    }

    override fun onAccuracyChanged(sensor: Sensor?, i: Int) {}

    override fun onGrabState(isGrabbing: Boolean) {
        mWarmup = ROTATION_VECTOR_WARMUP_PERIOD
        mShouldHandleEvents = isGrabbing
    }


    /**
     * Compute the moving average of the gyroscope to reduce jitter
     * @param newAngleDifference The new angle difference
     */
    private fun damperValue(newAngleDifference: FloatArray) {
        mHistoryIndex++
        if (mHistoryIndex >= mAngleBuffer.size) mHistoryIndex = 0

        xTotal -= mAngleBuffer[mHistoryIndex][1]
        yTotal -= mAngleBuffer[mHistoryIndex][2]

        System.arraycopy(newAngleDifference, 0, mAngleBuffer[mHistoryIndex], 0, 3)

        xTotal += mAngleBuffer[mHistoryIndex][1]
        yTotal += mAngleBuffer[mHistoryIndex][2]

        // compute the moving average
        xAverage = xTotal / mAngleBuffer.size
        yAverage = yTotal / mAngleBuffer.size
    }

    /** Reset the moving average data  */
    private fun resetDamper() {
        mHistoryIndex = -1
        xTotal = 0f
        yTotal = 0f
        xAverage = 0f
        yAverage = 0f
        for (oldAngle in mAngleBuffer) {
            Arrays.fill(oldAngle, 0f)
        }
    }

    internal inner class OrientationCorrectionListener(context: Context?) :
        OrientationEventListener(context, SensorManager.SENSOR_DELAY_NORMAL) {
        override fun onOrientationChanged(i: Int) {
            // Force to wait to be in game before setting factors
            // Theoretically, one could use the whole interface in portrait...
            if (!mShouldHandleEvents) return

            if (i == ORIENTATION_UNKNOWN) {
                return  //change nothing
            }



            when (mSurfaceRotation) {
                Surface.ROTATION_90, Surface.ROTATION_270 -> {
                    mSwapXY = false
                    if (225 < i && i < 315) {
                        xFactor = -1f
                        yFactor = 1f
                    } else if (45 < i && i < 135) {
                        xFactor = 1f
                        yFactor = -1f
                    }
                }

                Surface.ROTATION_0, Surface.ROTATION_180 -> {
                    mSwapXY = true
                    if ((315 < i && i <= 360) || (i < 45)) {
                        xFactor = 1f
                        yFactor = 1f
                    } else if (135 < i && i < 225) {
                        xFactor = -1f
                        yFactor = -1f
                    }
                }
            }

            if (LauncherPreferences.PREF_GYRO_INVERT_X) xFactor *= -1f
            if (LauncherPreferences.PREF_GYRO_INVERT_Y) yFactor *= -1f
        }
    }

    companion object {
        /* How much distance has to be moved before taking into account the gyro */
        private const val SINGLE_AXIS_LOW_PASS_THRESHOLD = 1.13f
        private const val MULTI_AXIS_LOW_PASS_THRESHOLD = 1.3f

        // Warmup period of 2 since the first read from the sensor seems to produce a bogus value,
        // which creates a far too large of a difference on the Y axis once actual sensor data comes in
        private const val ROTATION_VECTOR_WARMUP_PERIOD = 2
    }
}
