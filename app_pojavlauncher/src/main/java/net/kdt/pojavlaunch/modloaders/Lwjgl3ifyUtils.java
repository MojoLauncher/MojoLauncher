package net.kdt.pojavlaunch.modloaders;

import net.kdt.pojavlaunch.JVersionList;
import net.kdt.pojavlaunch.Tools;
import net.kdt.pojavlaunch.tasks.MoJsonDownloader;
import net.kdt.pojavlaunch.utils.FileUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class Lwjgl3ifyUtils {
    private static final String RELAUNCHER_JSON_PATH = "me/eigenraven/lwjgl3ify/relauncher/version.json";
    public static File detectLwjgl3ifyJar(File instanceDir) throws IOException {
        File modsDir = new File(instanceDir, "mods");
        File[] lwjgl3ifyJars = modsDir.listFiles(file -> {
            if(!file.isFile()) return false;
            String name = file.getName();
            return name.startsWith("lwjgl3ify-") && name.endsWith(".jar");
        });
        if(lwjgl3ifyJars == null || lwjgl3ifyJars.length == 0) return null;
        if(lwjgl3ifyJars.length > 1)
            throw new IOException("Malformed modpack: multiple lwjgl3ify JARs detected");
        return lwjgl3ifyJars[0];
    }

    private static boolean findRelauncherEntry(ZipInputStream zipInputStream) throws IOException {
        ZipEntry entry;
        while((entry = zipInputStream.getNextEntry()) != null) {
            String entryName = entry.getName();
            if(!entryName.equals(RELAUNCHER_JSON_PATH)) continue;
            return true;
        }
        return false;
    }

    public static String installRelauncherVersionJson(File jarPath) throws IOException {
        String versionContent;
        try(FileInputStream fileInputStream = new FileInputStream(jarPath);
            ZipInputStream zipInputStream = new ZipInputStream(fileInputStream)) {
            if(!findRelauncherEntry(zipInputStream))
                throw new IOException("Failed to find relauncher version.json");
            versionContent = Tools.read(zipInputStream);
        }
        JVersionList.FileProperties properties = Tools.GLOBAL_GSON.fromJson(versionContent, JVersionList.FileProperties.class);
        if(properties == null || properties.id == null) return null;
        File versionFile = MoJsonDownloader.createGameJsonPath(properties.id);
        FileUtils.ensureParentDirectory(versionFile);
        Tools.write(versionFile, versionContent);
        return properties.id;
    }
}
