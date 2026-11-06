package git.artdeell.mojo.tasks;

import static git.artdeell.mojo.MojoApplication.sExecutorService;

import androidx.annotation.Nullable;

import git.artdeell.mojo.JVersionList;
import git.artdeell.mojo.Tools;
import git.artdeell.mojo.prefs.LauncherPreferences;
import git.artdeell.mojo.utils.DownloadUtils;

import java.io.IOException;

/** Class getting the version list, and that's all really */
public class AsyncVersionList {
    private static final int MAX_RETRIES = 5;

    private static JVersionList parseList(String input) throws DownloadUtils.ParseException{
        try {
            return Tools.GLOBAL_GSON.fromJson(input, JVersionList.class);
        }catch (Exception e) {
            throw new DownloadUtils.ParseException(e);
        }
    }

    private void getVersionListAsync(VersionDoneListener versionDoneListener, int retries) {
        try {
            JVersionList versionList = DownloadUtils.downloadStringCached(
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
        void onVersionDone(JVersionList versions);
    }

}
