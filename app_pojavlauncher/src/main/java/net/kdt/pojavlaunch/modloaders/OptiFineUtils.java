package net.kdt.pojavlaunch.modloaders;

import android.content.Intent;

import net.kdt.pojavlaunch.Tools;
import net.kdt.pojavlaunch.instances.InstanceInstaller;
import net.kdt.pojavlaunch.utils.DownloadUtils;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Objects;

public class OptiFineUtils {

    public static OptiFineVersions downloadOptiFineVersions() throws IOException {
        try {
            return DownloadUtils.downloadStringCached("https://optifine.net/downloads",
                    "of_downloads_page", new OptiFineScraper());
        }catch (DownloadUtils.ParseException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static InstanceInstaller createInstaller(OptiFineVersion version) {
        int installerHash = Objects.hash(version.versionName, version.minecraftVersion);
        File installerLocation = new File(Tools.DIR_CACHE, "optifine-installer-"+installerHash+".jar");
        InstanceInstaller instanceInstaller = new InstanceInstaller();
        instanceInstaller.installerUrlTransformer = "optifine";
        instanceInstaller.installerDownloadUrl = version.downloadUrl;
        instanceInstaller.installerJar = installerLocation.getAbsolutePath();
        instanceInstaller.commandLineArgs = "-javaagent:"+ Tools.DIR_DATA+"/forge_installer/forge_installer.jar=OF";
        return instanceInstaller;
    }

    public static class OptiFineVersions {
        public List<String> minecraftVersions;
        public List<List<OptiFineVersion>> optifineVersions;
    }
    public static class OptiFineVersion {
        public String minecraftVersion;
        public String versionName;
        public String downloadUrl;
    }
}
