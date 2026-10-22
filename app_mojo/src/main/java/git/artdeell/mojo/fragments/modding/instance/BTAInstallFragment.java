package git.artdeell.mojo.fragments.modding.instance;

import android.content.Context;
import android.view.LayoutInflater;
import android.widget.ExpandableListAdapter;

import com.kdt.mcgui.ProgressLayout;

import git.artdeell.mojo.R;
import git.artdeell.mojo.fragments.modding.ModVersionListFragment;
import git.artdeell.mojo.instances.InstanceInstaller;
import git.artdeell.mojo.instances.Instances;
import git.artdeell.mojo.modding.bta.BTADownloadTask;
import git.artdeell.mojo.modding.bta.BTAUtils;
import git.artdeell.mojo.modding.bta.BTAVersionListAdapter;
import git.artdeell.mojo.modding.ModloaderListenerProxy;
import git.artdeell.mojo.modding.optifine.OptiFineDownloadTask;
import git.artdeell.mojo.modding.optifine.OptiFineUtils;
import git.artdeell.mojo.modding.optifine.OptiFineVersionListAdapter;

import java.io.File;
import java.io.IOException;

public class BTAInstallFragment extends ModVersionListFragment<BTAUtils.BTAVersionList> {
    public static final String TAG = "BTAInstallFragment";

    public BTAInstallFragment() {
        super(TAG);
    }

    @Override
    public int getTitleText() {
        return R.string.select_bta_version;
    }

    @Override
    public int getNoDataMsg() {
        return R.string.modloader_dl_failed_to_load_list;
    }

    @Override
    public BTAUtils.BTAVersionList loadVersionList() throws IOException {
        return BTAUtils.downloadVersionList();
    }

    @Override
    public ExpandableListAdapter createAdapter(BTAUtils.BTAVersionList versionList, LayoutInflater layoutInflater) {
        return new BTAVersionListAdapter(versionList, layoutInflater);
    }

    @Override
    public Runnable createDownloadTask(Object selectedVersion, ModloaderListenerProxy listenerProxy) {
        return new BTADownloadTask(listenerProxy, (BTAUtils.BTAVersion) selectedVersion);
    }

    @Override
    public void onDownloadFinished(Context context, File downloadedFile) {
        // We don't have to do anything after the BTADownloadTask ends, so this is a stub
    }

    public static class OptiFineInstallFragment extends ModVersionListFragment<OptiFineUtils.OptiFineVersions> {
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
                Instances.createInstance(instance -> {
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
}
