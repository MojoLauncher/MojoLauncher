package net.kdt.pojavlaunch.fragments;

import net.kdt.pojavlaunch.modloaders.FabriclikeUtils;
import net.kdt.pojavlaunch.modloaders.ModloaderListenerProxy;

public class FabricInstallFragment extends FabriclikeInstallFragment {

    public static final String TAG = "FabricInstallFragment";

    public FabricInstallFragment() {
        super(FabriclikeUtils.FABRIC_UTILS, TAG);
    }
}
