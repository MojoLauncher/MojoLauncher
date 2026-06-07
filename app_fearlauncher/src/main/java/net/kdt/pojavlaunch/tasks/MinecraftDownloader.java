package net.kdt.pojavlaunch.tasks;

import static net.kdt.pojavlaunch.PojavApplication.sExecutorService;

import android.content.res.AssetManager;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.gson.JsonParseException;
import com.kdt.mcgui.ProgressLayout;

import net.kdt.pojavlaunch.Architecture;
import net.kdt.pojavlaunch.JAssetInfo;
import net.kdt.pojavlaunch.JAssets;
import net.kdt.pojavlaunch.JMinecraftVersionList;
import net.kdt.pojavlaunch.NewJREUtil;
import git.artdeell.mojo.R;

import net.kdt.pojavlaunch.Tools;
import net.kdt.pojavlaunch.downloader.Downloader;
import net.kdt.pojavlaunch.downloader.TaskMetadata;
import net.kdt.pojavlaunch.mirrors.DownloadMirror;
import net.kdt.pojavlaunch.mirrors.MirrorTamperedException;
import net.kdt.pojavlaunch.prefs.LauncherPreferences;
import net.kdt.pojavlaunch.utils.DownloadUtils;
import net.kdt.pojavlaunch.utils.FileUtils;
import net.kdt.pojavlaunch.utils.JSONUtils;
import net.kdt.pojavlaunch.utils.MavenNameUtils;
import net.kdt.pojavlaunch.utils.jre.RuntimeSelectionException;
import net.kdt.pojavlaunch.value.DependentLibrary;
import net.kdt.pojavlaunch.value.LibrarySubstitution;
import net.kdt.pojavlaunch.value.MinecraftClientInfo;
import net.kdt.pojavlaunch.value.MinecraftLibraryArtifact;
import net.kdt.pojavlaunch.value.MoJsonRule;
import net.kdt.pojavlaunch.value.NativeLibraryExtractable;
import net.kdt.pojavlaunch.value.SubstitutionMap;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Future;

public class MinecraftDownloader extends Downloader {

    public static final String MINECRAFT_RES = "https://resources.download.minecraft.net/";

    private final String mNativeName = "android-"+Architecture.archAsString(Architecture.getDeviceArchitecture());

    private static Future<SubstitutionMap> sSubstitutionMapFuture;

    private ArrayList<TaskMetadata> mScheduledDownloadTasks;
    private ArrayList<NativeLibraryExtractable> mDeclaredNatives;
    private LinkedHashMap<String, DependentLibrary> mAllLibraries;
    private LinkedHashSet<File> mClassPath;
    private SubstitutionMap mSubstitutionMap;

    private File mSourceJarFile; // The source client JAR picked during the inheritance process
    private File mTargetJarFile; // The destination client JAR to which the source will be copied to.
    private String mVersionName;

    public MinecraftDownloader() {
        super(ProgressLayout.DOWNLOAD_MINECRAFT);
    }

    public static void prepareSubstitutionMap(AssetManager assetManager) {
        sSubstitutionMapFuture = sExecutorService.submit(()->{
            try (InputStream stream = assetManager.open("substitutions.json")) {
                return JSONUtils.readFromStream(stream, SubstitutionMap.class);
            }
        });
    }

    /**
     * Start the game version download process on the global executor service.
     * @param assetManager AssetManager, used for automatic installation of JRE 17 if needed
     * @param version The JMinecraftVersionList.Version from the version list, if available
     * @param realVersion The version ID (necessary)
     * @param listener The download status listener
     */
    public void start(@Nullable AssetManager assetManager, @Nullable JMinecraftVersionList.Version version,
                      @NonNull String realVersion, // this was there for a reason
                      @NonNull AsyncMinecraftDownloader.DoneListener listener) {
        sExecutorService.execute(() -> {
            try {
                downloadGame(assetManager, version, realVersion);
                listener.onDownloadDone(mClassPath.toArray(new File[0]));
            } catch(JsonParseException e) {
                listener.onDownloadFailed(e); // Handled separately from the general case because it subclasses RuntimeException. Ugh.
            } catch(RuntimeException e) {
                throw e; // log fatal errors to Google Play
            } catch (Exception e) {
                listener.onDownloadFailed(e);
            }
            ProgressLayout.clearProgress(ProgressLayout.DOWNLOAD_MINECRAFT);
        });
    }

