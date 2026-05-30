package net.kdt.pojavlaunch.modloaders

import net.kdt.pojavlaunch.Tools
import net.kdt.pojavlaunch.instances.InstanceInstaller
import net.kdt.pojavlaunch.utils.DownloadUtils
import java.io.File
import java.io.IOException
import java.util.Objects

object OptiFineUtils {
    @JvmStatic
    @Throws(IOException::class)
    fun downloadOptiFineVersions(): OptiFineVersions? {
        try {
            return DownloadUtils.downloadStringCached<OptiFineVersions?>(
                "https://optifine.net/downloads",
                "of_downloads_page", OptiFineScraper()
            )
        } catch (e: DownloadUtils.ParseException) {
            e.printStackTrace()
            return null
        }
    }

    @JvmStatic
    fun createInstaller(version: OptiFineVersion): InstanceInstaller {
        val installerHash = Objects.hash(version.versionName, version.minecraftVersion)
        val installerLocation =
            File(Tools.DIR_CACHE, "optifine-installer-" + installerHash + ".jar")
        val instanceInstaller = InstanceInstaller()
        instanceInstaller.installerUrlTransformer = "optifine"
        instanceInstaller.installerDownloadUrl = version.downloadUrl
        instanceInstaller.installerJar = installerLocation.getAbsolutePath()
        instanceInstaller.commandLineArgs =
            mutableListOf("-javaagent:" + Tools.DIR_DATA + "/forge_installer/forge_installer.jar=OF")
        return instanceInstaller
    }

    class OptiFineVersions {
        var minecraftVersions: MutableList<String?>? = null
        var optifineVersions: MutableList<MutableList<OptiFineVersion?>?>? = null
    }

    class OptiFineVersion {
        var minecraftVersion: String? = null
        var versionName: String? = null
        var downloadUrl: String? = null
    }
}
