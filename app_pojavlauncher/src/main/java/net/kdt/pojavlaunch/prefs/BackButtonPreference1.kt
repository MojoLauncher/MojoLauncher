package net.kdt.pojavlaunch.prefs

import android.content.Context
import android.util.AttributeSet
import androidx.core.content.res.ResourcesCompat
import androidx.core.graphics.drawable.DrawableCompat
import androidx.preference.Preference
import androidx.preference.PreferenceViewHolder
import net.ashmeet.hyperlauncher.R
import net.kdt.pojavlaunch.extra.ExtraConstants
import net.kdt.pojavlaunch.extra.ExtraCore

class BackButtonPreference(context: Context, attrs: AttributeSet?) : Preference(context, attrs) {
    init {
        init()
    }

    @Suppress("unused")
    constructor(context: Context) : this(context, null)

    private fun init() {
        if (getTitle() == null) {
            setTitle(R.string.preference_back_title)
        }
        if (getIcon() == null) {
            var icon = ResourcesCompat.getDrawable(
                getContext().getResources(),
                R.drawable.ic_px_arrow_left,
                getContext().getTheme()
            )
            if (icon != null) {
                icon = DrawableCompat.wrap(icon).mutate()
                DrawableCompat.setTint(icon, LauncherPreferences.PREF_GLOBAL_ICON_COLOR)
                setIcon(icon)
            } else {
                setIcon(R.drawable.ic_px_arrow_left)
            }
        }
    }


    override fun onBindViewHolder(holder: PreferenceViewHolder) {
        super.onBindViewHolder(holder)
        // Custom logic to ensure it stands out if needed
    }

    override fun onClick() {
        // It is caught by an ExtraListener in the LauncherActivity
        ExtraCore.setValue(ExtraConstants.BACK_PREFERENCE, "true")
    }
}
