package net.kdt.pojavlaunch.modloaders.modpacks.api

import net.kdt.pojavlaunch.instances.InstanceInstaller
import net.kdt.pojavlaunch.modloaders.FabriclikeUtils
import net.kdt.pojavlaunch.modloaders.ForgelikeUtils
import java.io.IOException

class ModLoader(
    val modLoaderType: Int,
    val modLoaderVersion: String?,
    val minecraftVersion: String?
) {
    val versionId: String?
        /**
         * Get the Version ID (the name of the mod loader in the versions/ folder)
         * @return the Version ID as a string
         */
        get() {
            when (modLoaderType) {
                MOD_LOADER_FORGE -> return minecraftVersion + "-forge-" + modLoaderVersion
                MOD_LOADER_FABRIC -> return "fabric-loader-" + modLoaderVersion + "-" + minecraftVersion
                MOD_LOADER_QUILT -> return "quilt-loader-" + modLoaderVersion + "-" + minecraftVersion
                MOD_LOADER_NEOFORGE -> return "neoforge-" + modLoaderVersion
                MOD_LOADER_LEGACY_FABRIC -> return "legacy-fabric-loader-" + modLoaderVersion + "-" + minecraftVersion
                else -> return null
            }
        }

    /**
     * Perform the installation of a mod loader headlessly, if possible
     * @return the real version ID
     */
    @Throws(IOException::class)
    fun installHeadlessly(): String? {
        when (modLoaderType) {
            MOD_LOADER_FABRIC -> return FabriclikeUtils.Companion.FABRIC_UTILS.install(
                minecraftVersion,
                modLoaderVersion
            )

            MOD_LOADER_QUILT -> return FabriclikeUtils.Companion.QUILT_UTILS.install(
                minecraftVersion,
                modLoaderVersion
            )

            MOD_LOADER_LEGACY_FABRIC -> return FabriclikeUtils.Companion.LEGACY_FABRIC_UTILS.install(
                minecraftVersion,
                modLoaderVersion
            )

            MOD_LOADER_FORGE, MOD_LOADER_NEOFORGE -> return null
            else -> return null
        }
    }

    /**
     * Create an InstanceInstaller, if GUI installation is required by this mod loader.
     * @return the InstanceInstaller that is used to complete mod loader installation.
     */
    @Throws(IOException::class)
    fun createInstaller(): InstanceInstaller? {
        if (minecraftVersion == null) return null
        when (modLoaderType) {
            MOD_LOADER_NEOFORGE -> return ForgelikeUtils.Companion.NEOFORGE_UTILS.createInstaller(
                minecraftVersion,
                modLoaderVersion
            )

            MOD_LOADER_FORGE -> return ForgelikeUtils.Companion.FORGE_UTILS.createInstaller(
                minecraftVersion,
                modLoaderVersion
            )

            MOD_LOADER_QUILT, MOD_LOADER_FABRIC, MOD_LOADER_LEGACY_FABRIC -> return null
            else -> return null
        }
    }

    /**
     * Check whether the mod loader this object denotes requires GUI installation
     * @return true if mod loader requires GUI installation, false otherwise
     */
    fun requiresGuiInstallation(): Boolean {
        when (modLoaderType) {
            MOD_LOADER_NEOFORGE, MOD_LOADER_FORGE -> return true
            MOD_LOADER_FABRIC, MOD_LOADER_QUILT, MOD_LOADER_LEGACY_FABRIC -> return false
            else -> return false
        }
    }

    companion object {
        const val MOD_LOADER_FORGE: Int = 0
        const val MOD_LOADER_FABRIC: Int = 1
        const val MOD_LOADER_QUILT: Int = 2
        const val MOD_LOADER_NEOFORGE: Int = 3
        const val MOD_LOADER_LEGACY_FABRIC: Int = 4
    }
}
