package net.kdt.pojavlaunch.customcontrols.handleview

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.text.Editable
import android.util.Log
import android.view.View
import android.view.View.OnLayoutChangeListener
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.CheckBox
import android.widget.CompoundButton
import android.widget.EditText
import android.widget.SeekBar
import android.widget.Spinner
import android.widget.Switch
import android.widget.TextView
import com.kdt.SideDialogView
import net.ashmeet.hyperlauncher.R
import net.kdt.pojavlaunch.CustomControlsActivity
import net.kdt.pojavlaunch.EfficientAndroidLWJGLKeycode.generateKeyName
import net.kdt.pojavlaunch.EfficientAndroidLWJGLKeycode.getIndexByValue
import net.kdt.pojavlaunch.EfficientAndroidLWJGLKeycode.getValueByIndex
import net.kdt.pojavlaunch.Tools.isValidString
import net.kdt.pojavlaunch.Tools.runOnUiThread
import net.kdt.pojavlaunch.Tools.showError
import net.kdt.pojavlaunch.colorselector.ColorSelectionListener
import net.kdt.pojavlaunch.colorselector.ColorSelector
import net.kdt.pojavlaunch.customcontrols.ControlData
import net.kdt.pojavlaunch.customcontrols.ControlDrawerData
import net.kdt.pojavlaunch.customcontrols.ControlJoystickData
import net.kdt.pojavlaunch.customcontrols.buttons.ControlDrawer
import net.kdt.pojavlaunch.customcontrols.buttons.ControlInterface
import net.kdt.pojavlaunch.utils.CropperUtils.CropperReceiver
import net.kdt.pojavlaunch.utils.interfaces.SimpleItemSelectedListener
import net.kdt.pojavlaunch.utils.interfaces.SimpleSeekBarListener
import net.kdt.pojavlaunch.utils.interfaces.SimpleTextWatcher
import kotlin.math.abs
import kotlin.math.max

