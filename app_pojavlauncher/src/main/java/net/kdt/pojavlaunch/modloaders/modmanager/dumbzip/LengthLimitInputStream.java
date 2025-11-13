package net.kdt.pojavlaunch.modloaders.modmanager.dumbzip;

import android.util.Log;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;

public class LengthLimitInputStream extends FilterInputStream {
    private long mLengthLimit;

    protected LengthLimitInputStream(InputStream in, long lengthLimit) {
        super(in);
        mLengthLimit = lengthLimit;
    }

    @Override
    public int read() throws IOException {
        if(mLengthLimit <= 0) return -1;
        mLengthLimit--;
        return super.read();
    }

    @Override
    public int read(byte[] b) throws IOException {
        return this.read(b, 0, b.length);
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        if (mLengthLimit <= 0) return -1;
        len = (int) Math.min(len, mLengthLimit);
        int bytesRead = super.read(b, off, len);
        mLengthLimit -= bytesRead;
        return bytesRead;
    }

    @Override
    public int available() throws IOException {
        if(mLengthLimit <= 0) return 0;
        return (int)Math.min(super.available(), mLengthLimit);
    }

    @Override
    public long skip(long n) throws IOException {
        n = Math.min(n, mLengthLimit);
        mLengthLimit -= n;
        return super.skip(n);
    }

    @Override
    public boolean markSupported() {
        return false;
    }

    @Override
    public synchronized void mark(int readlimit) {
        Log.w("LLIS", "Called mark() even though its not supported");
    }
}
