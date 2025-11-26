package net.kdt.pojavlaunch.downloader;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicLong;

public class DownloadFileTask extends DownloaderTask implements BytesCopiedListener {
    private final AtomicLong mBytesDownloaded = new AtomicLong();
    DownloadFileTask(TaskMetadata mMetadata, Downloader mHostDownloader) {
        super(mMetadata, mHostDownloader);
    }

    @Override
    protected void performTask() throws IOException {
        tryDownload(0, true);
        mDownloader.submitFileForRecheck(mMetadata);
    }

    private void performRetry(int attempt, boolean rangeAllowed) throws IOException{
        if(!rangeAllowed) mDownloader.addSize(-mBytesDownloaded.get());
        tryDownload(attempt + 1, rangeAllowed);
    }

    private void tryDownload(int attempt, boolean rangeAllowed) throws IOException {
        try {
            if(!mMetadata.path.exists() || !rangeAllowed) {
                mBytesDownloaded.set(0);
                mDownloader.downloadFile(mMetadata.path, mMetadata.url, this);
            } else {
                long alreadyDownloaded = mMetadata.path.length();
                mBytesDownloaded.set(alreadyDownloaded);
                mDownloader.addSize(alreadyDownloaded);
                rangeAllowed = mDownloader.tryContinueDownload(mMetadata.path, mMetadata.size, mMetadata.url, this);
                if(!rangeAllowed) performRetry(attempt, rangeAllowed);
            }
        }catch (IOException e) {
            if(attempt == 5) throw e;
            performRetry(attempt, rangeAllowed);
        }
    }

    @Override
    public void onBytesCopied(int nbytes) {
        mBytesDownloaded.getAndAdd(nbytes);
        mDownloader.addSize(nbytes);
    }
}
