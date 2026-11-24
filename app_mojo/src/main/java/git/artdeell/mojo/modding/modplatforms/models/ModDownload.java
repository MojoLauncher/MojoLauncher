package git.artdeell.mojo.modding.modplatforms.models;

import androidx.annotation.NonNull;

public class ModDownload {
    public final String versionName;
    public final String mcVersion;
    public final String versionUrl;
    public final String versionHash;

    public final String fileName;
    public final String versionId;
    public final long fileSize;
    public final String[] gameVersions;
    public final String[] loaders;

    public ModDownload(String versionName, String mcVersion, String versionUrl, String versionHash, String fileName, String versionId, long fileSize, String[] gameVersions, String[] loaders) {
        if(mcVersion != null && !versionName.contains(mcVersion)) versionName = versionName + " - " + mcVersion;
        this.versionName = versionName;
        this.mcVersion = mcVersion;
        this.versionUrl = versionUrl;
        this.versionHash = versionHash;
        this.fileName = fileName;
        this.versionId = versionId;
        this.fileSize = fileSize;
        this.gameVersions = gameVersions;
        this.loaders = loaders;
    }
    public ModDownload(String versionName, String mcVersion, String versionUrl, String versionHash) {
        this(versionName, mcVersion, versionUrl, versionHash, null, null, -1, null, null);
    }

    @NonNull
    @Override
    public String toString() {
        return versionName;
    }

    public boolean matches(String mcVersion, String modLoader) {
        if(modLoader != null && loaders != null) {
            boolean loaderMatch = false;
            for(String loader : loaders) {
                // As usual, the fabric/quilt handling
                if(modLoader.equalsIgnoreCase(loader) || (modLoader.equalsIgnoreCase("quilt") && loader.equalsIgnoreCase("fabric"))) {
                    loaderMatch = true;
                    break;
                }
            }
            if(!loaderMatch) return false;
        }
        if(mcVersion != null && gameVersions != null) {
            for(String version : gameVersions) {
                if(mcVersion.equalsIgnoreCase(version)) return true;
            }
            return false;
        }
        return true;
    }
}
