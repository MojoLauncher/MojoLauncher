package net.kdt.pojavlaunch.prefs.screens

import android.app.Activity
import android.content.SharedPreferences
import android.os.Build.VERSION
import android.os.Build.VERSION_CODES
import android.os.Bundle
import androidx.preference.ListPreference
import androidx.preference.Preference
import androidx.preference.TwoStatePreference
import net.ashmeet.hyperlauncher.R
import net.kdt.pojavlaunch.instances.Instances.Companion.loadSelectedInstance
import net.kdt.pojavlaunch.plugins.LibraryPlugin
import net.kdt.pojavlaunch.prefs.CustomSeekBarPreference
import net.kdt.pojavlaunch.prefs.LauncherPreferences
import net.kdt.pojavlaunch.utils.MCOptionUtils
import net.kdt.pojavlaunch.utils.RendererCompatUtil

/**
 * Fragment for any settings video related
 */
class LauncherPreferenceVideoFragment : LauncherPreferenceFragment() {
    override fun onCreatePreferences(b: Bundle?, str: String?) {
        addPreferencesFromResource(R.xml.pref_video)
        val resolution = (LauncherPreferences.PREF_SCALE_FACTOR * 100).toInt()

        val resolutionSeekbar = requirePreference("resolutionRatio") as CustomSeekBarPreference
        resolutionSeekbar.setSuffix(" %")

        // #724 bug fix
        if (resolution < 25) {
            resolutionSeekbar.setValue(100)
        } else {
            resolutionSeekbar.setValue(resolution)
        }

        // Sustained performance is only available since Nougat
        val sustainedPerfSwitch = requirePreference("sustainedPerformance") as TwoStatePreference
        sustainedPerfSwitch.setVisible(VERSION.SDK_INT >= VERSION_CODES.N)
        sustainedPerfSwitch.setChecked(LauncherPreferences.PREF_SUSTAINED_PERFORMANCE)

        (requirePreference("alternate_surface") as TwoStatePreference).setChecked(
            LauncherPreferences.PREF_USE_ALTERNATE_SURFACE
        )
        (requirePreference("force_vsync") as TwoStatePreference).setChecked(
            LauncherPreferences.PREF_FORCE_VSYNC
        )

        // Show ANGLE switch only if AnglePlugin is available
        val angle: LibraryPlugin? = LibraryPlugin.Companion.discoverPlugin(
            requireContext(),
            LibraryPlugin.Companion.ID_ANGLE_PLUGIN
        )
        val angleSwitch =
            requirePreference("use_angle") as TwoStatePreference
        angleSwitch.setVisible(angle != null)
        angleSwitch.setChecked(LauncherPreferences.PREF_USE_ANGLE)

        val rendererListPreference = requirePreference("renderer") as ListPreference
        val renderersList = RendererCompatUtil.getCompatibleRenderers(requireContext())
        rendererListPreference.setEntries(renderersList.rendererDisplayNames)
        rendererListPreference.setEntryValues(renderersList.rendererIds.toTypedArray())

        // Preferred Graphics Backend (sync with options.txt)
        val backendPref = requirePreference("preferredGraphicsBackend") as ListPreference

        val selectedInstance = loadSelectedInstance()
        if (selectedInstance?.gameDirectory != null) {
            MCOptionUtils.load(selectedInstance.gameDirectory!!.absolutePath)
        } else {
            MCOptionUtils.load()
        }

        val currentBackend = MCOptionUtils.get("preferredGraphicsBackend")
        if (currentBackend != null) {
            backendPref.setValue(currentBackend.replace("\"", ""))
        }
        backendPref.setOnPreferenceChangeListener(Preference.OnPreferenceChangeListener { preference: Preference?, newValue: Any? ->
            MCOptionUtils.set("preferredGraphicsBackend", "\"" + newValue + "\"")
            MCOptionUtils.save()
            true
        })

        computeVisibility()
    }

    override fun onResume() {
        super.onResume()
        val activity: Activity? = activity
        if (activity != null) {
            requirePreference("ignoreNotch").setVisible(LauncherPreferences.hasNotch(activity))
        }
    }

    override fun onSharedPreferenceChanged(p: SharedPreferences?, s: String?) {
        super.onSharedPreferenceChanged(p, s)
        computeVisibility()
    }

    private fun computeVisibility() {
        (requirePreference("force_vsync") as TwoStatePreference)
            .setVisible(LauncherPreferences.PREF_USE_ALTERNATE_SURFACE)
    }
}
