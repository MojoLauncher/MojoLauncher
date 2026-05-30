package net.kdt.pojavlaunch.progresskeeper

fun interface TaskCountListener {
    /**
     * @return whether to remove self after this callback.
     */
    fun onUpdateTaskCount(taskCount: Int): Boolean
}
