package net.kdt.pojavlaunch.prefs.screens

import android.os.Bundle
import net.ashmeet.hyperlauncher.R
import net.kdt.pojavlaunch.utils.GLInfoUtils
import net.kdt.pojavlaunch.utils.RendererCompatUtil

class LauncherPreferenceMiscellaneousFragment : LauncherPreferenceFragment() {
    override fun onCreatePreferences(b: Bundle?, str: String?) {
        addPreferencesFromResource(R.xml.pref_misc)
        val driverPreference = requirePreference("zinkPreferSystemDriver")
        val packageManager = driverPreference.getContext().getPackageManager()
        val supportsTurnip =
            RendererCompatUtil.checkVulkanSupport(packageManager) && (GLInfoUtils.glInfo?.isAdreno == true)
        driverPreference.setVisible(supportsTurnip)
    }
}
