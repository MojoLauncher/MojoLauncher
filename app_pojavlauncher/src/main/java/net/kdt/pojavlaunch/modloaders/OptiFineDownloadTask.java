package net.kdt.pojavlaunch.modloaders;

import net.kdt.pojavlaunch.JVersionList;
import net.kdt.pojavlaunch.tasks.MoJsonExtras;
import net.kdt.pojavlaunch.tasks.MoJsonDownloader;

import java.io.File;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class OptiFineDownloadTask implements MoJsonExtras.DoneListener {
    private static final Pattern sGameVersionPattern = Pattern.compile("([0-9]+)\\.([0-9]+)\\.?([0-9]+)?");
    private final OptiFineUtils.OptiFineVersion mOptiFineVersion;
    private final Object mDownloadLock = new Object();
    private Throwable mDownloaderThrowable;

    public OptiFineDownloadTask(OptiFineUtils.OptiFineVersion mOptiFineVersion) {
        this.mOptiFineVersion = mOptiFineVersion;
    }

    public void prepareForInstall() throws Exception {
        String gameVersion = determineGameVersion();
        if(gameVersion == null) return;
        if(!downloadGame(gameVersion)) {
            if(mDownloaderThrowable instanceof Exception) {
                throw (Exception) mDownloaderThrowable;
            }else {
                throw new Exception(mDownloaderThrowable);
            }
        }
    }

    public String determineGameVersion() {
        Matcher matcher = sGameVersionPattern.matcher(mOptiFineVersion.gameVersion);
        if(matcher.find()) {
            StringBuilder mcVersionBuilder = new StringBuilder();
            mcVersionBuilder.append(matcher.group(1));
            mcVersionBuilder.append('.');
            mcVersionBuilder.append(matcher.group(2));
            String thirdGroup = matcher.group(3);
            if(thirdGroup != null && !thirdGroup.isEmpty() && !"0".equals(thirdGroup)) {
                mcVersionBuilder.append('.');
                mcVersionBuilder.append(thirdGroup);
            }
            return mcVersionBuilder.toString();
        }else{
            return null;
        }
    }

    public boolean downloadGame(String gameVersion) {
        // the string is always normalized
        JVersionList.Version versionMeta = MoJsonExtras.getListedVersion(gameVersion);
        if(versionMeta == null) return false;
        try {
            synchronized (mDownloadLock) {
                new MoJsonDownloader().start(null, versionMeta, gameVersion, this);
                mDownloadLock.wait();
            }
        }catch (InterruptedException e) {
            e.printStackTrace();
        }
        return mDownloaderThrowable == null;
    }

    @Override
    public void onDownloadDone(File[] classpath) {
        synchronized (mDownloadLock) {
            mDownloaderThrowable = null;
            mDownloadLock.notifyAll();
        }
    }

    @Override
    public void onDownloadFailed(Throwable throwable) {
        synchronized (mDownloadLock) {
            mDownloaderThrowable = throwable;
            mDownloadLock.notifyAll();
        }
    }
}
