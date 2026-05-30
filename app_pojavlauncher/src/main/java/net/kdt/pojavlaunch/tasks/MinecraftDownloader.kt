package net.kdt.pojavlaunch.tasks

import android.content.res.AssetManager
import android.util.Log
import com.google.gson.JsonParseException
import com.kdt.mcgui.ProgressLayout
import net.ashmeet.hyperlauncher.R
import net.kdt.pojavlaunch.JAssets
import net.kdt.pojavlaunch.JMinecraftVersionList
import net.kdt.pojavlaunch.JMinecraftVersionList.LoggingConfig
import net.kdt.pojavlaunch.NewJREUtil
import net.kdt.pojavlaunch.PojavApplication
import net.kdt.pojavlaunch.Tools
import net.kdt.pojavlaunch.downloader.Downloader
import net.kdt.pojavlaunch.downloader.TaskMetadata
import net.kdt.pojavlaunch.mirrors.DownloadMirror
import net.kdt.pojavlaunch.mirrors.DownloadMirror.downloadFileMirrored
import net.kdt.pojavlaunch.mirrors.DownloadMirror.isMirrored
import net.kdt.pojavlaunch.mirrors.MirrorTamperedException
import net.kdt.pojavlaunch.prefs.LauncherPreferences
import net.kdt.pojavlaunch.tasks.AsyncMinecraftDownloader.DoneListener
import net.kdt.pojavlaunch.utils.DownloadUtils.SHA1VerificationException
import net.kdt.pojavlaunch.utils.DownloadUtils.ensureSha1
import net.kdt.pojavlaunch.utils.FileUtils.ensureDirectory
import net.kdt.pojavlaunch.utils.FileUtils.ensureParentDirectory
import net.kdt.pojavlaunch.utils.FileUtils.removeExtension
import net.kdt.pojavlaunch.utils.JSONUtils
import net.kdt.pojavlaunch.utils.jre.RuntimeSelectionException
import net.kdt.pojavlaunch.value.DependentLibrary
import net.kdt.pojavlaunch.value.MinecraftClientInfo
import java.io.File
import java.io.IOException
import java.net.URL

class MinecraftDownloader : Downloader(ProgressLayout.DOWNLOAD_MINECRAFT) {
    private var mScheduledDownloadTasks: ArrayList<TaskMetadata?>? = null
    private var mDeclaredNatives: ArrayList<File?>? = null
    private var mSourceJarFile: File? =
        null // The source client JAR picked during the inheritance process
    private var mTargetJarFile: File? =
        null // The destination client JAR to which the source will be copied to.
    private var mVersionName: String? = null

    /**
     * Start the game version download process on the global executor service.
     * @param assetManager AssetManager, used for automatic installation of JRE 17 if needed
     * @param version The JMinecraftVersionList.Version from the version list, if available
     * @param realVersion The version ID (necessary)
     * @param listener The download status listener
     */
    fun start(
        assetManager: AssetManager?, version: JMinecraftVersionList.Version?,
        realVersion: String,  // this was there for a reason
        listener: DoneListener
    ) {
        PojavApplication.sExecutorService.execute {
            try {
                downloadGame(assetManager, version, realVersion)
                listener.onDownloadDone()
            } catch (e: JsonParseException) {
                listener.onDownloadFailed(e) // Handled separately from the general case because it subclasses RuntimeException. Ugh.
            } catch (e: RuntimeException) {
                throw e // log fatal errors to Google Play
            } catch (e: Exception) {
                listener.onDownloadFailed(e)
            }
            ProgressLayout.clearProgress(ProgressLayout.DOWNLOAD_MINECRAFT)
        }
    }

    /**
     * Download the game version.
     * @param assetManager AssetManager, used for automatic installation of JRE 17 if needed
     * @param verInfo The JMinecraftVersionList.Version from the version list, if available
     * @param versionName The version ID (necessary)
     * @throws Exception when an exception occurs in the function body or in any of the downloading threads.
     */
    @Throws(Exception::class)
    private fun downloadGame(
        assetManager: AssetManager?,
        verInfo: JMinecraftVersionList.Version?,
        versionName: String?
    ) {
        // Put up a dummy progress line, for the activity to start the service and do all the other necessary
        // work to keep the launcher alive. We will replace this line when we will start downloading stuff.
        ProgressLayout.setProgress(ProgressLayout.DOWNLOAD_MINECRAFT, 0, R.string.newdl_starting)

        mTargetJarFile = createGameJarPath(versionName)
        mScheduledDownloadTasks = ArrayList()
        mDeclaredNatives = ArrayList()
        mVersionName = versionName

        downloadAndProcessMetadata(assetManager, verInfo, versionName)

        runDownloads(mScheduledDownloadTasks)

        ensureJarFileCopy()
        extractNatives(mVersionName)
    }