class EditControlSideDialog(context: Context, private val mParent: ViewGroup) : SideDialogView(
    context,
    mParent, R.layout.dialog_control_button_setting
) {
    private val mKeycodeSpinners: Array<Spinner?> = arrayOfNulls(4)
    var internalChanges: Boolean = false // True when we programmatically change stuff.
    private val mLayoutChangedListener: OnLayoutChangeListener = object : OnLayoutChangeListener {
        override fun onLayoutChange(
            v: View?,
            left: Int,
            top: Int,
            right: Int,
            bottom: Int,
            oldLeft: Int,
            oldTop: Int,
            oldRight: Int,
            oldBottom: Int
        ) {
            if (internalChanges) return

            internalChanges = true
            val width = (safeParseFloat(mWidthEditText!!.getText().toString())).toInt()

            if (width >= 0 && abs(right - width) > 1) {
                mWidthEditText!!.setText((right - left).toString())
            }
            val height = (safeParseFloat(mHeightEditText!!.getText().toString())).toInt()
            if (height >= 0 && abs(bottom - height) > 1) {
                mHeightEditText!!.setText((bottom - top).toString())
            }

            internalChanges = false
        }
    }
    private var mNameEditText: EditText? = null
    private var mWidthEditText: EditText? = null
    private var mHeightEditText: EditText? = null

    @SuppressLint("UseSwitchCompatOrMaterialCode")
    private var mToggleSwitch: Switch? = null

    @SuppressLint("UseSwitchCompatOrMaterialCode")
    private var mPassthroughSwitch: Switch? = null

    @SuppressLint("UseSwitchCompatOrMaterialCode")
    private var mSwipeableSwitch: Switch? = null

    @SuppressLint("UseSwitchCompatOrMaterialCode")
    private var mForwardLockSwitch: Switch? = null

    @SuppressLint("UseSwitchCompatOrMaterialCode")
    private var mAbsoluteTrackingSwitch: Switch? = null
    private var mOrientationSpinner: Spinner? = null
    private val mKeycodeTextviews = arrayOfNulls<TextView>(4)
    private var mStrokeWidthSeekbar: SeekBar? = null
    private var mCornerRadiusSeekbar: SeekBar? = null
    private var mAlphaSeekbar: SeekBar? = null
    private var mStrokePercentTextView: TextView? = null
    private var mCornerRadiusPercentTextView: TextView? = null
    private var mAlphaPercentTextView: TextView? = null
    private var mSelectBackgroundBitmap: TextView? = null
    private var mSelectBackgroundColor: TextView? = null
    private var mSelectStrokeColor: TextView? = null
    private var mAdapter: ArrayAdapter<String?>? = null
    private var mSpecialArray: MutableList<String?>? = null
    private var mDisplayInGameCheckbox: CheckBox? = null
    private var mDisplayInMenuCheckbox: CheckBox? = null
    private var mCurrentlyEditedButton: ControlInterface? = null

    // Decorative textviews
    private var mOrientationTextView: TextView? = null
    private var mMappingTextView: TextView? = null
    private var mNameTextView: TextView? = null
    private var mCornerRadiusTextView: TextView? = null
    private var mVisibilityTextView: TextView? = null
    private var mSizeTextview: TextView? = null
    private var mSizeXTextView: TextView? = null
    private var mStrokeWidthTextView: TextView? = null
    private var mColorSelectWarningTextView: TextView? = null

    // Color selector related stuff
    private var mColorSelector: ColorSelector? = null

    override fun onInflate() {
        bindLayout()
        buildColorSelector()
        loadAdapter()
        setupRealTimeListeners()
    }

    override fun onDestroy() {
        mColorSelector?.disappear(true)
    }

    private fun buildColorSelector() {
        mColorSelector = ColorSelector(mParent.getContext(), mParent, null)
    }

    /**
     * Slide the layout into the visible screen area
     */
    fun appearColor(fromRight: Boolean, color: Int) {
        mColorSelector!!.show(fromRight, if (color == -1) Color.WHITE else color)
    }

    /**
     * Slide out the layout
     */
    fun disappearColor() {
        mColorSelector!!.disappear(false)
    }

    /**
     * Slide out the first visible layer.
     * 
     * @return True if the last layer is disappearing
     */
    fun disappearLayer(): Boolean {
        if (mColorSelector?.isDisplaying == true) {
            disappearColor()
            return false
        } else {
            disappear(false)
            return true
        }
    }

    /**
     * Switch the panels position if needed
     */
    fun adaptPanelPosition() {
        if (!isDisplaying) return
        if (mCurrentlyEditedButton == null) return
        val parent = mCurrentlyEditedButton!!.controlLayoutParent
        if (parent == null) return

        val controlView = mCurrentlyEditedButton!!.controlView ?: return
        val isAtRight = controlView.x + controlView.width / 2f < parent.width / 2f
        appear(isAtRight)
        if (mColorSelector?.isDisplaying == true) {
            runOnUiThread(Runnable {
                appearColor(
                    isAtRight,
                    mCurrentlyEditedButton!!.properties?.bgColor ?: Color.WHITE
                )
            })
        }
    }

    /* LOADING VALUES */
    /**
     * Load values for basic control data
     */
    fun loadValues(data: ControlData) {
        setDefaultVisibilitySetting()
        mOrientationTextView!!.setVisibility(View.GONE)
        mOrientationSpinner!!.setVisibility(View.GONE)
        mForwardLockSwitch!!.setVisibility(View.GONE)
        mAbsoluteTrackingSwitch!!.setVisibility(View.GONE)

        mNameEditText!!.setText(data.name)
        mWidthEditText!!.setText(data.getWidth().toString())
        mHeightEditText!!.setText(data.getHeight().toString())

        mAlphaSeekbar!!.setProgress((data.opacity * 100).toInt())
        mStrokeWidthSeekbar!!.setProgress(data.strokeWidth.toInt() * 10)
        mCornerRadiusSeekbar!!.setProgress(data.cornerRadius.toInt())

        Companion.setPercentageText(mAlphaPercentTextView!!, (data.opacity * 100).toInt())
        Companion.setPercentageText(mStrokePercentTextView!!, data.strokeWidth.toInt() * 10)
        Companion.setPercentageText(mCornerRadiusPercentTextView!!, data.cornerRadius.toInt())

        mToggleSwitch!!.setChecked(data.isToggle)
        mPassthroughSwitch!!.setChecked(data.passThruEnabled)
        mSwipeableSwitch!!.setChecked(data.isSwipeable)

        mDisplayInGameCheckbox!!.setChecked(data.displayInGame)
        mDisplayInMenuCheckbox!!.setChecked(data.displayInMenu)

        for (i in data.keycodes.indices) {
            if (data.keycodes[i] < 0) {
                mKeycodeSpinners[i]?.setSelection(data.keycodes[i] + mSpecialArray!!.size)
            } else {
                mKeycodeSpinners[i]?.setSelection(getIndexByValue(data.keycodes[i]) + mSpecialArray!!.size)
            }
        }

        setHasBitmap(isValidString(data.bitmapTag))

        val viewContext = mCurrentlyEditedButton?.controlView?.context

        // Don't allow editing the bitmap in-game (i don't want to bother with implementing that,
        // and it has potential to kill the game during icon selection)
        if (viewContext !is CustomControlsActivity) mSelectBackgroundBitmap!!.setVisibility(View.GONE)
    }

    /**
     * Load values for extended control data
     */
    fun loadValues(data: ControlDrawerData) {
        loadValues(data.properties)

        mOrientationSpinner!!.setSelection(
            ControlDrawerData.Companion.orientationToInt(data.orientation ?: ControlDrawerData.Orientation.LEFT)
        )

        mMappingTextView!!.setVisibility(View.GONE)
        for (i in mKeycodeSpinners.indices) {
            mKeycodeSpinners[i]?.setVisibility(View.GONE)
            mKeycodeTextviews[i]!!.setVisibility(View.GONE)
        }

        mOrientationTextView!!.setVisibility(View.VISIBLE)
        mOrientationSpinner!!.setVisibility(View.VISIBLE)

        mSwipeableSwitch!!.setVisibility(View.GONE)
        mPassthroughSwitch!!.setVisibility(View.GONE)
        mToggleSwitch!!.setVisibility(View.GONE)
    }

    /**
     * Load values for the joystick
     */
    fun loadJoystickValues(data: ControlJoystickData) {
        loadValues(data)

        mMappingTextView!!.setVisibility(View.GONE)
        for (i in mKeycodeSpinners.indices) {
            mKeycodeSpinners[i]?.setVisibility(View.GONE)
            mKeycodeTextviews[i]!!.setVisibility(View.GONE)
        }

        mNameTextView!!.setVisibility(View.GONE)
        mNameEditText!!.setVisibility(View.GONE)

        mCornerRadiusTextView!!.setVisibility(View.GONE)
        mCornerRadiusSeekbar!!.setVisibility(View.GONE)
        mCornerRadiusPercentTextView!!.setVisibility(View.GONE)

        mSwipeableSwitch!!.setVisibility(View.GONE)
        mPassthroughSwitch!!.setVisibility(View.GONE)
        mToggleSwitch!!.setVisibility(View.GONE)

        mForwardLockSwitch!!.setVisibility(View.VISIBLE)
        mForwardLockSwitch!!.setChecked(data.forwardLock)

        mAbsoluteTrackingSwitch!!.setVisibility(View.VISIBLE)
        mAbsoluteTrackingSwitch!!.setChecked(data.absolute)

        mSelectBackgroundBitmap!!.setVisibility(View.GONE)
    }

    /**
     * Load values for sub buttons
     */
    fun loadSubButtonValues(data: ControlData, drawerOrientation: ControlDrawerData.Orientation?) {
        loadValues(data)

        // Size linked to the parent drawer depending on the drawer settings
        if (drawerOrientation != ControlDrawerData.Orientation.FREE) {
            mSizeTextview!!.setVisibility(View.GONE)
            mSizeXTextView!!.setVisibility(View.GONE)
            mWidthEditText!!.setVisibility(View.GONE)
            mHeightEditText!!.setVisibility(View.GONE)
        }

        // No conditional, already depends on the parent drawer visibility
        mVisibilityTextView!!.setVisibility(View.GONE)
        mDisplayInMenuCheckbox!!.setVisibility(View.GONE)
        mDisplayInGameCheckbox!!.setVisibility(View.GONE)
    }

    private fun loadAdapter() {
        //Initialize adapter for keycodes
        mAdapter =
            ArrayAdapter<String?>(mDialogContent?.context ?: mParent.context, R.layout.item_centered_textview)
        mSpecialArray = ControlData.Companion.buildSpecialButtonArray()

        mAdapter!!.addAll(mSpecialArray!!)
        mAdapter!!.addAll(*generateKeyName())
        mAdapter!!.setDropDownViewResource(android.R.layout.simple_list_item_single_choice)

        for (spinner in mKeycodeSpinners) {
            spinner?.adapter = mAdapter
        }

        // Orientation spinner
        val adapter = ArrayAdapter<ControlDrawerData.Orientation?>(
            mDialogContent?.context ?: mParent.context,
            android.R.layout.simple_spinner_item
        )
        adapter.addAll(*ControlDrawerData.Companion.orientations)
        adapter.setDropDownViewResource(android.R.layout.simple_list_item_single_choice)

        mOrientationSpinner!!.setAdapter(adapter)
    }

    private fun setDefaultVisibilitySetting() {
        for (i in 0..<(mDialogContent as ViewGroup).getChildCount()) {
            (mDialogContent as ViewGroup).getChildAt(i).setVisibility(View.VISIBLE)
        }
        for (s in mKeycodeSpinners) {
            s?.visibility = View.INVISIBLE
        }
        mColorSelectWarningTextView!!.setVisibility(View.GONE)
    }

    private fun setHasBitmap(hasBitmap: Boolean) {
        val visibility = if (!hasBitmap) View.VISIBLE else View.GONE
        val visibilityOpposite = if (hasBitmap) View.VISIBLE else View.GONE

        // Disable all settings not available in bitmap background mode
        mSelectStrokeColor!!.setVisibility(visibility)
        mStrokePercentTextView!!.setVisibility(visibility)
        mStrokeWidthSeekbar!!.setVisibility(visibility)
        mCornerRadiusSeekbar!!.setVisibility(visibility)
        mCornerRadiusPercentTextView!!.setVisibility(visibility)
        mCornerRadiusTextView!!.setVisibility(visibility)
        mStrokeWidthTextView!!.setVisibility(visibility)

        // Show the warning that will notify the user that color selection will reset the bitmap
        mColorSelectWarningTextView!!.setVisibility(visibilityOpposite)
    }

    private fun bindLayout() {
        mNameEditText = mDialogContent?.findViewById<EditText>(R.id.editName_editText)
        mWidthEditText = mDialogContent?.findViewById<EditText>(R.id.editSize_editTextX)
        mHeightEditText = mDialogContent?.findViewById<EditText>(R.id.editSize_editTextY)
        mToggleSwitch = mDialogContent?.findViewById<Switch>(R.id.checkboxToggle)
        mPassthroughSwitch = mDialogContent?.findViewById<Switch>(R.id.checkboxPassThrough)
        mSwipeableSwitch = mDialogContent?.findViewById<Switch>(R.id.checkboxSwipeable)
        mForwardLockSwitch = mDialogContent?.findViewById<Switch>(R.id.checkboxForwardLock)
        mAbsoluteTrackingSwitch =
            mDialogContent?.findViewById<Switch>(R.id.checkboxAbsoluteFingerTracking)
        mKeycodeSpinners[0] = mDialogContent?.findViewById<Spinner>(R.id.editMapping_spinner_1)
        mKeycodeSpinners[1] = mDialogContent?.findViewById<Spinner>(R.id.editMapping_spinner_2)
        mKeycodeSpinners[2] = mDialogContent?.findViewById<Spinner>(R.id.editMapping_spinner_3)
        mKeycodeSpinners[3] = mDialogContent?.findViewById<Spinner>(R.id.editMapping_spinner_4)
        mKeycodeTextviews[0] = mDialogContent?.findViewById<TextView>(R.id.mapping_1_textview)
        mKeycodeTextviews[1] = mDialogContent?.findViewById<TextView>(R.id.mapping_2_textview)
        mKeycodeTextviews[2] = mDialogContent?.findViewById<TextView>(R.id.mapping_3_textview)
        mKeycodeTextviews[3] = mDialogContent?.findViewById<TextView>(R.id.mapping_4_textview)
        mOrientationSpinner = mDialogContent?.findViewById<Spinner>(R.id.editOrientation_spinner)
        mStrokeWidthSeekbar = mDialogContent?.findViewById<SeekBar>(R.id.editStrokeWidth_seekbar)
        mCornerRadiusSeekbar = mDialogContent?.findViewById<SeekBar>(R.id.editCornerRadius_seekbar)
        mAlphaSeekbar = mDialogContent?.findViewById<SeekBar>(R.id.editButtonOpacity_seekbar)
        mSelectBackgroundBitmap =
            mDialogContent?.findViewById<TextView>(R.id.setBackgroundBitmap_textView)
        mSelectBackgroundColor =
            mDialogContent?.findViewById<TextView>(R.id.editBackgroundColor_textView)
        mSelectStrokeColor = mDialogContent?.findViewById<TextView>(R.id.editStrokeColor_textView)
        mStrokePercentTextView =
            mDialogContent?.findViewById<TextView>(R.id.editStrokeWidth_textView_percent)
        mAlphaPercentTextView =
            mDialogContent?.findViewById<TextView>(R.id.editButtonOpacity_textView_percent)
        mCornerRadiusPercentTextView =
            mDialogContent?.findViewById<TextView>(R.id.editCornerRadius_textView_percent)
        mDisplayInGameCheckbox =
            mDialogContent?.findViewById<CheckBox>(R.id.visibility_game_checkbox)
        mDisplayInMenuCheckbox =
            mDialogContent?.findViewById<CheckBox>(R.id.visibility_menu_checkbox)

        //Decorative stuff
        mMappingTextView = mDialogContent?.findViewById<TextView>(R.id.editMapping_textView)
        mOrientationTextView = mDialogContent?.findViewById<TextView>(R.id.editOrientation_textView)
        mNameTextView = mDialogContent?.findViewById<TextView>(R.id.editName_textView)
        mCornerRadiusTextView =
            mDialogContent?.findViewById<TextView>(R.id.editCornerRadius_textView)
        mVisibilityTextView = mDialogContent?.findViewById<TextView>(R.id.visibility_textview)
        mSizeTextview = mDialogContent?.findViewById<TextView>(R.id.editSize_textView)
        mSizeXTextView = mDialogContent?.findViewById<TextView>(R.id.editSize_x_textView)
        mStrokeWidthTextView = mDialogContent?.findViewById<TextView>(R.id.editStrokeWidth_textView)
        mColorSelectWarningTextView =
            mDialogContent?.findViewById<TextView>(R.id.editBackgroundColorWarning_textView)
    }

    private fun removeBitmap(button: ControlInterface) {
        val storage = button.controlLayoutParent?.bitmaps ?: return
        val properties = button.properties ?: return
        storage.putBitmap(null, properties.bitmapTag)
        properties.bitmapTag = null
        setHasBitmap(false)
    }

    /**
     * A long function linking all the displayed data on the popup and,
     * the currently edited mCurrentlyEditedButton
     * @noinspection SuspiciousNameCombination
     */
    private fun setupRealTimeListeners() {
        mNameEditText!!.addTextChangedListener(object : SimpleTextWatcher {
            override fun afterTextChanged(s: Editable?) {
                if (internalChanges) return
                val button = mCurrentlyEditedButton ?: return
                val properties = button.properties ?: return
                properties.name = s?.toString().orEmpty()
                button.setProperties(properties, false)
            }
        })

        mWidthEditText!!.addTextChangedListener(object : SimpleTextWatcher {
            override fun afterTextChanged(s: Editable?) {
                if (internalChanges) return
                internalChanges = true
                val width = safeParseFloat(s?.toString().orEmpty())
                val button = mCurrentlyEditedButton
                val properties = button?.properties
                if (width >= 0 && properties != null) {
                    properties.width = width
                    if (properties is ControlJoystickData) {
                        properties.height = width
                    }
                    button.updateProperties()
                }
                button?.controlView?.post { internalChanges = false }
            }
        })

        mHeightEditText!!.addTextChangedListener(object : SimpleTextWatcher {
            override fun afterTextChanged(s: Editable?) {
                if (internalChanges) return
                internalChanges = true
                val height = safeParseFloat(s?.toString().orEmpty())
                val button = mCurrentlyEditedButton
                val properties = button?.properties
                if (height >= 0 && properties != null) {
                    properties.height = height
                    if (properties is ControlJoystickData) {
                        properties.width = height
                    }
                    button.updateProperties()
                }
                button?.controlView?.post { internalChanges = false }
            }
        })

        mSwipeableSwitch!!.setOnCheckedChangeListener { _, isChecked ->
            if (internalChanges) return@setOnCheckedChangeListener
            mCurrentlyEditedButton?.properties?.isSwipeable = isChecked
        }
        mToggleSwitch!!.setOnCheckedChangeListener { _, isChecked ->
            if (internalChanges) return@setOnCheckedChangeListener
            mCurrentlyEditedButton?.properties?.isToggle = isChecked
        }
        mPassthroughSwitch!!.setOnCheckedChangeListener { _, isChecked ->
            if (internalChanges) return@setOnCheckedChangeListener
            mCurrentlyEditedButton?.properties?.passThruEnabled = isChecked
        }
        mForwardLockSwitch!!.setOnCheckedChangeListener { _, isChecked ->
            if (internalChanges) return@setOnCheckedChangeListener
            (mCurrentlyEditedButton?.properties as? ControlJoystickData)?.forwardLock = isChecked
        }
        mAbsoluteTrackingSwitch!!.setOnCheckedChangeListener { _, isChecked ->
            if (internalChanges) return@setOnCheckedChangeListener
            (mCurrentlyEditedButton?.properties as? ControlJoystickData)?.absolute = isChecked
        }

        mAlphaSeekbar!!.setOnSeekBarChangeListener(object : SimpleSeekBarListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (internalChanges) return
                mCurrentlyEditedButton?.properties?.opacity = mAlphaSeekbar!!.progress / 100f
                mCurrentlyEditedButton?.controlView?.alpha = mAlphaSeekbar!!.progress / 100f
                Companion.setPercentageText(mAlphaPercentTextView!!, progress)
            }
        })

        mStrokeWidthSeekbar!!.setOnSeekBarChangeListener(object : SimpleSeekBarListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (internalChanges) return
                mCurrentlyEditedButton?.properties?.strokeWidth = mStrokeWidthSeekbar!!.progress / 10f
                mCurrentlyEditedButton?.setBackground()
                Companion.setPercentageText(mStrokePercentTextView!!, progress)
            }
        })

        mCornerRadiusSeekbar!!.setOnSeekBarChangeListener(object : SimpleSeekBarListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (internalChanges) return
                mCurrentlyEditedButton?.properties?.cornerRadius = mCornerRadiusSeekbar!!.progress.toFloat()
                mCurrentlyEditedButton?.setBackground()
                Companion.setPercentageText(mCornerRadiusPercentTextView!!, progress)
            }
        })


        for (i in mKeycodeSpinners.indices) {
            val finalI = i
            mKeycodeTextviews[i]!!.setOnClickListener { mKeycodeSpinners[finalI]?.performClick() }

            mKeycodeSpinners[i]?.onItemSelectedListener = object : SimpleItemSelectedListener {
                override fun onItemSelected(
                    parent: AdapterView<*>?,
                    view: View?,
                    position: Int,
                    id: Long
                ) {
                    val button = mCurrentlyEditedButton ?: return
                    val properties = button.properties ?: return
                    val specialSize = mSpecialArray?.size ?: 0
                    val spinner = mKeycodeSpinners[finalI] ?: return

                    properties.keycodes[finalI] = if (position < specialSize) {
                        spinner.selectedItemPosition - specialSize
                    } else {
                        getValueByIndex(spinner.selectedItemPosition - specialSize).toInt()
                    }
                    mKeycodeTextviews[finalI]!!.text = spinner.selectedItem as String?
                }
            }
        }


        mOrientationSpinner!!.onItemSelectedListener = object : SimpleItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long
            ) {
                val drawer = mCurrentlyEditedButton as? ControlDrawer ?: return
                drawer.drawerData.orientation =
                    ControlDrawerData.Companion.intToOrientation(mOrientationSpinner!!.selectedItemPosition)
                drawer.syncButtons()
            }
        }

        mDisplayInGameCheckbox!!.setOnCheckedChangeListener { _, isChecked ->
            if (internalChanges) return@setOnCheckedChangeListener
            mCurrentlyEditedButton?.properties?.displayInGame = isChecked
        }

        mDisplayInMenuCheckbox!!.setOnCheckedChangeListener { _, isChecked ->
            if (internalChanges) return@setOnCheckedChangeListener
            mCurrentlyEditedButton?.properties?.displayInMenu = isChecked
        }

        mSelectStrokeColor!!.setOnClickListener {
            mColorSelector!!.setAlphaEnabled(false)
            mColorSelector!!.setColorSelectionListener(object : ColorSelectionListener {
                override fun onColorSelected(color: Int) {
                    val button = mCurrentlyEditedButton ?: return
                    val properties = button.properties ?: return
                    removeBitmap(button)
                    properties.strokeColor = color
                    button.setBackground()
                }
            })
            val strokeColor = mCurrentlyEditedButton?.properties?.strokeColor ?: Color.WHITE
            appearColor(isAtRight, strokeColor)
        }

        mSelectBackgroundBitmap!!.setOnClickListener {
            val mTargetView = mCurrentlyEditedButton?.controlView ?: return@setOnClickListener
            val receiver: CropperReceiver = object : CropperReceiver {
                override val aspectRatio: Float
                    get() = mTargetView.width.toFloat() / mTargetView.height

                override val targetMaxSide: Int
                    get() = max(mTargetView.width, mTargetView.height)

                override fun onCropped(contentBitmap: Bitmap?) {
                    val button = mCurrentlyEditedButton ?: return
                    val buttonProperties = button.properties ?: return
                    val storage = button.controlLayoutParent?.bitmaps ?: return
                    val oldTag = buttonProperties.bitmapTag
                    buttonProperties.bitmapTag = storage.putBitmap(contentBitmap, oldTag)
                    setHasBitmap(true)
                    button.setBackground()
                }

                override fun onFailed(exception: Exception?) {
                    showError(mTargetView.context, exception ?: RuntimeException("Cropping failed"))
                }
            }
            val context = mTargetView.context
            if (context is CustomControlsActivity) {
                context.startCropping(receiver)
            }
        }

        mSelectBackgroundColor!!.setOnClickListener {
            mColorSelector!!.setAlphaEnabled(true)
            mColorSelector!!.setColorSelectionListener(object : ColorSelectionListener {
                override fun onColorSelected(color: Int) {
                    val button = mCurrentlyEditedButton ?: return
                    val properties = button.properties ?: return
                    removeBitmap(button)
                    properties.bgColor = color
                    button.setBackground()
                }
            })
            val bgColor = mCurrentlyEditedButton?.properties?.bgColor ?: Color.WHITE
            appearColor(isAtRight, bgColor)
        }
    }

    private fun safeParseFloat(string: String): Float {
        var out = -1f // -1
        try {
            out = string.toFloat()
        } catch (e: NumberFormatException) {
            Log.e("EditControlPopup", e.toString())
        }
        return out
    }

    fun setCurrentlyEditedButton(button: ControlInterface?) {
        mCurrentlyEditedButton?.controlView?.removeOnLayoutChangeListener(mLayoutChangedListener)
        mCurrentlyEditedButton = button
        mCurrentlyEditedButton?.controlView?.addOnLayoutChangeListener(mLayoutChangedListener)
    }

    companion object {
        fun setPercentageText(textView: TextView, progress: Int) {
            textView.setText(textView.getContext().getString(R.string.percent_format, progress))
        }
    }
}
