package net.kdt.pojavlaunch.utils.memory

import net.kdt.pojavlaunch.Architecture

class MemoryHoleFinder : SelfMapsParser.Callback {
    private var mPreviousEnd: Long = 0
    var largestHole: Long = -1
        private set
    private val mAddressingLimit = Architecture.addressSpaceLimit
    override fun process(begin: Long, end: Long, wholeLine: String?): Boolean {
        var currentBegin = begin
        if (currentBegin >= mAddressingLimit) currentBegin = mAddressingLimit
        val holeSize = currentBegin - mPreviousEnd
        if (this.largestHole < holeSize) this.largestHole = holeSize
        if (currentBegin == mAddressingLimit) return false
        mPreviousEnd = end
        return true
    }
}
