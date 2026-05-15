package net.kdt.pojavlaunch.prefs.screens;


import android.app.Activity;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.preference.EditTextPreference;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputLayout;

import net.kdt.pojavlaunch.LauncherActivity;
import net.ashmeet.hyperlauncher.R;
import net.kdt.pojavlaunch.prefs.LauncherPreferences;

/**
 * Preference for the main screen, any sub-screen should inherit this class for consistent behavior,
 * overriding only onCreatePreferences
 */
public class LauncherPreferenceFragment extends PreferenceFragmentCompat implements SharedPreferences.OnSharedPreferenceChangeListener {

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        TypedValue typedValue = new TypedValue();
        if (getContext().getTheme().resolveAttribute(com.google.android.material.R.attr.colorSurface, typedValue, true)) {
            view.setBackgroundColor(typedValue.data);
        } else {
            view.setBackgroundColor(getResources().getColor(R.color.background_app));
        }
        super.onViewCreated(view, savedInstanceState);
        
        RecyclerView listView = getListView();
        if (listView != null) {
            listView.setPadding(0, 0, 0, 80); // Bottom padding for navigation bar
            listView.setClipToPadding(false);
        }
        setDivider(null);
    }

    @Override
    public void onCreatePreferences(Bundle b, String str) {
        addPreferencesFromResource(R.xml.pref_main);
        setupNotificationRequestPreference();
    }

    private void setupNotificationRequestPreference() {
        Preference mRequestNotificationPermissionPreference = findPreference("notification_permission_request");
        if (mRequestNotificationPermissionPreference == null) return;

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

    @Override
    public void onDisplayPreferenceDialog(@NonNull Preference preference) {
        if (preference instanceof ListPreference) {
            showMaterialListPreferenceDialog((ListPreference) preference);
            return;
        }
        if (preference instanceof EditTextPreference) {
            showMaterialEditTextPreferenceDialog((EditTextPreference) preference);
            return;
        }
        super.onDisplayPreferenceDialog(preference);
    }

    private void showMaterialListPreferenceDialog(@NonNull ListPreference preference) {
        CharSequence[] entries = preference.getEntries();
        CharSequence[] entryValues = preference.getEntryValues();
        if (entries == null || entryValues == null) {
            super.onDisplayPreferenceDialog(preference);
            return;
        }

        String currentValue = preference.getValue();
        int checkedItem = -1;
        if (currentValue != null) {
            for (int i = 0; i < entryValues.length; i++) {
                if (currentValue.contentEquals(entryValues[i])) {
                    checkedItem = i;
                    break;
                }
            }
        }

        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(preference.getTitle())
                .setSingleChoiceItems(entries, checkedItem, (dialog, which) -> {
                    String newValue = entryValues[which].toString();
                    if (preference.callChangeListener(newValue)) {
                        preference.setValue(newValue);
                    }
                    dialog.dismiss();
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    private void showMaterialEditTextPreferenceDialog(@NonNull EditTextPreference preference) {
        View view = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_material_edit_text, null);
        EditText editText = view.findViewById(R.id.edit_text);
        TextInputLayout textInputLayout = view.findViewById(R.id.edit_text_layout);

        editText.setText(preference.getText());
        textInputLayout.setHint(preference.getTitle());

        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(preference.getTitle())
                .setView(view)
                .setPositiveButton(android.R.string.ok, (dialog, which) -> {
                    String newValue = editText.getText().toString();
                    if (preference.callChangeListener(newValue)) {
                        preference.setText(newValue);
                    }
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
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
