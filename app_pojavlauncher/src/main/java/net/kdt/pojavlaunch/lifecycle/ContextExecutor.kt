package net.kdt.pojavlaunch.lifecycle

import android.app.Activity
import android.app.Application
import net.kdt.pojavlaunch.Tools
import java.lang.ref.WeakReference

object ContextExecutor {
    private var sApplication: WeakReference<Application?>? = null
    private var sActivity: WeakReference<Activity?>? = null


    /**
     * Schedules a ContextExecutorTask to be executed. For more info on tasks, please read
     * ContextExecutorTask.java
     * @param contextExecutorTask the task to be executed
     */
    @JvmStatic
    fun execute(contextExecutorTask: ContextExecutorTask) {
        Tools.runOnUiThread(Runnable { executeOnUiThread(contextExecutorTask) })
    }

    private fun executeOnUiThread(contextExecutorTask: ContextExecutorTask) {
        val activity = Tools.getWeakReference<Activity?>(sActivity)
        if (activity != null) {
            contextExecutorTask.executeWithActivity(activity)
            return
        }
        val application = Tools.getWeakReference<Application?>(sApplication)
        if (application != null) {
            contextExecutorTask.executeWithApplication(application)
        } else {
            throw RuntimeException("ContextExecutor.execute() called before Application.onCreate!")
        }
    }

    /**
     * Set the Activity that this ContextExecutor will use for executing tasks
     * @param activity the activity to be used
     */
    @JvmStatic
    fun setActivity(activity: Activity?) {
        sActivity = WeakReference<Activity?>(activity)
    }

    /**
     * Clear the Activity previously set, so thet ContextExecutor won't use it to execute tasks.
     */
    @JvmStatic
    fun clearActivity() {
        if (sActivity != null) sActivity!!.clear()
    }

    /**
     * Set the Application that will be used to execute tasks if the Activity won't be available.
     * @param application the application to use as the fallback
     */
    @JvmStatic
    fun setApplication(application: Application?) {
        sApplication = WeakReference<Application?>(application)
    }

    /**
     * Clear the Application previously set, so that ContextExecutor will notify the user of a critical error
     * that is executing code after the application is ended by the system.
     */
    @JvmStatic
    fun clearApplication() {
        if (sApplication != null) sApplication!!.clear()
    }
}
