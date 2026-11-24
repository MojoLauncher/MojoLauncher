package git.artdeell.mojo.instances;

import me.andreasmelone.basicmodinfoparser.platform.Platform;

/**
 * Additional instance metadata for modding
 */
public class InstanceMetadata {
    public String modloaderVersion;
    public String gameVersion;
    public Platform platform;

    protected InstanceMetadata() {}

    public InstanceMetadata(String modloaderVersion, String gameVersion, Platform platform) {
        this.modloaderVersion = modloaderVersion;
        this.gameVersion = gameVersion;
        this.platform = platform;
    }
}
