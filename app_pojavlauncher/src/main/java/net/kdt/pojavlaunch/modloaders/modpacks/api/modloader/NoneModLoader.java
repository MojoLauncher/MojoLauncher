package net.kdt.pojavlaunch.modloaders.modpacks.api.modloader;

import net.kdt.pojavlaunch.instances.InstanceInstaller;

import java.io.IOException;

public class NoneModLoader implements ModLoader {
    public final String version;

    public NoneModLoader(String version) {
        this.version = version;
    }

    @Override
    public boolean requiresGuiInstallation() {
        return false;
    }

    @Override
    public InstanceInstaller createInstaller() throws IOException {
        return null;
    }

    @Override
    public String installHeadlessly() throws IOException {
        return "";
    }
}
