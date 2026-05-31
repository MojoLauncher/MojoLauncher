package net.kdt.pojavlaunch

import android.annotation.SuppressLint
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.annotation.Keep
import androidx.compose.runtime.*
import net.ashmeet.hyperlauncher.R
import net.kdt.pojavlaunch.ui.screens.ExitScreen
import net.kdt.pojavlaunch.ui.theme.PojavTheme
import java.io.File
import java.io.IOException

@Keep
class ExitActivity : BaseActivity() {

    @SuppressLint("StringFormatInvalid")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        var code = -1
        var isSignal = false
        val extras = intent.extras
        if (extras != null) {
            code = extras.getInt("code", -1)
            isSignal = extras.getBoolean("isSignal", false)
        }

        val title = if (isSignal) getString(R.string.mcn_abort_title) else getString(
            R.string.mcn_exit_title,
            code
        )

        setContent {
            PojavTheme(dynamicColor = true) {
                var logs by remember { mutableStateOf("Loading logs...") }

                LaunchedEffect(Unit) {
                    try {
                        val logFile = File(Tools.DIR_GAME_HOME, "latestlog.txt")
                        if (logFile.exists()) {
                            logs = Tools.read(logFile) ?: "Log file is empty"
                        } else {
                            logs = "No log file found at ${logFile.absolutePath}"
                        }
                    } catch (e: IOException) {
                        logs = "Failed to read logs: ${e.message}"
                    }
                }

                ExitScreen(
                    title = title,
                    logs = logs,
                    onShareClick = { Tools.shareLog(this@ExitActivity) },
                    onCopyClick = {
                        val clipboard = getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
                        val clip = ClipData.newPlainText("PojavLauncher Log", logs)
                        clipboard.setPrimaryClip(clip)
                        Toast.makeText(this@ExitActivity, "Logs copied to clipboard", Toast.LENGTH_SHORT).show()
                    },
                    // ✅ Fix: Go back to main menu instead of killing the app process
                    onRestartClick = { Tools.exitToLauncher(this@ExitActivity) },
                    onOpenCrashReport = { path ->
                        val file = File(path)
                        if (file.exists()) {
                            Tools.openPath(this@ExitActivity, file, false)
                        } else {
                            Toast.makeText(this@ExitActivity, "Crash report file not found", Toast.LENGTH_SHORT).show()
                        }
                    }
                )
            }
        }
    }

    companion object {
        @JvmStatic
        @Suppress("unused") //used by native jre_launcher_new
        fun showExitMessage(ctx: Context?, code: Int, isSignal: Boolean) {
            if (ctx == null) {
                System.exit(0)
                return
            }

            if (!isSignal && code == 0) {
                try {
                    val intent = Intent(ctx, LauncherActivity::class.java)
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
                    ctx.startActivity(intent)
                } catch (e: Exception) {
                    Log.e("ExitActivity", "Failed to start LauncherActivity, falling back to restart", e)
                    Tools.restart(ctx)
                    return
                }
                System.exit(0)
                return
            }

            val lock = Any()
            Tools.runOnUiThread {
                val i = Intent(ctx, ExitActivity::class.java).apply {
                    putExtra("code", code)
                    putExtra("isSignal", isSignal)
                    addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                ctx.startActivity(i)
                synchronized(lock) {
                    (lock as Object).notify()
                }
            }
            synchronized(lock) {
                try {
                    (lock as Object).wait()
                } catch (e: InterruptedException) {
                    Log.e("ExitActivity", "Waiting on lock failed: $e")
                }
            }
            System.exit(0)
        }
    }
}
