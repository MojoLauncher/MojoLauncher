package net.kdt.pojavlaunch.utils.jre

import android.app.Activity
import android.content.Context
import androidx.appcompat.app.AlertDialog
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import net.ashmeet.hyperlauncher.R
import net.kdt.pojavlaunch.ShowErrorActivity
import net.kdt.pojavlaunch.Tools
import net.kdt.pojavlaunch.lifecycle.ContextExecutorTask

class RuntimeSelectionException(private val mRuntimeState: Int, private val mRuntimeVersion: Int) :
    Exception(), ContextExecutorTask {
    override fun executeWithActivity(activity: Activity?) {
        val act = activity ?: return
        val builder: AlertDialog.Builder = MaterialAlertDialogBuilder(act)
        builder.setTitle(R.string.runtime_error_title)
        val msgString = when (mRuntimeState) {
            RUNTIME_STATE_INSTALLATION_FAILED -> R.string.runtime_error_install_failed
            RUNTIME_STATE_INTERNAL_RUNTIME_MISSING -> R.string.runtime_error_missing
            RUNTIME_STATE_SELECTION_FAILED -> R.string.multirt_nocompatiblert
            else -> throw RuntimeException("Unknown runtime state")
        }
        builder.setMessage(act.getString(msgString, mRuntimeVersion))
        builder.setPositiveButton(android.R.string.ok, null)
        if (mRuntimeState == RUNTIME_STATE_INSTALLATION_FAILED || cause != null) {
            builder.setNegativeButton(R.string.error_show_more) { _, _ ->
                Tools.showError(
                    act,
                    R.string.runtime_error_title,
                    cause ?: this,
                    act is ShowErrorActivity
                )
            }
        }
        ShowErrorActivity.installRemoteDialogHandling(act, builder)
        builder.show()
    }

    override fun executeWithApplication(context: Context?) {}

    companion object {
        // Do not change. Android really hates when this value changes for some reason.
        @Suppress("unused")
        private const val serialVersionUID = -7482301619612640658L
        const val RUNTIME_STATE_INSTALLATION_FAILED: Int = 0
        const val RUNTIME_STATE_SELECTION_FAILED: Int = 1
        const val RUNTIME_STATE_INTERNAL_RUNTIME_MISSING: Int = 2
    }
}
