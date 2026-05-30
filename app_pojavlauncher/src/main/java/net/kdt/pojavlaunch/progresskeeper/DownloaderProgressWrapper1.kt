package net.kdt.pojavlaunch.progresskeeper

import net.kdt.pojavlaunch.Tools
import net.kdt.pojavlaunch.Tools.DownloaderFeedback
import kotlin.math.max

class DownloaderProgressWrapper
/**
 * A simple wrapper to send the downloader progress to ProgressKeeper
 * @param mProgressString the string that will be used in the progress reporter
 * @param mProgressRecord the record for ProgressKeeper
 */(private val mProgressString: Int, private val mProgressRecord: String?) : DownloaderFeedback {
    var extraString: String? = null

    override fun updateProgress(curr: Int, max: Int) {
        val va: Array<Any?>?
        if (extraString != null) {
            va = arrayOfNulls<Any>(3)
            va[0] = extraString
            va[1] = curr / Tools.BYTE_TO_MB
            va[2] = max / Tools.BYTE_TO_MB
        } else {
            va = arrayOfNulls<Any>(2)
            va[0] = curr / Tools.BYTE_TO_MB
            va[1] = max / Tools.BYTE_TO_MB
        }
        // the allocations are fine because thats how java implements variadic arguments in bytecode: an array of whatever
        ProgressKeeper.submitProgress(
            mProgressRecord,
            max(curr.toFloat() / max * 100, 0f).toInt(),
            mProgressString,
            *va
        )
    }
}
