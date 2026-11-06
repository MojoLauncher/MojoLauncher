package git.artdeell.mojo.modding.modpacks.api.modloader;

import git.artdeell.mojo.instances.InstanceInstaller;

import java.io.IOException;

public interface LoaderInstaller {
    boolean requiresGuiInstallation();
    InstanceInstaller createInstaller() throws IOException;
    String installHeadlessly() throws IOException;
}
