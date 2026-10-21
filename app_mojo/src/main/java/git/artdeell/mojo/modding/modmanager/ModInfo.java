package git.artdeell.mojo.modding.modmanager;

import java.io.File;

import git.artdeell.mojo.utils.FileUtils;

public abstract class ModInfo {
    public final File jarFile;
    public final String searchTerms;
    public boolean expanded;

    protected ModInfo(File jarFile, String searchTerms) {
        this.jarFile = jarFile;
        this.searchTerms = FileUtils.removeExtension(jarFile.getName().toLowerCase()) + searchTerms;
    }
}
