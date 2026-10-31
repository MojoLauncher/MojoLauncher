package git.artdeell.mojo.modding.modmanager;

import java.io.File;

import me.andreasmelone.basicmodinfoparser.modfile.ModFile;

public class ContainedModInfo extends ModInfo {
    public ModFile modFile;

    public ContainedModInfo(File jarFile, ModFile modFile, String extraSearchTerms) {
        super(jarFile, extraSearchTerms);
        this.modFile = modFile;
    }
}
