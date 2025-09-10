package net.kdt.pojavlaunch.modloaders;

/**
 * Resolves the loader version from a minecraft version and a loader version
 *
 * Used in {@link ForgelikeUtils}
 */
@FunctionalInterface
public interface VersionResolver {
    String resolveVersion(String minecraftVersion, String modloaderVersion);
}
