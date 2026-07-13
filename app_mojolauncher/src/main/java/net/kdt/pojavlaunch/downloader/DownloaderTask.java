package net.kdt.pojavlaunch.downloader;

import java.io.IOException;

public abstract class DownloaderTask implements Runnable {
    protected final TaskMetadata mMetadata;
    protected final Downloader mDownloader;

    protected DownloaderTask(TaskMetadata mMetadata, Downloader mHostDownloader) {
        this.mMetadata = mMetadata;
        this.mDownloader = mHostDownloader;
    }

    @Override
    public final void run() {
        try {
            performTask();
        }catch (IOException e) {
            mDownloader.taskException(e);
        }
    }

    protected abstract void performTask() throws IOException;
}
