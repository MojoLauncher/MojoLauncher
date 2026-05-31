package net.kdt.pojavlaunch.fragments

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.compose.ui.platform.ComposeView
import androidx.fragment.app.Fragment
import net.kdt.pojavlaunch.Tools
import net.kdt.pojavlaunch.extra.ExtraConstants
import net.kdt.pojavlaunch.extra.ExtraCore
import net.kdt.pojavlaunch.prefs.LauncherPreferences
import net.kdt.pojavlaunch.ui.screens.SettingsScreen
import net.kdt.pojavlaunch.ui.theme.PojavTheme
import net.kdt.pojavlaunch.utils.CropperUtils
import net.kdt.pojavlaunch.utils.CropperUtils.CropperReceiver
import java.io.File
import java.io.FileOutputStream

class SettingsFragment : Fragment(), CropperReceiver {
    private lateinit var mCropperLauncher: ActivityResultLauncher<Array<String>>
    
    enum class PickingMode {
        BACKGROUND,
        MOUSE_POINTER,
        DRAWER_BUTTON
    }
    private var mPickingMode = PickingMode.BACKGROUND

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mCropperLauncher = CropperUtils.registerCropper(this, this)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setContent {
                PojavTheme(dynamicColor = true) {
                    SettingsScreen(
                        onBack = {
                            Tools.backToMainMenu(requireActivity())
                        },
                        onPickBackground = {
                            mPickingMode = PickingMode.BACKGROUND
                            CropperUtils.startCropper(mCropperLauncher)
                        },
                        onPickMousePointer = {
                            mPickingMode = PickingMode.MOUSE_POINTER
                            CropperUtils.startCropper(mCropperLauncher)
                        },
                        onPickDrawerButtonImage = {
                            mPickingMode = PickingMode.DRAWER_BUTTON
                            CropperUtils.startCropper(mCropperLauncher)
                        }
                    )
                }
            }
        }
    }

    override val aspectRatio: Float
        get() = 0f

    override val targetMaxSide: Int
        get() = when (mPickingMode) {
            PickingMode.BACKGROUND -> 2048
            PickingMode.MOUSE_POINTER -> 512
            PickingMode.DRAWER_BUTTON -> 512
        }

    override fun onCropped(contentBitmap: Bitmap?) {
        val ctx = context ?: return
        val bitmap = contentBitmap ?: return onFailed(IllegalArgumentException("Cropped bitmap is null"))
        try {
            when (mPickingMode) {
                PickingMode.BACKGROUND -> {
                    val bgFile = File(ctx.filesDir, "custom_background.png")
                    FileOutputStream(bgFile).use { out ->
                        bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
                    }

                    LauncherPreferences.DEFAULT_PREF?.edit()?.putString("appBackgroundPath", bgFile.absolutePath)?.apply()
                    LauncherPreferences.loadPreferences(ctx)
                    ExtraCore.setValue(ExtraConstants.REFRESH_BACKGROUND, true)
                    Toast.makeText(ctx, "Launcher background updated", Toast.LENGTH_SHORT).show()
                }
                PickingMode.MOUSE_POINTER -> {
                    val pointerFile = File(ctx.filesDir, "custom_pointer.png")
                    FileOutputStream(pointerFile).use { out ->
                        bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
                    }

                    LauncherPreferences.DEFAULT_PREF?.edit()?.apply {
                        putString("mouseCursorPath", pointerFile.absolutePath)
                        putInt("mouseHotspotX", 0)
                        putInt("mouseHotspotY", 0)
                    }?.apply()
                    LauncherPreferences.loadPreferences(ctx)
                    Toast.makeText(
                        ctx,
                        "Mouse cursor updated. Now set the click point.",
                        Toast.LENGTH_LONG
                    ).show()
                }
                PickingMode.DRAWER_BUTTON -> {
                    val drawerFile = File(ctx.filesDir, "custom_drawer_button.png")
                    FileOutputStream(drawerFile).use { out ->
                        bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
                    }

                    LauncherPreferences.DEFAULT_PREF?.edit()?.apply {
                        putString("drawerButtonImagePath", drawerFile.absolutePath)
                        putString("drawerButtonPreset", "custom")
                    }?.apply()
                    LauncherPreferences.loadPreferences(ctx)
                    Toast.makeText(ctx, "Drawer button image updated", Toast.LENGTH_SHORT).show()
                }
            }
        } catch (e: Exception) {
            onFailed(e)
        }
    }

    override fun onFailed(exception: Exception?) {
        Toast.makeText(context, "Failed: ${exception?.message}", Toast.LENGTH_LONG).show()
    }

    companion object {
        const val TAG = "SettingsFragment"
    }
}
