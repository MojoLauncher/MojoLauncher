package net.kdt.pojavlaunch.prefs.screens

import android.content.SharedPreferences
import android.os.Build
import android.os.Bundle
import androidx.annotation.RequiresApi
import androidx.preference.PreferenceCategory
import net.ashmeet.hyperlauncher.R
import net.kdt.pojavlaunch.Tools
import net.kdt.pojavlaunch.prefs.CustomSeekBarPreference
import net.kdt.pojavlaunch.prefs.LauncherPreferences

class LauncherPreferenceControlFragment : LauncherPreferenceFragment() {
    private var mGyroAvailable = false
    @RequiresApi(Build.VERSION_CODES.S)
    override fun onCreatePreferences(b: Bundle?, str: String?) {
        // Get values
        val longPressTrigger = LauncherPreferences.PREF_LONGPRESS_TRIGGER
        val prefButtonSize = LauncherPreferences.PREF_BUTTONSIZE.toInt()
        val mouseScale = (LauncherPreferences.PREF_MOUSESCALE * 100).toInt()
        val gyroSampleRate = LauncherPreferences.PREF_GYRO_SAMPLE_RATE
        val mouseSpeed = LauncherPreferences.PREF_MOUSESPEED
        val gyroSpeed = LauncherPreferences.PREF_GYRO_SENSITIVITY
        val joystickDeadzone = LauncherPreferences.PREF_DEADZONE_SCALE


        //Triggers a write for some reason which resets the value
        addPreferencesFromResource(R.xml.pref_control)

        val seek2 = requirePreference("timeLongPressTrigger") as CustomSeekBarPreference
        seek2.setValue(longPressTrigger)
        seek2.setSuffix(" ms")

        val seek3 = requirePreference("buttonscale") as CustomSeekBarPreference
        seek3.setValue(prefButtonSize)
        seek3.setSuffix(" %")

        val seek4 = requirePreference("mousescale") as CustomSeekBarPreference
        seek4.setValue(mouseScale)
        seek4.setSuffix(" %")

        val seek6 = requirePreference("mousespeed") as CustomSeekBarPreference
        seek6.setValue((mouseSpeed * 100f).toInt())
        seek6.setSuffix(" %")

        val deadzoneSeek = requirePreference("gamepad_deadzone_scale") as CustomSeekBarPreference
        deadzoneSeek.setValue((joystickDeadzone * 100f).toInt())
        deadzoneSeek.setSuffix(" %")


        val context = getContext()
        if (context != null) {
            mGyroAvailable = Tools.deviceSupportsGyro(context)
        }
        val gyroCategory = requirePreference("gyroCategory") as PreferenceCategory
        gyroCategory.setVisible(mGyroAvailable)

        val gyroSensitivitySeek = requirePreference("gyroSensitivity") as CustomSeekBarPreference
        gyroSensitivitySeek.setValue((gyroSpeed * 100f).toInt())
        gyroSensitivitySeek.setSuffix(" %")

        val gyroSampleRateSeek = requirePreference("gyroSampleRate") as CustomSeekBarPreference
        gyroSampleRateSeek.setValue(gyroSampleRate)
        gyroSampleRateSeek.setSuffix(" ms")
        computeVisibility()
    }

    override fun onSharedPreferenceChanged(p: SharedPreferences?, s: String?) {
        super.onSharedPreferenceChanged(p, s)
        computeVisibility()
    }

    private fun computeVisibility() {
        requirePreference("timeLongPressTrigger").setVisible(!LauncherPreferences.PREF_DISABLE_GESTURES)
        requirePreference("gyroSensitivity").setVisible(LauncherPreferences.PREF_ENABLE_GYRO)
        requirePreference("gyroSampleRate").setVisible(LauncherPreferences.PREF_ENABLE_GYRO)
        requirePreference("gyroInvertX").setVisible(LauncherPreferences.PREF_ENABLE_GYRO)
        requirePreference("gyroInvertY").setVisible(LauncherPreferences.PREF_ENABLE_GYRO)
        requirePreference("gyroSmoothing").setVisible(LauncherPreferences.PREF_ENABLE_GYRO)
    }
}
