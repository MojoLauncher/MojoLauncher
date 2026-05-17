package net.kdt.pojavlaunch.adrenotools;

import android.util.Log;

import net.kdt.pojavlaunch.Tools;
import net.kdt.pojavlaunch.prefs.LauncherPreferences;
import net.kdt.pojavlaunch.utils.GLInfoUtils;
import net.kdt.pojavlaunch.utils.ZipUtils;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipFile;

// AdrenoTools package manager
public class AdrenoManager {
    public static final String METADATA_FILENAME = "meta.json";
    public static final String TAG = "AdrenoTools";
    private static final DefaultDriver DEFAULT_DRIVER = new DefaultDriver();
    private static File packagesPath = new File(Tools.DIR_DATA, "vulkan");

    private AdrenoManager() {}

    public static BaseDriver getPreferredDriver() {
        BaseDriver driver = LauncherPreferences.PREF_VULKAN_PACKAGE == null ? DEFAULT_DRIVER : loadPackage(LauncherPreferences.PREF_VULKAN_PACKAGE);
        if(driver == null) {
            Log.e(TAG, "Failed to load preferred driver package " + LauncherPreferences.PREF_VULKAN_PACKAGE);
            return DEFAULT_DRIVER;
        }
        return driver;
    }
    public static boolean isPreferredDriver(BaseDriver driver){
        String pkg = getPreferredDriverPackage();
        // selected default package
        if(pkg == null && driver == DEFAULT_DRIVER)
            return true;
        if(pkg != null && driver instanceof AdrenoDriver) {
            return pkg.equals(((AdrenoDriver) driver).toHash());
        }
        return false;
    }
    public static String getPreferredDriverPackage() {
        return LauncherPreferences.PREF_VULKAN_PACKAGE;
    }
    public static String getPreferredDriverLibraryPath(){
        BaseDriver driver = getPreferredDriver();
        if(driver.isDefault()) return driver.getMainLibrary(); // Default driver should be in the library path already
        File path = new File(packagesPath,  ((AdrenoDriver) driver).toHash() + "/" + driver.getMainLibrary());
        if(path.exists()) return path.getAbsolutePath();

        // Invalid driver
        Log.e(TAG, "Unable to resolve Vulkan library: path " + path.getAbsolutePath() + " is not a file or does not exist");
        return DEFAULT_DRIVER.getMainLibrary();
    }
    public static String getPreferredDriverRootPath(){
        BaseDriver driver = getPreferredDriver();
        if(driver.isDefault()) return null; // Already in the library path
        File path = new File(packagesPath,  ((AdrenoDriver) driver).toHash());
        if(path.exists())
            return path.getAbsolutePath();
        else return null;
    }
    public static void setPreferredDriver(BaseDriver driver){
        LauncherPreferences.PREF_VULKAN_PACKAGE = !driver.isDefault() ? ((AdrenoDriver) driver).toHash() : null;
        LauncherPreferences.DEFAULT_PREF.edit().putString("vulkanPackage", LauncherPreferences.PREF_VULKAN_PACKAGE).apply();
    }

    public static void init(){
        Log.i(TAG, "Initializing");
        if(!GLInfoUtils.getGlInfo().isAdreno())
            return;
        if(!packagesPath.exists())
            packagesPath.mkdirs();
        String selectedDriver = LauncherPreferences.PREF_VULKAN_PACKAGE;
        if(selectedDriver == null || selectedDriver.isEmpty())
            setPreferredDriver(DEFAULT_DRIVER);
    }

    public static List<String> getDriverPaths(){
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
    public static List<BaseDriver> getDrivers(){
        if(!packagesPath.exists())
            return null;
        List<BaseDriver> drivers = new ArrayList<>();
        drivers.add(DEFAULT_DRIVER);
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
        try {
            FileUtils.deleteDirectory(path);
            return true;
        } catch (IOException e) {
            Log.e(TAG, "Unable to remove driver package " + name);
            return false;
        }
    }
}
