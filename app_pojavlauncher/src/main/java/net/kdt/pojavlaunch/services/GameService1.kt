package net.kdt.pojavlaunch.services

import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Binder
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import net.ashmeet.hyperlauncher.R
import net.kdt.pojavlaunch.MainActivity
import net.kdt.pojavlaunch.Tools
import net.kdt.pojavlaunch.utils.NotificationUtils

class GameService : Service() {
    private val mLocalBinder = LocalBinder()

    override fun onCreate() {
        Tools.buildNotificationChannel(getApplicationContext())
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent != null && intent.getBooleanExtra("kill", false)) {
            stopSelf()
            Tools.restart(this)
            return START_NOT_STICKY
        }
        val killIntent = Intent(getApplicationContext(), GameService::class.java)
        killIntent.putExtra("kill", true)
        val pendingKillIntent = PendingIntent.getService(
            this, NotificationUtils.PENDINGINTENT_CODE_KILL_GAME_SERVICE,
            killIntent, if (Build.VERSION.SDK_INT >= 23) PendingIntent.FLAG_IMMUTABLE else 0
        )
        val contentIntent = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java).setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT),
            PendingIntent.FLAG_IMMUTABLE
        )

        val notificationBuilder = NotificationCompat.Builder(this, "channel_id")
            .setContentTitle(getString(R.string.lazy_service_default_title))
            .setContentText(getString(R.string.notification_game_runs))
            .setContentIntent(contentIntent)
            .addAction(
                android.R.drawable.ic_menu_close_clear_cancel,
                getString(R.string.notification_terminate),
                pendingKillIntent
            )
            .setSmallIcon(R.drawable.notif_icon)
            .setNotificationSilent()

        val notification = notificationBuilder.build()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NotificationUtils.NOTIFICATION_ID_GAME_SERVICE,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MANIFEST
            )
        } else {
            startForeground(NotificationUtils.NOTIFICATION_ID_GAME_SERVICE, notification)
        }
        return START_NOT_STICKY // non-sticky so android wont try restarting the game after the user uses the "Quit" button
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        //At this point in time  only the game runs and the user poofed the window, time to die
        stopSelf()
        Tools.restart(this)
    }

    override fun onBind(intent: Intent?): IBinder? {
        return mLocalBinder
    }

    class LocalBinder : Binder() {
        @JvmField
        var isActive: Boolean = false
    }

    companion object {
    }
}
