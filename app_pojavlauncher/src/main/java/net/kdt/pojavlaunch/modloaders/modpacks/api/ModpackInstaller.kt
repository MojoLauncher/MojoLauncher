package net.kdt.pojavlaunch.modloaders.modpacks.api

import com.kdt.mcgui.ProgressLayout
import net.ashmeet.hyperlauncher.R
import net.kdt.pojavlaunch.Tools
import net.kdt.pojavlaunch.instances.Instance
import net.kdt.pojavlaunch.instances.InstanceSetter
import net.kdt.pojavlaunch.instances.Instances.Companion.createInstance
import net.kdt.pojavlaunch.instances.Instances.Companion.removeInstance
import net.kdt.pojavlaunch.instances.Instances.Companion.setSelectedInstance
import net.kdt.pojavlaunch.modloaders.modpacks.imagecache.ModIconCache
import net.kdt.pojavlaunch.modloaders.modpacks.models.ModDetail
import net.kdt.pojavlaunch.progresskeeper.DownloaderProgressWrapper
import net.kdt.pojavlaunch.utils.DownloadUtils
import java.io.File
import java.io.IOException
import java.util.concurrent.Callable
import kotlin.math.min

object ModpackInstaller {
    @Throws(IOException::class)
    fun installModpack(
        modpackName: String,
        title: String?,
        modpackFile: File,
        icon: String?,
        installFunction: InstallFunction
    ): ModLoader {
        // Build a new minecraft instance, folder first
        val instance = createInstance(
            { i: Instance? -> i!!.name = title },
            modpackName.substring(0, min(16, modpackName.length))
        )
        val modLoaderInfo: ModLoader
        try {
            // Install the modpack
            val gameDir = instance.gameDirectory ?: throw IOException("Instance game directory is null")
            modLoaderInfo = installFunction.installModpack(modpackFile, gameDir)
                ?: throw IOException("Unknown modpack mod loader information")

            if (modLoaderInfo.requiresGuiInstallation()) {
                val instanceInstaller = modLoaderInfo.createInstaller()
                    ?: throw IOException("Failed to prepare data for instance installation")
                instance.installer = instanceInstaller
            } else {
                val versionId = modLoaderInfo.installHeadlessly()
                    ?: throw IOException("Unknown mod loader version")
                instance.versionId = versionId
            }
            instance.write()
            ModIconCache.writeInstanceImage(instance, icon)

            setSelectedInstance(instance)
            if (modLoaderInfo.requiresGuiInstallation()) {
                instance.installer!!.start()
            }
        } catch (e: IOException) {
            removeInstance(instance)
            throw e
        } finally {
            modpackFile.delete()
            ProgressLayout.clearProgress(ProgressLayout.INSTALL_MODPACK)
        }

        return modLoaderInfo
    }

    @Throws(IOException::class)
    fun downloadModpack(
        modDetail: ModDetail,
        selectedVersion: Int,
        installFunction: InstallFunction
    ): ModLoader {
        val versionUrl = modDetail.versionUrls?.get(selectedVersion) ?: throw IOException("Missing version URL")
        val versionHash = modDetail.versionHashes?.get(selectedVersion)
        val versionName = modDetail.versionNames?.get(selectedVersion) ?: throw IOException("Missing version name")
        val title = modDetail.title ?: "Unknown Modpack"

        var modpackName = (title.lowercase() + " " + versionName)
                .trim { it <= ' ' }.replace("[\\\\/:*?\"<>| \\t\\n]".toRegex(), "_")
        val icon = modDetail.iconCacheTag

        if (versionHash != null) {
            modpackName += "_$versionHash"
        }

        if (modpackName.length > 255) {
            modpackName = modpackName.substring(0, 255)
        }

        val modpackFile = File(Tools.DIR_CACHE, "$modpackName.cf")

        val downloadBuffer = ByteArray(8192)
        try {
            DownloadUtils.ensureSha1(modpackFile, versionHash, Callable<Void?> {
                DownloadUtils.downloadFileMonitored(
                    versionUrl, modpackFile, downloadBuffer,
                    DownloaderProgressWrapper(
                        R.string.modpack_download_downloading_metadata,
                        ProgressLayout.INSTALL_MODPACK
                    )
                )
                null
            })
        } catch (e: IOException) {
            modpackFile.delete()
            throw e
        }

        return installModpack(modpackName, title, modpackFile, icon, installFunction)
    }

    fun interface InstallFunction {
        @Throws(IOException::class)
        fun installModpack(modpackFile: File, instanceDestination: File): ModLoader?
    }
}
