package net.kdt.pojavlaunch.modloaders.modmanager;

import java.io.File;

public class CorruptModInfo extends ModInfo {
    public static final int CORRUPTION_REASON_NOT_READABLE = 0;
    public static final int CORRUPTION_REASON_NOT_A_MOD = 1;
    public final int corruptionReason;

    public CorruptModInfo(File jarFile, int corruptionReason) {
        super(jarFile, "corrupted");
        this.corruptionReason = corruptionReason;
    }
}
