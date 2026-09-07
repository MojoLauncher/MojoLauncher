package net.kdt.pojavlaunch.modloaders.modpacks.api.modloader;

import net.kdt.pojavlaunch.instances.InstanceInstaller;
import net.kdt.pojavlaunch.modloaders.ForgelikeUtils;

import java.io.IOException;

public class ForgelikeLoaderInstaller implements LoaderInstaller {
    private final ForgelikeUtils mUtils;
    private final String gameVersion;
    private final String loaderVersion;

    public ForgelikeLoaderInstaller(ForgelikeUtils utils, String gameVersion, String loaderVersion) {
        this.mUtils = utils;
        this.gameVersion = gameVersion;
        this.loaderVersion = loaderVersion;
    }

    @Override
    public boolean requiresGuiInstallation() {
        return true;
    }

    @Override
    public InstanceInstaller createInstaller() throws IOException {
        return mUtils.createInstaller(gameVersion, loaderVersion);
    }

    @Override
    public String installHeadlessly() throws IOException {
        throw new RuntimeException("ForgelikeLoaderInstaller only supports GUI installation");
    }
}
