package net.kdt.pojavlaunch.services

import android.Manifest
import android.annotation.SuppressLint
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.annotation.RequiresPermission
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import net.ashmeet.hyperlauncher.R
import net.kdt.pojavlaunch.Tools
import net.kdt.pojavlaunch.progresskeeper.ProgressKeeper.addTaskCountListener
import net.kdt.pojavlaunch.progresskeeper.ProgressKeeper.clearAll
import net.kdt.pojavlaunch.progresskeeper.ProgressKeeper.removeTaskCountListener
import net.kdt.pojavlaunch.progresskeeper.ProgressKeeper.taskCount
import net.kdt.pojavlaunch.progresskeeper.TaskCountListener
import net.kdt.pojavlaunch.utils.NotificationUtils

/**
 * Lazy service which allows the process not to get killed.
 * Can be created from context, can be killed statically
 */
class ProgressService : Service(), TaskCountListener {
    private var notificationManagerCompat: NotificationManagerCompat? = null

    private var mNotificationBuilder: NotificationCompat.Builder? = null

    override fun onCreate() {
        Tools.buildNotificationChannel(getApplicationContext())
        notificationManagerCompat = NotificationManagerCompat.from(getApplicationContext())
        val killIntent = Intent(getApplicationContext(), ProgressService::class.java)
        killIntent.putExtra("kill", true)
        val pendingKillIntent = PendingIntent.getService(
            this, NotificationUtils.PENDINGINTENT_CODE_KILL_PROGRESS_SERVICE,
            killIntent, if (Build.VERSION.SDK_INT >= 23) PendingIntent.FLAG_IMMUTABLE else 0
        )
        mNotificationBuilder = NotificationCompat.Builder(this, "channel_id")
            .setContentTitle(getString(R.string.lazy_service_default_title))
            .addAction(
                android.R.drawable.ic_menu_close_clear_cancel,
                getString(R.string.notification_terminate),
                pendingKillIntent
            )
            .setSmallIcon(R.drawable.notif_icon)
            .setNotificationSilent()
    }

    @SuppressLint("StringFormatInvalid")
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent != null) {
            if (intent.getBooleanExtra("kill", false)) {
                // Modified: Clear all tasks instead of killing the entire process.
                // This will naturally trigger onUpdateTaskCount(0) and stop the service.
                clearAll()
                return START_NOT_STICKY
            }
        }
        Log.d("ProgressService", "Started!")
        mNotificationBuilder!!.setContentText(
            getString(
                R.string.progresslayout_tasks_in_progress,
                taskCount
            )
        )
        val notification = mNotificationBuilder!!.build()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NotificationUtils.NOTIFICATION_ID_PROGRESS_SERVICE,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MANIFEST
            )
        } else {
            startForeground(NotificationUtils.NOTIFICATION_ID_PROGRESS_SERVICE, notification)
        }
        if (taskCount < 1) {
            stopForeground(true)
            stopSelf()
        } else addTaskCountListener(this, false)

        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onDestroy() {
        removeTaskCountListener(this)
        stopForeground(true)
    }

    @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
    override fun onUpdateTaskCount(taskCount: Int): Boolean {
        Tools.MAIN_HANDLER.post(Runnable {
            if (taskCount > 0) {
                mNotificationBuilder!!.setContentText(
                    getString(
                        R.string.progresslayout_tasks_in_progress,
                        taskCount
                    )
                )
                notificationManagerCompat!!.notify(
                    NotificationUtils.NOTIFICATION_ID_PROGRESS_SERVICE,
                    mNotificationBuilder!!.build()
                )
            } else {
                stopForeground(true)
                stopSelf()
            }
        })
        return false
    }

    override fun onTimeout(startId: Int, fgsType: Int) {
        super.onTimeout(startId, fgsType)
        stopForeground(true)
        stopSelf()
    }

    companion object {
        /** Simple wrapper to start the service  */
        @JvmStatic
        fun startService(context: Context) {
            val intent = Intent(context, ProgressService::class.java)
            ContextCompat.startForegroundService(context, intent)
        }
    }
}
