package net.kdt.pojavlaunch.tasks

import android.content.res.AssetManager
import android.util.Log
import com.google.gson.JsonParseException
import com.kdt.mcgui.ProgressLayout
import net.ashmeet.hyperlauncher.R
import net.kdt.pojavlaunch.*
import net.kdt.pojavlaunch.downloader.Downloader
import net.kdt.pojavlaunch.downloader.TaskMetadata
import net.kdt.pojavlaunch.mirrors.DownloadMirror
import net.kdt.pojavlaunch.mirrors.DownloadMirror.downloadFileMirrored
import net.kdt.pojavlaunch.mirrors.DownloadMirror.isMirrored
import net.kdt.pojavlaunch.mirrors.MirrorTamperedException
import net.kdt.pojavlaunch.prefs.LauncherPreferences
import net.kdt.pojavlaunch.tasks.AsyncMinecraftDownloader.DoneListener
import net.kdt.pojavlaunch.utils.DownloadUtils
import net.kdt.pojavlaunch.utils.DownloadUtils.SHA1VerificationException
import net.kdt.pojavlaunch.utils.DownloadUtils.ensureSha1
import net.kdt.pojavlaunch.utils.FileUtils
import net.kdt.pojavlaunch.utils.FileUtils.ensureDirectory
import net.kdt.pojavlaunch.utils.FileUtils.ensureParentDirectory
import net.kdt.pojavlaunch.utils.JSONUtils
import net.kdt.pojavlaunch.utils.MavenNameUtils
import net.kdt.pojavlaunch.utils.jre.RuntimeSelectionException
import net.kdt.pojavlaunch.value.*
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.net.URL
import java.util.*
import java.util.concurrent.Future

class MinecraftDownloader : Downloader(ProgressLayout.DOWNLOAD_MINECRAFT) {
    private val mNativeName = "android-" + Architecture.archAsString(Architecture.deviceArchitecture)

    private var mScheduledDownloadTasks: ArrayList<TaskMetadata>? = null
    private var mDeclaredNatives: ArrayList<NativeLibraryExtractable>? = null
    private var mAllLibraries: LinkedHashMap<String, DependentLibrary>? = null
    private var mClassPath: LinkedHashSet<File>? = null
    private var mSubstitutionMap: SubstitutionMap? = null

    private var mSourceJarFile: File? = null
    private var mTargetJarFile: File? = null
    private var mVersionName: String? = null

    fun start(
        assetManager: AssetManager?, version: JMinecraftVersionList.Version?,
        realVersion: String,
        listener: DoneListener
    ) {
        PojavApplication.sExecutorService.execute {
            try {
                downloadGame(assetManager, version, realVersion)
                listener.onDownloadDone(mClassPath!!.toTypedArray())
            } catch (e: JsonParseException) {
                listener.onDownloadFailed(e)
            } catch (e: RuntimeException) {
                throw e
            } catch (e: Exception) {
                listener.onDownloadFailed(e)
            }
            ProgressLayout.clearProgress(ProgressLayout.DOWNLOAD_MINECRAFT)
        }
    }

