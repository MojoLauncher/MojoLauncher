package net.kdt.pojavlaunch.prefs.screens;


import android.Manifest;
import android.app.Activity;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.View;

import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;

import net.kdt.pojavlaunch.LauncherActivity;
import git.artdeell.mojo.R;
import net.kdt.pojavlaunch.Tools;
import net.kdt.pojavlaunch.prefs.LauncherPreferences;

/**
 * Preference for the main screen, any sub-screen should inherit this class for consistent behavior,
 * overriding only onCreatePreferences
 */
public class LauncherPreferenceFragment extends PreferenceFragmentCompat implements SharedPreferences.OnSharedPreferenceChangeListener {
    protected Runnable mVisibilityUpdater = () -> {};

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_preference_custom, container, false);
        FrameLayout listContainer = view.findViewById(R.id.list_container);
        View prefView = super.onCreateView(inflater, listContainer, savedInstanceState);
        if (prefView != null) {
            listContainer.addView(prefView);
        }

        // Update titles for sub-screens if needed
        TextView title = view.findViewById(R.id.settings_title);
        TextView subtitle = view.findViewById(R.id.settings_subtitle);
        updateHeader(title, subtitle);

        View backBtn = view.findViewById(R.id.btn_back);
        if (backBtn != null) {
            backBtn.setOnClickListener(v -> {
                if (getActivity() != null) {
                    getActivity().onBackPressed();
                }
            });
        }

        // Landscape sidebar (see layout-land/fragment_preference_custom.xml)
        bindSidebar(view);

        return view;
    }

    /**
     * Wires up the landscape sidebar navigation. The buttons only exist in the
     * landscape layout, so every lookup is null-safe.
     */
    private void bindSidebar(View view) {
        FragmentActivity activity = getActivity();
        if (activity == null) return;

        View sbHome = view.findViewById(R.id.sb_home);
        if (sbHome != null) {
            sbHome.setOnClickListener(v -> Tools.backToMainMenu(activity));
        }

        View sbSettings = view.findViewById(R.id.sb_settings);
        if (sbSettings != null) {
            sbSettings.setOnClickListener(v -> {
                // On a sub-screen, go back to the main settings list
                if (this.getClass() != LauncherPreferenceFragment.class) {
                    activity.onBackPressed();
                }
            });
        }

        View sbControls = view.findViewById(R.id.sb_controls);
        if (sbControls != null) {
            sbControls.setOnClickListener(v -> openSubScreen(LauncherPreferenceControlFragment.class));
        }

        View sbJava = view.findViewById(R.id.sb_java);
        if (sbJava != null) {
            sbJava.setOnClickListener(v -> openSubScreen(LauncherPreferenceJavaFragment.class));
        }

        View sbInfo = view.findViewById(R.id.sb_info);
        if (sbInfo != null) {
            sbInfo.setOnClickListener(v -> Tools.shareLog(activity));
        }
    }

    /**
     * Opens a settings sub-screen as a full-screen overlay on the back stack,
     * the same way the preference framework opens sub-screens.
     */
    private void openSubScreen(Class<? extends LauncherPreferenceFragment> clazz) {
        FragmentActivity activity = getActivity();
        if (activity == null) return;
        FragmentManager fm = activity.getSupportFragmentManager();
        if (fm.findFragmentByTag(clazz.getName()) != null) return; // already open
        fm.beginTransaction()
                .setReorderingAllowed(true)
                .setCustomAnimations(R.anim.slide_in_right, R.anim.slide_out_left,
                        R.anim.slide_in_left, R.anim.slide_out_right)
                .addToBackStack(clazz.getName())
                .replace(android.R.id.content, clazz, null, clazz.getName())
                .commit();
    }

    protected void updateHeader(TextView title, TextView subtitle) {
        if (this instanceof LauncherPreferenceVideoFragment) {
            title.setText(R.string.preference_video_title);
            subtitle.setText(R.string.preference_video_description);
        } else if (this instanceof LauncherPreferenceControlFragment) {
            title.setText(R.string.preference_control_title);
            subtitle.setText(R.string.preference_control_description);
        } else if (this instanceof LauncherPreferenceJavaFragment) {
            title.setText(R.string.preference_java_title);
            subtitle.setText(R.string.preference_java_description);
        } else if (this instanceof LauncherPreferenceMiscellaneousFragment) {
            title.setText(R.string.preference_misc_title);
            subtitle.setText(R.string.preference_misc_description);
        } else if (this instanceof LauncherPreferenceExperimentalFragment) {
            title.setText(R.string.preference_experimental_title);
            subtitle.setText(R.string.preference_experimental_description);
        }
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        view.setBackgroundColor(android.graphics.Color.TRANSPARENT);
        super.onViewCreated(view, savedInstanceState);
        
        // Remove standard background from the RecyclerView itself if it has one
        if (getListView() != null) {
            getListView().setBackgroundColor(android.graphics.Color.TRANSPARENT);
        }

        // The landscape settings screen is flat (sidebar + list, see
        // layout-land/fragment_preference_custom.xml). Sub-screens open on top
        // of everything, so they get the card, the header and an opaque
        // background back.
        if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE
                && this.getClass() != LauncherPreferenceFragment.class) {
            View content = view.findViewById(R.id.settings_content);
            View divider = view.findViewById(R.id.settings_header_divider);
            View backBtn = view.findViewById(R.id.btn_back);
            TextView title = view.findViewById(R.id.settings_title);
            TextView subtitle = view.findViewById(R.id.settings_subtitle);
            if (content != null) {
                content.setBackgroundResource(R.drawable.launcher_card_flat);
                content.setPadding(dp(16), dp(14), dp(16), dp(16));
            }
            if (backBtn != null) backBtn.setVisibility(View.VISIBLE);
            if (title != null) title.setVisibility(View.VISIBLE);
            if (subtitle != null) subtitle.setVisibility(View.VISIBLE);
            if (divider != null) divider.setVisibility(View.VISIBLE);
            view.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.background_app));
        }
    }

    private int dp(float value) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, value, getResources().getDisplayMetrics());
    }

    @Override
    public void onCreatePreferences(Bundle b, String str) {
        mVisibilityUpdater = this::updateVisibility;
        addPreferencesFromResource(R.xml.pref_main);
        // Navigation rows open their sub-screen with a slide animation
        bindSubScreenNavigation("video_screen_setting", LauncherPreferenceVideoFragment.class);
        bindSubScreenNavigation("control_screen_setting", LauncherPreferenceControlFragment.class);
        bindSubScreenNavigation("java_screen_setting", LauncherPreferenceJavaFragment.class);
        bindSubScreenNavigation("misc_screen_setting", LauncherPreferenceMiscellaneousFragment.class);
        bindSubScreenNavigation("experimental_screen_setting", LauncherPreferenceExperimentalFragment.class);
        setupNotificationRequestPreference();
    }

    private void bindSubScreenNavigation(String key, Class<? extends LauncherPreferenceFragment> clazz) {
        Preference preference = findPreference(key);
        if (preference != null) {
            preference.setOnPreferenceClickListener(p -> {
                openSubScreen(clazz);
                return true;
            });
        }
    }

    private void updateVisibility(){
        requirePreference("notification_permission_request").setVisible(!getLauncherActivity().checkForPermission(33, Manifest.permission.POST_NOTIFICATIONS));
    }

    private void setupNotificationRequestPreference() {
        Preference mRequestNotificationPermissionPreference = requirePreference("notification_permission_request");
        Activity activity = getActivity();
        if(activity instanceof LauncherActivity) {
            mRequestNotificationPermissionPreference.setOnPreferenceClickListener(preference -> {
                ((LauncherActivity) activity).askForPermission(33, Manifest.permission.POST_NOTIFICATIONS);
                return true;
            });
        }else{
            mRequestNotificationPermissionPreference.setVisible(false);
        }
        updateVisibility();
    }

    @Override
    public void onResume() {
        super.onResume();
        SharedPreferences sharedPreferences = getPreferenceManager().getSharedPreferences();
        if(sharedPreferences != null) sharedPreferences.registerOnSharedPreferenceChangeListener(this);
        mVisibilityUpdater.run();
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
    protected LauncherActivity getLauncherActivity(){
        return ((LauncherActivity) getActivity());
    }
}
