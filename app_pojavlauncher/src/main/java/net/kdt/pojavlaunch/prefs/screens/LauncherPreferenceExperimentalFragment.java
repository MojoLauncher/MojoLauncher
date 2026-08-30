package net.kdt.pojavlaunch.prefs.screens;

import android.os.Bundle;

import androidx.preference.SwitchPreference;

import net.kdt.pojavlaunch.utils.GpuUtils;

import git.artdeell.mojo.R;

public class LauncherPreferenceExperimentalFragment extends LauncherPreferenceFragment {

    @Override
    public void onCreatePreferences(Bundle b, String str) {
        addPreferencesFromResource(R.xml.pref_experimental);
        SwitchPreference pref = requirePreference("freedrenoSysmem", SwitchPreference.class);
        boolean hasFreedreno = GpuUtils.getGlInfo().isAdreno();
        pref.setVisible(hasFreedreno);
    }
}
