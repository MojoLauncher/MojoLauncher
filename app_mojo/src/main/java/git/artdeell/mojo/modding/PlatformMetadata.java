package git.artdeell.mojo.modding;

import me.andreasmelone.basicmodinfoparser.platform.Platform;

/**
 * Additional instance metadata for modding
 */
public class PlatformMetadata {
    public String modloaderVersion;
    public String gameVersion;
    public Platform platform;

    protected PlatformMetadata() {}

    public PlatformMetadata(String modloaderVersion, String gameVersion, Platform platform) {
        this.modloaderVersion = modloaderVersion;
        this.gameVersion = gameVersion;
        this.platform = platform;
    }
}
