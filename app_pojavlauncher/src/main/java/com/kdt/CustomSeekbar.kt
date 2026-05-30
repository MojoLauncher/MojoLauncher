package com.kdt

import android.annotation.SuppressLint
import android.content.Context
import android.os.Build
import android.util.AttributeSet
import android.widget.SeekBar
import net.ashmeet.hyperlauncher.R

/**
 * Seekbar with ability to handle ranges and increments
 */
@SuppressLint("AppCompatCustomView")
class CustomSeekbar : SeekBar {
    private var mMin = 0
    private var mIncrement = 1
    private var mListener: OnSeekBarChangeListener? = null

    private val mInternalListener: OnSeekBarChangeListener = object : OnSeekBarChangeListener {
        /** When using increments, this flag is used to prevent double calls to the listener  */
        private var internalChanges = false

        /** Store the previous progress to prevent double calls with increments  */
        private var previousProgress = 0
        override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
            var progress = progress
            if (internalChanges) return
            internalChanges = true

            progress += mMin
            progress = applyIncrement(progress)

            if (progress != previousProgress) {
                if (mListener != null) {
                    previousProgress = progress
                    mListener!!.onProgressChanged(seekBar, progress, fromUser)
                }
            }

            // Forces the thumb to snap to the increment
            setProgress(progress)
            internalChanges = false
        }

        override fun onStartTrackingTouch(seekBar: SeekBar?) {
            if (internalChanges) return

            if (mListener != null) {
                mListener!!.onStartTrackingTouch(seekBar)
            }
        }

        override fun onStopTrackingTouch(seekBar: SeekBar) {
            if (internalChanges) return
            internalChanges = true

            setProgress(seekBar.getProgress())

            if (mListener != null) {
                mListener!!.onStopTrackingTouch(seekBar)
            }
            internalChanges = false
        }
    }

    constructor(context: Context?) : super(context) {
        setup(null)
    }

    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs) {
        setup(attrs)
    }

    constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    ) {
        setup(attrs)
    }

    fun setIncrement(increment: Int) {
        mIncrement = increment
    }

    fun setRange(min: Int, max: Int) {
        mMin = min
        setMax(max - min)
    }

    @Synchronized
    override fun setProgress(progress: Int) {
        super.setProgress(applyIncrement(progress - mMin))
    }

    override fun setProgress(progress: Int, animate: Boolean) {
        super.setProgress(applyIncrement(progress - mMin), animate)
    }

    @Synchronized
    override fun getProgress(): Int {
        return applyIncrement(super.getProgress() + mMin)
    }

    @Synchronized
    override fun setMin(min: Int) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            super.setMin(0)
        }
        mMin = min
        //todo perform something to update the progress ?
    }


    /**
     * Wrapper to allow for a listener to be set around the internal listener
     */
    override fun setOnSeekBarChangeListener(l: OnSeekBarChangeListener?) {
        mListener = l
    }

    fun setup(attrs: AttributeSet?) {
        getContext().obtainStyledAttributes(attrs, R.styleable.CustomSeekbar).use { attributes ->
            setIncrement(attributes.getInt(R.styleable.CustomSeekbar_seekBarIncrement, 1))
            val min = attributes.getInt(R.styleable.CustomSeekbar_android_min, 0)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                super.setMin(0)
            }
            setRange(min, super.getMax())
        }
        // Due to issues with negative progress when setting up the seekbar
        // We need to set a random progress to force the refresh of the thumb
        if (super.getProgress() == 0) {
            super.setProgress(super.getProgress() + 1)
            post(Runnable {
                super.setProgress(super.getProgress() - 1)
                post(Runnable { super.setOnSeekBarChangeListener(mInternalListener) })
            })
        } else {
            super.setOnSeekBarChangeListener(mInternalListener)
        }
    }

    /**
     * Apply increment to the progress
     * @param progress Progress to apply increment to
     * @return Progress with increment applied
     */
    private fun applyIncrement(progress: Int): Int {
        var progress = progress
        if (mIncrement < 1) return progress

        progress = progress / mIncrement
        progress = progress * mIncrement
        return progress
    }
}
