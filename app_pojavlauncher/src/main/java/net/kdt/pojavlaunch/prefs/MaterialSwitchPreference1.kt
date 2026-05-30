package net.kdt.pojavlaunch.prefs

import android.content.Context
import android.util.AttributeSet
import android.view.View
import androidx.preference.PreferenceViewHolder
import androidx.preference.SwitchPreferenceCompat
import net.ashmeet.hyperlauncher.R

class MaterialSwitchPreference @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : SwitchPreferenceCompat(context, attrs) {
    init {
        setLayoutResource(R.layout.app_preference_layout)
        setWidgetLayoutResource(R.layout.preference_material_switch)
    }

    override fun onBindViewHolder(holder: PreferenceViewHolder) {
        super.onBindViewHolder(holder)

        val switchWidget = holder.findViewById(android.R.id.switch_widget)

        if (switchWidget is ToggleView) {
            val toggle = switchWidget

            // Sync state from preference
            toggle.setChecked(isChecked())

            // Handle clicks
            toggle.setOnClickListener(View.OnClickListener { v: View? ->
                val newValue = !isChecked()
                if (callChangeListener(newValue)) {
                    setChecked(newValue)
                    toggle.setChecked(newValue)
                }
            })
        }

        if (getSwitchTextOn() == null) setSwitchTextOn("")
        if (getSwitchTextOff() == null) setSwitchTextOff("")
    }
}
