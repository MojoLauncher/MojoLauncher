package net.kdt.pojavlaunch

import android.content.Context
import android.content.SharedPreferences
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import net.ashmeet.hyperlauncher.BuildConfig
import java.util.UUID

/**
 * Lightweight Firebase analytics for tracking active players.
 */
class PlayerAnalytics private constructor(context: Context) : DefaultLifecycleObserver {
    private val mUserId: String
    private val mPlayerRef: DatabaseReference
    private val mHandler: Handler = Handler(Looper.getMainLooper())
    private val mHeartbeatRunnable = Runnable { sendHeartbeat() }
    private var mIsActive = false

    init {
        val prefs: SharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        var userId: String? = prefs.getString(KEY_USER_ID, null)
        if (userId == null) {
            userId = UUID.randomUUID().toString()
            prefs.edit().putString(KEY_USER_ID, userId).apply()
        }
        mUserId = userId

        // Reference: players/randomUserId
        mPlayerRef = FirebaseDatabase.getInstance().getReference("players").child(mUserId)
    }

    private fun sendHeartbeat() {
        if (!mIsActive) return

        val updates = HashMap<String, Any?>()
        updates["lastSeen"] = System.currentTimeMillis()
        updates["version"] = BuildConfig.VERSION_NAME
        updates["platform"] = "android"

        mPlayerRef.setValue(updates).addOnFailureListener { e ->
            Log.w("PlayerAnalytics", "Failed to send heartbeat", e)
        }

        // Schedule the next heartbeat in 60 seconds
        mHandler.postDelayed(mHeartbeatRunnable, HEARTBEAT_INTERVAL_MS)
    }

    override fun onStart(owner: LifecycleOwner) {
        // App has come to the foreground
        mIsActive = true
        sendHeartbeat()
    }

    override fun onStop(owner: LifecycleOwner) {
        // App has gone to the background
        mIsActive = false
        mHandler.removeCallbacks(mHeartbeatRunnable)
    }

    companion object {
        private const val PREF_NAME = "hollow_launcher_analytics"
        private const val KEY_USER_ID = "user_id"
        private const val HEARTBEAT_INTERVAL_MS: Long = 60000

        private var sInstance: PlayerAnalytics? = null

        /**
         * Initializes and starts the analytics tracking.
         */
        @JvmStatic
        fun init(context: Context) {
            if (sInstance == null) {
                val analytics = PlayerAnalytics(context.applicationContext)
                sInstance = analytics
                ProcessLifecycleOwner.get().lifecycle.addObserver(analytics)
            }
        }
    }
}
