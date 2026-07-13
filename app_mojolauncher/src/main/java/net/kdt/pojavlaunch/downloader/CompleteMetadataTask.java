package net.kdt.pojavlaunch.downloader;

import android.util.Log;

import net.kdt.pojavlaunch.mirrors.DownloadMirror;
import net.kdt.pojavlaunch.prefs.LauncherPreferences;

import java.io.IOException;
import java.net.URL;

public class CompleteMetadataTask extends DownloaderTask {
    CompleteMetadataTask(TaskMetadata mMetadata, Downloader mHostDownloader) {
        super(mMetadata, mHostDownloader);
    }

    @Override
    protected void performTask() throws IOException {
        if(mMetadata instanceof AcquireableTaskMetadata) {
            ((AcquireableTaskMetadata)mMetadata).acquireMetadata();
            if(mMetadata.url == null) throw new IOException("Metadata acquisition did not supply the URL!");
        }
        if(mMetadata.url != null) {
            getFileSize();
            getLibrarySha1Hash();
        }
        if(mMetadata.size == -1) {
            mDownloader.disableSizeCounter();
        }
        mDownloader.fileComplete();
    }

    private void getLibrarySha1Hash() {
        if(mMetadata.sha1Hash != null) return;
        if(mMetadata.mirrorType != DownloadMirror.DOWNLOAD_CLASS_LIBRARIES) return;

        if(!LauncherPreferences.PREF_VERIFY_FILES) return;

        // No need to try and obtain the hash if the file is qualified for rapid start check skip
        if(LauncherPreferences.PREF_RAPID_START && mMetadata.size != -1 && mMetadata.path.length() == mMetadata.size) return;

        try {
            mMetadata.sha1Hash = mDownloader.downloadString(new URL(mMetadata.url + ".sha1"));
        }catch (IOException e) {
            Log.i("CompleteMetadataTask", "Failed to get server hash for "+mMetadata.path.getName(), e);
        }
    }

    private void getFileSize() {
        if(mMetadata.size != -1) return;
        try {
            mMetadata.size = mDownloader.getFileContentLength(mMetadata.url);
            Log.i("CompleteMetadataTask", "Got size: " + mMetadata.size +" for " + mMetadata.path.getName());
        }catch (IOException e) {
            Log.i("CompleteMetadataTask", "Failed to get size for " + mMetadata.path.getName(), e);
        }
    }

    protected static boolean shouldCompleteMetadata(TaskMetadata metadata) {
        return metadata instanceof AcquireableTaskMetadata || (metadata.sha1Hash == null && metadata.mirrorType == DownloadMirror.DOWNLOAD_CLASS_LIBRARIES) || metadata.size == -1;
    }
}
