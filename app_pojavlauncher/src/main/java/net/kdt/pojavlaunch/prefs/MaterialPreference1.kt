package net.kdt.pojavlaunch.prefs

import android.content.Context
import android.util.AttributeSet
import androidx.preference.Preference
import net.ashmeet.hyperlauncher.R

class MaterialPreference @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null) :
    Preference(context, attrs) {
    init {
        setLayoutResource(R.layout.app_preference_layout)
    }
}
