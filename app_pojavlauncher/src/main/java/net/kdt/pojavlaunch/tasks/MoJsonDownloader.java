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
import net.kdt.pojavlaunch.JVersionList;
import net.kdt.pojavlaunch.NewJREUtil;
import git.artdeell.mojo.R;

import net.kdt.pojavlaunch.Tools;
import net.kdt.pojavlaunch.downloader.Downloader;
import net.kdt.pojavlaunch.downloader.TaskMetadata;
import net.kdt.pojavlaunch.downloader.VerificationException;
import net.kdt.pojavlaunch.mirrors.DownloadMirror;
import net.kdt.pojavlaunch.mirrors.MirrorTamperedException;
import net.kdt.pojavlaunch.prefs.LauncherPreferences;
import net.kdt.pojavlaunch.utils.DownloadUtils;
import net.kdt.pojavlaunch.utils.FileUtils;
import net.kdt.pojavlaunch.utils.JSONUtils;
import net.kdt.pojavlaunch.utils.PresentableException;
import net.kdt.pojavlaunch.utils.jre.RuntimeSelectionException;
import net.kdt.pojavlaunch.utils.maven.MavenName;
import net.kdt.pojavlaunch.value.DependentLibrary;
import net.kdt.pojavlaunch.value.LibrarySubstitution;
import net.kdt.pojavlaunch.value.ClientInfo;
import net.kdt.pojavlaunch.value.LibraryArtifact;
import net.kdt.pojavlaunch.value.MoJsonRule;
import net.kdt.pojavlaunch.value.NativeLibraryExtractable;
import net.kdt.pojavlaunch.value.SubstitutionMap;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Future;

public class MoJsonDownloader extends Downloader {

    public static final String MC_RES = "https://resources.download.minecraft.net/";

    private final String mNativeName = "android-"+Architecture.archAsString(Architecture.getDeviceArchitecture());

    private static Future<SubstitutionMap> sSubstitutionMapFuture;

    private ArrayList<TaskMetadata> mScheduledDownloadTasks;
    private ArrayList<NativeLibraryExtractable> mDeclaredNatives;
    private LinkedHashSet<DependentLibrary> mAllLibraries;
    private LinkedHashSet<File> mClassPath;
    private SubstitutionMap mSubstitutionMap;
    private File mSourceJarFile; // The source client JAR picked during the inheritance process
    private File mTargetJarFile; // The destination client JAR to which the source will be copied to.

    public MoJsonDownloader() {
        super(ProgressLayout.DOWNLOAD_GAME);
    }

