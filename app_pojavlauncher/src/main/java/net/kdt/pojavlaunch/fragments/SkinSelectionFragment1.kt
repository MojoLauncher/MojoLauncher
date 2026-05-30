package net.kdt.pojavlaunch.fragments

import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.result.ActivityResultCallback
import androidx.activity.result.contract.ActivityResultContracts.GetContent
import androidx.fragment.app.Fragment
import net.ashmeet.hyperlauncher.R
import net.kdt.pojavlaunch.Tools
import net.kdt.pojavlaunch.Tools.showError
import net.kdt.pojavlaunch.authenticator.accounts.Accounts
import net.kdt.pojavlaunch.authenticator.accounts.MinecraftAccount
import net.kdt.pojavlaunch.utils.FileUtils.ensureDirectory
import org.apache.commons.io.IOUtils
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

class SkinSelectionFragment : Fragment(R.layout.fragment_skin_selection) {
    private var mSkinPreview: ImageView? = null
    private var mAccount: MinecraftAccount? = null

    private val mSkinPickerLauncher =
        registerForActivityResult<String?, Uri?>(GetContent(), ActivityResultCallback { uri: Uri? ->
            if (uri != null) {
                saveLocalSkin(uri)
            }
        })

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        mSkinPreview = view.findViewById<ImageView>(R.id.skin_preview)
        mAccount = Accounts.current

        view.findViewById<View?>(R.id.button_pick_skin)
            .setOnClickListener(View.OnClickListener { v: View? -> mSkinPickerLauncher.launch("image/png") })
        view.findViewById<View?>(R.id.button_reset_skin)
            .setOnClickListener(View.OnClickListener { v: View? -> resetSkin() })

        updatePreview()
    }

    private fun updatePreview() {
        if (mAccount != null) {
            val head = mAccount!!.skinFacePlain
            if (head != null) {
                mSkinPreview!!.setImageBitmap(head)
            } else {
                mSkinPreview!!.setImageResource(R.drawable.ic_px_home)
            }
        }
    }

    private fun saveLocalSkin(uri: Uri) {
        if (mAccount == null) return

        try {
            // 1. Copy the skin to a permanent location
            val skinsDir = File(Tools.DIR_GAME_HOME, "skins")
            ensureDirectory(skinsDir)

            val destFile = File(skinsDir, mAccount!!.username + ".png")
            requireContext().getContentResolver().openInputStream(uri).use { `is` ->
                FileOutputStream(destFile).use { os ->
                    IOUtils.copy(`is`, os)
                }
            }

            // 2. Update account info
            mAccount!!.localSkinPath = destFile.getAbsolutePath()
            mAccount!!.save()


            // 3. Trigger re-render
            mAccount!!.updateSkinFace()

            updatePreview()
            Toast.makeText(requireContext(), R.string.skin_update_success, Toast.LENGTH_SHORT)
                .show()
        } catch (e: IOException) {
            showError(requireContext(), e)
        }
    }

    private fun resetSkin() {
        if (mAccount == null) return

        mAccount!!.localSkinPath = null
        try {
            mAccount!!.save()


            // Delete cached renders to force fallback
            val cacheDir = Tools.DIR_CACHE
            val baseName =
                "skin-face-plain-" + mAccount!!.profileId + "-" + mAccount!!.authType.name
            File(cacheDir, baseName + ".webp").delete()
            File(cacheDir, baseName.replace("-plain-", "-3d-") + ".webp").delete()

            updatePreview()
            Toast.makeText(requireContext(), R.string.skin_reset_success, Toast.LENGTH_SHORT).show()
        } catch (e: IOException) {
            showError(requireContext(), e)
        }
    }

    companion object {
        const val TAG: String = "SkinSelectionFragment"
    }
}
