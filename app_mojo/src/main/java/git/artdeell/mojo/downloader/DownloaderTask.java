package git.artdeell.mojo.downloader;

import android.util.Log;

import java.io.IOException;
import java.io.InterruptedIOException;

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
        } catch (InterruptedIOException ignored) {
        } catch (IOException e) {
            mDownloader.taskException(e);
        }
    }

    protected abstract void performTask() throws IOException;
}
