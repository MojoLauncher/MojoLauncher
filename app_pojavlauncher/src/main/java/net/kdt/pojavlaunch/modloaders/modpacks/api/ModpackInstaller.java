package net.kdt.pojavlaunch.modloaders.modpacks.api;

import com.kdt.mcgui.ProgressLayout;

import git.artdeell.mojo.R;
import net.kdt.pojavlaunch.Tools;
import net.kdt.pojavlaunch.instances.InstanceManager;
import net.kdt.pojavlaunch.instances.Instance;
import net.kdt.pojavlaunch.modloaders.modpacks.imagecache.ModIconCache;
import net.kdt.pojavlaunch.modloaders.modpacks.models.ModDetail;
import net.kdt.pojavlaunch.modloaders.modpacks.models.ModDownload;
import net.kdt.pojavlaunch.progresskeeper.DownloaderProgressWrapper;
import net.kdt.pojavlaunch.utils.DownloadUtils;

import java.io.File;
import java.io.IOException;
import java.util.Locale;
import java.util.concurrent.Callable;

public class ModpackInstaller {

    protected static ModLoader installModpack(ModDetail modDetail, int selectedVersion, InstallFunction installFunction) throws IOException {
        ModDownload modDownload = modDetail.downloads[selectedVersion];
        String modpackName = (modDetail.title.toLowerCase(Locale.ROOT) + " " + modDownload.versionName)
                .trim().replaceAll("[\\\\/:*?\"<>| \\t\\n]", "_" );
        if (modDownload.versionHash != null) {
            modpackName += "_" + modDownload.versionHash;
        }
        if (modpackName.length() > 255){
            modpackName = modpackName.substring(0,255);
        }

        // Build a new minecraft instance, folder first

        // Get the modpack file
        File modpackFile = new File(Tools.DIR_CACHE, modpackName + ".cf"); // Cache File
        ModLoader modLoaderInfo;
        Instance instance = InstanceManager.createInstance(
                i->i.name = modDetail.title,
                modpackName.substring(0, Math.min(16,modpackName.length()))
        );
        try {
            byte[] downloadBuffer = new byte[8192];
            DownloadUtils.ensureSha1(modpackFile, modDownload.versionHash, (Callable<Void>) () -> {
                DownloadUtils.downloadFileMonitored(modDownload.versionUrl, modpackFile, downloadBuffer,
                        new DownloaderProgressWrapper(R.string.modpack_download_downloading_metadata,
                                ProgressLayout.INSTALL_MODPACK));
                return null;
            });

            // Install the modpack
            modLoaderInfo = installFunction.installModpack(modpackFile, instance.getGameDirectory());

            if(modLoaderInfo == null) throw new IOException("Unknown modpack mod loader information");

            if(modLoaderInfo.requiresGuiInstallation()) {
                instance.installer = modLoaderInfo.createInstaller();
            } else {
                String versionId = modLoaderInfo.installHeadlessly();
                if(versionId == null) throw new IOException("Unknown mod loader version");
                instance.versionId = versionId;
            }
            instance.write();
            ModIconCache.writeInstanceImage(instance, modDetail.getIconCacheTag());

            InstanceManager.setSelectedInstance(instance);
            if(modLoaderInfo.requiresGuiInstallation()) {
                instance.installer.start();
            }
        } catch (IOException e) {
            InstanceManager.removeInstance(instance);
            throw e;
        } finally {
            modpackFile.delete();
            ProgressLayout.clearProgress(ProgressLayout.INSTALL_MODPACK);
        }

        return modLoaderInfo;
    }

    protected interface InstallFunction {
        ModLoader installModpack(File modpackFile, File instanceDestination) throws IOException;
    }
}
