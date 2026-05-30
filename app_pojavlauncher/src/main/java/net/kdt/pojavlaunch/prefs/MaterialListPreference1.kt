package net.kdt.pojavlaunch.prefs

import android.content.Context
import android.util.AttributeSet
import android.widget.TextView
import androidx.preference.ListPreference
import androidx.preference.PreferenceViewHolder
import net.ashmeet.hyperlauncher.R

class MaterialListPreference @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : ListPreference(context, attrs) {
    init {
        setLayoutResource(R.layout.app_preference_layout)
        setWidgetLayoutResource(R.layout.preference_material_list)
    }

    override fun onBindViewHolder(holder: PreferenceViewHolder) {
        super.onBindViewHolder(holder)
        val valueView = holder.findViewById(R.id.preference_value) as TextView?
        if (valueView != null) {
            val entry = getEntry()
            valueView.setText(if (entry != null) entry else "")
        }
    }
}
