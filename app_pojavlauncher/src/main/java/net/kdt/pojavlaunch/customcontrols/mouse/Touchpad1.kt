package net.kdt.pojavlaunch.customcontrols.mouse

import android.content.Context
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.os.Build
import android.util.AttributeSet
import android.view.View
import androidx.core.content.res.ResourcesCompat
import androidx.core.util.Consumer
import net.ashmeet.hyperlauncher.R
import net.kdt.pojavlaunch.GrabListener
import net.kdt.pojavlaunch.prefs.LauncherPreferences
import org.lwjgl.glfw.CallbackBridge.addCursorChangeListener
import org.lwjgl.glfw.CallbackBridge.cursor
import org.lwjgl.glfw.CallbackBridge.isGrabbing
import org.lwjgl.glfw.CallbackBridge.removeCursorChangeListener
import org.lwjgl.glfw.CallbackBridge.sendCursorPos
import kotlin.math.max
import kotlin.math.min

/**
 * Class dealing with the virtual mouse
 */
class Touchpad @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null) :
    View(context, attrs), GrabListener, AbstractTouchpad {
    /* Whether the Touchpad should be displayed */
    private var mDisplayState = false

    /* Mouse pointer icon used by the touchpad */
    private var mMouseX = 0f
    private var mMouseY = 0f
    private var mMoveOnLayout = false
    private var mMouseCursorDrawable: Drawable? = null
    private var mCustomCursorScale = 1.0f
    private val onCursorChange = Consumer { cursor: CursorContainer? -> invalidate() }

    init {
        init()
    }

    /** Enable the touchpad  */
    private fun _enable() {
        setVisibility(VISIBLE)
        mMoveOnLayout = true
    }

    /** Disable the touchpad and hides the mouse  */
    private fun _disable() {
        setVisibility(GONE)
    }

    /** @return The new state, enabled or disabled
     */
    fun switchState(): Boolean {
        mDisplayState = !mDisplayState
        if (!isGrabbing) {
            if (mDisplayState) _enable()
            else _disable()
        }
        return mDisplayState
    }

    fun placeMouseAt(x: Float, y: Float) {
        mMouseX = x
        mMouseY = y
        updateMousePosition()
    }

    private fun sendMousePosition() {
        sendCursorPos(
            (mMouseX * LauncherPreferences.PREF_SCALE_FACTOR),
            (mMouseY * LauncherPreferences.PREF_SCALE_FACTOR)
        )
    }

    private fun updateMousePosition() {
        sendMousePosition()
        // I wanted to implement a dirty rect for this, but it is ignored since API level 21
        // (which is our min API)
        // Let's hope the "internally calculated area" is good enough.
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        canvas.save()
        canvas.translate(mMouseX, mMouseY)
        canvas.scale(LauncherPreferences.PREF_MOUSESCALE, LauncherPreferences.PREF_MOUSESCALE)
        if (cursor != null) {
            cursor!!.draw(canvas)
        } else {
            if (LauncherPreferences.PREF_MOUSE_CURSOR_PATH != null) {
                canvas.scale(mCustomCursorScale, mCustomCursorScale)
                canvas.translate(
                    -LauncherPreferences.PREF_MOUSE_HOTSPOT_X.toFloat(),
                    -LauncherPreferences.PREF_MOUSE_HOTSPOT_Y.toFloat()
                )
            }
            mMouseCursorDrawable!!.draw(canvas)
        }
        canvas.restore()
    }

    private fun init() {
        if (LauncherPreferences.PREF_MOUSE_CURSOR_PATH != null) {
            val bitmap = BitmapFactory.decodeFile(LauncherPreferences.PREF_MOUSE_CURSOR_PATH)
            if (bitmap != null) {
                mMouseCursorDrawable = BitmapDrawable(getResources(), bitmap)
                mMouseCursorDrawable!!.setBounds(0, 0, bitmap.getWidth(), bitmap.getHeight())
                // Normalize custom cursor size (base height 48px)
                mCustomCursorScale = 48f / bitmap.getHeight()
            }
        }

        if (mMouseCursorDrawable == null) {
            mMouseCursorDrawable = ResourcesCompat.getDrawable(
                getResources(),
                R.drawable.ic_mouse_pointer,
                getContext().getTheme()
            )
            // For some reason it's annotated as Nullable even though it doesn't seem to actually
            // ever return null
            checkNotNull(mMouseCursorDrawable)
            mMouseCursorDrawable!!.setBounds(0, 0, 36, 54)
            mCustomCursorScale = 1.0f
        }

        addCursorChangeListener(onCursorChange)

        setFocusable(false)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            setDefaultFocusHighlightEnabled(false)
        }

        // When the game is grabbing, we should not display the mouse
        disable()
        mDisplayState = false
    }

    override fun onGrabState(isGrabbing: Boolean) {
        post(Runnable { updateGrabState(isGrabbing) })
    }

    private fun updateGrabState(isGrabbing: Boolean) {
        if (!isGrabbing) {
            if (mDisplayState && getVisibility() != VISIBLE) _enable()
            if (!mDisplayState && getVisibility() == VISIBLE) _disable()
        } else {
            if (getVisibility() != GONE) _disable()
        }
    }

    override fun getDisplayState(): Boolean {
        return mDisplayState
    }

    override fun applyMotionVector(x: Float, y: Float) {
        mMouseX =
            max(0f, min(getWidth().toFloat(), mMouseX + x * LauncherPreferences.PREF_MOUSESPEED))
        mMouseY =
            max(0f, min(getHeight().toFloat(), mMouseY + y * LauncherPreferences.PREF_MOUSESPEED))
        updateMousePosition()
    }

    override fun enable(supposed: Boolean) {
        if (mDisplayState) return
        mDisplayState = true
        if (supposed && isGrabbing) return
        _enable()
    }

    override fun disable() {
        if (!mDisplayState) return
        mDisplayState = false
        _disable()
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        if (!mMoveOnLayout) return
        var w = getMeasuredWidth()
        var h = getMeasuredHeight()
        if (w == 0) w = getWidth()
        if (h == 0) h = getHeight()
        placeMouseAt(w / 2f, h / 2f)
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        addCursorChangeListener(onCursorChange)
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        // if we do not detach the listener
        // it may cause a memory leak due to the object
        // storing an instance of this View
        removeCursorChangeListener(onCursorChange)
    }
}
