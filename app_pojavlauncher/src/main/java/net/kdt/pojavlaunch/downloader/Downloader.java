package net.kdt.pojavlaunch.downloader;

import android.util.Log;

import com.kdt.mcgui.ProgressLayout;

import net.kdt.pojavlaunch.tasks.SpeedCalculator;
import net.kdt.pojavlaunch.utils.DownloadUtils;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import git.artdeell.mojo.R;

public class Downloader {
    private static final double ONE_MEGABYTE = (1024d * 1024d);
    private static final ThreadLocal<byte[]> sThreadLocalBuffer = new ThreadLocal<>();
    private final String mProgressKey;
    private final AtomicReference<IOException> mThreadException = new AtomicReference<>();
    private final AtomicInteger mDownloadedFileCounter = new AtomicInteger();
    private final AtomicLong mDownloadedSizeCounter = new AtomicLong();
    private final AtomicLong mInternetUsageCounter = new AtomicLong();
    private final AtomicBoolean mUseSizeProgress = new AtomicBoolean(true);
    private final SpeedCalculator mSpeedCalculator = new SpeedCalculator();
    private ExecutorService mDownloadService;
    private ExecutorService mVerifyService;

    public Downloader(String mProgressKey) {
        this.mProgressKey = mProgressKey;
    }

    protected void runDownloads(ArrayList<? extends TaskMetadata> downloads) throws IOException, InterruptedException {
        try {
            insertMetadata(downloads);
        }catch (IOException e) {
            Log.w("Downloader", "Failed to complete the task metadata!", e);
            disableSizeCounter();
        }
        performDownloads(downloads);
    }

    private void performDownloads(ArrayList<? extends TaskMetadata> metadata) throws IOException, InterruptedException {
        mThreadException.set(null);
        mDownloadedFileCounter.set(0);
        mDownloadedSizeCounter.set(0);
        mDownloadService = Executors.newFixedThreadPool(3);
        int verifyThreads = Math.max(2, Runtime.getRuntime().availableProcessors() - 2);
        mVerifyService = Executors.newFixedThreadPool(verifyThreads, r -> {
            Thread thread = new Thread(r);
            thread.setPriority(10);
            thread.setName("verify thread");
            return thread;
        });
        long totalSize = 0;
        int totalCount = metadata.size();
        boolean sizeCounter = mUseSizeProgress.get();
        for(TaskMetadata element : metadata) {
            totalSize += element.size;
            mVerifyService.submit(new CheckFileOnDiskTask(element, this));
        }
        double totalMegabytes = totalSize / ONE_MEGABYTE;
        while(mDownloadedFileCounter.get() < totalCount) {
            IOException exception = mThreadException.get();
            if(exception != null) throw exception;
            if(sizeCounter) reportSizeProgress(totalMegabytes);
            else reportCountProgress(R.string.newerdl_downloading_files_count, totalCount);
            Thread.sleep(33);
        }
        mDownloadService.shutdown();
        mVerifyService.shutdown();
        if(!mDownloadService.awaitTermination(100, TimeUnit.MILLISECONDS) ||
                !mVerifyService.awaitTermination(100, TimeUnit.MILLISECONDS)) {
            throw new RuntimeException("BUG! The file counter is wrong. Maybe. Send this to artDev.");
        }
    }

    private void insertMetadata(ArrayList<? extends TaskMetadata> metadata) throws IOException, InterruptedException {
        mThreadException.set(null);
        mDownloadedFileCounter.set(0);
        ArrayList<TaskMetadata> reducedList = new ArrayList<>();
        for(TaskMetadata element : metadata) {
            if(!CompleteMetadataTask.shouldCompleteMetadata(element)) continue;
            reducedList.add(element);
        }
        if(reducedList.isEmpty()) return;
        try (ExecutorService executorService = Executors.newFixedThreadPool(4)) {
            for(TaskMetadata element : reducedList) executorService.submit(new CompleteMetadataTask(element, this));
            executorService.shutdown();
            while (!executorService.awaitTermination(33, TimeUnit.MILLISECONDS)) {
                IOException exception = mThreadException.get();
                if(exception != null) throw exception;
                reportCountProgress(R.string.newerdl_inserting_metadata_count, reducedList.size());
            }
        }
    }

    private double getSpeed() {
        return mSpeedCalculator.feed(mInternetUsageCounter.get()) / ONE_MEGABYTE;
    }