    private fun createGameJsonPath(versionId: String?): File {
        return File(Tools.DIR_HOME_VERSION, "$versionId${File.separator}$versionId.json")
    }

    private fun createGameJarPath(versionId: String?): File {
        return File(Tools.DIR_HOME_VERSION, "$versionId${File.separator}$versionId.jar")
    }

    /**
     * Ensure that there is a copy of the client JAR file in the version folder, if a copy is
     * needed.
     * @throws IOException if the copy fails
     */
    @Throws(IOException::class)
    private fun ensureJarFileCopy() {
        if (mSourceJarFile == null) return
        if (mSourceJarFile == mTargetJarFile) return
        if (mTargetJarFile?.exists() == true) return
        ensureParentDirectory(mTargetJarFile!!)
        Log.i(
            "NewMCDownloader",
            "Copying ${mSourceJarFile!!.name} to ${mTargetJarFile!!.absolutePath}"
        )
        org.apache.commons.io.FileUtils.copyFile(mSourceJarFile, mTargetJarFile, false)
    }

    @Throws(IOException::class)
    private fun extractNatives(versionName: String?) {
        val declaredNatives = mDeclaredNatives ?: return
        if (declaredNatives.isEmpty()) return
        val totalCount = declaredNatives.size

        ProgressLayout.setProgress(
            ProgressLayout.DOWNLOAD_MINECRAFT, 0,
            R.string.newdl_extracting_native_libraries, 0, totalCount
        )

        val targetDirectory = File(Tools.DIR_CACHE, "natives/$versionName")
        ensureDirectory(targetDirectory)
        val nativesExtractor = NativesExtractor(targetDirectory)
        var extractedCount = 0
        for (source in declaredNatives) {
            nativesExtractor.extractFromAar(source)
            extractedCount++
            ProgressLayout.setProgress(
                ProgressLayout.DOWNLOAD_MINECRAFT, extractedCount * 100 / totalCount,
                R.string.newdl_extracting_native_libraries, extractedCount, totalCount
            )
        }
    }

    @Throws(IOException::class, MirrorTamperedException::class)
    private fun downloadGameJson(verInfo: JMinecraftVersionList.Version): File {
        val targetFile = createGameJsonPath(verInfo.id)
        if (verInfo.sha1 == null && canReuseMetadataWithoutSha1(targetFile)) return targetFile
        ensureParentDirectory(targetFile)
        try {
            ensureSha1(
                targetFile,
                if (LauncherPreferences.PREF_VERIFY_MANIFEST) verInfo.sha1 else null
            ) {
                ProgressLayout.setProgress(
                    ProgressLayout.DOWNLOAD_MINECRAFT, 0,
                    R.string.newdl_downloading_metadata, targetFile.name
                )
                downloadFileMirrored(
                    DownloadMirror.DOWNLOAD_CLASS_METADATA,
                    verInfo.url ?: throw IOException("Version JSON URL is null"),
                    targetFile
                )
                null
            }
        } catch (e: SHA1VerificationException) {
            if (isMirrored) throw MirrorTamperedException()
            else throw e
        }
        return targetFile
    }

    private fun canReuseMetadataWithoutSha1(targetFile: File): Boolean {
        if (!targetFile.isFile || !targetFile.canRead()) return false
        return try {
            val cachedVersion = JSONUtils.readFromFile(
                targetFile,
                JMinecraftVersionList.Version::class.java
            ) ?: return false

            val hasLaunchMetadata = Tools.isValidString(cachedVersion.mainClass)
                    || Tools.isValidString(cachedVersion.inheritsFrom)
            val hasAssetMetadata = cachedVersion.assetIndex != null
                    || Tools.isValidString(cachedVersion.assets)
                    || Tools.isValidString(cachedVersion.inheritsFrom)

            hasLaunchMetadata && hasAssetMetadata
        } catch (_: Exception) {
            false
        }
    }

