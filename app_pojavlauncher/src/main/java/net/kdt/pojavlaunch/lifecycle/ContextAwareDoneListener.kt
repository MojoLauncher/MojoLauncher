package net.kdt.pojavlaunch.lifecycle

import android.app.Activity
import android.content.Context
import android.content.Intent
import net.ashmeet.hyperlauncher.R
import net.kdt.pojavlaunch.MainActivity
import net.kdt.pojavlaunch.Tools
import net.kdt.pojavlaunch.progresskeeper.ProgressKeeper
import net.kdt.pojavlaunch.tasks.AsyncMinecraftDownloader.DoneListener
import net.kdt.pojavlaunch.utils.NotificationUtils
import java.io.File

class ContextAwareDoneListener(baseContext: Context, private val mNormalizedVersionid: String?) :
    DoneListener, ContextExecutorTask {
    private val mErrorString: String
    private var mClassPath: Array<File> = emptyArray()

    init {
        this.mErrorString = baseContext.getString(R.string.mc_download_failed)
    }

    private fun createGameStartIntent(context: Context?): Intent {
        val mainIntent = Intent(context, MainActivity::class.java)
        mainIntent.putExtra(MainActivity.INTENT_MINECRAFT_VERSION, mNormalizedVersionid)
        // Optionally pass classpath if needed by MainActivity
        // mainIntent.putExtra("classPath", mClassPath.map { it.absolutePath }.toTypedArray())
        mainIntent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
        return mainIntent
    }

    override fun onDownloadDone(classPath: Array<File>) {
        this.mClassPath = classPath
        ProgressKeeper.waitUntilDone(Runnable { ContextExecutor.execute(this) })
    }

    override fun onDownloadFailed(throwable: Throwable?) {
        Tools.showErrorRemote(mErrorString, throwable)
    }

    override fun executeWithActivity(activity: Activity?) {
        val act = activity ?: return
        try {
            val gameStartIntent = createGameStartIntent(act)
            act.startActivity(gameStartIntent)
            // Removed activity.finish() and killProcess to keep the launcher alive in the background.
            // This allows the launcher to be immediately available when the game process exits.
        } catch (e: Throwable) {
            Tools.showError(act.baseContext, e)
        }
    }

    override fun executeWithApplication(context: Context?) {
        if (context == null) return
        val gameStartIntent = createGameStartIntent(context)
        // Since the game is a separate process anyway, it does not matter if it gets invoked
        // from somewhere other than the launcher activity.
        // The only problem may arise if the launcher starts doing something when the user starts the notification.
        // So, the notification is automatically removed once there are tasks ongoing in the ProgressKeeper
        NotificationUtils.sendBasicNotification(
            context,
            R.string.notif_download_finished,
            R.string.notif_download_finished_desc,
            gameStartIntent,
            NotificationUtils.PENDINGINTENT_CODE_GAME_START,
            NotificationUtils.NOTIFICATION_ID_GAME_START
        )
        // You should keep yourself safe, NOW!
        // otherwise android does weird things... LOL
    }
}
