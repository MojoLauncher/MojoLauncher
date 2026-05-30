package net.kdt.pojavlaunch

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import net.ashmeet.hyperlauncher.R

class FatalErrorActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val extras = getIntent().getExtras()
        if (extras == null) {
            finish()
            return
        }
        val storageAllow = extras.getBoolean("storageAllow", false)
        val throwable = extras.getSerializable("throwable") as Throwable?
        val stackTrace = if (throwable != null) Tools.printToString(throwable) else "<null>"
        val strSavePath = extras.getString("savePath")
        val errHeader =
            if (storageAllow) "Crash stack trace saved to " + strSavePath + "." else "Storage permission is required to save crash stack trace!"

        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.error_fatal)
            .setMessage(errHeader + "\n\n" + stackTrace)
            .setPositiveButton(
                android.R.string.ok,
                DialogInterface.OnClickListener { p1: DialogInterface?, p2: Int -> finish() })
            .setNegativeButton(
                R.string.global_restart,
                DialogInterface.OnClickListener { p1: DialogInterface?, p2: Int ->
                    startActivity(Intent(this@FatalErrorActivity, LauncherActivity::class.java))
                })
            .setNeutralButton(
                android.R.string.copy,
                DialogInterface.OnClickListener { p1: DialogInterface?, p2: Int ->
                    val mgr = this@FatalErrorActivity.getSystemService(
                        CLIPBOARD_SERVICE
                    ) as ClipboardManager
                    mgr.setPrimaryClip(ClipData.newPlainText("error", stackTrace))
                    finish()
                })
            .setCancelable(false)
            .show()
    }

    companion object {
        fun showError(ctx: Context, savePath: String?, storageAllow: Boolean, th: Throwable?) {
            val fatalErrorIntent = Intent(ctx, FatalErrorActivity::class.java)
            fatalErrorIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
            fatalErrorIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            fatalErrorIntent.putExtra("throwable", th)
            fatalErrorIntent.putExtra("savePath", savePath)
            fatalErrorIntent.putExtra("storageAllow", storageAllow)
            ctx.startActivity(fatalErrorIntent)
        }
    }
}
