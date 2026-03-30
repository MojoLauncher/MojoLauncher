package net.kdt.pojavlaunch.prefs.screens;


import android.app.Activity;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;

import net.kdt.pojavlaunch.Tools;
import net.kdt.pojavlaunch.utils.PerformancePreset;
import com.google.android.material.snackbar.Snackbar;

/**
 * Preference for the main screen, any sub-screen should inherit this class for consistent behavior,
 * overriding only onCreatePreferences
 */
public class LauncherPreferenceFragment extends PreferenceFragmentCompat implements SharedPreferences.OnSharedPreferenceChangeListener {

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        view.setBackgroundColor(getResources().getColor(R.color.background_app));
        super.onViewCreated(view, savedInstanceState);
    }

    @Override
    public void onCreatePreferences(Bundle b, String str) {
        addPreferencesFromResource(R.xml.pref_main);
        setupNotificationRequestPreference();

        // Setup preset preferences
        int deviceRam = Tools.getTotalDeviceMemory(getContext());
        findPreference("preset_normal").setSummary("RAM: " + (int)(deviceRam * 0.35) + "MB, Render Distance: 8");
        findPreference("preset_extreme").setSummary("RAM: " + (int)(deviceRam * 0.55) + "MB, Render Distance: 16");

        findPreference("preset_potato").setOnPreferenceClickListener(preference -> {
            PerformancePreset.applyPreset(getContext(), PerformancePreset.Mode.POTATO);
            Snackbar.make(getView(), "Preset applied!", Snackbar.LENGTH_SHORT).show();
            return true;
        });
        findPreference("preset_normal").setOnPreferenceClickListener(preference -> {
            PerformancePreset.applyPreset(getContext(), PerformancePreset.Mode.NORMAL);
            Snackbar.make(getView(), "Preset applied!", Snackbar.LENGTH_SHORT).show();
            return true;
        });
        findPreference("preset_extreme").setOnPreferenceClickListener(preference -> {
            PerformancePreset.applyPreset(getContext(), PerformancePreset.Mode.EXTREME);
            Snackbar.make(getView(), "Preset applied!", Snackbar.LENGTH_SHORT).show();
            return true;
        });
    }

    private void setupNotificationRequestPreference() {
        Preference mRequestNotificationPermissionPreference = requirePreference("notification_permission_request");
        Activity activity = getActivity();
        if(activity instanceof LauncherActivity) {
            LauncherActivity launcherActivity = (LauncherActivity)activity;
            mRequestNotificationPermissionPreference.setVisible(!launcherActivity.checkForNotificationPermission());
            mRequestNotificationPermissionPreference.setOnPreferenceClickListener(preference -> {
                launcherActivity.askForNotificationPermission(()->mRequestNotificationPermissionPreference.setVisible(false));
                return true;
            });
        }else{
            mRequestNotificationPermissionPreference.setVisible(false);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        SharedPreferences sharedPreferences = getPreferenceManager().getSharedPreferences();
        if(sharedPreferences != null) sharedPreferences.registerOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void onPause() {
        SharedPreferences sharedPreferences = getPreferenceManager().getSharedPreferences();
        if(sharedPreferences != null) sharedPreferences.unregisterOnSharedPreferenceChangeListener(this);
        super.onPause();
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences p, String s) {
        LauncherPreferences.loadPreferences(getContext());
    }

    protected Preference requirePreference(CharSequence key) {
        Preference preference = findPreference(key);
        if(preference != null) return preference;
        throw new IllegalStateException("Preference "+key+" is null");
    }
    @SuppressWarnings("unchecked")
    protected <T extends Preference> T requirePreference(CharSequence key, Class<T> preferenceClass) {
        Preference preference = requirePreference(key);
        if(preferenceClass.isInstance(preference)) return (T)preference;
        throw new IllegalStateException("Preference "+key+" is not an instance of "+preferenceClass.getSimpleName());
    }
}
