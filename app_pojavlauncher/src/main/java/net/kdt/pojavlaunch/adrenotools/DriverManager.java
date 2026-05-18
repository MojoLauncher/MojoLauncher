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
import java.util.Collections;
import java.util.List;
import java.util.zip.ZipFile;

// AdrenoTools package manager
public class DriverManager {
    public static final String METADATA_FILENAME = "meta.json";
    public static final String TAG = "AdrenoTools";
    private static final DefaultDriver DEFAULT_DRIVER = new DefaultDriver();
    // Must be in the exec place, /sdcard/... is noexec
    private static File packagesPath = new File(Tools.DIR_DATA, "vulkan");
    private static Boolean supported = null;
    private DriverManager() {}

    public static boolean isSupportedByDevice(){
        return supported == null ? (supported = GLInfoUtils.getGlInfo().isAdreno()) : supported;
    }

    public static Driver getPreferredDriver() {
        Driver driver = LauncherPreferences.PREF_VULKAN_PACKAGE == null ? DEFAULT_DRIVER : loadDriver(LauncherPreferences.PREF_VULKAN_PACKAGE);
        if(driver == null) {
            Log.e(TAG, "Failed to load preferred driver package " + LauncherPreferences.PREF_VULKAN_PACKAGE);
            return DEFAULT_DRIVER;
        }
        return driver;
    }
    public static boolean isPreferredDriver(Driver driver){
        String pkg = getPreferredDriverHash();
        // selected default package
        if(pkg == null && driver == DEFAULT_DRIVER)
            return true;
        if(pkg != null && driver instanceof AdrenoDriver) {
            return pkg.equals(driver.getHash());
        }
        return false;
    }
    public static String getPreferredDriverHash() {
        return LauncherPreferences.PREF_VULKAN_PACKAGE;
    }
    public static String getPreferredDriverLibraryPath(){
        Driver driver = getPreferredDriver();
        if(driver.isDefault()) return driver.getMainLibrary(); // Default driver should be in the library path already
        File path = new File(packagesPath,  (driver.getHash() + "/" + driver.getMainLibrary()));
        if(path.exists()) return path.getAbsolutePath();

        // Invalid driver
        Log.e(TAG, "Unable to resolve Vulkan library: path " + path.getAbsolutePath() + " is not a file or does not exist");
        return DEFAULT_DRIVER.getMainLibrary();
    }
    public static String getPreferredDriverRootPath(){
        Driver driver = getPreferredDriver();
        if(driver.isDefault()) return null; // Already in the library path
        File path = new File(packagesPath,  driver.getHash());
        if(path.exists())
            return path.getAbsolutePath();
        else return null;
    }
    public static void setPreferredDriver(Driver driver){
        LauncherPreferences.PREF_VULKAN_PACKAGE = !driver.isDefault() ? driver.getHash() : null;
        LauncherPreferences.DEFAULT_PREF.edit().putString("vulkanPackage", LauncherPreferences.PREF_VULKAN_PACKAGE).apply();
    }
    private static boolean ensureRootExists(){
        if(!packagesPath.exists())
            return packagesPath.mkdirs();
        return true;
    }

    public static List<String> getDriverPaths(){
        if(!packagesPath.exists())
            return Collections.emptyList();
        List<String> packages = new ArrayList<>();
        for(File dir : packagesPath.listFiles(File::isDirectory)) {
            if(!new File(dir, METADATA_FILENAME).exists())
                continue;
            packages.add(dir.getName());
        }
        return packages;
    }
    public static List<Driver> getDrivers(){
        List<Driver> drivers = new ArrayList<>();
        drivers.add(DEFAULT_DRIVER);
        if(!packagesPath.exists())
            return drivers;
        // I'm not sure how heavy is this crap
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

    public static boolean driverExists(String hash){
        return packagesPath.exists() && new File(packagesPath, hash + "/" + METADATA_FILENAME).exists();
    }

    public static AdrenoDriver loadDriver(String hash) {
        File metadata = new File(packagesPath, hash + "/" + METADATA_FILENAME);
        if (!metadata.exists())
            return null;
        return AdrenoDriver.fromJson(metadata);
    }

    public static AdrenoDriver installDriver(File path, boolean overwrite){
        ensureRootExists();
        try(ZipFile zf = new ZipFile(path)){

            AdrenoDriver driver = AdrenoDriver.fromJson(ZipUtils.getEntryStream(zf, METADATA_FILENAME));
            String hash = driver.getHash();
            if(driverExists(hash)){
                if(!overwrite) {
                    Log.w(TAG, "Driver package already installed and overwrite is not requested");
                    return null;
                }
                else {
                    removeDriver(hash);
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
    public static boolean removeDriver(String name) {
        if(!packagesPath.exists())
            return false;
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
