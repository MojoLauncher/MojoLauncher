package net.kdt.pojavlaunch.modloaders.modmanager.dumbzip;

import org.jetbrains.annotations.NotNull;

import me.andreasmelone.abstractzip.IZipEntry;

public class DumbZipEntry implements IZipEntry {
    private final String mName;
    private final long mTime;
    private final long mSize;
    private final long mCompressedSize;
    private final long mCrc;
    private final String mComment;
    protected final long mDataOffset;
    protected final int mCompressionMethod;

    public DumbZipEntry(String mName, long mTime, long mSize, long mCompressedSize, long mCrc, String mComment, long mFileOffset, int mCompressionMethod) {
        this.mName = mName;
        this.mTime = mTime;
        this.mSize = mSize;
        this.mCompressedSize = mCompressedSize;
        this.mCrc = mCrc;
        this.mComment = mComment;
        this.mDataOffset = mFileOffset;
        this.mCompressionMethod = mCompressionMethod;
    }

    @NotNull
    @Override
    public String getName() {
        return mName;
    }

    @Override
    public long getTime() {
        return mTime;
    }

    @Override
    public long getSize() {
        return mSize;
    }

    @Override
    public long getCompressedSize() {
        return mCompressedSize;
    }

    @Override
    public long getCrc() {
        return mCrc;
    }

    @Override
    public String getComment() {
        return mComment;
    }
}