    /**
     * Download the game version.
     * @param assetManager AssetManager, used for automatic installation of JRE 17 if needed
     * @param verInfo The JMinecraftVersionList.Version from the version list, if available
     * @param versionName The version ID (necessary)
     * @throws Exception when an exception occurs in the function body or in any of the downloading threads.
     */
    private void downloadGame(AssetManager assetManager, JMinecraftVersionList.Version verInfo, String versionName) throws Exception {
        // Put up a dummy progress line, for the activity to start the service and do all the other necessary
        // work to keep the launcher alive. We will replace this line when we will start downloading stuff.
        ProgressLayout.setProgress(ProgressLayout.DOWNLOAD_MINECRAFT, 0, R.string.newdl_starting);

        mTargetJarFile = createGameJarPath(versionName);
        mScheduledDownloadTasks = new ArrayList<>();
        mDeclaredNatives = new ArrayList<>();
        mAllLibraries = new LinkedHashMap<>();

        if(sSubstitutionMapFuture == null) throw new RuntimeException("SubstitutionMap not prepared");
        mSubstitutionMap = sSubstitutionMapFuture.get();

        mVersionName = versionName;

        downloadAndProcessMetadata(assetManager, verInfo, versionName);

        int downloadLibCount = mAllLibraries.size();
        mClassPath = new LinkedHashSet<>(downloadLibCount);
        growDownloadList(downloadLibCount);
        for(DependentLibrary dependentLibrary : mAllLibraries.values()) {
            // Special handling for JNA Android natives
            if(dependentLibrary.name.startsWith("net.java.dev.jna:jna:") && !dependentLibrary.replaced) {
                scheduleAarDownload(Tools.MAVEN_CENTRAL, dependentLibrary);
            }

            if(dependentLibrary.downloads != null) processLibraryWithDownloads(dependentLibrary);
            else processRawLibrary(dependentLibrary);
        }

        mAllLibraries.clear();
        mClassPath.add(mTargetJarFile);

        runDownloads(mScheduledDownloadTasks);

        ensureJarFileCopy();
        extractNatives(mVersionName);
    }

    private File createGameJsonPath(String versionId) {
        return new File(Tools.DIR_HOME_VERSION, versionId + File.separator + versionId + ".json");
    }

    private File createGameJarPath(String versionId) {
        return new File(Tools.DIR_HOME_VERSION, versionId + File.separator + versionId + ".jar");
    }

    /**
     * Ensure that there is a copy of the client JAR file in the version folder, if a copy is
     * needed.
     * @throws IOException if the copy fails
     */
    private void ensureJarFileCopy() throws IOException {
        if(mSourceJarFile == null) return;
        if(mSourceJarFile.equals(mTargetJarFile)) return;
        if(mTargetJarFile.exists()) return;
        FileUtils.ensureParentDirectory(mTargetJarFile);
        Log.i("NewMCDownloader", "Copying " + mSourceJarFile.getName() + " to "+mTargetJarFile.getAbsolutePath());
        org.apache.commons.io.FileUtils.copyFile(mSourceJarFile, mTargetJarFile, false);
    }

    private void extractNatives(String versionName) throws IOException {
        if(mDeclaredNatives.isEmpty()) return;
        int totalCount = mDeclaredNatives.size();

        ProgressLayout.setProgress(ProgressLayout.DOWNLOAD_MINECRAFT, 0,
                R.string.newdl_extracting_native_libraries, 0, totalCount);

        File targetDirectory = new File(Tools.DIR_CACHE, "natives/"+versionName);
        FileUtils.ensureDirectory(targetDirectory);
        NativesExtractor nativesExtractor = new NativesExtractor(targetDirectory);
        int extractedCount = 0;
        for(NativeLibraryExtractable extractable : mDeclaredNatives) {
            if(extractable.extractInfo == null) nativesExtractor.extractFromAar(extractable.path);
            else nativesExtractor.extractMoJson(extractable.path, extractable.extractInfo);
            extractedCount++;
            ProgressLayout.setProgress(ProgressLayout.DOWNLOAD_MINECRAFT, extractedCount * 100 / totalCount,
                    R.string.newdl_extracting_native_libraries, extractedCount, totalCount);
        }
    }

