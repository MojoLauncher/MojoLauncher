package net.kdt.pojavlaunch.customcontrols.keyboard

import android.content.Context
import android.text.Editable
import android.text.Selection
import android.text.TextWatcher
import android.util.AttributeSet
import android.view.KeyEvent
import android.view.inputmethod.InputMethodManager
import android.widget.TextView
import androidx.appcompat.widget.AppCompatEditText
import net.ashmeet.hyperlauncher.R


/**
 * This class is intended for sending characters used in chat via the virtual keyboard
 */
class TouchCharInput @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = R.attr.editTextStyle
) : AppCompatEditText(context, attrs, defStyleAttr) {
    private var mIsDoingInternalChanges = false
    private var mCharacterSender: CharacterSenderStrategy? = null

    init {
        setup()
    }

    /**
     * When we change from app to app, the keyboard gets disabled.
     * So, we disable the object
     */
    override fun onWindowFocusChanged(hasWindowFocus: Boolean) {
        super.onWindowFocusChanged(hasWindowFocus)
        disable()
    }

    /**
     * Intercepts the back key to disable focus
     * Does not affect the rest of the activity.
     */
    override fun onKeyPreIme(keyCode: Int, event: KeyEvent): Boolean {
        if (event.getKeyCode() == KeyEvent.KEYCODE_BACK && event.getAction() == KeyEvent.ACTION_UP) {
            disable()
        }
        return super.onKeyPreIme(keyCode, event)
    }


    /**
     * Toggle on and off the soft keyboard, depending of the state
     */
    fun switchKeyboardState() {
        val imm = getContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        // Allow, regardless of whether or not a hardware keyboard is declared
        if (hasFocus()) {
            clear()
            disable()
        } else {
            enable()
            imm.showSoftInput(this, InputMethodManager.SHOW_IMPLICIT)
        }
    }


    /**
     * Clear the EditText from any leftover inputs
     * It does not affect the in-game input
     */
    fun clear() {
        mIsDoingInternalChanges = true
        // Edit the Editable directly as it doesn't affect the state
        // of the TextView.
        val editable = getEditableText()
        editable.clear()
        //Braille space, doesn't trigger keyboard auto-complete
        editable.append(TEXT_FILLER)
        Selection.setSelection(editable, TEXT_FILLER.length)
        mIsDoingInternalChanges = false
    }

    /** Regain ability to exist, take focus and have some text being input  */
    fun enable() {
        setEnabled(true)
        setFocusable(true)
        setVisibility(VISIBLE)
        requestFocus()
    }

    /** Lose ability to exist, take focus and have some text being input  */
    fun disable() {
        clear()
        setVisibility(GONE)
        clearFocus()
        setEnabled(false)
        //setFocusable(false);
    }

    /** Send the enter key.  */
    private fun sendEnter() {
        mCharacterSender!!.sendEnter()
        clear()
    }

    /** Just sets the char sender that should be used.  */
    fun setCharacterSender(characterSender: CharacterSenderStrategy?) {
        mCharacterSender = characterSender
    }

    /** This function deals with anything that has to be executed when the constructor is called  */
    private fun setup() {
        // Using TextWatcher instead of overriding onTextChanged because some Huawei firmware
        // calls setText in constructor, causing havoc for our listener
        addTextChangedListener(InputTextWatcher())
        setOnEditorActionListener(OnEditorActionListener { textView: TextView?, i: Int, keyEvent: KeyEvent? ->
            sendEnter()
            clear()
            disable()
            false
        })
        clear()
        disable()
    }

    private inner class InputTextWatcher : TextWatcher {
        override fun beforeTextChanged(charSequence: CharSequence?, i: Int, i1: Int, i2: Int) {
        }

        /**
         * We take the new chars, and send them to the game.
         * If less chars are present, remove some.
         * The text is always cleaned up.
         */
        override fun onTextChanged(
            text: CharSequence,
            start: Int,
            lengthBefore: Int,
            lengthAfter: Int
        ) {
            if (mIsDoingInternalChanges) return
            if (mCharacterSender != null) {
                for (i in 0..<lengthBefore) {
                    mCharacterSender!!.sendBackspace()
                }

                var i = start
                var count = 0
                while (count < lengthAfter) {
                    mCharacterSender!!.sendChar(text.get(i))
                    ++count
                    ++i
                }
            }
        }

        override fun afterTextChanged(editable: Editable) {
            if (mIsDoingInternalChanges) return
            // Moved from onTextChanged because "It is an error to attempt to make changes to s from this callback."
            // reference: https://developer.android.com/reference/android/text/TextWatcher#onTextChanged(java.lang.CharSequence,%20int,%20int,%20int)
            if (editable.length < 1) clear()
        }
    }

    companion object {
        const val TEXT_FILLER: String = "                              "
    }
}
