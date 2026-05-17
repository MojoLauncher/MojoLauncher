package net.kdt.pojavlaunch.adrenotools;


import android.util.Log;

import net.kdt.pojavlaunch.Tools;
import net.kdt.pojavlaunch.prefs.LauncherPreferences;
import net.kdt.pojavlaunch.utils.GLInfoUtils;
import net.kdt.pojavlaunch.utils.ZipUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipFile;

// AdrenoTools package manager
public class AdrenoManager {
    public static final String METADATA_FILENAME = "meta.json";
    public static final String TAG = "AdrenoTools";
    private static BaseDriver preferredDriver = null;
    private static File packagesPath = new File(Tools.DIR_DATA, "vulkan");

    private AdrenoManager() {}

    private static void initDefaultDriver(){
        AdrenoManager.preferredDriver = new DefaultDriver();
    }

    public static BaseDriver getPreferredDriver() {
        return preferredDriver;
    }

    public static void init(){
        Log.i(TAG, "Initializing");
        if(!GLInfoUtils.getGlInfo().isAdreno())
            return;
        if(!packagesPath.exists())
            packagesPath.mkdirs();

        String selectedDriver = LauncherPreferences.PREF_VULKAN_PACKAGE;
        if(selectedDriver == null || selectedDriver.isEmpty())
            initDefaultDriver();

    }

    public static List<String> getDrivers(){
        if(!packagesPath.exists())
            return null;
        List<String> packages = new ArrayList<>();
        for(File dir : packagesPath.listFiles(File::isDirectory)) {
            if(!new File(dir, METADATA_FILENAME).exists())
                continue;
            packages.add(dir.getName());
        }
        return packages;
    }
    public static List<AdrenoDriver> getDriverPaths(){
        if(!packagesPath.exists())
            return null;
        List<AdrenoDriver> drivers = new ArrayList<>();
        for(File dir : packagesPath.listFiles(File::isDirectory)) {
            File metadata = new File(dir, METADATA_FILENAME);
            if(!metadata.exists())
                continue;
            AdrenoDriver driver = AdrenoDriver.fromJson(metadata);
            if(driver != null)
                drivers.add(driver);
        }
        return drivers;
    }

    public static boolean packageExists(String hash){
        return packagesPath.exists() && new File(packagesPath, hash + "/" + METADATA_FILENAME).exists();
    }

    public static AdrenoDriver loadPackage(String hash) {
        File metadata = new File(packagesPath, hash + "/" + METADATA_FILENAME);
        if (!metadata.exists())
            return null;
        return AdrenoDriver.fromJson(metadata);
    }

    public static AdrenoDriver installPackage(File path, boolean overwrite){
        if(!path.isFile())
            return null;
        try(ZipFile zf = new ZipFile(path)){
            AdrenoDriver driver = AdrenoDriver.fromJson(ZipUtils.getEntryStream(zf, METADATA_FILENAME));
            String hash = driver.toHash();
            if(packageExists(hash)){
                if(!overwrite) {
                    Log.w(TAG, "Driver package already installed and overwrite is not requested");
                    return null;
                }
                else {
                    removePackage(hash);
                }
            }
            File installPath = new File(packagesPath, hash);
            ZipUtils.zipExtract(zf, "", installPath);
            Log.i(TAG, "Installed driver package " + driver.getName() + " into " + installPath.getAbsolutePath());
            return driver;
        } catch (Exception e) {
            Log.e(TAG, "Malformed driver package file at " + path.getAbsolutePath());
            return null;
        }
    }
    public static boolean removePackage(String name) {
        File path = new File(packagesPath, name);
        if (!path.isDirectory())
            return false;
        return path.delete();
    }
}