    @Throws(Exception::class)
    private fun downloadGame(
        assetManager: AssetManager?,
        verInfo: JMinecraftVersionList.Version?,
        versionName: String?
    ) {
        ProgressLayout.setProgress(ProgressLayout.DOWNLOAD_MINECRAFT, 0, R.string.newdl_starting)

        mTargetJarFile = createGameJarPath(versionName)
        mScheduledDownloadTasks = ArrayList()
        mDeclaredNatives = ArrayList()
        mAllLibraries = LinkedHashMap()

        if (sSubstitutionMapFuture == null && assetManager != null) {
            prepareSubstitutionMap(assetManager)
        }
        
        mSubstitutionMap = sSubstitutionMapFuture?.get() ?: SubstitutionMap()

        mVersionName = versionName

        downloadAndProcessMetadata(assetManager, verInfo, versionName)

        val downloadLibCount = mAllLibraries!!.size
        mClassPath = LinkedHashSet(downloadLibCount)
        growDownloadList(downloadLibCount)
        
        for (dependentLibrary in mAllLibraries!!.values) {
            if (dependentLibrary.name?.startsWith("net.java.dev.jna:jna:") == true && !dependentLibrary.replaced) {
                scheduleAarDownload(Tools.MAVEN_CENTRAL, dependentLibrary)
            }

            if (dependentLibrary.downloads != null) {
                processLibraryWithDownloads(dependentLibrary)
            } else {
                processRawLibrary(dependentLibrary)
            }
        }

        mAllLibraries!!.clear()
        mClassPath!!.add(mTargetJarFile!!)

        runDownloads(mScheduledDownloadTasks!!)

        ensureJarFileCopy()
        extractNatives(mVersionName)
    }

    private fun createGameJsonPath(versionId: String?): File {
        return File(Tools.DIR_HOME_VERSION, "$versionId${File.separator}$versionId.json")
    }

    private fun createGameJarPath(versionId: String?): File {
        return File(Tools.DIR_HOME_VERSION, "$versionId${File.separator}$versionId.jar")
    }

    @Throws(IOException::class)
    private fun ensureJarFileCopy() {
        if (mSourceJarFile == null) return
        if (mSourceJarFile == mTargetJarFile) return
        if (mTargetJarFile?.exists() == true) return
        ensureParentDirectory(mTargetJarFile!!)
        Log.i("NewMCDownloader", "Copying ${mSourceJarFile!!.name} to ${mTargetJarFile!!.absolutePath}")
        org.apache.commons.io.FileUtils.copyFile(mSourceJarFile, mTargetJarFile, false)
    }