    private File downloadGameJson(JMinecraftVersionList.Version verInfo) throws IOException, MirrorTamperedException {
        File targetFile = createGameJsonPath(verInfo.id);
        if(verInfo.sha1 == null && targetFile.canRead() && targetFile.isFile())
            return targetFile;
        FileUtils.ensureParentDirectory(targetFile);
        try {
            DownloadUtils.ensureSha1(targetFile, LauncherPreferences.PREF_VERIFY_MANIFEST ? verInfo.sha1 : null, () -> {
                ProgressLayout.setProgress(ProgressLayout.DOWNLOAD_MINECRAFT, 0,
                        R.string.newdl_downloading_metadata, targetFile.getName());
                DownloadMirror.downloadFileMirrored(DownloadMirror.DOWNLOAD_CLASS_METADATA, verInfo.url, targetFile);
                return null;
            });
        }catch (DownloadUtils.SHA1VerificationException e) {
            if(DownloadMirror.isMirrored()) throw new MirrorTamperedException();
            else throw e;
        }
        return targetFile;
    }

    private JAssets downloadAssetsIndex(JMinecraftVersionList.Version verInfo) throws IOException{
        JMinecraftVersionList.AssetIndex assetIndex = verInfo.assetIndex;
        if(assetIndex == null || verInfo.assets == null) return null;
        File targetFile = new File(Tools.ASSETS_PATH, "indexes"+ File.separator + verInfo.assets + ".json");
        FileUtils.ensureParentDirectory(targetFile);
        DownloadUtils.ensureSha1(targetFile, assetIndex.sha1, ()-> {
            ProgressLayout.setProgress(ProgressLayout.DOWNLOAD_MINECRAFT, 0,
                    R.string.newdl_downloading_metadata, targetFile.getName());
            DownloadMirror.downloadFileMirrored(DownloadMirror.DOWNLOAD_CLASS_METADATA, assetIndex.url, targetFile);
            return null;
        });
        return Tools.GLOBAL_GSON.fromJson(Tools.read(targetFile), JAssets.class);
    }
    
    private MinecraftClientInfo getClientInfo(JMinecraftVersionList.Version verInfo) {
        Map<String, MinecraftClientInfo> downloads = verInfo.downloads;
        if(downloads == null) return null;
        return downloads.get("client");
    }

    /**
     * Download (if necessary) and process a version's metadata, scheduling all downloads that this
     * version needs.
     * @param assetManager AssetManager, used for automatic installation of JRE 17 if needed
     * @param verInfo The JMinecraftVersionList.Version from the version list, if available
     * @param versionName The version ID (necessary)
     * @throws IOException if the download of any of the metadata files fails
     */
    private void downloadAndProcessMetadata(AssetManager assetManager, JMinecraftVersionList.Version verInfo, String versionName) throws IOException, MirrorTamperedException, RuntimeSelectionException, JsonParseException {
        File versionJsonFile;
        if(verInfo != null) versionJsonFile = downloadGameJson(verInfo);
        else versionJsonFile = createGameJsonPath(versionName);
        if(versionJsonFile.canRead()) {
            verInfo = JSONUtils.readFromFile(versionJsonFile, JMinecraftVersionList.Version.class);
            if(verInfo == null) throw new IOException("Deserialized json is null. Contact developer.");
        } else {
            throw new IOException("Unable to read Version JSON for version " + versionName);
        }

        if(assetManager != null)
            NewJREUtil.installNewJreIfNeeded(assetManager, verInfo);

        if(Tools.isValidString(verInfo.inheritsFrom)) {
            JMinecraftVersionList.Version inheritedVersion = AsyncMinecraftDownloader.getListedVersion(verInfo.inheritsFrom);
            // Infinite inheritance !?! :noway:
            downloadAndProcessMetadata(assetManager, inheritedVersion, verInfo.inheritsFrom);
        }

        JAssets assets = downloadAssetsIndex(verInfo);
        if(assets != null) scheduleAssetDownloads(assets);

        MinecraftClientInfo minecraftClientInfo = getClientInfo(verInfo);
        if(minecraftClientInfo != null) scheduleGameJarDownload(minecraftClientInfo, versionName);

        if(verInfo.libraries != null) scheduleLibraryDownloads(verInfo.libraries);

        if(verInfo.logging != null) scheduleLoggingAssetDownloadIfNeeded(verInfo.logging);
    }

