package net.kdt.pojavlaunch.prefs

import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences
import android.view.View
import android.view.ViewGroup
import android.widget.CompoundButton
import android.widget.SeekBar
import android.widget.Switch
import android.widget.TextView
import com.kdt.CustomSeekbar
import com.kdt.SideDialogView
import net.ashmeet.hyperlauncher.R
import net.kdt.pojavlaunch.Tools
import net.kdt.pojavlaunch.utils.interfaces.SimpleSeekBarListener

/**
 * Side dialog for quick settings that you can change in game
 * The implementation has to take action on some preference changes
 */
abstract class QuickSettingSideDialog(context: Context, parent: ViewGroup) :
    SideDialogView(context, parent, R.layout.dialog_quick_setting) {
    private var mEditor: SharedPreferences.Editor? = null

    @SuppressLint("UseSwitchCompatOrMaterialCode")
    private var mGyroSwitch: Switch? = null

    @SuppressLint("UseSwitchCompatOrMaterialCode")
    private var mGyroXSwitch: Switch? = null

    @SuppressLint("UseSwitchCompatOrMaterialCode")
    private var mGyroYSwitch: Switch? = null

    @SuppressLint("UseSwitchCompatOrMaterialCode")
    private var mGestureSwitch: Switch? = null
    private var mGyroSensitivityBar: CustomSeekbar? = null
    private var mMouseSpeedBar: CustomSeekbar? = null
    private var mGestureDelayBar: CustomSeekbar? = null
    private var mResolutionBar: CustomSeekbar? = null
    private var mGyroSensitivityText: TextView? = null
    private var mGyroSensitivityDisplayText: TextView? = null
    private var mMouseSpeedText: TextView? = null
    private var mGestureDelayText: TextView? = null
    private var mGestureDelayDisplayText: TextView? = null
    private var mResolutionText: TextView? = null

    private var mOriginalGyroEnabled = false
    private var mOriginalGyroXEnabled = false
    private var mOriginalGyroYEnabled = false
    private var mOriginalGestureDisabled = false
    private var mOriginalGyroSensitivity = 0f
    private var mOriginalMouseSpeed = 0f
    private var mOriginalResolution = 0f
    private var mOriginalGestureDelay = 0

    init {
        setTitle(R.string.quick_setting_title)
        setupCancelButton()
    }

    override fun onInflate() {
        bindLayout()
        Tools.runOnUiThread(Runnable {
            this.setupListeners()
            this.updateGyroCompatibility()
        })
    }

    override fun onDestroy() {
        removeListeners()
    }

    private fun bindLayout() {
        val content = mDialogContent ?: return
        // Bind layout elements
        mGyroSwitch = content.findViewById<Switch>(R.id.checkboxGyro)
        mGyroXSwitch = content.findViewById<Switch>(R.id.checkboxGyroX)
        mGyroYSwitch = content.findViewById<Switch>(R.id.checkboxGyroY)
        mGestureSwitch = content.findViewById<Switch>(R.id.checkboxGesture)

        mGyroSensitivityBar = content.findViewById<CustomSeekbar>(R.id.editGyro_seekbar)
        mMouseSpeedBar = content.findViewById<CustomSeekbar>(R.id.editMouseSpeed_seekbar)
        mGestureDelayBar = content.findViewById<CustomSeekbar>(R.id.editGestureDelay_seekbar)
        mResolutionBar = content.findViewById<CustomSeekbar>(R.id.editResolution_seekbar)

        mGyroSensitivityText = content.findViewById<TextView>(R.id.editGyro_textView_percent)
        mGyroSensitivityDisplayText = content.findViewById<TextView>(R.id.editGyro_textView)
        mMouseSpeedText =
            content.findViewById<TextView>(R.id.editMouseSpeed_textView_percent)
        mGestureDelayText =
            content.findViewById<TextView>(R.id.editGestureDelay_textView_percent)
        mGestureDelayDisplayText =
            content.findViewById<TextView>(R.id.editGestureDelay_textView)
        mResolutionText =
            content.findViewById<TextView>(R.id.editResolution_textView_percent)
    }

    private fun setupListeners() {
        mEditor = LauncherPreferences.DEFAULT_PREF?.edit()

        mOriginalGyroEnabled = LauncherPreferences.PREF_ENABLE_GYRO
        mOriginalGyroXEnabled = LauncherPreferences.PREF_GYRO_INVERT_X
        mOriginalGyroYEnabled = LauncherPreferences.PREF_GYRO_INVERT_Y
        mOriginalGestureDisabled = LauncherPreferences.PREF_DISABLE_GESTURES

        mOriginalGyroSensitivity = LauncherPreferences.PREF_GYRO_SENSITIVITY
        mOriginalMouseSpeed = LauncherPreferences.PREF_MOUSESPEED
        mOriginalGestureDelay = LauncherPreferences.PREF_LONGPRESS_TRIGGER
        mOriginalResolution = LauncherPreferences.PREF_SCALE_FACTOR

        mGyroSwitch!!.setChecked(mOriginalGyroEnabled)
        mGyroXSwitch!!.setChecked(mOriginalGyroXEnabled)
        mGyroYSwitch!!.setChecked(mOriginalGyroYEnabled)
        mGestureSwitch!!.setChecked(mOriginalGestureDisabled)

        mGyroSwitch!!.setOnCheckedChangeListener(CompoundButton.OnCheckedChangeListener { buttonView: CompoundButton?, isChecked: Boolean ->
            LauncherPreferences.PREF_ENABLE_GYRO = isChecked
            onGyroStateChanged()
            updateGyroVisibility(isChecked)
            mEditor?.putBoolean("enableGyro", isChecked)
        })

        mGyroXSwitch!!.setOnCheckedChangeListener(CompoundButton.OnCheckedChangeListener { buttonView: CompoundButton?, isChecked: Boolean ->
            LauncherPreferences.PREF_GYRO_INVERT_X = isChecked
            onGyroStateChanged()
            mEditor?.putBoolean("gyroInvertX", isChecked)
        })

        mGyroYSwitch!!.setOnCheckedChangeListener(CompoundButton.OnCheckedChangeListener { buttonView: CompoundButton?, isChecked: Boolean ->
            LauncherPreferences.PREF_GYRO_INVERT_Y = isChecked
            onGyroStateChanged()
            mEditor?.putBoolean("gyroInvertY", isChecked)
        })

        mGestureSwitch!!.setOnCheckedChangeListener(CompoundButton.OnCheckedChangeListener { buttonView: CompoundButton?, isChecked: Boolean ->
            LauncherPreferences.PREF_DISABLE_GESTURES = isChecked
            updateGestureVisibility(isChecked)
            mEditor?.putBoolean("disableGestures", isChecked)
        })

        mGyroSensitivityBar!!.setOnSeekBarChangeListener(object : SimpleSeekBarListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                LauncherPreferences.PREF_GYRO_SENSITIVITY = progress / 100f
                mEditor?.putInt("gyroSensitivity", progress)
                Companion.setSeekTextPercent(mGyroSensitivityText!!, progress)
            }
        })
        mGyroSensitivityBar!!.setProgress((mOriginalGyroSensitivity * 100f).toInt())
        Companion.setSeekTextPercent(mGyroSensitivityText!!, mGyroSensitivityBar!!.getProgress())

        mMouseSpeedBar!!.setOnSeekBarChangeListener(object : SimpleSeekBarListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                LauncherPreferences.PREF_MOUSESPEED = progress / 100f
                mEditor?.putInt("mousespeed", progress)
                Companion.setSeekTextPercent(mMouseSpeedText!!, progress)
            }
        })
        mMouseSpeedBar!!.setProgress((mOriginalMouseSpeed * 100f).toInt())
        Companion.setSeekTextPercent(mMouseSpeedText!!, mMouseSpeedBar!!.getProgress())

        mGestureDelayBar!!.setOnSeekBarChangeListener(object : SimpleSeekBarListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                LauncherPreferences.PREF_LONGPRESS_TRIGGER = progress
                mEditor?.putInt("timeLongPressTrigger", progress)
                Companion.setSeekTextMillisecond(mGestureDelayText!!, progress)
            }
        })
        mGestureDelayBar!!.setProgress(mOriginalGestureDelay)
        Companion.setSeekTextMillisecond(mGestureDelayText!!, mGestureDelayBar!!.getProgress())

        mResolutionBar!!.setOnSeekBarChangeListener(object : SimpleSeekBarListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                LauncherPreferences.PREF_SCALE_FACTOR = progress / 100f
                mEditor?.putInt("resolutionRatio", progress)
                Companion.setSeekTextPercent(mResolutionText!!, progress)
                onResolutionChanged()
            }
        })
        mResolutionBar!!.setProgress((mOriginalResolution * 100).toInt())
        Companion.setSeekTextPercent(mResolutionText!!, mResolutionBar!!.getProgress())


        updateGyroVisibility(mOriginalGyroEnabled)
        updateGestureVisibility(mOriginalGestureDisabled)
    }

    private fun updateGyroVisibility(isEnabled: Boolean) {
        val visibility = if (isEnabled) View.VISIBLE else View.GONE
        mGyroXSwitch!!.setVisibility(visibility)
        mGyroYSwitch!!.setVisibility(visibility)

        mGyroSensitivityBar!!.setVisibility(visibility)
        mGyroSensitivityText!!.setVisibility(visibility)
        mGyroSensitivityDisplayText!!.setVisibility(visibility)
    }

    private fun updateGyroCompatibility() {
        val content = mDialogContent ?: return
        val isGyroAvailable = Tools.deviceSupportsGyro(content.context)
        if (!isGyroAvailable) {
            mGyroSwitch!!.setVisibility(View.GONE)
            updateGestureVisibility(false)
        }
    }

    private fun updateGestureVisibility(isDisabled: Boolean) {
        val visibility = if (isDisabled) View.GONE else View.VISIBLE
        mGestureDelayBar!!.setVisibility(visibility)
        mGestureDelayText!!.setVisibility(visibility)
        mGestureDelayDisplayText!!.setVisibility(visibility)
    }

    private fun removeListeners() {
        mGyroSwitch!!.setOnCheckedChangeListener(null)
        mGyroXSwitch!!.setOnCheckedChangeListener(null)
        mGyroYSwitch!!.setOnCheckedChangeListener(null)
        mGestureSwitch!!.setOnCheckedChangeListener(null)

        mGyroSensitivityBar!!.setOnSeekBarChangeListener(null)
        mMouseSpeedBar!!.setOnSeekBarChangeListener(null)
        mGestureDelayBar!!.setOnSeekBarChangeListener(null)
        mResolutionBar!!.setOnSeekBarChangeListener(null)
    }

    private fun setupCancelButton() {
        setStartButtonListener(
            android.R.string.cancel,
            View.OnClickListener { v: View? -> cancel() })
        setEndButtonListener(android.R.string.ok, View.OnClickListener { v: View? ->
            mEditor!!.apply()
            disappear(true)
        })
        setCloseButtonListener(View.OnClickListener { v: View? -> cancel() })
    }

    /** Resets all settings to their original values  */
    fun cancel() {
        // Reset all settings if we were editing
        if (isDisplaying) {
            LauncherPreferences.PREF_ENABLE_GYRO = mOriginalGyroEnabled
            LauncherPreferences.PREF_GYRO_INVERT_X = mOriginalGyroXEnabled
            LauncherPreferences.PREF_GYRO_INVERT_Y = mOriginalGyroYEnabled
            LauncherPreferences.PREF_DISABLE_GESTURES = mOriginalGestureDisabled

            LauncherPreferences.PREF_GYRO_SENSITIVITY = mOriginalGyroSensitivity
            LauncherPreferences.PREF_MOUSESPEED = mOriginalMouseSpeed
            LauncherPreferences.PREF_LONGPRESS_TRIGGER = mOriginalGestureDelay
            LauncherPreferences.PREF_SCALE_FACTOR = mOriginalResolution

            onGyroStateChanged()
            onResolutionChanged()
        }

        disappear(true)
    }

    /** Called when the resolution is changed. Use [LauncherPreferences.PREF_SCALE_FACTOR]  */
    abstract fun onResolutionChanged()

    /** Called when the gyro state is changed.
     * Use [LauncherPreferences.PREF_ENABLE_GYRO]
     * Use [LauncherPreferences.PREF_GYRO_INVERT_X]
     * Use [LauncherPreferences.PREF_GYRO_INVERT_Y]
     */
    abstract fun onGyroStateChanged()

    companion object {
        private fun setSeekTextMillisecond(target: TextView, value: Int) {
            setSeekText(target, R.string.millisecond_format, value)
        }

        private fun setSeekTextPercent(target: TextView, value: Int) {
            setSeekText(target, R.string.percent_format, value)
        }

        private fun setSeekText(target: TextView, format: Int, value: Int) {
            target.setText(target.getContext().getString(format, value))
        }
    }
}
