package net.kdt.pojavlaunch.prefs

import android.content.Context
import android.util.AttributeSet
import android.widget.TextView
import androidx.preference.EditTextPreference
import androidx.preference.PreferenceViewHolder
import net.ashmeet.hyperlauncher.R

class MaterialEditTextPreference @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : EditTextPreference(context, attrs) {
    init {
        setLayoutResource(R.layout.app_preference_layout)
    }

    override fun onBindViewHolder(holder: PreferenceViewHolder) {
        super.onBindViewHolder(holder)
        val valueView = holder.findViewById(R.id.preference_value) as TextView?
        if (valueView != null) {
            valueView.setText(getText())
        }
    }
}
