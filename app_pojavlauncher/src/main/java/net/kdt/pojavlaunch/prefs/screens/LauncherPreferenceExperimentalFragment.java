package net.kdt.pojavlaunch.prefs.screens;

import android.os.Bundle;

import androidx.preference.ListPreference;

import java.util.ArrayList;
import java.util.List;

import git.artdeell.mojo.R;

public class LauncherPreferenceExperimentalFragment extends LauncherPreferenceFragment {

    @Override
    public void onCreatePreferences(Bundle b, String str) {
        addPreferencesFromResource(R.xml.pref_experimental);

        ListPreference themesPref = requirePreference("select_theme", ListPreference.class);
        List<String> newEntries = new ArrayList<>();
        newEntries.add("Default");
        newEntries.add("Midnight");
        themesPref.setEntries(newEntries.toArray(new String[0]));

        themesPref.setOnPreferenceChangeListener((preference, newValue) -> {
            if(newValue.equals("Default")) {
                requireActivity().setTheme(R.style.AppTheme);
            } else if(newValue.equals("Midnight")) {
                requireActivity().setTheme(R.style.MidnightTheme);
            }
            return true;
        });
    }
}
