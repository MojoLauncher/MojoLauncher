package net.kdt.pojavlaunch.prefs.screens;

import android.app.Activity;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;

import androidx.preference.DropDownPreference;
import androidx.preference.ListPreference;
import androidx.preference.SwitchPreference;
import androidx.preference.SwitchPreferenceCompat;

import git.artdeell.mojo.R;

import net.kdt.pojavlaunch.plugins.LibraryPlugin;
import net.kdt.pojavlaunch.prefs.CustomSeekBarPreference;
import net.kdt.pojavlaunch.prefs.LauncherPreferences;
import net.kdt.pojavlaunch.utils.RendererCompatUtil;

/**
 * Fragment for any settings video related
 */
public class LauncherPreferenceVideoFragment extends LauncherPreferenceFragment {
    @Override
    public void onCreatePreferences(Bundle b, String str) {
        addPreferencesFromResource(R.xml.pref_video);
        int resolution = (int) (LauncherPreferences.PREF_SCALE_FACTOR * 100);

        CustomSeekBarPreference resolutionSeekbar = requirePreference("resolutionRatio",
                CustomSeekBarPreference.class);
        resolutionSeekbar.setSuffix(" %");

        // #724 bug fix
        if (resolution < 25) {
            resolutionSeekbar.setValue(100);
        } else {
            resolutionSeekbar.setValue(resolution);
        }

        // Sustained performance is only available since Nougat
        SwitchPreference sustainedPerfSwitch = requirePreference("sustainedPerformance",
                SwitchPreference.class);
        sustainedPerfSwitch.setVisible(Build.VERSION.SDK_INT >= Build.VERSION_CODES.N);
        sustainedPerfSwitch.setChecked(LauncherPreferences.PREF_SUSTAINED_PERFORMANCE);

        requirePreference("alternate_surface", SwitchPreferenceCompat.class).setChecked(LauncherPreferences.PREF_USE_ALTERNATE_SURFACE);
        requirePreference("force_vsync", SwitchPreferenceCompat.class).setChecked(LauncherPreferences.PREF_FORCE_VSYNC);

        // Show ANGLE switch only if AnglePlugin is available
        LibraryPlugin angle = LibraryPlugin.discoverPlugin(getContext(), LibraryPlugin.ID_ANGLE_PLUGIN);
        SwitchPreferenceCompat angleSwitch = requirePreference("use_angle", SwitchPreferenceCompat.class);
        angleSwitch.setVisible(angle != null);
        angleSwitch.setChecked(LauncherPreferences.PREF_USE_ANGLE);

        ListPreference rendererListPreference = requirePreference("renderer",
                ListPreference.class);
        RendererCompatUtil.RenderersList renderersList = RendererCompatUtil.getCompatibleRenderers(getContext());
        rendererListPreference.setEntries(renderersList.rendererDisplayNames);
        rendererListPreference.setEntryValues(renderersList.rendererIds.toArray(new String[0]));

        computeVisibility();
    }

    private void updateRefreshRates() {
        DropDownPreference dropDownPreference = requirePreference("frame_rate", DropDownPreference.class);
        float[] refreshRates = requireView().getDisplay().getSupportedRefreshRates();
        int ratesCount = 0;
        for (float refreshRate : refreshRates) {
            if (refreshRate < 59) continue;
            ratesCount++;
        }
        String[] refreshRateNames = new String[ratesCount];
        String[] refreshRateValues = new String[ratesCount];
        int j = -1;
        for (float rate : refreshRates) {
            if (rate < 59) continue;
            refreshRateNames[++j] = getString(R.string.refresh_rate, rate);
            refreshRateValues[j] = Float.toString(rate);
        }
        dropDownPreference.setEntries(refreshRateNames);
        dropDownPreference.setEntryValues(refreshRateValues);
        dropDownPreference.setSummaryProvider((pref)-> getString(R.string.preference_set_refresh_rate_summary, ((DropDownPreference)pref).getEntry()));
    }

    @Override
    public void onResume() {
        super.onResume();
        Activity activity = getActivity();
        if(activity != null) {
            requirePreference("ignoreNotch").setVisible(LauncherPreferences.hasNotch(activity));
        }
        updateRefreshRates();
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences p, String s) {
        super.onSharedPreferenceChanged(p, s);
        computeVisibility();
    }

    private void computeVisibility(){
        requirePreference("force_vsync", SwitchPreferenceCompat.class)
                .setVisible(LauncherPreferences.PREF_USE_ALTERNATE_SURFACE);
        requirePreference("frame_rate", DropDownPreference.class)
                .setVisible(LauncherPreferences.PREF_USE_ALTERNATE_SURFACE && Build.VERSION.SDK_INT >= Build.VERSION_CODES.R);
    }
}