    @Throws(IOException::class)
    private fun downloadAssetsIndex(verInfo: JMinecraftVersionList.Version): JAssets? {
        val assetIndex = verInfo.assetIndex
        if (assetIndex == null || verInfo.assets == null) return null
        val targetFile =
            File(Tools.ASSETS_PATH, "indexes" + File.separator + verInfo.assets + ".json")
        ensureParentDirectory(targetFile)
        ensureSha1(targetFile, assetIndex.sha1) {
            ProgressLayout.setProgress(
                ProgressLayout.DOWNLOAD_MINECRAFT, 0,
                R.string.newdl_downloading_metadata, targetFile.name
            )
            downloadFileMirrored(
                DownloadMirror.DOWNLOAD_CLASS_METADATA,
                assetIndex.url ?: throw IOException("Asset index URL is null"),
                targetFile
            )
            null
        }
        return Tools.GLOBAL_GSON.fromJson(Tools.read(targetFile), JAssets::class.java)
    }

    private fun getClientInfo(verInfo: JMinecraftVersionList.Version): MinecraftClientInfo? {
        val downloads = verInfo.downloads
        return downloads?.get("client")
    }

    /**
     * Download (if necessary) and process a version's metadata, scheduling all downloads that this
     * version needs.
     * @param assetManager AssetManager, used for automatic installation of JRE 17 if needed
     * @param verInfo The JMinecraftVersionList.Version from the version list, if available
     * @param versionName The version ID (necessary)
     * @throws IOException if the download of any of the metadata files fails
     */
    @Throws(
        IOException::class,
        MirrorTamperedException::class,
        RuntimeSelectionException::class,
        JsonParseException::class
    )
    private fun downloadAndProcessMetadata(
        assetManager: AssetManager?,
        verInfo: JMinecraftVersionList.Version?,
        versionName: String?
    ) {
        var currentVerInfo = verInfo
        val versionJsonFile = if (currentVerInfo != null) downloadGameJson(currentVerInfo)
        else createGameJsonPath(versionName)
        if (versionJsonFile.canRead()) {
            currentVerInfo = JSONUtils.readFromFile(
                versionJsonFile,
                JMinecraftVersionList.Version::class.java
            ) ?: throw IOException("Deserialized json is null. Contact developer.")
        } else {
            throw IOException("Unable to read Version JSON for version $versionName")
        }

        if (assetManager != null) NewJREUtil.installNewJreIfNeeded(assetManager, currentVerInfo)

        val assets = downloadAssetsIndex(currentVerInfo)
        if (assets != null) scheduleAssetDownloads(assets)

        val minecraftClientInfo = getClientInfo(currentVerInfo)
        if (minecraftClientInfo != null) scheduleGameJarDownload(minecraftClientInfo, versionName)

        currentVerInfo.libraries?.filterNotNull()?.toTypedArray()?.let { scheduleLibraryDownloads(it) }

        currentVerInfo.logging?.let { scheduleLoggingAssetDownloadIfNeeded(it) }

        if (Tools.isValidString(currentVerInfo.inheritsFrom)) {
            val inheritedVersion = AsyncMinecraftDownloader.getListedVersion(currentVerInfo.inheritsFrom)
            // Infinite inheritance !?! :noway:
            downloadAndProcessMetadata(assetManager, inheritedVersion, currentVerInfo.inheritsFrom)
        }
    }

    private fun growDownloadList(addedElementCount: Int) {
        mScheduledDownloadTasks!!.ensureCapacity(mScheduledDownloadTasks!!.size + addedElementCount)
    }

    @Throws(IOException::class)
    private fun scheduleDownload(
        targetFile: File, downloadClass: Int, url: String?, sha1: String?,
        size: Long
    ) {
        var currentSha1 = sha1
        ensureParentDirectory(targetFile)
        if (!Tools.isValidString(currentSha1)) currentSha1 = null
        var urlObject: URL? = null
        if (Tools.isValidString(url)) urlObject = URL(url)
        val taskMetadata = TaskMetadata(targetFile, urlObject, size, currentSha1, downloadClass)
        mScheduledDownloadTasks!!.add(taskMetadata)
    }

    /**
     * Schedule the download of an AAR library containing the required natives, for later extraction
     * and adding to the library path.
     * @param baseRepository the source Maven repository to download from.
     * @param dependentLibrary the DependentLibrary to get the path from
     * @throws IOException in case if download scheduling fails.
     */
    @Throws(IOException::class)
    private fun scheduleNativeLibraryDownload(
        baseRepository: String?,
        dependentLibrary: DependentLibrary
    ) {
        val path = removeExtension(Tools.artifactToPath(dependentLibrary)) + ".aar"
        val downloadUrl = baseRepository + path
        val targetPath = File(Tools.DIR_HOME_LIBRARY, path)
        mDeclaredNatives!!.add(targetPath)
        scheduleDownload(targetPath, DownloadMirror.DOWNLOAD_CLASS_LIBRARIES, downloadUrl, null, -1)
    }

