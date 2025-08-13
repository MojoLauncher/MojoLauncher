package net.kdt.pojavlaunch.fragments;

import android.content.Context;
import android.view.LayoutInflater;
import android.widget.ExpandableListAdapter;

import androidx.annotation.NonNull;

import com.kdt.mcgui.ProgressLayout;

import git.artdeell.mojo.R;

import net.kdt.pojavlaunch.instances.InstanceInstaller;
import net.kdt.pojavlaunch.instances.InstanceManager;
import net.kdt.pojavlaunch.modloaders.ForgeUtils;
import net.kdt.pojavlaunch.modloaders.ForgeVersionListAdapter;
import net.kdt.pojavlaunch.modloaders.ModloaderListenerProxy;

import java.io.File;
import java.io.IOException;
import java.util.List;

public class ForgeInstallFragment extends ModVersionListFragment<List<String>> {
    public static final String TAG = "ForgeInstallFragment";
    public ForgeInstallFragment() {
        super(TAG);
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
    }

    @Override
    public int getTitleText() {
        return R.string.forge_dl_select_version;
    }

    @Override
    public int getNoDataMsg() {
        return R.string.forge_dl_no_installer;
    }

    @Override
    public List<String> loadVersionList() throws IOException {
        return ForgeUtils.downloadForgeVersions();
    }

    @Override
    public ExpandableListAdapter createAdapter(List<String> versionList, LayoutInflater layoutInflater) {
        return new ForgeVersionListAdapter(versionList, layoutInflater);
    }

    @Override
    public Runnable createDownloadTask(Object selectedVersion, ModloaderListenerProxy listenerProxy) {
        return ()->createInstance((String) selectedVersion, listenerProxy);
    }

    private static void createInstance(String selectedVersion, ModloaderListenerProxy listenerProxy) {
        try {
            ProgressLayout.setProgress(ProgressLayout.INSTALL_MODPACK, 0);
            InstanceInstaller instanceInstaller = ForgeUtils.createInstaller(selectedVersion);
            InstanceManager.createInstance(instance -> {
                instance.name = "Forge";
                instance.icon = "forge";
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
