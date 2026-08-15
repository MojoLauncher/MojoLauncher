package net.kdt.pojavlaunch.instances;

import net.kdt.pojavlaunch.modloaders.modpacks.api.ModLoader;

public enum InstanceHint {
    VANILLA(false),
    OPTIFINE(false),
    FABRIC(true),
    QUILT(true),
    LEGACY_FABRIC(true),
    FORGE(true),
    NEOFORGE(true),
    BTA(true);
    public final boolean immutable;
    InstanceHint(boolean immutable){
        this.immutable = immutable;
    }
    public static InstanceHint fromModLoader(ModLoader e){
        switch(e.modLoaderType){
            case ModLoader.MOD_LOADER_FABRIC: return InstanceHint.FABRIC;
            case ModLoader.MOD_LOADER_QUILT: return InstanceHint.QUILT;
            case ModLoader.MOD_LOADER_LEGACY_FABRIC: return InstanceHint.LEGACY_FABRIC;
            case ModLoader.MOD_LOADER_FORGE: return InstanceHint.FORGE;
            case ModLoader.MOD_LOADER_NEOFORGE: return InstanceHint.NEOFORGE;
            default: return InstanceHint.VANILLA;
        }
    }
}
