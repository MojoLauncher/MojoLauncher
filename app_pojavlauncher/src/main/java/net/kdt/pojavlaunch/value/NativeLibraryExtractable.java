package net.kdt.pojavlaunch.value;

import java.io.File;

public class NativeLibraryExtractable {
    public final File path;
    public final ExtractSettings extractInfo;

    public NativeLibraryExtractable(File path, ExtractSettings extractInfo) {
        this.path = path;
        this.extractInfo = extractInfo;
    }
}
