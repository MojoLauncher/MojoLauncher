package net.kdt.pojavlaunch.progresskeeper

import net.kdt.pojavlaunch.downloader.Downloader

object ProgressKeeper {
    private val sProgressListeners = HashMap<String?, MutableList<ProgressListener>>()
    private val sProgressStates = HashMap<String?, ProgressState>()
    private val sTaskCountListeners: MutableList<TaskCountListener> =
        ArrayList()

    @JvmStatic
    @Synchronized
    fun submitProgress(progressRecord: String?, progress: Int, resid: Int, vararg va: Any?) {
        var progressState = sProgressStates[progressRecord]
        var shouldCallStarted = progressState == null
        val shouldCallEnded = resid == -1 && progress == -1
        if (shouldCallEnded) {
            shouldCallStarted = false
            sProgressStates.remove(progressRecord)
        } else if (shouldCallStarted) {
            sProgressStates[progressRecord] = ProgressState().also { progressState = it }
        }
        if (shouldCallEnded || shouldCallStarted) updateTaskCount(sProgressStates.size)
        if (progressState != null) {
            progressState.progress = progress
            progressState.resid = resid
            progressState.varArg = va
        }

        val progressListeners = sProgressListeners[progressRecord]
        if (progressListeners != null) for (listener in progressListeners) {
            if (shouldCallStarted) listener.onProgressStarted()
            else if (shouldCallEnded) listener.onProgressEnded()
            else listener.onProgressUpdated(progress, resid, *va)
        }
    }

    private fun updateTaskCount(count: Int) {
        synchronized(sTaskCountListeners) {
            val iterator = sTaskCountListeners.iterator()
            while (iterator.hasNext()) {
                if (iterator.next().onUpdateTaskCount(count)) iterator.remove()
            }
        }
    }

    @JvmStatic
    @Synchronized
    fun hasProgressKey(key: String?): Boolean {
        return sProgressStates[key] != null
    }

    @JvmStatic
    @Synchronized
    fun addListener(progressRecord: String?, listener: ProgressListener) {
        val state = sProgressStates[progressRecord]
        if (state != null && (state.resid != -1 || state.progress != -1)) {
            listener.onProgressStarted()
            listener.onProgressUpdated(state.progress, state.resid, *(state.varArg ?: emptyArray()))
        } else {
            listener.onProgressEnded()
        }
        var listenerWeakReferenceList = sProgressListeners[progressRecord]
        if (listenerWeakReferenceList == null) {
            listenerWeakReferenceList = ArrayList()
            sProgressListeners[progressRecord] = listenerWeakReferenceList
        }
        listenerWeakReferenceList.add(listener)
    }

    @JvmStatic
    @Synchronized
    fun removeListener(progressRecord: String?, listener: ProgressListener) {
        sProgressListeners[progressRecord]?.remove(listener)
    }

    @JvmStatic
    @JvmOverloads
    fun addTaskCountListener(listener: TaskCountListener, runUpdate: Boolean = true) {
        if (runUpdate) synchronized(ProgressKeeper::class.java) {
            listener.onUpdateTaskCount(
                sProgressStates.size
            )
        }
        synchronized(sTaskCountListeners) {
            if (!sTaskCountListeners.contains(listener)) sTaskCountListeners.add(listener)
        }
    }

    @JvmStatic
    fun removeTaskCountListener(listener: TaskCountListener?) {
        synchronized(sTaskCountListeners) {
            sTaskCountListeners.remove(listener)
        }
    }

    /**
     * Waits until all tasks are done and runs the runnable, or if there were no pending process remaining
     * The runnable runs from the thread that updated the task count last, and it might be the UI thread,
     * so don't put long running processes in it
     * @param runnable the runnable to run when no tasks are remaining
     */
    @JvmStatic
    fun waitUntilDone(runnable: Runnable) {
        // If we do it the other way the listener would be removed before it was added, which will cause a listener object leak
        if (taskCount == 0) {
            runnable.run()
            return
        }
        val listener = TaskCountListener { taskCount: Int ->
            if (taskCount == 0) {
                runnable.run()
                return@TaskCountListener true
            }
            false
        }
        addTaskCountListener(listener)
    }

    @JvmStatic
    @get:Synchronized
    val taskCount: Int
        get() = sProgressStates.size

    @JvmStatic
    fun hasOngoingTasks(): Boolean {
        return taskCount > 0
    }

    @JvmStatic
    @Synchronized
    fun clearAll() {

        val keys = ArrayList(sProgressStates.keys)
        for (key in keys) {
            submitProgress(key, -1, -1)
        }
    }
}
