package net.kdt.pojavlaunch.fragments;

import android.content.Context;

import com.kdt.mcgui.ProgressLayout;

import net.kdt.pojavlaunch.instances.InstanceInstaller;
import net.kdt.pojavlaunch.instances.InstanceManager;
import net.kdt.pojavlaunch.modloaders.ForgelikeUtils;
import net.kdt.pojavlaunch.modloaders.ModloaderListenerProxy;

import java.io.File;
import java.io.IOException;
import java.util.List;

public abstract class ForgelikeInstallFragment extends ModVersionListFragment<List<String>> {
    private final ForgelikeUtils mUtils;
    public ForgelikeInstallFragment(ForgelikeUtils utils, String mFragmentTag) {
        super(mFragmentTag);
        this.mUtils = utils;
    }

    @Override
    public List<String> loadVersionList() throws IOException {
        return mUtils.downloadVersions();
    }

    @Override
    public Runnable createDownloadTask(Object selectedVersion, ModloaderListenerProxy listenerProxy) {
        return ()->createInstance((String) selectedVersion, listenerProxy);
    }

    @Override
    public void onDownloadFinished(Context context, File downloadedFile) {
    }

    private void createInstance(String selectedVersion, ModloaderListenerProxy listenerProxy) {
        try {
            ProgressLayout.setProgress(ProgressLayout.INSTALL_MODPACK, 0);
            InstanceInstaller instanceInstaller = mUtils.createInstaller(selectedVersion);
            InstanceManager.createInstance(instance -> {
                instance.name = mUtils.getName();
                instance.icon = mUtils.getIconName();
                instance.installer = instanceInstaller;
            }, selectedVersion);
            ProgressLayout.clearProgress(ProgressLayout.INSTALL_MODPACK);
            instanceInstaller.start();
            listenerProxy.onDownloadFinished(null);
        }catch (IOException e) {
            listenerProxy.onDownloadError(e);
        }
    }
}
