package net.kdt.pojavlaunch.modloaders.modpacks.models;

import androidx.annotation.NonNull;

public class ModDownload {
    public final String versionName;
    public final String mcVersion;
    public final String versionUrl;
    public final String versionHash;

    public ModDownload(String versionName, String mcVersion, String versionUrl, String versionHash) {
        if(!versionName.contains(mcVersion)) versionName = versionName + " - " + mcVersion;
        this.versionName = versionName;
        this.mcVersion = mcVersion;
        this.versionUrl = versionUrl;
        this.versionHash = versionHash;
    }

    @NonNull
    @Override
    public String toString() {
        return versionName;
    }
}
