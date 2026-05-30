package net.kdt.pojavlaunch.customcontrols.mouse

import android.os.Build
import android.view.InputDevice
import android.view.MotionEvent
import android.view.View
import android.view.View.OnCapturedPointerListener
import android.view.ViewTreeObserver.OnWindowFocusChangeListener
import androidx.annotation.RequiresApi
import net.kdt.pojavlaunch.MinecraftGLSurface.Companion.sendMouseButtonUnconverted
import net.kdt.pojavlaunch.Tools.dpToPx
import net.kdt.pojavlaunch.Tools.isAndroid8OrHigher
import net.kdt.pojavlaunch.prefs.LauncherPreferences
import org.lwjgl.glfw.CallbackBridge
import org.lwjgl.glfw.CallbackBridge.isGrabbing
import org.lwjgl.glfw.CallbackBridge.sendCursorPos
import org.lwjgl.glfw.CallbackBridge.sendScroll

@RequiresApi(api = Build.VERSION_CODES.O)
class AndroidPointerCapture(private val mTouchpad: AbstractTouchpad, private val mHostView: View) :
    OnWindowFocusChangeListener, OnCapturedPointerListener {
    private val mMousePrescale = dpToPx(1f)
    private val mPointerTracker = PointerTracker()
    private val mScroller = Scroller(TOUCHPAD_SCROLL_THRESHOLD)
    private val mVector: FloatArray = mPointerTracker.motionVector

    private var mInputDeviceIdentifier = 0
    private var mDeviceSupportsRelativeAxis = false

    init {
        mHostView.setOnCapturedPointerListener(this)
        mHostView.getViewTreeObserver().addOnWindowFocusChangeListener(this)
    }

    private fun enableTouchpadIfNecessary() {
        if (!mTouchpad.getDisplayState()) mTouchpad.enable(true)
    }

    fun handleAutomaticCapture() {
        if (!mHostView.hasWindowFocus()) {
            mHostView.requestFocus()
        } else {
            mHostView.requestPointerCapture()
        }
    }

    private fun accumulateHistoricalValues(motionEvent: MotionEvent, axisX: Int, axisY: Int) {
        var relX = motionEvent.getAxisValue(axisX)
        var relY = motionEvent.getAxisValue(axisY)

        if (motionEvent.getHistorySize() > 1) for (i in 0..<motionEvent.getHistorySize()) {
            relX += motionEvent.getHistoricalAxisValue(axisX, i)
            relY += motionEvent.getHistoricalAxisValue(axisY, i)
        }

        mVector[0] = relX
        mVector[1] = relY
    }

    override fun onCapturedPointer(view: View?, event: MotionEvent): Boolean {
        checkSameDevice(event.getDevice())

        // Yes, we actually not only receive relative mouse events here, but also absolute touchpad ones!
        // Therefore, we need to know when it's a touchpad and when it's a mouse.
        if ((event.getSource() and InputDevice.SOURCE_CLASS_TRACKBALL) != 0) {
            // If the source claims to be a relative device by belonging to the trackball class,
            // use its coordinates directly.
            if (mDeviceSupportsRelativeAxis) {
                // If some OEM decides to do a funny and make an absolute touchpad report itself as
                // a trackball, we will at least have semi-valid relative positions
                accumulateHistoricalValues(
                    event,
                    MotionEvent.AXIS_RELATIVE_X,
                    MotionEvent.AXIS_RELATIVE_Y
                )
            } else {
                // Otherwise trust the OS, i guess??
                accumulateHistoricalValues(event, MotionEvent.AXIS_X, MotionEvent.AXIS_Y)
            }
        } else {
            // If it's not a trackball, it's likely a touchpad and needs tracking like a touchscreen.
            mPointerTracker.trackEvent(event)
            // The relative position will already be written down into the mVector variable.
        }

        if (!isGrabbing) {
            enableTouchpadIfNecessary()
            // Yes, if the user's touchpad is multi-touch we will also receive events for that.
            // So, handle the scrolling gesture ourselves.
            mVector[0] *= mMousePrescale
            mVector[1] *= mMousePrescale
            if (event.getPointerCount() < 2) {
                mTouchpad.applyMotionVector(mVector)
                mScroller.resetScrollOvershoot()
            } else {
                mScroller.performScroll(mVector)
            }
        } else {
            // Position is updated by many events, hence it is send regardless of the event value
            CallbackBridge.mouseX += (mVector[0] * LauncherPreferences.PREF_SCALE_FACTOR)
            CallbackBridge.mouseY += (mVector[1] * LauncherPreferences.PREF_SCALE_FACTOR)
            sendCursorPos(CallbackBridge.mouseX, CallbackBridge.mouseY)
        }

        when (event.getActionMasked()) {
            MotionEvent.ACTION_MOVE -> return true
            MotionEvent.ACTION_BUTTON_PRESS -> return sendMouseButtonUnconverted(
                event.getActionButton(),
                true
            )

            MotionEvent.ACTION_BUTTON_RELEASE -> return sendMouseButtonUnconverted(
                event.getActionButton(),
                false
            )

            MotionEvent.ACTION_SCROLL -> {
                sendScroll(
                    event.getAxisValue(MotionEvent.AXIS_HSCROLL).toDouble(),
                    event.getAxisValue(MotionEvent.AXIS_VSCROLL).toDouble()
                )
                return true
            }

            MotionEvent.ACTION_UP -> {
                mPointerTracker.cancelTracking()
                return true
            }

            else -> return false
        }
    }

    private fun checkSameDevice(inputDevice: InputDevice?) {
        val newIdentifier: Int
        if (inputDevice != null) newIdentifier = inputDevice.getId()
        else newIdentifier = Int.Companion.MAX_VALUE
        if (mInputDeviceIdentifier != newIdentifier) {
            reinitializeDeviceSpecificProperties(inputDevice)
            mInputDeviceIdentifier = newIdentifier
        }
    }

    private fun reinitializeDeviceSpecificProperties(inputDevice: InputDevice?) {
        mPointerTracker.cancelTracking()
        if (inputDevice == null) {
            mDeviceSupportsRelativeAxis = false
            return
        }
        val relativeXSupported = inputDevice.getMotionRange(MotionEvent.AXIS_RELATIVE_X) != null
        val relativeYSupported = inputDevice.getMotionRange(MotionEvent.AXIS_RELATIVE_Y) != null
        mDeviceSupportsRelativeAxis = relativeXSupported && relativeYSupported
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        if (hasFocus && isAndroid8OrHigher) mHostView.requestPointerCapture()
    }

    fun detach() {
        mHostView.setOnCapturedPointerListener(null)
        mHostView.getViewTreeObserver().removeOnWindowFocusChangeListener(this)
    }

    companion object {
        private const val TOUCHPAD_SCROLL_THRESHOLD = 1f
    }
}
