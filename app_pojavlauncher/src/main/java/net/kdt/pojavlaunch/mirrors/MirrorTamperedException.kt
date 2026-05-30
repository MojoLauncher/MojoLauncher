package net.kdt.pojavlaunch.mirrors

import android.app.Activity
import android.content.Context
import android.content.DialogInterface
import android.text.Html
import androidx.appcompat.app.AlertDialog
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import net.ashmeet.hyperlauncher.R
import net.kdt.pojavlaunch.ShowErrorActivity
import net.kdt.pojavlaunch.lifecycle.ContextExecutorTask
import net.kdt.pojavlaunch.prefs.LauncherPreferences

class MirrorTamperedException : Exception(), ContextExecutorTask {
    override fun executeWithActivity(activity: Activity?) {
        val act = activity ?: return
        val builder: AlertDialog.Builder = MaterialAlertDialogBuilder(act)
        builder.setTitle(R.string.dl_tampered_manifest_title)
        builder.setMessage(Html.fromHtml(act.getString(R.string.dl_tampered_manifest)))
        addButtons(builder)
        ShowErrorActivity.installRemoteDialogHandling(act, builder)
        builder.show()
    }

    private fun addButtons(builder: AlertDialog.Builder) {
        builder.setPositiveButton(
            R.string.dl_switch_to_official_site,
            DialogInterface.OnClickListener { d: DialogInterface?, w: Int ->
                LauncherPreferences.DEFAULT_PREF?.edit()?.putString("downloadSource", "default")
                    ?.apply()
                LauncherPreferences.PREF_DOWNLOAD_SOURCE = "default"
            })
        builder.setNegativeButton(
            R.string.dl_turn_off_manifest_checks,
            DialogInterface.OnClickListener { d: DialogInterface?, w: Int ->
                LauncherPreferences.DEFAULT_PREF?.edit()?.putBoolean("verifyManifest", false)?.apply()
                LauncherPreferences.PREF_VERIFY_MANIFEST = false
            })
        builder.setNeutralButton(
            android.R.string.cancel,
            DialogInterface.OnClickListener { d: DialogInterface?, w: Int -> })
    }

    override fun executeWithApplication(context: Context?) {}

    companion object {
        // Do not change. Android really hates when this value changes for some reason.
        private val serialVersionUID = -7482301619612640658L
    }
}
