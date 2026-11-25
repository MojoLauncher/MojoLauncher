package git.artdeell.mojo.modding.modplatforms.api.modloader;

import git.artdeell.mojo.instances.InstanceInstaller;
import git.artdeell.mojo.modding.PlatformMetadata;
import git.artdeell.mojo.modding.lwjgl3ify.Lwjgl3ifyUtils;
import me.andreasmelone.basicmodinfoparser.platform.Platform;

import java.io.IOException;
import java.io.File;

public class Lwjgl3ifyLoaderInstaller implements LoaderInstaller {
    private final File lwjgl3ifyJarPath;
    private final String gameVersion;

    public Lwjgl3ifyLoaderInstaller(File lwjgl3ifyJarPath, String gameVersion) {
        this.lwjgl3ifyJarPath = lwjgl3ifyJarPath;
        this.gameVersion = gameVersion;
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

    @Override
    public PlatformMetadata getMetadata() {
        return new PlatformMetadata(null, gameVersion, Platform.FORGE_LEGACY);
    }
}
