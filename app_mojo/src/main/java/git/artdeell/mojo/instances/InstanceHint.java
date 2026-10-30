package git.artdeell.mojo.instances;

import me.andreasmelone.basicmodinfoparser.platform.Platform;

public enum InstanceHint {
    VANILLA(false),
    OPTIFINE(false),
    FABRIC(true),
    QUILT(true),
    LEGACY_FABRIC(true),
    FORGE(true),
    NEOFORGE(true),
    BTA(false),
    LWJGL3IFY(true);
    public final boolean immutable;
    InstanceHint(boolean immutable) {
        this.immutable = immutable;
    }

    public static Platform asPlatform(InstanceHint hint, String versionId){
        if(hint == null) return null;
        switch(hint) {
            // TODO: detect legacy forge mods
            // TODO: use legacy forge for lwjgl3ify
            case LWJGL3IFY:
            case FORGE: return Platform.FORGE;
            case NEOFORGE: return Platform.NEOFORGE;
            case QUILT: return Platform.QUILT;
            case LEGACY_FABRIC:
            case FABRIC: return Platform.FABRIC;
            // This means either unsupported modding platform or the instance does not support modding at all
            default: return null;
        }
    }
}