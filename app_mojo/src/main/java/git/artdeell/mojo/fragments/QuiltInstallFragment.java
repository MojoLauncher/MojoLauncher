package git.artdeell.mojo.fragments;

import git.artdeell.mojo.modding.fabric.FabriclikeUtils;
import git.artdeell.mojo.modding.ModloaderListenerProxy;

public class QuiltInstallFragment extends FabriclikeInstallFragment {

    public static final String TAG = "QuiltInstallFragment";
    private static ModloaderListenerProxy sTaskProxy;

    public QuiltInstallFragment() {
        super(FabriclikeUtils.QUILT_UTILS, TAG);
    }
}