    public static void prepareSubstitutionMap(AssetManager assetManager) {
        sSubstitutionMapFuture = sExecutorService.submit(()->{
            try (InputStream stream = assetManager.open("substitutions.json")) {
                return JSONUtils.readFromStream(stream, SubstitutionMap.class).prepare();
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
    public void start(@Nullable AssetManager assetManager, @Nullable JVersionList.Version version,
                      @NonNull String realVersion, // this was there for a reason
                      @NonNull MoJsonExtras.DoneListener listener) {
        sExecutorService.execute(() -> {
            try {
                downloadGame(assetManager, version, realVersion);
                listener.onDownloadDone(mClassPath.toArray(new File[0]));
            } catch(JsonParseException e) {
                listener.onDownloadFailed(e); // Handled separately from the general case because it subclasses RuntimeException. Ugh.
            } catch(RuntimeException e) {
                throw e; // log fatal errors to Google Play
            } catch (InterruptedException ignored) {
            } catch (Exception e) {
                listener.onDownloadFailed(e);
            }
            ProgressLayout.clearProgress(ProgressLayout.DOWNLOAD_GAME);
        });
    }

    /**
     * Download the game version.
     * @param assetManager AssetManager, used for automatic installation of JRE 17 if needed
     * @param verInfo The JMinecraftVersionList.Version from the version list, if available
     * @param versionName The version ID (necessary)
     */
    private void downloadGame(AssetManager assetManager, JVersionList.Version verInfo, String versionName)
            throws PresentableException, RuntimeSelectionException, MirrorTamperedException, InterruptedException {
        // Put up a dummy progress line, for the activity to start the service and do all the other necessary
        // work to keep the launcher alive. We will replace this line when we will start downloading stuff.
        ProgressLayout.setProgress(ProgressLayout.DOWNLOAD_GAME, 0, R.string.newdl_starting);

        mTargetJarFile = createGameJarPath(versionName);
        mScheduledDownloadTasks = new ArrayList<>();
        mDeclaredNatives = new ArrayList<>();
        mAllLibraries = new LinkedHashSet<>();

        try {
            mSubstitutionMap = sSubstitutionMapFuture.get();
        } catch (Exception e) {
            throw new PresentableException(e, R.string.mjdl_submap_error_title, R.string.mjdl_submap_error_subtitle);
        }

        try {
            downloadAndProcessMetadata(assetManager, verInfo, versionName);

            int downloadLibCount = mAllLibraries.size();
            mClassPath = new LinkedHashSet<>(downloadLibCount);
            growDownloadList(downloadLibCount);

            prepareLibraryDownloads(downloadLibCount);

            mAllLibraries.clear();
            mClassPath.add(mTargetJarFile);

            runDownloads(mScheduledDownloadTasks);

        } catch (VerificationException e) {
            throw new PresentableException(R.string.mjdl_download_error_title, R.string.mjdl_download_error_subtitle_2);
        } catch (IOException e) {
            throw new PresentableException(R.string.mjdl_download_error_title, R.string.mjdl_download_error_subtitle);
        }

        try {
            ensureJarFileCopy();
        } catch (IOException e) {
            throw new PresentableException(e, R.string.mjdl_disk_error_title, R.string.mjdl_disk_error_subtitle_3);
        }

        try {
            extractNatives(versionName);
        } catch (Exception e) {
            throw new PresentableException(e, R.string.mjdl_native_error_title, R.string.mjdl_native_error_subtitle_2);
        }
    }

    public static File createGameJsonPath(String versionId) {
        return new File(Tools.DIR_HOME_VERSION, versionId + File.separator + versionId + ".json");
    }

    public static File createGameJarPath(String versionId) {
        return new File(Tools.DIR_HOME_VERSION, versionId + File.separator + versionId + ".jar");
    }

    /**
     * Ensure that there is a copy of the client JAR file in the version folder, if a copy is
     * needed.
     * @throws IOException if the copy fails
     */
    private void ensureJarFileCopy() throws IOException, PresentableException {
        if (mSourceJarFile == null) return;
        if (mSourceJarFile.equals(mTargetJarFile)) return;
        if (mTargetJarFile.exists()) return;
        checkedCreateDirectory(mTargetJarFile);

        Log.i("NewMCDownloader", "Copying " + mSourceJarFile.getName() + " to " + mTargetJarFile.getAbsolutePath());
        org.apache.commons.io.FileUtils.copyFile(mSourceJarFile, mTargetJarFile, false);
    }

    private void prepareLibraryDownloads(int downloadLibCount) throws PresentableException {
        HashSet<MavenName> processedLibraries = new HashSet<>(downloadLibCount);

        for(DependentLibrary dependentLibrary : mAllLibraries) {
            MavenName name = dependentLibrary.name;
            if(dependentLibrary.rules != null) {
                String ruleSetAction = MoJsonRule.ruleSetCheck(dependentLibrary.rules);
                if(!ruleSetAction.equals("allow")) {
                    // Mark version-wildcard libraries as processed to allow replacing libraries using wildcard
                    if(name.isAnyVersion()) processedLibraries.add(name);
                    continue;
                }
            }

            LibrarySubstitution substitution = mSubstitutionMap.findSubstitution(dependentLibrary.name);
            if(substitution != null) {
                if(substitution.skip) continue;
                dependentLibrary = substitution;
            }

            Tools.preProcessLibrary(dependentLibrary);

            if(!processedLibraries.add(name.anyVersion())) {
                continue;
            }

            if(name.module.equals("jna") && name.provider.equals("net.java.dev.jna") && !dependentLibrary.replaced) {
                // Special handling for JNA Android natives
                scheduleAarDownload(Tools.MAVEN_CENTRAL, dependentLibrary);
            }

            if(name.module.equals("asm") && name.provider.equals("org.ow2.asm")) {
                // Refuse to process asm-all when modern, modularized asm is present
                processedLibraries.add(new MavenName("org.ow2.asm", "asm-all"));
            }

            if(dependentLibrary.downloads != null) processLibraryWithDownloads(dependentLibrary);
            else processRawLibrary(dependentLibrary);
        }
    }

    private void extractNatives(String versionName) throws IOException {
        if(mDeclaredNatives.isEmpty()) return;
        int totalCount = mDeclaredNatives.size();

        ProgressLayout.setProgress(ProgressLayout.DOWNLOAD_GAME, 0,
                R.string.newdl_extracting_native_libraries, 0, totalCount);

        File targetDirectory = new File(Tools.DIR_CACHE, "natives/"+versionName);
        FileUtils.ensureDirectory(targetDirectory);
        NativesExtractor nativesExtractor = new NativesExtractor(targetDirectory);
        int extractedCount = 0;
        for(NativeLibraryExtractable extractable : mDeclaredNatives) {
            if(extractable.extractInfo == null) nativesExtractor.extractFromAar(extractable.path);
            else nativesExtractor.extractMoJson(extractable.path, extractable.extractInfo);
            extractedCount++;
            ProgressLayout.setProgress(ProgressLayout.DOWNLOAD_GAME, extractedCount * 100 / totalCount,
                    R.string.newdl_extracting_native_libraries, extractedCount, totalCount);
        }
    }

    private File downloadGameJson(JVersionList.Version verInfo) throws IOException, PresentableException, MirrorTamperedException {
        File targetFile = createGameJsonPath(verInfo.id);
        if(verInfo.sha1 == null && targetFile.canRead() && targetFile.isFile())
            return targetFile;
        checkedCreateDirectory(targetFile);
        try {
            DownloadUtils.ensureSha1(targetFile, LauncherPreferences.PREF_VERIFY_MANIFEST ? verInfo.sha1 : null, () -> {
                ProgressLayout.setProgress(ProgressLayout.DOWNLOAD_GAME, 0,
                        R.string.newdl_downloading_metadata, targetFile.getName());
                DownloadMirror.downloadFileMirrored(DownloadMirror.DOWNLOAD_CLASS_METADATA, verInfo.url, targetFile);
            });
        }catch (VerificationException e) {
            if(DownloadMirror.isMirrored()) throw new MirrorTamperedException();
            else throw e;
        }
        return targetFile;
    }

    private JAssets downloadAssetsIndex(JVersionList.Version verInfo) throws IOException, PresentableException {
        JVersionList.AssetIndex assetIndex = verInfo.assetIndex;
        if(assetIndex == null || verInfo.assets == null) return null;
        File targetFile = new File(Tools.ASSETS_PATH, "indexes"+ File.separator + verInfo.assets + ".json");
        checkedCreateDirectory(targetFile);
        DownloadUtils.ensureSha1(targetFile, assetIndex.sha1, ()-> {
            ProgressLayout.setProgress(ProgressLayout.DOWNLOAD_GAME, 0,
                    R.string.newdl_downloading_metadata, targetFile.getName());
            DownloadMirror.downloadFileMirrored(DownloadMirror.DOWNLOAD_CLASS_METADATA, assetIndex.url, targetFile);
        });
        return checkedParseJson(targetFile, JAssets.class);
    }
    
    private ClientInfo getClientInfo(JVersionList.Version verInfo) {
        Map<String, ClientInfo> downloads = verInfo.downloads;
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
    private void downloadAndProcessMetadata(AssetManager assetManager, JVersionList.Version verInfo, String versionName)
            throws PresentableException, MirrorTamperedException, RuntimeSelectionException, IOException {
        File versionJsonFile;
        if(verInfo != null) versionJsonFile = downloadGameJson(verInfo);
        else versionJsonFile = createGameJsonPath(versionName);

        if(!versionJsonFile.canRead()) metadataAccessException();
        verInfo = checkedParseJson(versionJsonFile, JVersionList.Version.class);

        try {
            if (assetManager != null)
                NewJREUtil.installNewJreIfNeeded(assetManager, verInfo);
        }catch (IOException e) {
            throw new PresentableException(R.string.mjdl_disk_error_title, R.string.mjdl_disk_error_subtitle_2);
        }

        JAssets assets = downloadAssetsIndex(verInfo);
        if(assets != null) scheduleAssetDownloads(assets);

        ClientInfo clientInfo = getClientInfo(verInfo);
        if(clientInfo != null) scheduleGameJarDownload(clientInfo, versionName);

        if(verInfo.libraries != null) {
            mAllLibraries.addAll(Arrays.asList(verInfo.libraries));
        }

        if(verInfo.logging != null) scheduleLoggingAssetDownloadIfNeeded(verInfo.logging);

        if(Tools.isValidString(verInfo.inheritsFrom)) {
            JVersionList.Version inheritedVersion = MoJsonExtras.getListedVersion(verInfo.inheritsFrom);
            // Infinite inheritance !?! :noway:
            downloadAndProcessMetadata(assetManager, inheritedVersion, verInfo.inheritsFrom);
        }
    }

    private void growDownloadList(int addedElementCount) {
        mScheduledDownloadTasks.ensureCapacity(mScheduledDownloadTasks.size() + addedElementCount);
    }

    private void scheduleDownload(File targetFile, int downloadClass, String url, String sha1,
                                  long size) throws PresentableException {


        if(!Tools.isValidString(sha1)) sha1 = null;
        URL urlObject = null;
        try {
            if(Tools.isValidString(url)) urlObject = new URL(url);
        } catch (MalformedURLException e) {
            genericSyntaxException(url + " is not a valid URL");
        }

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
    private void scheduleAarDownload(String baseRepository, DependentLibrary dependentLibrary) throws PresentableException {
        String path = dependentLibrary.name.toPath(null, ".aar");
        String downloadUrl = baseRepository + path;
        File targetPath = new File(Tools.DIR_HOME_LIBRARY, path);
        mDeclaredNatives.add(new NativeLibraryExtractable(targetPath, null));
        scheduleDownload(targetPath, DownloadMirror.DOWNLOAD_CLASS_LIBRARIES, downloadUrl, null, -1);
    }

    private void submitBareLibrary(String path, String baseUrl) throws PresentableException {
        File artifactPath = new File(Tools.DIR_HOME_LIBRARY, path);
        if(!mClassPath.add(artifactPath)) {
            Log.w("MoJsonDownloader", "Repeated classpath entry "+ path +" skipped");
            return;
        }
        scheduleDownload(artifactPath,
                DownloadMirror.DOWNLOAD_CLASS_LIBRARIES,
                baseUrl + path, null, -1
        );
    }

    private File submitArtifact(LibraryArtifact artifact, String subPath) throws PresentableException {
        File artifactPath = new File(Tools.DIR_HOME_LIBRARY, subPath);
        if(!mClassPath.add(artifactPath)) {
            Log.w("MoJsonDownloader", "Repeated classpath entry " + artifact.path +" skipped");
            return null;
        }
        scheduleDownload(artifactPath,
                DownloadMirror.DOWNLOAD_CLASS_LIBRARIES,
                artifact.url, artifact.sha1, artifact.size
        );

        return artifactPath;
    }

    private static boolean canIgnoreNatives(MavenName name) {
        return name.provider.equals("com.mojang") && name.module.equals("text2speech");
    }

    private void processNatives(DependentLibrary library) throws PresentableException {
        String libraryClassifier = library.natives.get(mNativeName);
        if(libraryClassifier == null) {
            boolean canIgnore = canIgnoreNatives(library.name);
            if(!canIgnore)
                throw new PresentableException(R.string.mjdl_native_error_title, R.string.mjdl_native_error_subtitle_1, library.name.toString(), mNativeName);
            return;
        }

        LibraryArtifact artifact = library.downloads.classifiers.get(libraryClassifier);
        if(artifact == null)
            genericSyntaxException("library "+library.name +" is missing required classifier "+ libraryClassifier);

        String subPath = artifact.path;
        if(subPath == null) subPath = library.name.toPath(libraryClassifier);

        File artifactPath = submitArtifact(artifact, subPath);
        if(library.extract != null && artifactPath != null) {
            mDeclaredNatives.add(new NativeLibraryExtractable(artifactPath, library.extract));
        }
    }

    private void processLibraryWithDownloads(DependentLibrary library) throws PresentableException {
        DependentLibrary.LibraryDownloads downloads = library.downloads;
        if(downloads.artifact != null) {

            String subPath = downloads.artifact.path;
            if(subPath == null) subPath = library.name.toPath();

            submitArtifact(downloads.artifact, subPath);
        }
        if(library.natives != null && downloads.classifiers != null) processNatives(library);
    }

    private void processRawLibrary(DependentLibrary library) throws PresentableException {
        String path = library.name.toPath();
        String baseUrl = library.url;
        if(baseUrl != null) baseUrl = baseUrl.replace("http://","https://");
        else baseUrl = "https://libraries.minecraft.net/";
        submitBareLibrary(path, baseUrl);
    }
    
    private void scheduleAssetDownloads(JAssets assets) throws PresentableException {
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
                    MC_RES + hashedPath,
                    assetInfo.hash,
                    assetInfo.size);
        }
    }

    private void scheduleLoggingAssetDownloadIfNeeded(JVersionList.LoggingConfig loggingConfig) throws PresentableException {
        if(loggingConfig.client == null || loggingConfig.client.file == null) return;
        JVersionList.FileProperties loggingFileProperties = loggingConfig.client.file;
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

    private void scheduleGameJarDownload(ClientInfo clientInfo, String versionName) throws PresentableException {
        File clientJar = createGameJarPath(versionName);
        growDownloadList(1);
        scheduleDownload(clientJar,
                DownloadMirror.DOWNLOAD_CLASS_LIBRARIES,
                clientInfo.url,
                clientInfo.sha1,
                clientInfo.size
        );
        // Store the path of the JAR to copy it into our new version folder later.
        mSourceJarFile = clientJar;
    }

    private static <T> T checkedParseJson(File file, Class<T> targetClass) throws PresentableException {
        try {
            T rv = JSONUtils.readFromFile(file, targetClass);
            if(rv == null)
                throw new PresentableException(R.string.mjdl_json_error_title, R.string.mjdl_json_error_subtitle_3);
            return rv;
        } catch (IOException e) {
            throw new PresentableException(R.string.mjdl_disk_error_title, R.string.mjdl_disk_error_subtitle_4);
        } catch (JsonParseException e) {
            throw new PresentableException(R.string.mjdl_json_error_title, R.string.mjdl_json_error_subtitle_3);
        }
    }

    private static void checkedCreateDirectory(File targetFile) throws PresentableException {
        try {
            FileUtils.ensureParentDirectory(targetFile);
        } catch (IOException e) {
            throw new PresentableException(R.string.mjdl_disk_error_title, R.string.mjdl_disk_error_subtitle_1, targetFile.getAbsolutePath());
        }
    }

    private void genericSyntaxException(String info) throws PresentableException {
        throw new PresentableException(R.string.mjdl_json_error_title, R.string.mjdl_json_error_subtitle_2, info);
    }

    private void metadataAccessException() throws PresentableException {
        throw new PresentableException(R.string.mjdl_json_error_title, R.string.mjdl_json_error_subtitle_1);
    }
}
