package net.kdt.pojavlaunch

import android.app.Activity
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import net.ashmeet.hyperlauncher.R
import net.kdt.pojavlaunch.lifecycle.ContextExecutorTask
import net.kdt.pojavlaunch.utils.NotificationUtils
import net.kdt.pojavlaunch.utils.NotificationUtils.sendBasicNotification
import java.io.Serializable

class ShowErrorActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val intent = intent
        if (intent == null) {
            finish()
            return
        }
        @Suppress("DEPRECATION")
        val remoteErrorTask =
            intent.getSerializableExtra(ERROR_ACTIVITY_REMOTE_TASK) as RemoteErrorTask?
        if (remoteErrorTask == null) {
            finish()
            return
        }
        remoteErrorTask.executeWithActivity(this)
    }


    class RemoteErrorTask(private val mThrowable: Throwable?, private val mRolledMsg: String?) :
        ContextExecutorTask, Serializable {
        override fun executeWithActivity(activity: Activity?) {
            val act = activity ?: return
            val throwable = mThrowable ?: return

            if (throwable is ContextExecutorTask) {
                throwable.executeWithActivity(act)
            } else {
                Tools.showError(act, mRolledMsg, throwable, act is ShowErrorActivity)
            }
        }

        override fun executeWithApplication(context: Context?) {
            if (context == null) return
            val showErrorIntent = Intent(context, ShowErrorActivity::class.java)
            showErrorIntent.putExtra(ERROR_ACTIVITY_REMOTE_TASK, this)
            sendBasicNotification(
                context,
                R.string.notif_error_occured,
                R.string.notif_error_occured_desc,
                showErrorIntent,
                NotificationUtils.PENDINGINTENT_CODE_SHOW_ERROR,
                NotificationUtils.NOTIFICATION_ID_SHOW_ERROR
            )
        }
    }

    companion object {
        private const val ERROR_ACTIVITY_REMOTE_TASK = "remoteTask"

        /**
         * Install remote dialog handling onto a dialog. This should be used when the dialog is planned to be presented
         * through Tools.showError or Tools.showErrorRemote as a Throwable implementing a ContextExecutorTask.
         * @param callerActivity the activity provided by the ContextExecutorTask.executeWithActivity
         * @param builder the alert dialog builder.
         */
        fun installRemoteDialogHandling(callerActivity: Activity?, builder: AlertDialog.Builder) {
            if (callerActivity is ShowErrorActivity) {
                builder.setOnDismissListener { callerActivity.finish() }
            }
        }
    }
}
