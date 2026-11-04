package git.artdeell.mojo.downloader;

import android.util.Log;

import java.io.File;
import java.io.IOException;
import java.io.InterruptedIOException;

import git.artdeell.mojo.MojoApplication;
import git.artdeell.mojo.utils.HashUtils;

public class DownloadFileTask extends DownloaderTask implements BytesCopiedListener {
    private long mBytesDownloaded = 0;
    private final File mPartFile;
    DownloadFileTask(TaskMetadata mMetadata, Downloader mHostDownloader) {
        super(mMetadata, mHostDownloader);
        String name = partFileName();
        mPartFile = new File(Downloader.sPartFileDir, name);
    }

    @Override
    protected void performTask() throws IOException {
        tryDownload(0, true);
        movePartFile();
        mDownloader.submitFileForRecheck(mMetadata);
    }

    private void performRetry(int attempt, boolean rangeAllowed) throws IOException{
        mDownloader.addSize(-mBytesDownloaded); // It will get readded again on next tryDownload() if range is allowed
        tryDownload(attempt + 1, rangeAllowed);
    }

    private String partFileName() {
        String path = mMetadata.path.getPath();
        String longHashBase = new StringBuilder(55 + path.length())
                .append(mMetadata.sha1Hash)
                .append(Long.toHexString(mMetadata.size))
                .append(path)
                .toString();
        String longHash = HashUtils.longHash(longHashBase);
        return new StringBuilder(33)
                .append(longHash)
                .append(".part")
                .toString();
    }

    /**
     * When you're hammering the FS with many renames, not all of them seem to come through the ARC
     * filesystem emulation layer. So, poll until they do, and only proceed then
     */
    private void arcFsMove(File source, File target) throws IOException {
        try {
            int pollTries = 0;
            long wantedSize = source.length();

            if(!source.renameTo(target))
                throw new IOException("Failed to move downloaded partfile");

            while (target.length() != wantedSize) {
                if(pollTries++ > 5) throw new IOException("ARC metadata check failed");
                Log.i("ArcFSWait", "Forced wait stall due to wrong metadata");
                Thread.sleep(33);
            }
        }catch (InterruptedException e)  {
            throw new InterruptedIOException();
        }
    }

    private void movePartFile() throws IOException {
        File original = mMetadata.path;
        if(original.exists() && !original.delete())
            throw new IOException("Failed to delete original file");
        if(MojoApplication.CHROMEBOOK) {
            arcFsMove(mPartFile, mMetadata.path);
            return;
        }
        if(!mPartFile.renameTo(mMetadata.path))
            throw new IOException("Failed to move downloaded partfile");
    }

    private void tryDownload(int attempt, boolean rangeAllowed) throws IOException {
        try {
            if(!mPartFile.exists() || !rangeAllowed) {
                mBytesDownloaded = 0;
                mDownloader.downloadFile(mPartFile, mMetadata.url, this);
            } else {
                long alreadyDownloaded = mPartFile.length();
                mBytesDownloaded = alreadyDownloaded;
                mDownloader.addSize(alreadyDownloaded);
                if(mMetadata.size != -1 && alreadyDownloaded == mMetadata.size) return;
                rangeAllowed = mDownloader.tryContinueDownload(mPartFile, mMetadata.size, mMetadata.url, this);
                if(!rangeAllowed) performRetry(attempt, false);
            }
        }catch (InterruptedIOException e) {
            throw e;
        } catch (IOException e) {
            if(attempt == 5) throw e;
            performRetry(attempt, rangeAllowed);
        }
    }

    @Override
    public void onBytesCopied(int nbytes) {
        mBytesDownloaded += nbytes;
        mDownloader.addSize(nbytes);
    }
}
