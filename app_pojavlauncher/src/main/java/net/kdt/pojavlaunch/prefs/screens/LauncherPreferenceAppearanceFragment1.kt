package net.kdt.pojavlaunch.prefs.screens

import android.content.DialogInterface
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.view.Gravity
import android.view.MotionEvent
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.core.content.edit
import androidx.preference.ListPreference
import androidx.preference.Preference
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import net.ashmeet.hyperlauncher.R
import net.kdt.pojavlaunch.extra.ExtraConstants
import net.kdt.pojavlaunch.extra.ExtraCore
import net.kdt.pojavlaunch.prefs.LauncherPreferences
import net.kdt.pojavlaunch.utils.CropperUtils
import net.kdt.pojavlaunch.utils.CropperUtils.CropperReceiver
import java.io.File
import java.io.FileOutputStream
import kotlin.math.max
import kotlin.math.min

class LauncherPreferenceAppearanceFragment : LauncherPreferenceFragment(), CropperReceiver {
    private val mCropperLauncher: ActivityResultLauncher<*> =
        CropperUtils.registerCropper(this, this)
    private var mIsPickingBackground = false

    override fun onCreatePreferences(b: Bundle?, str: String?) {
        addPreferencesFromResource(R.xml.pref_appearance)
        val prefs = LauncherPreferences.DEFAULT_PREF ?: return

        requirePreference("app_background_picker").onPreferenceClickListener = Preference.OnPreferenceClickListener {
            mIsPickingBackground = true
            CropperUtils.startCropper(mCropperLauncher)
            true
        }

        requirePreference("app_background_reset").onPreferenceClickListener = Preference.OnPreferenceClickListener {
            prefs.edit {
                putString("appBackgroundPath", null)
            }
            context?.let { LauncherPreferences.loadPreferences(it) }
            ExtraCore.setValue(ExtraConstants.REFRESH_BACKGROUND, true)
            Toast.makeText(context, "Background reset to default", Toast.LENGTH_SHORT).show()
            true
        }

        requirePreference("mouse_cursor_picker").onPreferenceClickListener = Preference.OnPreferenceClickListener {
            mIsPickingBackground = false
            CropperUtils.startCropper(mCropperLauncher)
            true
        }

        requirePreference("mouse_hotspot_picker").onPreferenceClickListener = Preference.OnPreferenceClickListener {
            showHotspotDialog()
            true
        }

        requirePreference("mouse_cursor_reset").onPreferenceClickListener = Preference.OnPreferenceClickListener {
            prefs.edit {
                putString("mouseCursorPath", null)
                putInt("mouseHotspotX", 0)
                putInt("mouseHotspotY", 0)
            }
            context?.let { LauncherPreferences.loadPreferences(it) }
            Toast.makeText(context, "Mouse cursor reset to default", Toast.LENGTH_SHORT).show()
            true
        }

        requirePreference("appearance_reset_all").onPreferenceClickListener = Preference.OnPreferenceClickListener {
            val ctx = context ?: return@OnPreferenceClickListener true
            MaterialAlertDialogBuilder(ctx)
                .setTitle("Reset Appearance")
                .setMessage("Are you sure you want to reset all appearance settings to default?")
                .setPositiveButton(android.R.string.ok) { _, _ ->
                    prefs.edit {
                        remove("appTheme")
                        remove("appBackgroundPath")
                        remove("appBackgroundBlur")
                        remove("appBackgroundBlurIntensity")
                        remove("backgroundImageOverlayEnabled")
                        remove("backgroundImageOverlayOpacity")
                        remove("animationType")
                        remove("animationIntensity")
                        remove("mouseCursorPath")
                        remove("mouseHotspotX")
                        remove("mouseHotspotY")
                    }
                    File(ctx.filesDir, "custom_background.png").delete()
                    File(ctx.filesDir, "custom_pointer.png").delete()

                    LauncherPreferences.loadPreferences(ctx)
                    ExtraCore.setValue(ExtraConstants.REFRESH_BACKGROUND, true)
                    activity?.recreate()
                    Toast.makeText(
                        ctx,
                        "All appearance settings reset",
                        Toast.LENGTH_SHORT
                    ).show()
                }
                .setNegativeButton(android.R.string.cancel, null)
                .show()
            true
        }

        val reloadListener = Preference.OnPreferenceChangeListener { preference, newValue ->
            prefs.edit {
                if (newValue is String) {
                    putString(preference.key, newValue)
                } else if (newValue is Boolean) {
                    putBoolean(preference.key, newValue)
                } else if (newValue is Int) {
                    putInt(preference.key, newValue)
                }
            }
            context?.let { LauncherPreferences.loadPreferences(it) }
            if (preference.key.startsWith("appBackground") || preference.key.startsWith("backgroundImageOverlay")) {
                ExtraCore.setValue(ExtraConstants.REFRESH_BACKGROUND, true)
            }
            true
        }

        findPreference<Preference?>("appTheme")?.onPreferenceChangeListener = reloadListener
        findPreference<Preference?>("animationType")?.onPreferenceChangeListener = reloadListener
        findPreference<Preference?>("appBackgroundBlur")?.onPreferenceChangeListener = reloadListener
        findPreference<Preference?>("appBackgroundBlurIntensity")?.onPreferenceChangeListener = reloadListener
        findPreference<Preference?>("backgroundImageOverlayEnabled")?.onPreferenceChangeListener = reloadListener
        findPreference<Preference?>("backgroundImageOverlayOpacity")?.onPreferenceChangeListener = reloadListener
    }

