package net.kdt.pojavlaunch.prefs.screens

import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.annotation.RequiresApi
import androidx.preference.EditTextPreference
import net.ashmeet.hyperlauncher.R
import net.kdt.pojavlaunch.Architecture
import net.kdt.pojavlaunch.Tools
import net.kdt.pojavlaunch.contracts.OpenDocumentWithExtension
import net.kdt.pojavlaunch.multirt.MultiRTConfigDialog
import net.kdt.pojavlaunch.prefs.CustomSeekBarPreference
import net.kdt.pojavlaunch.prefs.LauncherPreferences
import kotlin.math.min

class LauncherPreferenceJavaFragment : LauncherPreferenceFragment() {
    private var mDialogScreen: MultiRTConfigDialog? = null
    private val mVmInstallLauncher = registerForActivityResult(
        OpenDocumentWithExtension("xz")
    ) { data: Uri? ->
        if (data != null) Tools.installRuntimeFromUri(requireContext(), data)
    }

    @RequiresApi(Build.VERSION_CODES.S)
    override fun onCreatePreferences(b: Bundle?, str: String?) {
        val ramAllocation = LauncherPreferences.PREF_RAM_ALLOCATION
        // Triggers a write for some reason
        addPreferencesFromResource(R.xml.pref_java)

        val memorySeekbar = requirePreference(
            "allocation",
            CustomSeekBarPreference::class.java
        )

        val deviceRam = Tools.getTotalDeviceMemory(memorySeekbar.context)

        val maxRAM = if (Architecture.is32BitsDevice() || deviceRam < 2048) {
            min(1024, deviceRam)
        } else {
            deviceRam - (if (deviceRam < 3064) 800 else 1024) //To have a minimum for the device to breathe
        }

        memorySeekbar.setMaxKeepIncrement(maxRAM)
        memorySeekbar.value = ramAllocation
        memorySeekbar.setSuffix(" MB")

        findPreference<EditTextPreference>("javaArgs")?.setOnBindEditTextListener { obj ->
            obj.setSingleLine()
        }

        requirePreference("install_jre").setOnPreferenceClickListener {
            openMultiRTDialog()
            true
        }
    }

    private fun openMultiRTDialog() {
        if (mDialogScreen == null) {
            mDialogScreen = MultiRTConfigDialog()
            mDialogScreen!!.prepare(requireContext(), mVmInstallLauncher)
        }
        mDialogScreen!!.show()
    }
}
