package net.kdt.pojavlaunch.prefs.screens;

import android.os.Bundle;

import androidx.activity.result.ActivityResultLauncher;

import net.kdt.pojavlaunch.Tools;
import net.kdt.pojavlaunch.adrenotools.ui.DriverConfigDialog;
import net.kdt.pojavlaunch.contracts.OpenDocumentWithExtension;
import net.kdt.pojavlaunch.utils.GLInfoUtils;
import net.kdt.pojavlaunch.utils.RendererCompatUtil;

import git.artdeell.mojo.R;

public class LauncherPreferenceVulkanFragment extends LauncherPreferenceFragment {

    private DriverConfigDialog mDialogScreen;

    private final ActivityResultLauncher<Object> mInstallDriver =
            registerForActivityResult(new OpenDocumentWithExtension("zip"), data -> {
                if(data != null) Tools.installDriverFromUri(getContext(), data);
            });

    private void openDriverDialog(){
        if(mDialogScreen == null) {
            mDialogScreen = new DriverConfigDialog();
            mDialogScreen.prepare(getContext(), mInstallDriver);
        }
        mDialogScreen.show();
    }

    @Override
    public void onCreatePreferences(Bundle b, String str) {
        boolean supportsTurnip = RendererCompatUtil.checkVulkanSupport(getContext().getPackageManager()) && GLInfoUtils.getGlInfo().isAdreno();
        if(!supportsTurnip) return;
        addPreferencesFromResource(R.xml.pref_vulkan);
        requirePreference("manageDrivers").setOnPreferenceClickListener(pref -> {
            openDriverDialog();
            return true;
        });
    }
}
