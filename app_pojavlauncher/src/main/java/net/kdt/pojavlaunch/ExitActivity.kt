package net.kdt.pojavlaunch

import android.annotation.SuppressLint
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.Keep
import androidx.appcompat.app.AppCompatActivity
import net.ashmeet.hyperlauncher.R
import java.io.File
import java.io.IOException

@Keep
class ExitActivity : AppCompatActivity() {
    @SuppressLint("StringFormatInvalid")
    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Tools.setInsetsMode(this, true, true)
        setContentView(R.layout.activity_exit)

        var code = -1
        var isSignal = false
        val extras = getIntent().getExtras()
        if (extras != null) {
            code = extras.getInt("code", -1)
            isSignal = extras.getBoolean("isSignal", false)
        }

        val message = if (isSignal) getString(R.string.mcn_abort_title) else getString(
            R.string.mcn_exit_title,
            code
        )

        val titleView = findViewById<TextView>(R.id.exit_title)
        titleView.setText(message)

        val logTextView = findViewById<TextView>(R.id.exit_log_text)
        val scrollView = findViewById<ScrollView>(R.id.exit_scroll_view)

        try {
            val logFile = File(Tools.DIR_GAME_HOME, "latestlog.txt")
            if (logFile.exists()) {
                val logs = Tools.read(logFile)
                logTextView.setText(logs)
                // Scroll to bottom
                scrollView.post(Runnable { scrollView.fullScroll(ScrollView.FOCUS_DOWN) })
            } else {
                logTextView.setText("No log file found at " + logFile.getAbsolutePath())
            }
        } catch (e: IOException) {
            logTextView.setText("Failed to read logs: " + e.message)
        }

        val shareButton = findViewById<Button?>(R.id.exit_share_button)
        if (shareButton != null) {
            shareButton.setOnClickListener(View.OnClickListener { v: View? -> Tools.shareLog(this) })
        }

        val copyButton = findViewById<Button?>(R.id.exit_copy_button)
        if (copyButton != null) {
            copyButton.setOnClickListener(View.OnClickListener { v: View? ->
                val clipboard = getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
                val clip = ClipData.newPlainText("PojavLauncher Log", logTextView.getText())
                clipboard.setPrimaryClip(clip)
                Toast.makeText(this, "copied", Toast.LENGTH_SHORT).show()
            })
        }

        val restartButton = findViewById<Button?>(R.id.exit_restart_button)
        if (restartButton != null) {
            restartButton.setOnClickListener(View.OnClickListener { v: View? ->
                Tools.restart(this)
            })
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
                // If the game exited normally, try to bring the launcher back to front.
                // Using an Intent instead of a full restart is faster and preserves launcher state.
                try {
                    val intent = Intent(ctx, LauncherActivity::class.java)
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
                    ctx.startActivity(intent)
                } catch (e: Exception) {
                    Log.e(
                        "ExitActivity",
                        "Failed to start LauncherActivity, falling back to restart",
                        e
                    )
                    Tools.restart(ctx)
                    return
                }
                System.exit(0)
                return
            }

            val lock = Any()
            Tools.runOnUiThread(Runnable {
                val i = Intent(ctx, ExitActivity::class.java)
                i.putExtra("code", code)
                i.putExtra("isSignal", isSignal)
                i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
                i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                ctx.startActivity(i)
                synchronized(lock) {
                    (lock as Object).notify()
                }
            })
            synchronized(lock) {
                try {
                    (lock as Object).wait()
                } catch (e: InterruptedException) {
                    Log.e("ExitActivity", "Waiting on lock failed: " + e)
                }
            }
            System.exit(0)
        }
    }
}
