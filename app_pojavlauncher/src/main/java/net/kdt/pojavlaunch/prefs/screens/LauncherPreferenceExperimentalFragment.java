package net.kdt.pojavlaunch.prefs.screens;

import android.os.Bundle;

import androidx.preference.ListPreference;

import net.kdt.pojavlaunch.prefs.LauncherPreferences;

import java.util.ArrayList;
import java.util.List;

import git.artdeell.mojo.R;

public class LauncherPreferenceExperimentalFragment extends LauncherPreferenceFragment {

    @Override
    public void onCreatePreferences(Bundle b, String str) {
        addPreferencesFromResource(R.xml.pref_experimental);

        ListPreference themesPref = requirePreference("selectTheme", ListPreference.class);
        themesPref.setSummary(LauncherPreferences.PREF_SELECT_THEME);
        List<String> newEntries = new ArrayList<>();
        newEntries.add("Default");
        newEntries.add("Pojav");
        newEntries.add("Midnight");
        newEntries.add("Amethyst");
        themesPref.setEntries(newEntries.toArray(new String[0]));
        themesPref.setEntryValues(newEntries.toArray(new String[0]));

        themesPref.setOnPreferenceChangeListener((preference, newValue) -> {
            requireActivity().recreate();
            return true;
        });
    }
}
