package git.artdeell.mojo.modding.modplatforms.api.modloader;

import git.artdeell.mojo.instances.InstanceInstaller;
import git.artdeell.mojo.modding.PlatformMetadata;

import java.io.IOException;

public interface LoaderInstaller {
    boolean requiresGuiInstallation();
    InstanceInstaller createInstaller() throws IOException;
    String installHeadlessly() throws IOException;
    PlatformMetadata getMetadata();
}
