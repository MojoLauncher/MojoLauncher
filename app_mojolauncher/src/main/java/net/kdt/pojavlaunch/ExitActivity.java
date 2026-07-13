package net.kdt.pojavlaunch;

import static net.kdt.pojavlaunch.Tools.shareLog;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import androidx.annotation.Keep;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import git.artdeell.mojo.R;

@Keep
public class ExitActivity extends AppCompatActivity {

    @SuppressLint("StringFormatInvalid") //invalid on some translations but valid on most, cant fix that atm
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        int code = -1;
        boolean isSignal = false;
        Bundle extras = getIntent().getExtras();
        if(extras != null) {
            code = extras.getInt("code",-1);
            isSignal = extras.getBoolean("isSignal", false);
        }

        String message = isSignal ? getString(R.string.mcn_abort_title) : getString(R.string.mcn_exit_title, code);

        new AlertDialog.Builder(this)
                .setMessage(message)
                .setPositiveButton(R.string.main_share_logs, (dialog, which) -> shareLog(this))
                .setOnDismissListener(dialog -> ExitActivity.this.finish())
                .show();
    }

    @SuppressWarnings("unused") //used by native jre_launcher_new
    public static void showExitMessage(Context ctx, int code, boolean isSignal) {
        if((!isSignal && code == 0) || ctx == null) {
            System.exit(0);
            return;
        }

        Object lock = new Object();
        Tools.runOnUiThread(()->{
            Intent i = new Intent(ctx,ExitActivity.class);
            i.putExtra("code",code);
            i.putExtra("isSignal", isSignal);
            i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            ctx.startActivity(i);
            synchronized (lock) {
                lock.notify();
            }
        });
        synchronized (lock) {
            try {
                lock.wait();
            } catch (InterruptedException e) {
                Log.e("ExitActivity", "Waiting on lock failed: "+e);
            }
        }
        System.exit(0);
    }

}
