package net.kdt.pojavlaunch;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;
import androidx.lifecycle.DefaultLifecycleObserver;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.ProcessLifecycleOwner;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import net.ashmeet.hyperlauncher.BuildConfig;

/**
 * Lightweight Firebase analytics for tracking active players.
 * This implementation is Play Store safe:
 * - No PII collected (IMEI, Android ID, Advertising ID, etc. are avoided).
 * - Uses a randomly generated UUID stored in SharedPreferences.
 * - Respects user privacy by only sending minimal active-state heartbeats.
 * - Automatically stops heartbeats when the app is in the background to save battery and data.
 * - No background services or intrusive permissions required.
 */
public class PlayerAnalytics implements DefaultLifecycleObserver {
    private static final String PREF_NAME = "hollow_launcher_analytics";
    private static final String KEY_USER_ID = "user_id";
    private static final long HEARTBEAT_INTERVAL_MS = 60000; // 10 seconds

    private static PlayerAnalytics sInstance;

    private final String mUserId;
    private final DatabaseReference mPlayerRef;
    private final Handler mHandler = new Handler(Looper.getMainLooper());
    private final Runnable mHeartbeatRunnable = this::sendHeartbeat;
    private boolean mIsActive = false;

    /**
     * Initializes and starts the analytics tracking.
     * Should be called from your Application class or the main entry point.
     * @param context Application context.
     */
    public static void init(Context context) {
        if (sInstance == null) {
            sInstance = new PlayerAnalytics(context.getApplicationContext());
            ProcessLifecycleOwner.get().getLifecycle().addObserver(sInstance);
        }
    }

    private PlayerAnalytics(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        String userId = prefs.getString(KEY_USER_ID, null);
        if (userId == null) {
            userId = UUID.randomUUID().toString();
            prefs.edit().putString(KEY_USER_ID, userId).apply();
        }
        mUserId = userId;

        // Reference: players/randomUserId
        mPlayerRef = FirebaseDatabase.getInstance().getReference("players").child(mUserId);
    }

    private void sendHeartbeat() {
        if (!mIsActive) return;

        Map<String, Object> updates = new HashMap<>();
        updates.put("lastSeen", System.currentTimeMillis());
        updates.put("version", BuildConfig.VERSION_NAME);
        updates.put("platform", "android");

        // setValue() overwrites. To keep historical data like session start, use updateChildren().
        // For a simple "active now" dashboard, setValue() is efficient.
        mPlayerRef.setValue(updates).addOnFailureListener(e -> {
            // Silently fail to avoid affecting user experience on network issues
        });

        // Schedule the next heartbeat in 60 seconds
        mHandler.postDelayed(mHeartbeatRunnable, HEARTBEAT_INTERVAL_MS);
    }

    @Override
    public void onStart(@NonNull LifecycleOwner owner) {
        // App has come to the foreground
        mIsActive = true;
        sendHeartbeat();
    }

    @Override
    public void onStop(@NonNull LifecycleOwner owner) {
        // App has gone to the background
        mIsActive = false;
        mHandler.removeCallbacks(mHeartbeatRunnable);
    }
}
