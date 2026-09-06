package net.kdt.pojavlaunch.modloaders.modmanager;

import net.kdt.pojavlaunch.utils.FileUtils;

import java.io.File;

public abstract class ModInfo {
    public final File jarFile;
    public final String searchTerms;
    public boolean expanded;

    protected ModInfo(File jarFile, String searchTerms) {
        this.jarFile = jarFile;
        this.searchTerms = FileUtils.removeExtension(jarFile.getName().toLowerCase()) + searchTerms;
    }
}
