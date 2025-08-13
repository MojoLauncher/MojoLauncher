package net.kdt.pojavlaunch.tasks;

import static net.kdt.pojavlaunch.PojavApplication.sExecutorService;

import androidx.annotation.Nullable;

import net.kdt.pojavlaunch.JMinecraftVersionList;
import net.kdt.pojavlaunch.Tools;
import net.kdt.pojavlaunch.prefs.LauncherPreferences;
import net.kdt.pojavlaunch.utils.DownloadUtils;

import java.io.IOException;

/** Class getting the version list, and that's all really */
public class AsyncVersionList {
    private static final int MAX_RETRIES = 5;

    private static JMinecraftVersionList parseList(String input) throws DownloadUtils.ParseException{
        try {
            return Tools.GLOBAL_GSON.fromJson(input, JMinecraftVersionList.class);
        }catch (Exception e) {
            throw new DownloadUtils.ParseException(e);
        }
    }

    private void getVersionListAsync(VersionDoneListener versionDoneListener, int retries) {
        try {
            JMinecraftVersionList versionList = DownloadUtils.downloadStringCached(
                    LauncherPreferences.PREF_VERSION_REPOS,
                    "version_list",
                    AsyncVersionList::parseList
            );
            if(versionDoneListener != null) versionDoneListener.onVersionDone(versionList);
        }catch (IOException | DownloadUtils.ParseException e) {
            if(retries < MAX_RETRIES) {
                getVersionListAsync(versionDoneListener, retries + 1);
            } else {
                versionDoneListener.onVersionDone(null);
                Tools.showErrorRemote(e);
            }
        }
    }

    public void getVersionList(@Nullable VersionDoneListener listener) {
        sExecutorService.execute(() -> getVersionListAsync(listener, 0));
    }

    /** Basic listener, acting as a callback */
    public interface VersionDoneListener{
        void onVersionDone(JMinecraftVersionList versions);
    }

}
