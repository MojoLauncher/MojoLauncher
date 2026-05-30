package net.kdt.pojavlaunch.customcontrols.gamepad

import android.content.Context
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.CompoundButton
import android.widget.ImageView
import android.widget.Spinner
import android.widget.TextView
import androidx.appcompat.widget.SwitchCompat
import androidx.recyclerview.widget.RecyclerView
import net.ashmeet.hyperlauncher.R
import net.kdt.pojavlaunch.EfficientAndroidLWJGLKeycode.generateKeyName
import net.kdt.pojavlaunch.EfficientAndroidLWJGLKeycode.getIndexByValue
import net.kdt.pojavlaunch.EfficientAndroidLWJGLKeycode.getValueByIndex
import net.kdt.pojavlaunch.GrabListener
import net.kdt.pojavlaunch.Tools.showError

class GamepadMapperAdapter(context: Context) :
    RecyclerView.Adapter<GamepadMapperAdapter.ViewHolder>(), GamepadDataProvider {
    private var mSimulatedGamepadMap: GamepadMap? = null
    private lateinit var mRebinderButtons: Array<RebinderButton?>
    private lateinit var mRealButtons: Array<GamepadEmulatedButton?>
    private val mKeyAdapter: ArrayAdapter<String?>
    private val mSpecialKeycodeCount: Int
    private var mGamepadGrabListener: GrabListener? = null
    private var mGrabState = false
    private var mOldState = false

    init {
        GamepadMapStore.Companion.load()
        mKeyAdapter = ArrayAdapter<String?>(context, R.layout.item_centered_textview_large)
        val specialKeycodeNames: Array<String> = GamepadMap.Companion.specialKeycodeNames
        mSpecialKeycodeCount = specialKeycodeNames.size
        mKeyAdapter.addAll(*specialKeycodeNames)
        mKeyAdapter.addAll(*generateKeyName())
        createRebinderMap()
        updateRealButtons()
    }

    private fun createRebinderMap() {
        mRebinderButtons = arrayOfNulls<RebinderButton>(BUTTON_COUNT)
        mRealButtons = arrayOfNulls<GamepadEmulatedButton>(BUTTON_COUNT)
        mSimulatedGamepadMap = GamepadMap()
        var index = 0
        mRebinderButtons[index++] =
            RebinderButton(R.drawable.button_a, R.string.controller_button_a)
        mSimulatedGamepadMap!!.BUTTON_A = mRebinderButtons[index-1]
        mRebinderButtons[index++] =
            RebinderButton(R.drawable.button_b, R.string.controller_button_b)
        mSimulatedGamepadMap!!.BUTTON_B = mRebinderButtons[index-1]
        mRebinderButtons[index++] =
            RebinderButton(R.drawable.button_x, R.string.controller_button_x)
        mSimulatedGamepadMap!!.BUTTON_X = mRebinderButtons[index-1]
        mRebinderButtons[index++] =
            RebinderButton(R.drawable.button_y, R.string.controller_button_y)
        mSimulatedGamepadMap!!.BUTTON_Y = mRebinderButtons[index-1]
        mRebinderButtons[index++] =
            RebinderButton(R.drawable.button_start, R.string.controller_button_start)
        mSimulatedGamepadMap!!.BUTTON_START = mRebinderButtons[index-1]
        mRebinderButtons[index++] =
            RebinderButton(R.drawable.button_select, R.string.controller_button_select)
        mSimulatedGamepadMap!!.BUTTON_SELECT = mRebinderButtons[index-1]
        mRebinderButtons[index++] =
            RebinderButton(R.drawable.trigger_right, R.string.controller_button_trigger_right)
        mSimulatedGamepadMap!!.TRIGGER_RIGHT = mRebinderButtons[index-1]
        mRebinderButtons[index++] =
            RebinderButton(R.drawable.trigger_left, R.string.controller_button_trigger_left)
        mSimulatedGamepadMap!!.TRIGGER_LEFT = mRebinderButtons[index-1]
        mRebinderButtons[index++] =
            RebinderButton(R.drawable.shoulder_right, R.string.controller_button_shoulder_right)
        mSimulatedGamepadMap!!.SHOULDER_RIGHT = mRebinderButtons[index-1]
        mRebinderButtons[index++] =
            RebinderButton(R.drawable.shoulder_left, R.string.controller_button_shoulder_left)
        mSimulatedGamepadMap!!.SHOULDER_LEFT = mRebinderButtons[index-1]
        mRebinderButtons[index++] =
            RebinderButton(R.drawable.stick_right, R.string.controller_direction_forward)
        mSimulatedGamepadMap!!.DIRECTION_FORWARD = mRebinderButtons[index-1]
        mRebinderButtons[index++] =
            RebinderButton(R.drawable.stick_right, R.string.controller_direction_right)
        mSimulatedGamepadMap!!.DIRECTION_RIGHT = mRebinderButtons[index-1]
        mRebinderButtons[index++] =
            RebinderButton(R.drawable.stick_right, R.string.controller_direction_left)
        mSimulatedGamepadMap!!.DIRECTION_LEFT = mRebinderButtons[index-1]
        mRebinderButtons[index++] =
            RebinderButton(R.drawable.stick_right, R.string.controller_direction_backward)
        mSimulatedGamepadMap!!.DIRECTION_BACKWARD = mRebinderButtons[index-1]
        mRebinderButtons[index++] =
            RebinderButton(R.drawable.stick_right_click, R.string.controller_stick_press_r)
        mSimulatedGamepadMap!!.THUMBSTICK_RIGHT = mRebinderButtons[index-1]
        mRebinderButtons[index++] =
            RebinderButton(R.drawable.stick_left_click, R.string.controller_stick_press_l)
        mSimulatedGamepadMap!!.THUMBSTICK_LEFT = mRebinderButtons[index-1]
    }

    private fun updateRealButtons() {
        val currentRealMap: GamepadMap =
            if (mGrabState) GamepadMapStore.Companion.gameMap!! else GamepadMapStore.Companion.menuMap!!
        var index = 0
        mRealButtons[index++] = currentRealMap.BUTTON_A
        mRealButtons[index++] = currentRealMap.BUTTON_B
        mRealButtons[index++] = currentRealMap.BUTTON_X
        mRealButtons[index++] = currentRealMap.BUTTON_Y
        mRealButtons[index++] = currentRealMap.BUTTON_START
        mRealButtons[index++] = currentRealMap.BUTTON_SELECT
        mRealButtons[index++] = currentRealMap.TRIGGER_RIGHT
        mRealButtons[index++] = currentRealMap.TRIGGER_LEFT
        mRealButtons[index++] = currentRealMap.SHOULDER_RIGHT
        mRealButtons[index++] = currentRealMap.SHOULDER_LEFT
        mRealButtons[index++] = currentRealMap.DIRECTION_FORWARD
        mRealButtons[index++] = currentRealMap.DIRECTION_RIGHT
        mRealButtons[index++] = currentRealMap.DIRECTION_LEFT
        mRealButtons[index++] = currentRealMap.DIRECTION_BACKWARD
        mRealButtons[index++] = currentRealMap.THUMBSTICK_RIGHT
        mRealButtons[index++] = currentRealMap.THUMBSTICK_LEFT
        mRealButtons[index++] = currentRealMap.DPAD_UP
        mRealButtons[index++] = currentRealMap.DPAD_DOWN
        mRealButtons[index++] = currentRealMap.DPAD_RIGHT
        mRealButtons[index] = currentRealMap.DPAD_LEFT
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val layoutInflater = LayoutInflater.from(parent.getContext())
        val view = layoutInflater.inflate(R.layout.item_controller_mapping, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.attach(position)
    }

    override fun onViewRecycled(holder: ViewHolder) {
        super.onViewRecycled(holder)
        holder.detach()
    }

    override fun getItemCount(): Int {
        return mRebinderButtons.size
    }

    private fun updateStickIcons() {
        // Which stick is used for keyboard emulation depends on grab state, so we need
        // to update the mapper UI icons accordingly
        val stickIcon = if (mGrabState) R.drawable.stick_left else R.drawable.stick_right
        (mSimulatedGamepadMap!!.DIRECTION_FORWARD as? RebinderButton)?.iconResourceId = stickIcon
        (mSimulatedGamepadMap!!.DIRECTION_BACKWARD as? RebinderButton)?.iconResourceId = stickIcon
        (mSimulatedGamepadMap!!.DIRECTION_RIGHT as? RebinderButton)?.iconResourceId = stickIcon
        (mSimulatedGamepadMap!!.DIRECTION_LEFT as? RebinderButton)?.iconResourceId = stickIcon
    }

    private class RebinderButton(var iconResourceId: Int, val localeResourceId: Int) :
        GamepadButton() {
        private var mButtonHolder: ViewHolder? = null

        fun changeViewHolder(viewHolder: ViewHolder?) {
            mButtonHolder = viewHolder
            mButtonHolder?.setPressed(mIsDown)
        }

        override fun onDownStateChanged(isDown: Boolean) {
            val holder = mButtonHolder ?: return
            holder.setPressed(isDown)
        }
    }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView),
        AdapterView.OnItemSelectedListener, View.OnClickListener,
        CompoundButton.OnCheckedChangeListener {
        private val mContext: Context
        private val mButtonIcon: ImageView
        private val mExpansionIndicator: ImageView
        private val mKeySpinners: Array<Spinner?>
        private val mExpandedView: View
        private val mToggleableSwitch: SwitchCompat
        private val mKeycodeLabel: TextView
        private var mAttachedPosition = -1
        private var mAttachedButton: GamepadEmulatedButton? = null
        private lateinit var mKeycodes: ShortArray

        init {
            mContext = itemView.getContext()
            mButtonIcon = itemView.findViewById<ImageView>(R.id.controller_mapper_button)
            mExpandedView = itemView.findViewById<View>(R.id.controller_mapper_expanded_view)
            mExpansionIndicator =
                itemView.findViewById<ImageView>(R.id.controller_mapper_expand_button)
            mKeycodeLabel = itemView.findViewById<TextView>(R.id.controller_mapper_keycode_label)
            mToggleableSwitch =
                itemView.findViewById<SwitchCompat>(R.id.controller_mapper_toggleable_switch)
            mToggleableSwitch.setOnCheckedChangeListener(this)
            val defaultView = itemView.findViewById<View>(R.id.controller_mapper_default_view)
            defaultView.setOnClickListener(this)
            mKeySpinners = arrayOfNulls(4)
            mKeySpinners[0] = itemView.findViewById<Spinner?>(R.id.controller_mapper_key_spinner1)
            mKeySpinners[1] = itemView.findViewById<Spinner?>(R.id.controller_mapper_key_spinner2)
            mKeySpinners[2] = itemView.findViewById<Spinner?>(R.id.controller_mapper_key_spinner3)
            mKeySpinners[3] = itemView.findViewById<Spinner?>(R.id.controller_mapper_key_spinner4)
            for (spinner in mKeySpinners) {
                spinner?.setAdapter(mKeyAdapter)
                spinner?.onItemSelectedListener = this
            }
        }

        fun attach(index: Int) {
            val rebinderButton = mRebinderButtons[index] ?: return
            mExpandedView.setVisibility(View.GONE)
            mButtonIcon.setImageResource(rebinderButton.iconResourceId)
            val buttonName = mContext.getString(rebinderButton.localeResourceId)
            mButtonIcon.setContentDescription(buttonName)
            rebinderButton.changeViewHolder(this)

            val realButton = mRealButtons[index] ?: return

            mAttachedButton = realButton

            if (realButton is GamepadButton) {
                mToggleableSwitch.setChecked(realButton.isToggleable)
                mToggleableSwitch.setVisibility(View.VISIBLE)
            } else {
                mToggleableSwitch.setVisibility(View.GONE)
            }

            mKeycodes = realButton.keycodes

            var spinnerIndex: Int

            // Populate spinners with known keycodes until we run out of keycodes
            spinnerIndex = 0
            while (spinnerIndex < mKeycodes.size) {
                val keySpinner = mKeySpinners[spinnerIndex]
                keySpinner?.setEnabled(true)
                val keyCode = mKeycodes[spinnerIndex]
                val selected: Int
                if (keyCode < 0) selected = keyCode + mSpecialKeycodeCount
                else selected = getIndexByValue(keyCode.toInt()) + mSpecialKeycodeCount
                keySpinner?.setSelection(selected)
                spinnerIndex++
            }
            // In case if there is too much spinners, disable the rest of them
            while (spinnerIndex < mKeySpinners.size) {
                mKeySpinners[spinnerIndex]?.setEnabled(false)
                spinnerIndex++
            }
            updateKeycodeLabel()

            mAttachedPosition = index
        }

        fun detach() {
            mRebinderButtons.getOrNull(mAttachedPosition)?.changeViewHolder(null)
            mAttachedPosition = -1
            mAttachedButton = null
        }

        fun setPressed(pressed: Boolean) {
            itemView.setBackgroundColor(if (pressed) COLOR_ACTIVE_BUTTON else Color.TRANSPARENT)
        }

        private fun updateKeycodeLabel() {
            val labelBuilder = StringBuilder()
            var first = true
            val unspecifiedPosition: Int = GamepadMap.Companion.UNSPECIFIED + mSpecialKeycodeCount
            for (keySpinner in mKeySpinners) {
                if (keySpinner?.selectedItemPosition == unspecifiedPosition) continue
                if (!first) labelBuilder.append(" + ")
                else first = false
                labelBuilder.append(keySpinner?.selectedItem.toString())
            }
            if (labelBuilder.length == 0) labelBuilder.append(
                mKeyAdapter.getItem(
                    unspecifiedPosition
                )
            )
            mKeycodeLabel.setText(labelBuilder.toString())
        }

        override fun onItemSelected(
            adapterView: AdapterView<*>,
            view: View?,
            selectionIndex: Int,
            selectionId: Long
        ) {
            if (mAttachedPosition == -1) return
            var editedKeycodeIndex = -1
            var i = 0
            while (i < mKeySpinners.size && i < mKeycodes.size) {
                if (adapterView != mKeySpinners[i]) {
                    i++
                    continue
                }
                editedKeycodeIndex = i
                break
            }
            if (editedKeycodeIndex == -1) return
            val keycode_offset = selectionIndex - mSpecialKeycodeCount
            if (selectionIndex <= mSpecialKeycodeCount) mKeycodes[editedKeycodeIndex] =
                (keycode_offset).toShort()
            else mKeycodes[editedKeycodeIndex] = getValueByIndex(keycode_offset)
            updateKeycodeLabel()
            try {
                GamepadMapStore.Companion.save()
            } catch (e: Exception) {
                showError(adapterView.getContext(), e)
            }
        }

        override fun onNothingSelected(adapterView: AdapterView<*>?) {
        }

        override fun onClick(view: View?) {
            val visibility = mExpandedView.getVisibility()
            when (visibility) {
                View.INVISIBLE, View.GONE -> {
                    mExpansionIndicator.setRotation(0f)
                    mExpandedView.setVisibility(View.VISIBLE)
                }

                View.VISIBLE -> {
                    mExpansionIndicator.setRotation(180f)
                    mExpandedView.setVisibility(View.GONE)
                }
            }
        }

        override fun onCheckedChanged(compoundButton: CompoundButton, checked: Boolean) {
            if (mAttachedButton !is GamepadButton) return
            (mAttachedButton as GamepadButton).isToggleable = checked
            try {
                GamepadMapStore.Companion.save()
            } catch (e: Exception) {
                showError(compoundButton.getContext(), e)
            }
        }

        private val COLOR_ACTIVE_BUTTON = 0x2000FF00
    }

    override val menuMap: GamepadMap?
        get() = mSimulatedGamepadMap

    override val gameMap: GamepadMap?
        get() = mSimulatedGamepadMap

    override val isGrabbing: Boolean
        get() = mGrabState

    override fun attachGrabListener(grabListener: GrabListener?) {
        if (grabListener == null) return
        mGamepadGrabListener = grabListener
        grabListener.onGrabState(mGrabState)
    }

    override fun detachGrabListener(grabListener: GrabListener?) {
        mGamepadGrabListener = null
    }

    fun setGrabState(newState: Boolean) {
        mGrabState = newState
        if (mGamepadGrabListener != null) mGamepadGrabListener!!.onGrabState(newState)
        if (mGrabState == mOldState) return
        updateRealButtons()
        updateStickIcons()
        notifyItemRangeChanged(0, mRebinderButtons.size)
        mOldState = mGrabState
    }

    companion object {
        private const val BUTTON_COUNT = 20
    }
}
