package net.kdt.pojavlaunch.customcontrols.buttons

import android.annotation.SuppressLint
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PorterDuff
import android.util.Log
import android.util.TypedValue
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.widget.TextView
import net.ashmeet.hyperlauncher.R
import net.kdt.pojavlaunch.LwjglGlfwKeycode
import net.kdt.pojavlaunch.MainActivity
import net.kdt.pojavlaunch.Tools.isValidString
import net.kdt.pojavlaunch.customcontrols.ControlData
import net.kdt.pojavlaunch.customcontrols.ControlLayout
import net.kdt.pojavlaunch.customcontrols.handleview.EditControlSideDialog
import net.kdt.pojavlaunch.prefs.LauncherPreferences
import org.lwjgl.glfw.CallbackBridge

@SuppressLint("ViewConstructor", "AppCompatCustomView")
open class ControlButton(private val mControlLayout: ControlLayout, properties: ControlData) :
    TextView(
        mControlLayout.context
    ), ControlInterface {
    private val mRectPaint = Paint()
    protected var mProperties: ControlData? = null

    /* Cache value from the ControlData radius for drawing purposes */
    private var mComputedRadius = 0f
    private var mHasBitmap = false

    protected var mIsToggled: Boolean = false

    init {
        gravity = Gravity.CENTER
        isAllCaps = LauncherPreferences.PREF_BUTTON_ALL_CAPS
        setTextColor(Color.WHITE)
        setPadding(4, 4, 4, 4)
        textSize = 14f // Nullify the default size setting
        outlineProvider = null // Disable shadow casting, removing one drawing pass

        //When a button is created, the width/height has yet to be processed to fit the scaling.
        setProperties(preProcessProperties(properties, mControlLayout), true)

        injectBehaviors()
        injectLayoutParamBehavior()
    }

    override val controlView: View
        get() = this

    override val properties: ControlData
        get() = mProperties!!

    private fun setupBitmapTint() {
        BackgroundTint.applyToggleTint(context)
        val tintStateList =
            if (properties.isToggle) BackgroundTint.TOGGLE_TINT_LIST else BackgroundTint.DEFAULT_TINT_LIST
        backgroundTintList = tintStateList
        backgroundTintMode = PorterDuff.Mode.SRC_ATOP
    }

    private fun setupNormalTint() {
        mComputedRadius = computeCornerRadius(properties.cornerRadius)
        backgroundTintList = null
        if (properties.isToggle) {
            //For the toggle layer
            val value = TypedValue()
            context.theme.resolveAttribute(R.attr.colorAccent, value, true)
            mRectPaint.color = value.data
            mRectPaint.alpha = BackgroundTint.BACKGROUND_TOGGLE_TINT_ALPHA
        } else {
            mRectPaint.color = Color.WHITE
            mRectPaint.alpha = BackgroundTint.BACKGROUND_DEFAULT_TINT_ALPHA
        }
    }

    override fun setProperties(properties: ControlData?, changePos: Boolean) {
        mProperties = properties
        super<ControlInterface>.setProperties(properties, changePos)

        if (properties == null) return

        mHasBitmap = isValidString(properties.bitmapTag)

        if (mHasBitmap) setupBitmapTint()
        else setupNormalTint()

        text = properties.name
    }

    override fun setBackground() {
        super<ControlInterface>.setBackground()
        // Ensure mComputedRadius is updated when background (and thus radius) is re-evaluated
        if (mProperties != null) {
            mComputedRadius = computeCornerRadius(mProperties!!.cornerRadius)
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        // Bitmap uses a tint list, so don't do any custom rendering
        if (mHasBitmap || !isActivated) return
        canvas.drawRoundRect(
            0f,
            0f,
            width.toFloat(),
            height.toFloat(),
            mComputedRadius,
            mComputedRadius,
            mRectPaint
        )
    }

    override fun isActivated(): Boolean {
        // Any possible side effects?
        return super.isActivated() || (properties.isToggle && mIsToggled)
    }

    override fun loadEditValues(editControlDialog: EditControlSideDialog?) {
        editControlDialog?.loadValues(properties)
    }

    /** Add another instance of the ControlButton to the parent layout  */
    override fun cloneButton() {
        val cloneData = ControlData(properties)
        cloneData.dynamicX = "0.5 * \${screen_width}"
        cloneData.dynamicY = "0.5 * \${screen_height}"
        controlLayoutParent?.addControlButton(cloneData)
    }

    /** Remove any trace of this button from the layout  */
    override fun removeButton() {
        controlLayoutParent?.layout?.mControlDataList?.remove(properties)
        controlLayoutParent?.removeView(this)
    }

    override fun handlePressed() {
        if (properties.isToggle) {
            mIsToggled = !mIsToggled
            setupNormalTint()
            invalidate()
        }

        for (keycode in properties.keycodes) {
            if (keycode == ControlData.SPECIALBTN_KEYBOARD) {
                MainActivity.switchKeyboardState()
                continue
            }
            if (keycode == ControlData.SPECIALBTN_TOGGLECTRL) {
                controlLayoutParent?.toggleControlVisible()
                continue
            }
            if (keycode == ControlData.SPECIALBTN_VIRTUALMOUSE) {
                MainActivity.toggleMouse(context)
                continue
            }
            if (keycode == ControlData.SPECIALBTN_MENU) {
                mControlLayout.onClickedMenu()
                continue
            }
            CallbackBridge.sendKeyPress(keycode, CallbackBridge.currentMods, true)
        }
    }

    override fun handleReleased() {
        if (properties.isToggle) return
        for (keycode in properties.keycodes) {
            if (keycode < 0) continue
            CallbackBridge.sendKeyPress(keycode, CallbackBridge.currentMods, false)
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (mControlLayout.modifiable) return super.onTouchEvent(event)

        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                isPressed = true
                handlePressed()
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                isPressed = false
                handleReleased()
            }
        }
        return true
    }
}
