package net.kdt.pojavlaunch.utils.interfaces

import android.text.TextWatcher

/**
 * Most interfaces implementations of [TextWatcher] only implement the afterTextChanged method.
 * This class provides a default for other methods.
 */
interface SimpleTextWatcher : TextWatcher {
    override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

    override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
}
