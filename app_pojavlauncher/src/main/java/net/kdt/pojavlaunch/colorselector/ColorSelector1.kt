package net.kdt.pojavlaunch.colorselector

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import com.kdt.SideDialogView
import net.ashmeet.hyperlauncher.R

class ColorSelector(
    context: Context,
    parent: ViewGroup,
    private var mColorSelectionListener: ColorSelectionListener?
) : SideDialogView(context, parent, R.layout.dialog_color_selector), HueSelectionListener,
    RectangleSelectionListener, AlphaSelectionListener, TextWatcher {
    private var mHueView: HueView? = null
    private var mLuminosityIntensityView: SVRectangleView? = null
    private var mAlphaView: AlphaView? = null
    private var mColorView: ColorSideBySideView? = null
    private var mTextView: EditText? = null

    private val mHueTemplate = floatArrayOf(0f, 1f, 1f)
    private val mHsvSelected = floatArrayOf(360f, 1f, 1f)
    private var mAlphaSelected = 0xff
    private var mTextColors: ColorStateList? = null
    private var mWatch = true

    private var mAlphaEnabled = true
    private var mSuppressNotify = false


    override fun onInflate() {
        super.onInflate()
        val content = mDialogContent ?: return
        // Initialize the view contents
        mHueView = content.findViewById<HueView>(R.id.color_selector_hue_view)
        mLuminosityIntensityView =
            content.findViewById<SVRectangleView>(R.id.color_selector_rectangle_view)
        mAlphaView = content.findViewById<AlphaView?>(R.id.color_selector_alpha_view)
        mColorView =
            content.findViewById<ColorSideBySideView>(R.id.color_selector_color_view)
        mTextView = content.findViewById<EditText>(R.id.color_selector_hex_edit)
        runColor(Color.RED)
        mHueView!!.setHueSelectionListener(this)
        mLuminosityIntensityView!!.setRectSelectionListener(this)
        mAlphaView!!.setAlphaSelectionListener(this)
        mTextView!!.addTextChangedListener(this)
        mTextColors = mTextView!!.getTextColors()
        mAlphaView!!.setVisibility(if (mAlphaEnabled) View.VISIBLE else View.GONE)

        // Set elevation to show above other side dialogs.
        // Jank, should be done better
        val contentParent = content.findViewById<View?>(R.id.side_dialog_scrollview)
        if (contentParent != null) {
            val dialogLayout = content.parent as ViewGroup
            dialogLayout.setElevation(11f)
            dialogLayout.setTranslationZ(11f)
        }
    }

    /**
     * Shows the color selector with the desired ARGB color selected
     * @param previousColor the desired ARGB color
     */
    /**
     * Shows the color selector with the default (red) color selected.
     */
    @JvmOverloads
    fun show(fromRight: Boolean, previousColor: Int = Color.RED) {
        appear(fromRight)
        mSuppressNotify = true
        runColor(previousColor) // initialize
        dispatchColorChange(false) // set the hex text (without notifying listener)
        mSuppressNotify = false
    }

    override fun onHueSelected(hue: Float) {
        mHueTemplate[0] = hue
        mHsvSelected[0] = mHueTemplate[0]
        mLuminosityIntensityView!!.setColor(Color.HSVToColor(mHueTemplate), true)
        dispatchColorChange(true)
    }

    override fun onLuminosityIntensityChanged(luminosity: Float, intensity: Float) {
        mHsvSelected[1] = intensity
        mHsvSelected[2] = luminosity
        dispatchColorChange(true)
    }

    override fun onAlphaSelected(alpha: Int) {
        mAlphaSelected = alpha
        dispatchColorChange(true)
    }

    //IUO: called on all color changes
    protected fun dispatchColorChange(notifyListener: Boolean) {
        val color = Color.HSVToColor(mAlphaSelected, mHsvSelected)
        mColorView!!.setColor(color)
        mWatch = false
        mTextView!!.setText(String.format("%08X", color))
        if (notifyListener) notifyColorSelector(color)
    }

    //IUO: sets all Views to render the desired color. Used for initialization and HEX color input
    protected fun runColor(color: Int) {
        Color.RGBToHSV(Color.red(color), Color.green(color), Color.blue(color), mHsvSelected)
        mHueTemplate[0] = mHsvSelected[0]
        mHueView!!.setHue(mHsvSelected[0])
        mLuminosityIntensityView!!.setColor(Color.HSVToColor(mHueTemplate), false)
        mLuminosityIntensityView!!.setLuminosityIntensity(mHsvSelected[2], mHsvSelected[1])
        mAlphaSelected = Color.alpha(color)
        mAlphaView!!.setAlpha(if (mAlphaEnabled) mAlphaSelected else 255)
        mColorView!!.setColor(color)
    }

    override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

    override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

    override fun afterTextChanged(s: Editable) {
        if (mWatch) {
            try {
                val color = s.toString().toInt(16)
                mTextView!!.setTextColor(mTextColors)
                runColor(color)
                notifyColorSelector(color)
            } catch (exception: NumberFormatException) {
                mTextView!!.setTextColor(Color.RED)
            }
        } else {
            mWatch = true
        }
    }

    fun setColorSelectionListener(listener: ColorSelectionListener?) {
        mColorSelectionListener = listener
    }

    fun setAlphaEnabled(alphaEnabled: Boolean) {
        mAlphaEnabled = alphaEnabled
        if (mAlphaView != null) {
            mAlphaView!!.setVisibility(if (alphaEnabled) View.VISIBLE else View.GONE)
            mAlphaView!!.setAlpha(255)
        }
    }

    private fun notifyColorSelector(color: Int) {
        if (mSuppressNotify) return
        if (mColorSelectionListener != null) mColorSelectionListener!!.onColorSelected(color)
    }

    companion object {
        private val ALPHA_MASK = (0xFF shl 24).inv()

        /**
         * Replaces the alpha value of the color passed in, and returns the result.
         * @param color the color to replace the alpha of
         * @param alpha the alpha to use
         * @return the new color
         */
        fun setAlpha(color: Int, alpha: Int): Int {
            return color and ALPHA_MASK or ((alpha and 0xFF) shl 24)
        }
    }
}
