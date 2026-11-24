package git.artdeell.mojo.modding.modplatforms.api.modloader;

import git.artdeell.mojo.instances.InstanceInstaller;
import git.artdeell.mojo.instances.InstanceMetadata;

import java.io.IOException;

public interface LoaderInstaller {
    boolean requiresGuiInstallation();
    InstanceInstaller createInstaller() throws IOException;
    String installHeadlessly() throws IOException;
    InstanceMetadata getMetadata();
}
