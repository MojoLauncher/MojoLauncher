package git.artdeell.mojo.utils.plugins;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.Signature;
import android.content.pm.SigningInfo;
import android.util.Base64;
import android.util.Log;

import java.io.File;
import java.security.MessageDigest;
import java.util.Set;

import git.artdeell.mojo.BuildConfig;

public class LibraryPlugin {
    private static final String TAG = "LibraryPlugin";

    // Known plugins constants
    public static final String ID_ANGLE_PLUGIN = "git.mojo.angle";
    public static final String ID_FFMPEG_PLUGIN = "git.mojo.ffmpeg";
    public static final String ID_ZINK_PLUGIN = "git.mojo.zink";
    public static final String ID_MESA_PLUGIN = "git.mojo.mesa";

    // TODO: Add plugin signature
    private static final Set<String> SIGNATURES = Set.of(
            "blablabla"
    );

    private String appId;
    private String libraryPath;
    private LibraryPlugin(String app, String libraryPath){
        this.appId = app;
        this.libraryPath = libraryPath;
    }

    public static LibraryPlugin discoverPlugin(Context ctx, String appId){
        String libraryPath;
        try {
            PackageInfo pluginPackage = ctx.getPackageManager().getPackageInfo(appId, PackageManager.GET_SHARED_LIBRARY_FILES | PackageManager.GET_SIGNATURES);
            if(BuildConfig.BUILD_TYPE.equals("gplay")) {
                Signature[] sigs = pluginPackage.signatures;
                if(sigs == null  || sigs.length != 1) throw new IllegalArgumentException("Invalid plugin signature!");
                byte[] dig = MessageDigest.getInstance("SHA-256").digest(sigs[0].toByteArray());
                String b64dig = Base64.encodeToString(dig, Base64.NO_WRAP);
                if(!SIGNATURES.contains(b64dig)) throw new IllegalArgumentException("Signatures don't match: unknown sig " + b64dig + "! Plugin won't be loaded");
            }
            libraryPath = pluginPackage.applicationInfo.nativeLibraryDir;
        } catch (Exception e){
            Log.e(TAG, "Plugin discover failed: " + e.getMessage());
            return null;
        }
       return new LibraryPlugin(appId, libraryPath);
    }

    public String getId(){
        return appId;
    }

    public String getLibraryPath(){
        return libraryPath;
    }
    public String resolveAbsolutePath(String library) {
        return new File(libraryPath, library).getAbsolutePath();
    }
    public File resolve(String library) {
        return new File(libraryPath, library);
    }

    public boolean checkLibraries(String... libs){
        for(String lib : libs){
            if(!(new File(libraryPath, lib).exists())) return false;
        }
        return true;
    }
}