    private void reportCountProgress(int resource, int total) {
        int downloadedCount = mDownloadedFileCounter.get();
        int progress = (int) ((downloadedCount / (float)total) * 100f);
        ProgressLayout.setProgress(mProgressKey, progress, resource,
                downloadedCount, total, getSpeed()
        );
    }

    private void reportSizeProgress(double totalMegabytes) {
        double downloadedMegabytes = mDownloadedSizeCounter.get() / ONE_MEGABYTE;
        int progress = (int) (downloadedMegabytes / totalMegabytes * 100d);
        ProgressLayout.setProgress(mProgressKey, progress, R.string.newerdl_downloading_files_size,
                downloadedMegabytes, totalMegabytes, getSpeed()
        );
    }

    protected void taskException(IOException e) {
        mThreadException.set(e);
    }

    protected void disableSizeCounter() {
        mUseSizeProgress.lazySet(false);
    }

    protected void submitFileForDownload(TaskMetadata taskMetadata) {
        mDownloadService.submit(new DownloadFileTask(taskMetadata, this));
    }

    protected void submitFileForRecheck(TaskMetadata taskMetadata) {
        mVerifyService.submit(new CheckFileOnDiskTask(taskMetadata, this, true));
    }

    protected void fileComplete() {
        mDownloadedFileCounter.getAndIncrement();
    }

    protected void addSize(long bytes) {
        mDownloadedSizeCounter.getAndAdd(bytes);
    }

    private void copy(InputStream inputStream, OutputStream outputStream, BytesCopiedListener listener) throws IOException {
        byte[] buffer = getBuffer();
        int readLen;
        while((readLen = inputStream.read(buffer)) != -1) {
            outputStream.write(buffer, 0, readLen);
            if(listener != null) listener.onBytesCopied(readLen);
            mInternetUsageCounter.getAndAdd(readLen);
        }
    }

    private static HttpURLConnection openConnection(URL url) throws IOException {
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setReadTimeout(10000);
        connection.setRequestProperty("User-Agent", DownloadUtils.USER_AGENT);
        connection.setDoInput(true);
        connection.setDoOutput(false);
        return connection;
    }

    protected void downloadToStream(HttpURLConnection connection, OutputStream outputStream, BytesCopiedListener listener) throws IOException {
        InputStream inputStream = connection.getInputStream();
        copy(inputStream, outputStream, listener);
    }

    protected String downloadString(URL url) throws IOException {
        HttpURLConnection connection = openConnection(url);
        int length = connection.getContentLength();
        if(length < 0) length = 32;
        try(ByteArrayOutputStream outputStream = new ByteArrayOutputStream(length)) {
            downloadToStream(connection, outputStream, null);
            return new String(outputStream.toByteArray(), StandardCharsets.UTF_8);
        }finally {
            connection.disconnect();
        }
    }

    protected void downloadFile(File file, URL url, BytesCopiedListener listener) throws IOException {
        HttpURLConnection connection = openConnection(url);
        try(FileOutputStream outputStream = new FileOutputStream(file)) {
            downloadToStream(connection, outputStream, listener);
        }finally {
            connection.disconnect();
        }
    }

    protected boolean tryContinueDownload(File file, long wantedLength, URL url, BytesCopiedListener listener) throws IOException {
        HttpURLConnection connection = openConnection(url);
        String range = String.format(Locale.ENGLISH,"bytes %d-%d/%d", file.length(), wantedLength-1, wantedLength);
        connection.setRequestProperty("Content-Range", range);
        try {
            connection.connect();
            int responseCode = connection.getResponseCode();
            if(responseCode != 206) {
                return false;
            }
            try(FileOutputStream outputStream = new FileOutputStream(file, true)) {
                downloadToStream(connection, outputStream, listener);
                return true;
            }
        }finally {
            connection.disconnect();
        }
    }

    protected long getFileContentLength(URL url) throws IOException {
        HttpURLConnection connection = openConnection(url);
        connection.setRequestMethod("HEAD");
        connection.connect();
        int response = connection.getResponseCode();
        if(response >= 400) {
            return -1;
        }else {
            return connection.getContentLength();
        }
    }

    public static byte[] getBuffer() {
        byte[] buffer = sThreadLocalBuffer.get();
        if(buffer == null) {
            buffer = new byte[8192];
            sThreadLocalBuffer.set(buffer);
        }
        return buffer;
    }
}