    @Throws(IOException::class)
    private fun extractNatives(versionName: String?) {
        if (mDeclaredNatives!!.isEmpty()) return
        val totalCount = mDeclaredNatives!!.size

        ProgressLayout.setProgress(
            ProgressLayout.DOWNLOAD_MINECRAFT, 0,
            R.string.newdl_extracting_native_libraries, 0, totalCount
        )

        val targetDirectory = File(Tools.DIR_CACHE, "natives/$versionName")
        ensureDirectory(targetDirectory)
        val nativesExtractor = NativesExtractor(targetDirectory)
        var extractedCount = 0
        for (extractable in mDeclaredNatives!!) {
            if (extractable.extractInfo == null) {
                nativesExtractor.extractFromAar(extractable.path)
            } else {
                // nativesExtractor.extractMoJson(extractable.path, extractable.extractInfo)
            }
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
        if (verInfo.sha1 == null && targetFile.canRead() && targetFile.isFile) return targetFile
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

        if (Tools.isValidString(currentVerInfo.inheritsFrom)) {
            val inheritedVersion = AsyncMinecraftDownloader.getListedVersion(currentVerInfo.inheritsFrom)
            downloadAndProcessMetadata(assetManager, inheritedVersion, currentVerInfo.inheritsFrom)
        }

        val assets = downloadAssetsIndex(currentVerInfo)
        if (assets != null) scheduleAssetDownloads(assets)

        val minecraftClientInfo = getClientInfo(currentVerInfo)
        if (minecraftClientInfo != null) scheduleGameJarDownload(minecraftClientInfo, versionName)

        if (currentVerInfo.libraries != null) {
            scheduleLibraryDownloads(currentVerInfo.libraries!!.filterNotNull().toTypedArray())
        }

        if (currentVerInfo.logging != null) scheduleLoggingAssetDownloadIfNeeded(currentVerInfo.logging!!)
    }

    private fun growDownloadList(addedElementCount: Int) {
        mScheduledDownloadTasks!!.ensureCapacity(mScheduledDownloadTasks!!.size + addedElementCount)
    }

    @Throws(IOException::class)
    private fun scheduleDownload(
        targetFile: File, downloadClass: Int, url: String?, sha1: String?,
        size: Long
    ) {
        ensureParentDirectory(targetFile)
        var urlObject: URL? = null
        if (Tools.isValidString(url)) urlObject = URL(url)
        val taskMetadata = TaskMetadata(targetFile, urlObject, size, sha1, downloadClass)
        mScheduledDownloadTasks!!.add(taskMetadata)
    }

    @Throws(IOException::class)
    private fun scheduleAarDownload(baseRepository: String?, dependentLibrary: DependentLibrary) {
        val path = MavenNameUtils.mavenNameToAarPath(dependentLibrary.name!!)
        val downloadUrl = baseRepository + path
        val targetPath = File(Tools.DIR_HOME_LIBRARY, path)
        mDeclaredNatives!!.add(NativeLibraryExtractable(targetPath, null))
        scheduleDownload(targetPath, DownloadMirror.DOWNLOAD_CLASS_LIBRARIES, downloadUrl, null, -1)
    }

    @Throws(IOException::class)
    private fun submitBareLibrary(path: String, baseUrl: String) {
        val artifactPath = File(Tools.DIR_HOME_LIBRARY, path)
        if (!mClassPath!!.add(artifactPath)) {
            Log.w("MinecraftDownloader", "Repeated classpath entry $path skipped")
            return
        }
        scheduleDownload(
            artifactPath,
            DownloadMirror.DOWNLOAD_CLASS_LIBRARIES,
            baseUrl + path, null, -1
        )
    }

    @Throws(IOException::class)
    private fun submitArtifact(artifact: MinecraftLibraryArtifact): File? {
        val artifactPath = File(Tools.DIR_HOME_LIBRARY, artifact.path!!)
        if (!mClassPath!!.add(artifactPath)) {
            Log.w("MinecraftDownloader", "Repeated classpath entry ${artifact.path} skipped")
            return null
        }
        scheduleDownload(
            artifactPath,
            DownloadMirror.DOWNLOAD_CLASS_LIBRARIES,
            artifact.url, artifact.sha1, artifact.size
        )
        return artifactPath
    }

    private fun canIgnoreNatives(libName: String?): Boolean {
        return libName?.startsWith("com.mojang:text2speech") == true
    }

    @Throws(IOException::class)
    private fun processNatives(library: DependentLibrary) {
        val libraryClassifier = library.natives?.get(mNativeName)
        if (libraryClassifier == null) {
            val canIgnore = canIgnoreNatives(library.name)
            if (!canIgnore) throw IOException("library ${library.name} does not include native $mNativeName")
            Log.i("MinecraftDownloader", "Library ${library.name} doesn't have an $mNativeName natives-classifier (skipped)")
            return
        }

        val artifact = library.downloads?.classifiers?.get(libraryClassifier)
            ?: throw IOException("library ${library.name} is missing required classifier $libraryClassifier")

        val artifactPath = submitArtifact(artifact)
        if (library.extract != null && artifactPath != null) {
            mDeclaredNatives!!.add(NativeLibraryExtractable(artifactPath, library.extract))
        }
    }

    @Throws(IOException::class)
    private fun processLibraryWithDownloads(library: DependentLibrary) {
        val downloads = library.downloads ?: return
        if (downloads.artifact != null) submitArtifact(downloads.artifact!!)
        if (library.natives != null && downloads.classifiers != null) processNatives(library)
    }

    @Throws(IOException::class)
    private fun processRawLibrary(library: DependentLibrary) {
        val path = MavenNameUtils.mavenNameToPath(library.name!!)
        var baseUrl = library.url
        baseUrl = if (baseUrl != null) baseUrl.replace("http://", "https://")
        else "https://libraries.minecraft.net/"
        submitBareLibrary(path, baseUrl)
    }

    @Throws(IOException::class)
    private fun scheduleLibraryDownloads(dependentLibraries: Array<DependentLibrary>) {
        Tools.preProcessLibraries(dependentLibraries)
        for (dependentLibrary in dependentLibraries) {
            var lib = dependentLibrary
            if (lib.rules != null) {
                val ruleSetAction = MoJsonRule.ruleSetCheck(lib.rules)
                if (ruleSetAction != "allow") continue
            }

            val substitution = mSubstitutionMap!!.findSubstitution(lib.name!!)
            if (substitution != null) {
                if (substitution.skip) continue
                lib = substitution
            }

            val libraryTrimmedName = MavenNameUtils.mavenBaseName(lib.name!!)
            if (mAllLibraries!!.containsKey(libraryTrimmedName)) {
                mAllLibraries!!.remove(libraryTrimmedName)
            }
            mAllLibraries!!.put(libraryTrimmedName, lib)
        }
    }

    @Throws(IOException::class)
    private fun scheduleAssetDownloads(assets: JAssets) {
        val assetObjects = assets.objects ?: return
        val assetNames = assetObjects.keys
        growDownloadList(assetNames.size)
        for (asset in assetNames) {
            val assetInfo = assetObjects[asset] ?: continue
            val hashedPath = assetInfo.hash!!.substring(0, 2) + File.separator + assetInfo.hash
            val basePath = if (assets.mapToResources) Tools.OBSOLETE_RESOURCES_PATH else Tools.ASSETS_PATH
            val targetFile = if (assets.virtual || assets.mapToResources) {
                File(basePath, asset)
            } else {
                File(basePath, "objects" + File.separator + hashedPath)
            }
            scheduleDownload(
                targetFile,
                DownloadMirror.DOWNLOAD_CLASS_ASSETS,
                MINECRAFT_RES + hashedPath,
                assetInfo.hash,
                assetInfo.size
            )
        }
    }

    @Throws(IOException::class)
    private fun scheduleLoggingAssetDownloadIfNeeded(loggingConfig: JMinecraftVersionList.LoggingConfig) {
        val client = loggingConfig.client ?: return
        val loggingFileProperties = client.file ?: return
        val internalLoggingConfig = File(
            Tools.DIR_DATA + File.separator + "security",
            loggingFileProperties.id!!.replace("client", "log4j-rce-patch")
        )
        if (internalLoggingConfig.exists()) return
        val destination = File(Tools.DIR_GAME_NEW, loggingFileProperties.id!!)
        scheduleDownload(
            destination,
            DownloadMirror.DOWNLOAD_CLASS_LIBRARIES,
            loggingFileProperties.url,
            loggingFileProperties.sha1,
            loggingFileProperties.size
        )
    }

    @Throws(IOException::class)
    private fun scheduleGameJarDownload(minecraftClientInfo: MinecraftClientInfo, versionName: String?) {
        val clientJar = createGameJarPath(versionName)
        growDownloadList(1)
        scheduleDownload(
            clientJar,
            DownloadMirror.DOWNLOAD_CLASS_LIBRARIES,
            minecraftClientInfo.url,
            minecraftClientInfo.sha1,
            minecraftClientInfo.size
        )
        mSourceJarFile = clientJar
    }

    companion object {
        const val MINECRAFT_RES = "https://resources.download.minecraft.net/"
        private var sSubstitutionMapFuture: Future<SubstitutionMap>? = null

        @JvmStatic
        fun prepareSubstitutionMap(assetManager: AssetManager) {
            sSubstitutionMapFuture = PojavApplication.sExecutorService.submit<SubstitutionMap> {
                try {
                    assetManager.open("substitutions.json").use { stream ->
                        return@submit JSONUtils.readFromStream(stream, SubstitutionMap::class.java)
                    }
                } catch (e: IOException) {
                    return@submit SubstitutionMap()
                }
            }
        }
    }
}
