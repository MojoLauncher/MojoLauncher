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
        if(!versionName.contains(mcVersion)) versionName = versionName + " - " + mcVersion;
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
        // TODO: This is shit. I know. I will improve it in the future.
        if(loaders == null) return true;
        boolean res = false;
        for(String loader : loaders) {
            if (modLoader.equalsIgnoreCase(loader)) {
                res = true; break;
            }
        }
        if(gameVersions == null) return res;
        res = false;
        for(String version : gameVersions) {
            if (mcVersion.equalsIgnoreCase(version)) {
                res = true; break;
            }
        }
        return res;
    }
}