    private fun showHotspotDialog() {
        val ctx = context ?: return
        if (LauncherPreferences.PREF_MOUSE_CURSOR_PATH == null) {
            Toast.makeText(
                ctx,
                "Please select a custom mouse cursor first",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        val bitmap = BitmapFactory.decodeFile(LauncherPreferences.PREF_MOUSE_CURSOR_PATH)
        if (bitmap == null) {
            Toast.makeText(ctx, "Failed to load mouse cursor image", Toast.LENGTH_SHORT)
                .show()
            return
        }

        val scrollView = ScrollView(ctx)
        val root = LinearLayout(ctx)
        root.orientation = LinearLayout.VERTICAL
        root.gravity = Gravity.CENTER_HORIZONTAL
        root.setPadding(20, 20, 20, 20)
        scrollView.addView(root)

        val instructions = TextView(ctx)
        instructions.setText(R.string.mouse_hotspot_instructions)
        instructions.setPadding(0, 0, 0, 16)
        root.addView(instructions)

        val imageView = ImageView(ctx)
        imageView.setImageBitmap(bitmap)
        imageView.adjustViewBounds = true
        imageView.scaleType = ImageView.ScaleType.FIT_CENTER


        // Ensure content doesn't push dialog buttons off-screen in landscape
        val screenHeight = resources.displayMetrics.heightPixels
        val isLandscape =
            resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
        val maxContentHeight = (screenHeight * (if (isLandscape) 0.45f else 0.60f)).toInt()
        scrollView.isFillViewport = true
        scrollView.layoutParams = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            maxContentHeight
        )
        imageView.maxHeight = (maxContentHeight * 0.85f).toInt()
        imageView.layoutParams = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )

        root.addView(imageView)

        val hotspot = intArrayOf(
            LauncherPreferences.PREF_MOUSE_HOTSPOT_X,
            LauncherPreferences.PREF_MOUSE_HOTSPOT_Y
        )

        val dialog = MaterialAlertDialogBuilder(ctx)
            .setTitle("Select Click Point")
            .setView(scrollView)
            .setPositiveButton(R.string.mouse_hotspot_done) { _, _ ->
                LauncherPreferences.DEFAULT_PREF?.edit {
                    putInt("mouseHotspotX", hotspot[0])
                    putInt("mouseHotspotY", hotspot[1])
                }
                LauncherPreferences.loadPreferences(ctx)
            }
            .setNegativeButton(android.R.string.cancel, null)
            .create()

        imageView.setOnTouchListener { v, event ->
            if (event.action == MotionEvent.ACTION_DOWN) {
                val touchX = event.x
                val touchY = event.y

                val viewWidth = v.width.toFloat()
                val viewHeight = v.height.toFloat()
                val bitmapWidth = bitmap.width.toFloat()
                val bitmapHeight = bitmap.height.toFloat()

                val scale = min(viewWidth / bitmapWidth, viewHeight / bitmapHeight)
                val dx = (viewWidth - bitmapWidth * scale) / 2f
                val dy = (viewHeight - bitmapHeight * scale) / 2f

                hotspot[0] = ((touchX - dx) / scale).toInt()
                hotspot[1] = ((touchY - dy) / scale).toInt()

                hotspot[0] = max(0, min(bitmap.width, hotspot[0]))
                hotspot[1] = max(0, min(bitmap.height, hotspot[1]))

                Toast.makeText(
                    ctx,
                    ctx.getString(R.string.mouse_hotspot_set, hotspot[0], hotspot[1]),
                    Toast.LENGTH_SHORT
                ).show()
                v.performClick()
            }
            true
        }

        dialog.show()
    }

    override val aspectRatio: Float
        get() = 0f

    override val targetMaxSide: Int
        get() = if (mIsPickingBackground) 2048 else 512

    override fun onCropped(contentBitmap: Bitmap?) {
        val ctx = context ?: return
        val bitmap = contentBitmap ?: return onFailed(IllegalArgumentException("Cropped bitmap is null"))
        try {
            if (mIsPickingBackground) {
                val bgFile = File(ctx.filesDir, "custom_background.png")
                FileOutputStream(bgFile).use { out ->
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
                }

                LauncherPreferences.DEFAULT_PREF?.edit {
                    putString("appBackgroundPath", bgFile.absolutePath)
                }
                LauncherPreferences.loadPreferences(ctx)
                ExtraCore.setValue(ExtraConstants.REFRESH_BACKGROUND, true)
                Toast.makeText(ctx, "Launcher background updated", Toast.LENGTH_SHORT).show()
            } else {
                val pointerFile = File(ctx.filesDir, "custom_pointer.png")
                FileOutputStream(pointerFile).use { out ->
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
                }

                LauncherPreferences.DEFAULT_PREF?.edit {
                    putString("mouseCursorPath", pointerFile.absolutePath)
                    putInt("mouseHotspotX", 0)
                    putInt("mouseHotspotY", 0)
                }
                LauncherPreferences.loadPreferences(ctx)
                Toast.makeText(
                    ctx,
                    "Mouse cursor updated. Now set the click point.",
                    Toast.LENGTH_LONG
                ).show()
                showHotspotDialog()
            }
        } catch (e: Exception) {
            onFailed(e)
        }
    }

    override fun onFailed(exception: Exception?) {
        Toast.makeText(context, "Failed: ${exception?.message}", Toast.LENGTH_LONG).show()
    }
}
