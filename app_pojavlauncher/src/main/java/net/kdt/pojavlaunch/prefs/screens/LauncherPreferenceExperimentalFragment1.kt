package net.kdt.pojavlaunch.prefs.screens

import android.os.Bundle
import net.ashmeet.hyperlauncher.R

class LauncherPreferenceExperimentalFragment : LauncherPreferenceFragment() {
    override fun onCreatePreferences(b: Bundle?, str: String?) {
        addPreferencesFromResource(R.xml.pref_experimental)
    }
}
