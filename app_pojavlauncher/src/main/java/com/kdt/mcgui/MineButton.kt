package com.kdt.mcgui

import android.content.Context
import android.content.SharedPreferences
import android.content.SharedPreferences.OnSharedPreferenceChangeListener
import android.content.res.ColorStateList
import android.util.AttributeSet
import android.util.TypedValue
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import com.google.android.material.button.MaterialButton
import net.ashmeet.hyperlauncher.R
import net.kdt.pojavlaunch.prefs.LauncherPreferences
import net.kdt.pojavlaunch.prefs.LauncherPreferences.loadPreferences

/**
 * Custom button implementation using Material 3 (MaterialButton).
 * This class replaces the legacy AppCompatButton while maintaining compatibility
 * with existing preferences and layouts.
 */
class MineButton @JvmOverloads constructor(ctx: Context, attrs: AttributeSet? = null) :
    MaterialButton(ctx, attrs, com.google.android.material.R.attr.materialButtonStyle) {
    private var mCustomCornerRadius = -1f
    private var mPrefListener: OnSharedPreferenceChangeListener? = null

    init {
        if (attrs != null) {
            val a = ctx.obtainStyledAttributes(attrs, R.styleable.MineButton)
            mCustomCornerRadius = a.getDimension(R.styleable.MineButton_cornerRadius, -1f)
            a.recycle()
        }
        
        // Remove default vertical padding in MaterialButton that creates unclickable space
        insetTop = 0
        insetBottom = 0
        
        init()
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        registerPrefListener()
        init() // Refresh preferences when attached
    }

    override fun onDetachedFromWindow() {
        unregisterPrefListener()
        super.onDetachedFromWindow()
    }

    fun init() {
        // Set custom typeface
        try {
            typeface = ResourcesCompat.getFont(context, R.font.noto_sans_bold)
        } catch (e: Exception) {
            // Ignore if font is missing
        }

        // Corner Radius logic
        val radius: Int = if (mCustomCornerRadius >= 0) {
            mCustomCornerRadius.toInt()
        } else {
            TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                LauncherPreferences.PREF_MINEBUTTON_CORNER_RADIUS.toFloat(),
                resources.displayMetrics
            ).toInt()
        }
        cornerRadius = radius

        // Styling from preferences
        val bgColor = LauncherPreferences.PREF_MINEBUTTON_COLOR
        val outlineColor = LauncherPreferences.PREF_MINEBUTTON_OUTLINE_COLOR
        
        backgroundTintList = ColorStateList.valueOf(bgColor)
        
        strokeColor = ColorStateList.valueOf(outlineColor)
        strokeWidth = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            1f,
            resources.displayMetrics
        ).toInt()

        // Ripple effect
        rippleColor = ColorStateList.valueOf(0x40FFFFFF)

        // Text size and color
        setTextSize(
            TypedValue.COMPLEX_UNIT_PX,
            resources.getDimensionPixelSize(R.dimen._13ssp).toFloat()
        )
        setTextColor(ContextCompat.getColor(context, R.color.minebutton_text_color))
        
        // All caps setting
        isAllCaps = LauncherPreferences.PREF_BUTTON_ALL_CAPS
    }

    private fun registerPrefListener() {
        if (mPrefListener != null) return
        if (LauncherPreferences.DEFAULT_PREF == null) return

        mPrefListener = OnSharedPreferenceChangeListener { _, key ->
            if (key == null) return@OnSharedPreferenceChangeListener
            when (key) {
                "appearancePreset", "minebutton_corner_radius", "mineButtonColor", "appTheme" -> {
                    loadPreferences(context)
                    init()
                }
            }
        }
        LauncherPreferences.DEFAULT_PREF!!.registerOnSharedPreferenceChangeListener(mPrefListener)
    }

    private fun unregisterPrefListener() {
        if (mPrefListener == null) return
        LauncherPreferences.DEFAULT_PREF?.unregisterOnSharedPreferenceChangeListener(mPrefListener)
        mPrefListener = null
    }
}
