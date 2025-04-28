package net.kdt.pojavlaunch.fragments;

import android.content.Context;
import android.view.LayoutInflater;
import android.widget.ExpandableListAdapter;

import com.kdt.mcgui.ProgressLayout;

import git.artdeell.mojo.R;

import net.kdt.pojavlaunch.instances.InstanceInstaller;
import net.kdt.pojavlaunch.instances.InstanceManager;
import net.kdt.pojavlaunch.modloaders.ModloaderListenerProxy;
import net.kdt.pojavlaunch.modloaders.OptiFineDownloadTask;
import net.kdt.pojavlaunch.modloaders.OptiFineUtils;
import net.kdt.pojavlaunch.modloaders.OptiFineVersionListAdapter;

import java.io.File;
import java.io.IOException;

public class OptiFineInstallFragment extends ModVersionListFragment<OptiFineUtils.OptiFineVersions> {
    public static final String TAG = "OptiFineInstallFragment";
    public OptiFineInstallFragment() {
        super(TAG);
    }
    @Override
    public int getTitleText() {
        return R.string.of_dl_select_version;
    }

    @Override
    public int getNoDataMsg() {
        return R.string.of_dl_failed_to_scrape;
    }
    @Override
    public OptiFineUtils.OptiFineVersions loadVersionList() throws IOException {
        return OptiFineUtils.downloadOptiFineVersions();
    }

    @Override
    public ExpandableListAdapter createAdapter(OptiFineUtils.OptiFineVersions versionList, LayoutInflater layoutInflater) {
        return new OptiFineVersionListAdapter(versionList, layoutInflater);
    }

    private void createInstance(OptiFineUtils.OptiFineVersion version, ModloaderListenerProxy listenerProxy) {
        try {
            ProgressLayout.setProgress(ProgressLayout.INSTALL_MODPACK, 0);
            new OptiFineDownloadTask(version).prepareForInstall();
            InstanceInstaller instanceInstaller = OptiFineUtils.createInstaller(version);
            InstanceManager.createInstance(instance -> {
                instance.name = "OptiFine";
                instance.installer = instanceInstaller;
                instance.sharedData = true;
            }, "OptiFine");
            ProgressLayout.clearProgress(ProgressLayout.INSTALL_MODPACK);
            instanceInstaller.start();
            listenerProxy.onDownloadFinished(null);
        }catch (Exception e) {
            listenerProxy.onDownloadError(e);
        }
    }

    @Override
    public Runnable createDownloadTask(Object selectedVersion, ModloaderListenerProxy listenerProxy) {
        return ()->createInstance((OptiFineUtils.OptiFineVersion) selectedVersion, listenerProxy);
    }

    @Override
    public void onDownloadFinished(Context context, File downloadedFile) {

    }
}
