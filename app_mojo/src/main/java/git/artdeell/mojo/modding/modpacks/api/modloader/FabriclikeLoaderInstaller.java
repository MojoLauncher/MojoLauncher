package git.artdeell.mojo.modding.modpacks.api.modloader;

import git.artdeell.mojo.instances.InstanceInstaller;
import git.artdeell.mojo.modding.fabric.FabriclikeUtils;

import java.io.IOException;

public class FabriclikeLoaderInstaller implements LoaderInstaller {
    private final FabriclikeUtils mUtils;
    private final String gameVersion;
    private final String loaderVersion;
    public FabriclikeLoaderInstaller(FabriclikeUtils utils, String gameVersion, String loaderVersion) {
        mUtils = utils;
        this.gameVersion = gameVersion;
        this.loaderVersion = loaderVersion;
    }

    @Override
    public boolean requiresGuiInstallation() {
        return false;
    }

    @Override
    public InstanceInstaller createInstaller() {
        throw new RuntimeException("FabriclikeLoaderInstaller only supports headless installation");
    }

    @Override
    public String installHeadlessly() throws IOException {
        return mUtils.install(gameVersion, loaderVersion);
    }
}
