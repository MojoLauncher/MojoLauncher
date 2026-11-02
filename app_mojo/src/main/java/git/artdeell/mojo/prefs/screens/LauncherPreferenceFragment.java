package git.artdeell.mojo.prefs.screens;


import android.Manifest;
import android.app.Activity;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;

import git.artdeell.mojo.LauncherActivity;
import git.artdeell.mojo.R;
import git.artdeell.mojo.prefs.LauncherPreferences;
import git.artdeell.mojo.authenticator.accounts.Account;
import git.artdeell.mojo.authenticator.accounts.Accounts;
import git.artdeell.mojo.authenticator.accounts.SkinLibrary;
import git.artdeell.mojo.utils.SkinLibraryDialog;
import git.artdeell.mojo.utils.BannerManager;

/**
 * Preference for the main screen, any sub-screen should inherit this class for consistent behavior,
 * overriding only onCreatePreferences
 */
public class LauncherPreferenceFragment extends PreferenceFragmentCompat implements SharedPreferences.OnSharedPreferenceChangeListener {
    protected Runnable mVisibilityUpdater = () -> {};

    private final ActivityResultLauncher<String> mSkinPicker =
            registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
                if(uri == null) return;
                try {
                    SkinLibrary.importSkin(requireContext(), uri);
                    Toast.makeText(requireContext(), R.string.fork_skin_saved, Toast.LENGTH_SHORT).show();
                } catch (Exception e) {
                    Toast.makeText(requireContext(), R.string.fork_skin_error, Toast.LENGTH_LONG).show();
                }
                openSkinLibrary();
            });

    private final ActivityResultLauncher<String> mBannerPicker =
            registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
                if(uri == null) return;
                try {
                    BannerManager.saveCustom(requireContext(), uri);
                    Toast.makeText(requireContext(), R.string.fork_banner_applied, Toast.LENGTH_SHORT).show();
                } catch (Exception e) {
                    Toast.makeText(requireContext(), R.string.fork_banner_error, Toast.LENGTH_LONG).show();
                }
            });

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        view.setBackground(git.artdeell.mojo.utils.McStyle.screenBackground(requireContext()));
        super.onViewCreated(view, savedInstanceState);
        setDivider(null);
        setDividerHeight(0);
    }

    /** The root settings show the categories as tiles, three per row. The sub screens stay as a list. */
    @Override
    public androidx.recyclerview.widget.RecyclerView.LayoutManager onCreateLayoutManager() {
        if(getClass() != LauncherPreferenceFragment.class) return super.onCreateLayoutManager();
        androidx.recyclerview.widget.GridLayoutManager manager =
                new androidx.recyclerview.widget.GridLayoutManager(requireContext(), 6);
        manager.setSpanSizeLookup(new androidx.recyclerview.widget.GridLayoutManager.SpanSizeLookup() {
            @Override
            public int getSpanSize(int position) {
                Preference p = visiblePreferenceAt(position);
                return p != null && p.getLayoutResource() == R.layout.bedrock_preference_tile ? 2 : 6;
            }
        });
        return manager;
    }

    private Preference visiblePreferenceAt(int position) {
        if(getPreferenceScreen() == null) return null;
        java.util.List<Preference> flat = new java.util.ArrayList<>();
        flatten(getPreferenceScreen(), flat);
        return position >= 0 && position < flat.size() ? flat.get(position) : null;
    }

    private static void flatten(androidx.preference.PreferenceGroup group, java.util.List<Preference> out) {
        for(int i = 0; i < group.getPreferenceCount(); i++) {
            Preference p = group.getPreference(i);
            if(!p.isVisible()) continue;
            out.add(p);
            if(p instanceof androidx.preference.PreferenceGroup) flatten((androidx.preference.PreferenceGroup) p, out);
        }
    }

    @Override
    public void onCreatePreferences(Bundle b, String str) {
        mVisibilityUpdater = this::updateVisibility;
        addPreferencesFromResource(R.xml.pref_main);
        setupNotificationRequestPreference();
        setupBannerPreference();
        setupSkinPreference();
    }

    private void openSkinLibrary() {
        SkinLibraryDialog.show(requireContext(), () -> mSkinPicker.launch("image/png"));
    }

    private void setupSkinPreference() {
        Preference skinPreference = findPreference("offline_skin_setting");
        if(skinPreference == null) return;
        skinPreference.setOnPreferenceClickListener(preference -> {
            openSkinLibrary();
            return true;
        });
    }

    private void setupBannerPreference() {
        Preference bannerPreference = findPreference("banner_setting");
        if(bannerPreference == null) return;
        bannerPreference.setOnPreferenceClickListener(preference -> {
            BannerManager.showChooser(requireContext(), () -> mBannerPicker.launch("image/*"),
                    () -> Toast.makeText(requireContext(), R.string.fork_banner_applied, Toast.LENGTH_SHORT).show());
            return true;
        });
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