    private void growDownloadList(int addedElementCount) {
        mScheduledDownloadTasks.ensureCapacity(mScheduledDownloadTasks.size() + addedElementCount);
    }

    private void scheduleDownload(File targetFile, int downloadClass, String url, String sha1,
                                  long size) throws IOException {
        FileUtils.ensureParentDirectory(targetFile);
        if(!Tools.isValidString(sha1)) sha1 = null;
        URL urlObject = null;
        if(Tools.isValidString(url)) urlObject = new URL(url);
        TaskMetadata taskMetadata = new TaskMetadata(targetFile, urlObject, size, sha1, downloadClass);
        mScheduledDownloadTasks.add(taskMetadata);
    }

    /**
     * Schedule the download of an AAR library containing the required natives, for later extraction
     * and adding to the library path.
     * @param baseRepository the source Maven repository to download from.
     * @param dependentLibrary the DependentLibrary to get the path from
     * @throws IOException in case if download scheduling fails.
     */
    private void scheduleAarDownload(String baseRepository, DependentLibrary dependentLibrary) throws IOException {
        String path = MavenNameUtils.mavenNameToAarPath(dependentLibrary.name);
        String downloadUrl = baseRepository + path;
        File targetPath = new File(Tools.DIR_HOME_LIBRARY, path);
        mDeclaredNatives.add(new NativeLibraryExtractable(targetPath, null));
        scheduleDownload(targetPath, DownloadMirror.DOWNLOAD_CLASS_LIBRARIES, downloadUrl, null, -1);
    }

    private void submitBareLibrary(String path, String baseUrl) throws IOException {
        File artifactPath = new File(Tools.DIR_HOME_LIBRARY, path);
        if(!mClassPath.add(artifactPath)) {
            Log.w("MinecraftDownloader", "Repeated classpath entry "+ path +" skipped");
            return;
        }
        scheduleDownload(artifactPath,
                DownloadMirror.DOWNLOAD_CLASS_LIBRARIES,
                baseUrl + path, null, -1
        );
    }

    private File submitArtifact(MinecraftLibraryArtifact artifact, String subPath) throws IOException {
        File artifactPath = new File(Tools.DIR_HOME_LIBRARY, subPath);
        if(!mClassPath.add(artifactPath)) {
            Log.w("MinecraftDownloader", "Repeated classpath entry " + artifact.path +" skipped");
            return null;
        }
        scheduleDownload(artifactPath,
                DownloadMirror.DOWNLOAD_CLASS_LIBRARIES,
                artifact.url, artifact.sha1, artifact.size
        );

        return artifactPath;
    }

    private static boolean canIgnoreNatives(String libName) {
        return libName.startsWith("com.mojang:text2speech");
    }

    private void processNatives(DependentLibrary library) throws IOException {
        String libraryClassifier = library.natives.get(mNativeName);
        if(libraryClassifier == null) {
            boolean canIgnore = canIgnoreNatives(library.name);
            if(!canIgnore) throw new IOException("library "+library.name +" does not include native "+mNativeName);
            Log.i("MinecraftDownloader", "Library "+library.name + " doesn't have an "+mNativeName+" natives-classifier (skipped)");
            return;
        }

        MinecraftLibraryArtifact artifact = library.downloads.classifiers.get(libraryClassifier);
        if(artifact == null) throw new IOException("library "+library.name +" is missing required classifier "+ libraryClassifier);

        String subPath = artifact.path;
        if(subPath == null) subPath = MavenNameUtils.mavenNameToPath(library.name, libraryClassifier);

        File artifactPath = submitArtifact(artifact, subPath);
        if(library.extract != null && artifactPath != null) {
            mDeclaredNatives.add(new NativeLibraryExtractable(artifactPath, library.extract));
        }
    }

