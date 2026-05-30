package net.kdt.pojavlaunch.prefs.screens

import android.graphics.Bitmap
import android.graphics.Color
import android.os.Bundle
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.preference.ListPreference
import androidx.preference.Preference
import androidx.preference.TwoStatePreference
import net.ashmeet.hyperlauncher.R
import net.kdt.pojavlaunch.colorselector.ColorSelectionListener
import net.kdt.pojavlaunch.colorselector.ColorSelector
import net.kdt.pojavlaunch.prefs.CustomSeekBarPreference
import net.kdt.pojavlaunch.prefs.LauncherPreferences
import net.kdt.pojavlaunch.utils.CropperUtils
import net.kdt.pojavlaunch.utils.CropperUtils.CropperReceiver
import java.io.File
import java.io.FileOutputStream

class LauncherPreferenceDrawerButtonFragment : LauncherPreferenceFragment(), CropperReceiver {
    private val mCropperLauncher: ActivityResultLauncher<*> =
        CropperUtils.registerCropper(this, this)
    private var mIconOutlineColorSelector: ColorSelector? = null

    override fun onCreatePreferences(b: Bundle?, str: String?) {
        addPreferencesFromResource(R.xml.pref_drawer_button)
        val prefs = LauncherPreferences.DEFAULT_PREF ?: return

        requirePreference("drawer_button_image_picker").setOnPreferenceClickListener(Preference.OnPreferenceClickListener { preference: Preference? ->
            CropperUtils.startCropper(mCropperLauncher)
            true
        })

        requirePreference("icon_outline_color_picker").setOnPreferenceClickListener(Preference.OnPreferenceClickListener { preference: Preference? ->
            showIconOutlineColorPickerDialog()
            true
        })

        requirePreference("drawer_button_reset").setOnPreferenceClickListener(Preference.OnPreferenceClickListener { preference: Preference? ->
            prefs.edit()
                .putInt("drawerButtonX", 50)
                .putInt("drawerButtonY", 0)
                .putInt("drawerButtonBgOpacity", 33)
                .putInt("drawerButtonIconOpacity", 100)
                .putInt("drawerButtonSize", 50)
                .putInt("drawerButtonCornerRadius", 8)
                .putBoolean("drawerButtonMovable", false)
                .putString("drawerButtonImagePath", null)
                .putString("drawerButtonPreset", "top_center")
                .putInt("drawerListOpacity", 100)
                .putInt("iconOutlineColor", Color.WHITE)
                .apply()
            context?.let { LauncherPreferences.loadPreferences(it) }


            // Sync UI
            (requirePreference("drawerButtonPreset") as ListPreference).setValue("top_center")
            (requirePreference("drawerButtonX") as CustomSeekBarPreference).setValue(50)
            (requirePreference("drawerButtonY") as CustomSeekBarPreference).setValue(0)
            (requirePreference("drawerButtonSize") as CustomSeekBarPreference).setValue(50)
            (requirePreference("drawerButtonCornerRadius") as CustomSeekBarPreference).setValue(8)
            (requirePreference("drawerButtonBgOpacity") as CustomSeekBarPreference).setValue(33)
            (requirePreference("drawerButtonIconOpacity") as CustomSeekBarPreference).setValue(100)
            (requirePreference("drawerButtonMovable") as TwoStatePreference).setChecked(false)
            (requirePreference("drawerListOpacity") as CustomSeekBarPreference).setValue(100)

            Toast.makeText(context, "Drawer button reset to default", Toast.LENGTH_SHORT)
                .show()
            true
        })

        val presetPref = requirePreference("drawerButtonPreset") as ListPreference
        val xPref = requirePreference("drawerButtonX") as CustomSeekBarPreference
        val yPref = requirePreference("drawerButtonY") as CustomSeekBarPreference

        presetPref.setOnPreferenceChangeListener { _, newValue ->
            val `val` = newValue as String
            if (`val` == "custom") return@setOnPreferenceChangeListener true

            var x = 50
            var y = 50
            when (`val`) {
                "top_left" -> {
                    x = 0
                    y = 0
                }

                "top_center" -> {
                    x = 50
                    y = 0
                }

                "top_right" -> {
                    x = 100
                    y = 0
                }

                "bottom_left" -> {
                    x = 0
                    y = 100
                }

                "bottom_center" -> {
                    x = 50
                    y = 100
                }

                "bottom_right" -> {
                    x = 100
                    y = 100
                }

                "center_left" -> {
                    x = 0
                    y = 50
                }

                "center_right" -> {
                    x = 100
                    y = 50
                }

                "center" -> {
                    x = 50
                    y = 50
                }
            }

            xPref.setValue(x)
            yPref.setValue(y)
            true
        }

        val manualPosListener =
            Preference.OnPreferenceChangeListener { preference: Preference?, newValue: Any? ->
                presetPref.setValue("custom")
                true
            }

        xPref.setOnPreferenceChangeListener(manualPosListener)
        yPref.setOnPreferenceChangeListener(manualPosListener)
    }

    private fun showIconOutlineColorPickerDialog() {
        val root = requireActivity().findViewById<ViewGroup>(android.R.id.content)
        if (mIconOutlineColorSelector == null) {
            mIconOutlineColorSelector =
                ColorSelector(requireContext(), root, object : ColorSelectionListener {
                    override fun onColorSelected(color: Int) {
                        LauncherPreferences.DEFAULT_PREF?.edit()
                            ?.putInt("iconOutlineColor", color)
                            ?.apply()
                        context?.let { LauncherPreferences.loadPreferences(it) }
                    }
                })
            mIconOutlineColorSelector!!.setAlphaEnabled(true)
            mIconOutlineColorSelector!!.setTitle(R.string.customctrl_stroke_color)
        }
        mIconOutlineColorSelector!!.show(true, LauncherPreferences.PREF_ICON_OUTLINE_COLOR)
    }

    override val aspectRatio: Float
        get() = 1f

    override val targetMaxSide: Int
        get() = 256

    override fun onCropped(contentBitmap: Bitmap?) {
        val ctx = context ?: return
        val bitmap = contentBitmap ?: return onFailed(IllegalArgumentException("Cropped bitmap is null"))
        try {
            val drawerFile = File(ctx.filesDir, "custom_drawer_button.png")
            val out = FileOutputStream(drawerFile)
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            out.close()

            LauncherPreferences.DEFAULT_PREF?.edit()
                ?.putString("drawerButtonImagePath", drawerFile.getAbsolutePath())
                ?.apply()
            LauncherPreferences.loadPreferences(ctx)
            Toast.makeText(ctx, "Drawer button image updated", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            onFailed(e)
        }
    }

    override fun onFailed(exception: Exception?) {
        Toast.makeText(context, "Failed: " + exception?.message, Toast.LENGTH_LONG).show()
    }

    override fun onDestroy() {
        super.onDestroy()
        if (mIconOutlineColorSelector != null) {
            mIconOutlineColorSelector!!.disappear(true)
        }
    }
}
