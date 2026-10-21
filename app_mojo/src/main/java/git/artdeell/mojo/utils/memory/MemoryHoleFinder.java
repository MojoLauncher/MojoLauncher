package git.artdeell.mojo.utils.memory;

import git.artdeell.mojo.Architecture;

public class MemoryHoleFinder implements SelfMapsParser.Callback {
    private long mPreviousEnd = 0;
    private long mLargestHole = -1;
    private final long mAddressingLimit = Architecture.getAddressSpaceLimit();
    @Override
    public boolean process(long begin, long end, String wholeLine) {
        if(begin >= mAddressingLimit) begin = mAddressingLimit;
        long holeSize = begin - mPreviousEnd;
        if(mLargestHole < holeSize) mLargestHole = holeSize;
        if(begin == mAddressingLimit) return false;
        mPreviousEnd = end;
        return true;
    }

    public long getLargestHole() {
        return mLargestHole;
    }
}
