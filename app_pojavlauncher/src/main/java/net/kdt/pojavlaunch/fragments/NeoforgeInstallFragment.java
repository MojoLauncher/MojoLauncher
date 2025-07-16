package net.kdt.pojavlaunch.fragments;

import android.content.Context;
import android.view.LayoutInflater;
import android.widget.ExpandableListAdapter;

import androidx.annotation.NonNull;

import com.kdt.mcgui.ProgressLayout;

import net.kdt.pojavlaunch.instances.InstanceInstaller;
import net.kdt.pojavlaunch.instances.InstanceManager;
import net.kdt.pojavlaunch.modloaders.ModloaderListenerProxy;
import net.kdt.pojavlaunch.modloaders.NeoforgeUtils;
import net.kdt.pojavlaunch.modloaders.NeoforgeVersionListAdapter;

import java.io.File;
import java.io.IOException;
import java.util.List;

import git.artdeell.mojo.R;

public class NeoforgeInstallFragment extends ModVersionListFragment<List<String>> {
    public static final String TAG = "NeoforgeInstallFragment";
    public NeoforgeInstallFragment() {
        super(TAG);
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
    }

    @Override
    public int getTitleText() {
        return R.string.neoforge_dl_select_version;
    }

    @Override
    public int getNoDataMsg() {
        return R.string.neoforge_dl_no_installer;
    }

    @Override
    public List<String> loadVersionList() throws IOException {
        return NeoforgeUtils.fetchNeoForgeVersions();
    }

    @Override
    public ExpandableListAdapter createAdapter(List<String> versionList, LayoutInflater layoutInflater) {
        return new NeoforgeVersionListAdapter(versionList, layoutInflater);
    }

    @Override
    public Runnable createDownloadTask(Object selectedVersion, ModloaderListenerProxy listenerProxy) {
        return ()->createInstance((String) selectedVersion, listenerProxy);
    }

    private static void createInstance(String selectedVersion, ModloaderListenerProxy listenerProxy) {
        try {
            ProgressLayout.setProgress(ProgressLayout.INSTALL_MODPACK, 0);
            InstanceInstaller instanceInstaller = NeoforgeUtils.createInstaller(selectedVersion);
            InstanceManager.createInstance(instance -> {
                instance.name = "NeoForge";
                instance.icon = "neoforge";
                instance.installer = instanceInstaller;
            }, selectedVersion);
            ProgressLayout.clearProgress(ProgressLayout.INSTALL_MODPACK);
            instanceInstaller.start();
            listenerProxy.onDownloadFinished(null);
        }catch (IOException e) {
            listenerProxy.onDownloadError(e);
        }
    }

    @Override
    public void onDownloadFinished(Context context, File downloadedFile) {

    }
}