    private void processLibraryWithDownloads(DependentLibrary library) throws IOException {
        DependentLibrary.LibraryDownloads downloads = library.downloads;
        if(downloads.artifact != null) {

            String subPath = downloads.artifact.path;
            if(subPath == null) subPath = MavenNameUtils.mavenNameToPath(library.name);

            submitArtifact(downloads.artifact, subPath);
        }
        if(library.natives != null && downloads.classifiers != null) processNatives(library);
    }

    private void processRawLibrary(DependentLibrary library) throws IOException{
        String path = MavenNameUtils.mavenNameToPath(library.name);
        String baseUrl = library.url;
        if(baseUrl != null) baseUrl = baseUrl.replace("http://","https://");
        else baseUrl = "https://libraries.minecraft.net/";
        submitBareLibrary(path, baseUrl);
    }

    private void scheduleLibraryDownloads(DependentLibrary[] dependentLibraries) throws IOException {
        Tools.preProcessLibraries(dependentLibraries);
        for(DependentLibrary dependentLibrary : dependentLibraries) {
            if(dependentLibrary.rules != null) {
                String ruleSetAction = MoJsonRule.ruleSetCheck(dependentLibrary.rules);
                if(!ruleSetAction.equals("allow")) continue;
            }

            LibrarySubstitution substitution = mSubstitutionMap.findSubstitution(dependentLibrary.name);
            if(substitution != null) {
                if(substitution.skip) continue;
                dependentLibrary = substitution;
            }

            String libraryTrimmedName = MavenNameUtils.mavenBaseName(dependentLibrary.name);
            // Move the more recent library to the front of the list
            if (mAllLibraries.containsKey(libraryTrimmedName)) {
                mAllLibraries.remove(libraryTrimmedName);
            }
            mAllLibraries.put(libraryTrimmedName, dependentLibrary);
        }
    }
    
    private void scheduleAssetDownloads(JAssets assets) throws IOException {
        Map<String, JAssetInfo> assetObjects = assets.objects;
        if(assetObjects == null) return;
        Set<String> assetNames = assetObjects.keySet();
        growDownloadList(assetNames.size());
        for(String asset : assetNames) {
            JAssetInfo assetInfo = assetObjects.get(asset);
            if(assetInfo == null) continue;
            File targetFile;
            String hashedPath = assetInfo.hash.substring(0, 2) + File.separator + assetInfo.hash;
            String basePath = assets.mapToResources ? Tools.OBSOLETE_RESOURCES_PATH : Tools.ASSETS_PATH;
            if(assets.virtual || assets.mapToResources) {
                targetFile = new File(basePath, asset);
            } else {
                targetFile = new File(basePath, "objects" + File.separator + hashedPath);
            }
            scheduleDownload(targetFile,
                    DownloadMirror.DOWNLOAD_CLASS_ASSETS,
                    MINECRAFT_RES + hashedPath,
                    assetInfo.hash,
                    assetInfo.size);
        }
    }

    private void scheduleLoggingAssetDownloadIfNeeded(JMinecraftVersionList.LoggingConfig loggingConfig) throws IOException {
        if(loggingConfig.client == null || loggingConfig.client.file == null) return;
        JMinecraftVersionList.FileProperties loggingFileProperties = loggingConfig.client.file;
        File internalLoggingConfig = new File(Tools.DIR_DATA + File.separator + "security",
                loggingFileProperties.id.replace("client", "log4j-rce-patch"));
        if(internalLoggingConfig.exists()) return;
        File destination = new File(Tools.DIR_GAME_NEW, loggingFileProperties.id);
        scheduleDownload(destination,
                DownloadMirror.DOWNLOAD_CLASS_LIBRARIES,
                loggingFileProperties.url,
                loggingFileProperties.sha1,
                loggingFileProperties.size
        );
    }

    private void scheduleGameJarDownload(MinecraftClientInfo minecraftClientInfo, String versionName) throws IOException {
        File clientJar = createGameJarPath(versionName);
        growDownloadList(1);
        scheduleDownload(clientJar,
                DownloadMirror.DOWNLOAD_CLASS_LIBRARIES,
                minecraftClientInfo.url,
                minecraftClientInfo.sha1,
                minecraftClientInfo.size
        );
        // Store the path of the JAR to copy it into our new version folder later.
        mSourceJarFile = clientJar;
    }
}
