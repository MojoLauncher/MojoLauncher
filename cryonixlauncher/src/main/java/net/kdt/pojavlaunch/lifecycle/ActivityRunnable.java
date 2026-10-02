package net.kdt.pojavlaunch.lifecycle;

import android.app.Activity;

public interface ActivityRunnable {
    /**
     * ContextExecutor will execute this function first if a foreground Activity that was attached to the
     * ContextExecutor is available.
     * @param activity the activity
     */
    void executeWithActivity(Activity activity);
}
