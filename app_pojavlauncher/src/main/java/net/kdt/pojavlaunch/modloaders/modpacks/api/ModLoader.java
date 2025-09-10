package net.kdt.pojavlaunch.modloaders.modpacks.api;

import net.kdt.pojavlaunch.instances.InstanceInstaller;
import net.kdt.pojavlaunch.modloaders.FabriclikeUtils;
import net.kdt.pojavlaunch.modloaders.ForgelikeUtils;

import java.io.IOException;

public class ModLoader {
    public static final int MOD_LOADER_FORGE = 0;
    public static final int MOD_LOADER_FABRIC = 1;
    public static final int MOD_LOADER_QUILT = 2;
    public static final int MOD_LOADER_NEOFORGE = 3;
    public final int modLoaderType;
    public final String modLoaderVersion;
    public final String minecraftVersion;

    public ModLoader(int modLoaderType, String modLoaderVersion, String minecraftVersion) {
        this.modLoaderType = modLoaderType;
        this.modLoaderVersion = modLoaderVersion;
        this.minecraftVersion = minecraftVersion;
    }

    /**
     * Get the Version ID (the name of the mod loader in the versions/ folder)
     * @return the Version ID as a string
     */
    public String getVersionId() {
        switch (modLoaderType) {
            case MOD_LOADER_FORGE:
                return minecraftVersion+"-forge-"+modLoaderVersion;
            case MOD_LOADER_FABRIC:
                return "fabric-loader-"+modLoaderVersion+"-"+minecraftVersion;
            case MOD_LOADER_QUILT:
                return "quilt-loader-"+modLoaderVersion+"-"+minecraftVersion;
            case MOD_LOADER_NEOFORGE:
                return "neoforge-" + modLoaderVersion;
            default:
                return null;
        }
    }

    /**
     * Perform the installation of a mod loader headlessly, if possible
     * @return the real version ID
     */
    public String installHeadlessly() throws IOException{
        switch (modLoaderType) {
            case MOD_LOADER_FABRIC:
                return FabriclikeUtils.FABRIC_UTILS.install(minecraftVersion, modLoaderVersion);
            case MOD_LOADER_QUILT:
                return FabriclikeUtils.QUILT_UTILS.install(minecraftVersion, modLoaderVersion);
            case MOD_LOADER_FORGE:
            case MOD_LOADER_NEOFORGE:
            default:
                return null;
        }
    }

    /**
     * Create an InstanceInstaller, if GUI installation is required by this mod loader.
     * @return the InstanceInstaller that is used to complete mod loader installation.
     */
    public InstanceInstaller createInstaller() throws IOException {
        switch (modLoaderType) {
            case MOD_LOADER_NEOFORGE:
                return ForgelikeUtils.NEOFORGE_UTILS.createInstaller(minecraftVersion, modLoaderVersion);
            case MOD_LOADER_FORGE:
                return ForgelikeUtils.FORGE_UTILS.createInstaller(minecraftVersion, modLoaderVersion);
            case MOD_LOADER_QUILT:
            case MOD_LOADER_FABRIC:
            default:
                return null;
        }
    }

    /**
     * Check whether the mod loader this object denotes requires GUI installation
     * @return true if mod loader requires GUI installation, false otherwise
     */
    public boolean requiresGuiInstallation() {
        switch (modLoaderType) {
            // technically, neoforge can be installed without a gui
            // as the installer allows to install it by passing the --installClient arg
            // but this requires some extra code which I have not done yet
            // TODO implement headless neoforge installation
            case MOD_LOADER_NEOFORGE:
            case MOD_LOADER_FORGE:
                return true;
            case MOD_LOADER_FABRIC:
            case MOD_LOADER_QUILT:
            default:
                return false;
        }
    }
}
