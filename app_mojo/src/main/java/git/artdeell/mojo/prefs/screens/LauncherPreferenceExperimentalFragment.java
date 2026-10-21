package git.artdeell.mojo.prefs.screens;

import android.os.Bundle;

import androidx.preference.SwitchPreferenceCompat;

import git.artdeell.mojo.utils.GpuUtils;

import git.artdeell.mojo.R;

public class LauncherPreferenceExperimentalFragment extends LauncherPreferenceFragment {

    @Override
    public void onCreatePreferences(Bundle b, String str) {
        addPreferencesFromResource(R.xml.pref_experimental);
        SwitchPreferenceCompat sysmem = requirePreference("freedrenoSysmem", SwitchPreferenceCompat.class);
        SwitchPreferenceCompat ubwc = requirePreference("ubwcWorkaround", SwitchPreferenceCompat.class);
        boolean hasFreedreno = GpuUtils.getGlInfo().isAdreno();
        sysmem.setVisible(hasFreedreno);
        ubwc.setVisible(hasFreedreno);
    }
}
