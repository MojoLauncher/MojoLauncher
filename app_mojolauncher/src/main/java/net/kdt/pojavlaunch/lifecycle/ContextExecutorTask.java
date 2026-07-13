package net.kdt.pojavlaunch.lifecycle;

import android.content.Context;

/**
 * A ContextExecutorTask is a task that can dynamically change its behaviour, based on the context
 * used for its execution. This can be used to implement for ex. error/finish notifications from
 * background threads that may live with the Service after the activity that started them died.
 */
public interface ContextExecutorTask extends ActivityRunnable {
    /**
     * ContextExecutor will execute this function if a foreground Activity is not available, but the app
     * is still running.
     * @param context the application context
     */
    void executeWithApplication(Context context);
}
