package git.artdeell.mojo.modding.modplatforms.api;

import android.content.Context;
import android.util.Log;

import com.kdt.mcgui.ProgressLayout;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import git.artdeell.mojo.MojoApplication;
import git.artdeell.mojo.R;
import git.artdeell.mojo.Tools;
import git.artdeell.mojo.downloader.Downloader;
import git.artdeell.mojo.downloader.TaskMetadata;
import git.artdeell.mojo.instances.Instance;
import git.artdeell.mojo.modding.modplatforms.models.ModDetail;
import git.artdeell.mojo.modding.modplatforms.models.ModDownload;
import git.artdeell.mojo.tasks.mojson.mirrors.DownloadMirror;
import git.artdeell.mojo.utils.FileUtils;

public class ModInstaller {
    public interface Callback {
        void onInstalled(File mod);
    }
    // TODO: Do we even need this?
    static class ModsDownloader extends Downloader {
        ModsDownloader() {
            super(ProgressLayout.MOD_INSTALL);
        }

        void download(ArrayList<TaskMetadata> tasks) throws IOException, InterruptedException {
            runDownloads(tasks);
        }
    }

    public static void install(Context context, Instance to, ModDetail detail, int versionIndex, Callback callback) throws IOException {
        if(detail == null || detail.isModpack) {
            Log.e("ModInstaller", "Cannot install mod: null or modpack");
            return;
        }
        if(detail.downloads == null || detail.downloads.length < 1) {
            throw new IllegalArgumentException("Corrupted mod details");
        }
        if(versionIndex < 0 || versionIndex >=  detail.downloads.length) {
            throw new IllegalArgumentException("Incorrect mod version");
        }

        ModDownload downloader = detail.downloads[versionIndex];
        File modsDir = new File(to.getGameDirectory(), "mods");
        FileUtils.ensureDirectory(modsDir);
        if(downloader.fileName == null) {
            throw new IllegalArgumentException("Installing mods without fileName isn't supported yet");
        }
        String fileName = FileUtils.escapeFileName(downloader.fileName);
        File target = new File(modsDir, fileName);
        ProgressLayout.setProgress(ProgressLayout.MOD_INSTALL, 0, R.string.global_waiting);

        MojoApplication.sExecutorService.execute(() -> {
            try {
                // TODO: check for disabled mods
                ArrayList<TaskMetadata> tasks = new ArrayList<>(1);
                tasks.add(new TaskMetadata(target,
                        new URL(downloader.versionUrl),
                        downloader.fileSize > 0 ? downloader.fileSize : -1,
                        downloader.versionHash,
                        DownloadMirror.DOWNLOAD_CLASS_NONE));
                new ModsDownloader().download(tasks);
                // TODO: check for already installed mods (duplicates)
                Tools.runOnUiThread(() -> callback.onInstalled(target));
            } catch (InterruptedException | IOException e) {
                Tools.showErrorRemote(e); // TODO: proper error message
            } finally {
                ProgressLayout.clearProgress(ProgressLayout.MOD_INSTALL);
            }
        });
    }
}