    @Throws(IOException::class)
    private fun scheduleLibraryDownloads(dependentLibraries: Array<DependentLibrary>) {
        Tools.preProcessLibraries(dependentLibraries)
        growDownloadList(dependentLibraries.size)
        for (dependentLibrary in dependentLibraries) {
            if (Tools.shouldSkipLibrary(dependentLibrary)) continue
            // Special handling for JNA Android natives
            if (dependentLibrary.name?.startsWith("net.java.dev.jna:jna:") == true && !dependentLibrary.replaced) {
                scheduleNativeLibraryDownload(Tools.MAVEN_CENTRAL, dependentLibrary)
            }
            val libArtifactPath = Tools.artifactToPath(dependentLibrary)
            var sha1: String? = null
            var url: String? = null
            var size: Long = -1
            val downloads = dependentLibrary.downloads
            if (downloads != null) {
                val artifact = downloads.artifact
                if (artifact != null) {
                    sha1 = artifact.sha1
                    url = artifact.url
                    size = artifact.size.toLong()
                } else {
                    // If the library has a downloads section but doesn't have an artifact in
                    // it, it is likely natives-only, which means it can be skipped.
                    Log.i(
                        "NewMCDownloader",
                        "Skipped library ${dependentLibrary.name} due to lack of artifact"
                    )
                    continue
                }
            }
            if (url == null) {
                url = (if (dependentLibrary.url == null)
                    "https://libraries.minecraft.net/"
                else
                    dependentLibrary.url!!.replace("http://", "https://")) + libArtifactPath
            }
            scheduleDownload(
                File(Tools.DIR_HOME_LIBRARY, libArtifactPath),
                DownloadMirror.DOWNLOAD_CLASS_LIBRARIES,
                url, sha1, size
            )
        }
    }

    @Throws(IOException::class)
    private fun scheduleAssetDownloads(assets: JAssets) {
        val assetObjects = assets.objects ?: return
        val assetNames = assetObjects.keys.filterNotNull()
        growDownloadList(assetNames.size)
        for (asset in assetNames) {
            val assetInfo = assetObjects[asset] ?: continue
            val hash = assetInfo.hash ?: continue
            val hashedPath = hash.substring(0, 2) + File.separator + hash
            val basePath =
                if (assets.mapToResources) Tools.OBSOLETE_RESOURCES_PATH else Tools.ASSETS_PATH
            val targetFile = if (assets.virtual || assets.mapToResources) {
                File(basePath, asset)
            } else {
                File(basePath, "objects" + File.separator + hashedPath)
            }
            scheduleDownload(
                targetFile,
                DownloadMirror.DOWNLOAD_CLASS_ASSETS,
                MINECRAFT_RES + hashedPath,
                hash,
                assetInfo.size.toLong()
            )
        }
    }

    @Throws(IOException::class)
    private fun scheduleLoggingAssetDownloadIfNeeded(loggingConfig: LoggingConfig) {
        val client = loggingConfig.client ?: return
        val loggingFileProperties = client.file ?: return
        val logId = loggingFileProperties.id ?: return
        val patchId = logId.replace("client", "log4j-rce-patch")
        val internalLoggingConfig = File(
            Tools.DIR_DATA + File.separator + "security",
            patchId
        )
        if (internalLoggingConfig.exists()) return
        val destination = File(Tools.DIR_GAME_NEW, logId)
        scheduleDownload(
            destination,
            DownloadMirror.DOWNLOAD_CLASS_LIBRARIES,
            loggingFileProperties.url,
            loggingFileProperties.sha1,
            loggingFileProperties.size
        )
    }

    @Throws(IOException::class)
    private fun scheduleGameJarDownload(
        minecraftClientInfo: MinecraftClientInfo,
        versionName: String?
    ) {
        val clientJar = createGameJarPath(versionName)
        growDownloadList(1)
        scheduleDownload(
            clientJar,
            DownloadMirror.DOWNLOAD_CLASS_LIBRARIES,
            minecraftClientInfo.url,
            minecraftClientInfo.sha1,
            minecraftClientInfo.size.toLong()
        )
        // Store the path of the JAR to copy it into our new version folder later.
        mSourceJarFile = clientJar
    }

    companion object {
        const val MINECRAFT_RES: String = "https://resources.download.minecraft.net/"
    }
}
