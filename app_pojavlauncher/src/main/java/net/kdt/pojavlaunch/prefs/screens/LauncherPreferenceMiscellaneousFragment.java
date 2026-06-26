package net.kdt.pojavlaunch.prefs.screens;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.preference.Preference;

import com.kdt.mcgui.ProgressLayout;

import git.artdeell.mojo.R;

import net.kdt.pojavlaunch.LauncherActivity;
import net.kdt.pojavlaunch.Tools;
import net.kdt.pojavlaunch.extra.ExtraConstants;
import net.kdt.pojavlaunch.utils.GLInfoUtils;
import net.kdt.pojavlaunch.utils.RendererCompatUtil;

public class LauncherPreferenceMiscellaneousFragment extends LauncherPreferenceFragment {

    private final ActivityResultLauncher<Uri> mMigrateLauncher = registerForActivityResult(
            new ActivityResultContracts.OpenDocumentTree(), (uri) -> {
                if(uri != null) {
                    Tools.migrateData(getLauncherActivity(), uri);
                }
            }
    );

    @Override
    public void onCreatePreferences(Bundle b, String str) {
        mVisibilityUpdater = this::updateVisibility;
        addPreferencesFromResource(R.xml.pref_misc);
        Preference driverPreference = requirePreference("zinkPreferSystemDriver");
        PackageManager packageManager = driverPreference.getContext().getPackageManager();
        boolean supportsTurnip = RendererCompatUtil.checkVulkanSupport(packageManager) && GLInfoUtils.getGlInfo().isAdreno();
        driverPreference.setVisible(supportsTurnip);
        Preference importPreference = requirePreference("runDataMigration");
        importPreference.setOnPreferenceClickListener(preference -> {
            if(getLauncherActivity().hasActiveProcesses()) {
                Toast.makeText(getContext(), R.string.migration_progress_busy, Toast.LENGTH_SHORT).show();
                return true;
            }
            mMigrateLauncher.launch(null);
            return true;
        });
        setupMicrophoneRequestPreference();
    }

    private void updateVisibility(){
        requirePreference("microphoneAccessRequest").setVisible(!getLauncherActivity().checkForPermissionRationale(33, Manifest.permission.RECORD_AUDIO));
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    private void setupMicrophoneRequestPreference() {
        Preference mRequestMicrophonePermissionPreference = requirePreference("microphoneAccessRequest");
        Activity activity = getActivity();
        if(activity instanceof LauncherActivity) {
            mRequestMicrophonePermissionPreference.setOnPreferenceClickListener(preference -> {
                ((LauncherActivity) activity).askForPermission(23, Manifest.permission.RECORD_AUDIO);
                return true;
            });
        } else {
            mRequestMicrophonePermissionPreference.setVisible(false);
        }
        updateVisibility();
    }
}
