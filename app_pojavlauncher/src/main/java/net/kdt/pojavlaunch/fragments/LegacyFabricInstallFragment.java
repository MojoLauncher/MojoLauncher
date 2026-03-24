package net.kdt.pojavlaunch.fragments;

import net.kdt.pojavlaunch.modloaders.FabriclikeUtils;
import net.kdt.pojavlaunch.modloaders.ModloaderListenerProxy;

public class LegacyFabricInstallFragment extends FabriclikeInstallFragment {

    public static final String TAG = "LegacyFabricInstallFragment";
    private static ModloaderListenerProxy sTaskProxy;
    public LegacyFabricInstallFragment() {super(FabriclikeUtils.LEGACY_FABRIC_UTILS, TAG);}
}
