package net.kdt.pojavlaunch;

import static net.kdt.pojavlaunch.Tools.shareLog;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;
import android.widget.ScrollView;

import androidx.annotation.Keep;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import net.ashmeet.hyperlauncher.R;
import java.io.File;
import java.io.IOException;

@Keep
public class ExitActivity extends AppCompatActivity {

    @SuppressLint("StringFormatInvalid")
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Tools.setInsetsMode(this, true, true);
        setContentView(R.layout.activity_exit);

        int code = -1;
        boolean isSignal = false;
        Bundle extras = getIntent().getExtras();
        if(extras != null) {
            code = extras.getInt("code",-1);
            isSignal = extras.getBoolean("isSignal", false);
        }

        String message = isSignal ? getString(R.string.mcn_abort_title) : getString(R.string.mcn_exit_title, code);

        TextView titleView = findViewById(R.id.exit_title);
        titleView.setText(message);

        TextView logTextView = findViewById(R.id.exit_log_text);
        ScrollView scrollView = findViewById(R.id.exit_scroll_view);

        try {
            File logFile = new File(Tools.DIR_GAME_HOME, "latestlog.txt");
            if (logFile.exists()) {
                String logs = Tools.read(logFile);
                logTextView.setText(logs);
                // Scroll to bottom
                scrollView.post(() -> scrollView.fullScroll(ScrollView.FOCUS_DOWN));
            } else {
                logTextView.setText("No log file found at " + logFile.getAbsolutePath());
            }
        } catch (IOException e) {
            logTextView.setText("Failed to read logs: " + e.getMessage());
        }

        Button shareButton = findViewById(R.id.exit_share_button);
        if (shareButton != null) {
            shareButton.setOnClickListener(v -> shareLog(this));
        }

        Button restartButton = findViewById(R.id.exit_restart_button);
        if (restartButton != null) {
            restartButton.setOnClickListener(v -> {
                Intent intent = new Intent(this, LauncherActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
                finish();
            });
        }
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
