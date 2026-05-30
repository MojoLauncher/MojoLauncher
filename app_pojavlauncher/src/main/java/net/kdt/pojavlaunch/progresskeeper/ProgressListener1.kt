package net.kdt.pojavlaunch.progresskeeper

interface ProgressListener {
    fun onProgressStarted()
    fun onProgressUpdated(progress: Int, resid: Int, vararg va: Any?)
    fun onProgressEnded()
}
