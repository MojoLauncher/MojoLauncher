package net.kdt.pojavlaunch.services

import android.content.Context
import net.kdt.pojavlaunch.progresskeeper.TaskCountListener

class ProgressServiceKeeper(private val context: Context?) : TaskCountListener {
    override fun onUpdateTaskCount(taskCount: Int): Boolean {
        if (taskCount > 0 && context != null) ProgressService.startService(context)
        return false
    }
}
