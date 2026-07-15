package net.kdt.pojavlaunch.prefs.screens;

import android.content.SharedPreferences;
import android.os.Bundle;

import androidx.preference.EditTextPreference;
import androidx.preference.SwitchPreference;

import net.kdt.pojavlaunch.prefs.LauncherPreferences;
import net.kdt.pojavlaunch.utils.GLInfoUtils;

import git.artdeell.mojo.R;

public class LauncherPreferenceExperimentalFragment extends LauncherPreferenceFragment {

    @Override
    public void onCreatePreferences(Bundle b, String str) {
        addPreferencesFromResource(R.xml.pref_experimental);
        SwitchPreference pref = requirePreference("freedrenoSysmem", SwitchPreference.class);
        boolean hasFreedreno = GLInfoUtils.getGlInfo().isAdreno();
        pref.setVisible(hasFreedreno);

        EditTextPreference widthPref = requirePreference("customResWidth", EditTextPreference.class);
        widthPref.setVisible(LauncherPreferences.PREF_CUSTOM_RESOLUTION);
        EditTextPreference heightPref = requirePreference("customResHeight", EditTextPreference.class);
        heightPref.setVisible(LauncherPreferences.PREF_CUSTOM_RESOLUTION);
        widthPref.setSummary(widthPref.getText());
        heightPref.setSummary(heightPref.getText());
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences p, String s) {
        // using shared prefs here directly because LauncherPreferences is not updated on preference change :sob:
        EditTextPreference widthPref = requirePreference("customResWidth", EditTextPreference.class);
        widthPref.setVisible(p.getBoolean("customResolution", false));
        EditTextPreference heightPref = requirePreference("customResHeight", EditTextPreference.class);
        heightPref.setVisible(p.getBoolean("customResolution", false));

        int clampedWidth = clampPref(widthPref, 320, 7680);
        int clampedHeight = clampPref(heightPref, 240, 4320);
        widthPref.setText(String.valueOf(clampedWidth));
        heightPref.setText(String.valueOf(clampedHeight));
        widthPref.setSummary(String.valueOf(clampedWidth));
        heightPref.setSummary(String.valueOf(clampedHeight));
    }

    private static int clampPref(EditTextPreference pref, int min, int max){
        int res;
        try {
            res = Integer.parseInt(pref.getText());
        } catch (NumberFormatException e){
            res = 0;
        }
        if(res < min) return min;
        return Math.min(res, max);
    }
}
