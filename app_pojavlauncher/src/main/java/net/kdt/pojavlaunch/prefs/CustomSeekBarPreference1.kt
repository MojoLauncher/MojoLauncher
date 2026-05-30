package net.kdt.pojavlaunch.prefs

import android.annotation.SuppressLint
import android.content.Context
import android.os.Build
import android.util.AttributeSet
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.preference.PreferenceViewHolder
import androidx.preference.SeekBarPreference
import com.google.android.material.slider.Slider
import net.ashmeet.hyperlauncher.R
import kotlin.math.floor
import kotlin.math.max

@RequiresApi(Build.VERSION_CODES.S)
class CustomSeekBarPreference @SuppressLint("PrivateResource") constructor(
    context: Context,
    attrs: AttributeSet?,
    defStyleAttr: Int,
    defStyleRes: Int
) : SeekBarPreference(context, attrs, defStyleAttr, defStyleRes) {
    private var mSuffix: String? = ""
    private var mMin = 0
    private var mTextView: TextView? = null
    private var mIncrement = 1
    private var mMax = 100

    init {
        context.obtainStyledAttributes(
            attrs, R.styleable.SeekBarPreference, defStyleAttr, defStyleRes
        ).use { a ->
            mMin = a.getInt(R.styleable.SeekBarPreference_min, 0)
            mIncrement = a.getInt(R.styleable.SeekBarPreference_seekBarIncrement, 1)
        }
        val sa = context.obtainStyledAttributes(attrs, intArrayOf(android.R.attr.max))
        mMax = sa.getInt(0, 100)
        sa.recycle()

        layoutResource = R.layout.app_preference_seekbar_layout
    }

    @JvmOverloads
    constructor(
        context: Context,
        attrs: AttributeSet? = null,
        defStyleAttr: Int = androidx.preference.R.attr.seekBarPreferenceStyle
    ) : this(context, attrs, defStyleAttr, 0)

    override fun setMin(min: Int) {
        super.setMin(min)
        this.mMin = min
    }

    fun setSliderMax(max: Int) {
        super.setMax(max)
        this.mMax = max
    }

    override fun onBindViewHolder(view: PreferenceViewHolder) {
        super.onBindViewHolder(view)

        mTextView = view.findViewById(R.id.seekbar_value) as TextView?
        val slider = view.findViewById(R.id.material_slider) as Slider?

        if (slider != null) {
            val step = max(1f, mIncrement.toFloat())
            slider.valueFrom = mMin.toFloat()


            // Definitively fix IllegalStateException by adjusting range
            val range = mMax - mMin
            var adjustedMax = mMin + (floor((range / step).toDouble()) * step).toFloat()
            if (adjustedMax <= mMin) adjustedMax = mMin + step

            slider.valueTo = adjustedMax
            slider.stepSize = step

            var currentValue = value
            if (currentValue < mMin) currentValue = mMin
            if (currentValue > adjustedMax) currentValue = adjustedMax.toInt()

            // Align currentValue with stepSize to avoid IllegalStateException
            val remainder = (currentValue - mMin) % step
            if (remainder != 0f) {
                currentValue = Math.round(mMin + Math.round((currentValue - mMin) / step) * step)
                // Ensure we don't exceed adjustedMax after rounding
                if (currentValue > adjustedMax) {
                    currentValue = adjustedMax.toInt()
                }
            }

            slider.value = currentValue.toFloat()

            slider.clearOnChangeListeners()
            slider.addOnChangeListener(Slider.OnChangeListener { _, value, fromUser ->
                val intValue = value.toInt()
                if (mTextView != null) {
                    mTextView!!.text = intValue.toString() + mSuffix
                }
                if (fromUser) {
                    // Update value in real-time while dragging for visual feedback
                    if (callChangeListener(intValue)) {
                        this.value = intValue
                    }
                }
            })

            slider.addOnSliderTouchListener(object : Slider.OnSliderTouchListener {
                override fun onStartTrackingTouch(slider: Slider) {}

                override fun onStopTrackingTouch(slider: Slider) {
                    val value = slider.value.toInt()
                    if (callChangeListener(value)) {
                        this@CustomSeekBarPreference.value = value
                    }
                }
            })
        }

        if (mTextView != null) {
            mTextView!!.text = value.toString() + mSuffix
        }
    }

    fun setSuffix(suffix: String?) {
        this.mSuffix = suffix
    }

    fun setMaxKeepIncrement(max: Int) {
        setSliderMax(max)
    }
}
