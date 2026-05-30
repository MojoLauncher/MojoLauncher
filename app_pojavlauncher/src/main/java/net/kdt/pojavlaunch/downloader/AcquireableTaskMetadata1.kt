package net.kdt.pojavlaunch.downloader

import java.io.IOException

abstract class AcquireableTaskMetadata(mirrorType: Int) : TaskMetadata(null, null) {
    /**
     * Fill the missing fields of this AcquireableTaskMetadata (by, for example, performing an API request)
     * @throws IOException if metadata acquisition failed
     */
    @Throws(IOException::class)
    abstract fun acquireMetadata()
}
