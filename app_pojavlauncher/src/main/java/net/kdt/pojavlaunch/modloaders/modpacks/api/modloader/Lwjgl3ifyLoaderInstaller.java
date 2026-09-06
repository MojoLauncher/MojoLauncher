package net.kdt.pojavlaunch.modloaders.modpacks.api.modloader;

import net.kdt.pojavlaunch.instances.InstanceInstaller;
import net.kdt.pojavlaunch.modloaders.Lwjgl3ifyUtils;

import java.io.IOException;
import java.io.File;

public class Lwjgl3ifyLoaderInstaller implements LoaderInstaller {
    private final File lwjgl3ifyJarPath;

    public Lwjgl3ifyLoaderInstaller(File lwjgl3ifyJarPath) {
        this.lwjgl3ifyJarPath = lwjgl3ifyJarPath;
    }

    @Override
    public boolean requiresGuiInstallation() {
        return false;
    }

    @Override
    public InstanceInstaller createInstaller() throws IOException {
        throw new RuntimeException("Lwjgl3ifyLoaderInstaller only supports headless installation");
    }

    @Override
    public String installHeadlessly() throws IOException {
        return Lwjgl3ifyUtils.installRelauncherVersionJson(lwjgl3ifyJarPath);
    }
}
