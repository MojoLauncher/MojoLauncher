package net.kdt.pojavlaunch.instances;

import android.graphics.Bitmap;
import android.os.Build;
import android.util.Log;

import net.kdt.pojavlaunch.Tools;
import net.kdt.pojavlaunch.prefs.LauncherPreferences;
import net.kdt.pojavlaunch.utils.JSONUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

public class Instance {
    public static final int ARGS_MODE_REPLACE = 0;
    public static final int ARGS_MODE_MERGE_DEFAULT_FIRST = 1;
    public static final int ARGS_MODE_MERGE_INSTANCE_FIRST = 2;
    public static final int ARGS_MODE_LAST = ARGS_MODE_MERGE_INSTANCE_FIRST;

    public static final String VERSION_LATEST_RELEASE = "latest_release";
    public static final String VERSION_LATEST_SNAPSHOT = "latest_snapshot";

    protected transient File mInstanceRoot;
    public String name;
    public String versionId;
    public InstanceInstaller installer;
    public String renderer;
    public String jvmArgs;
    public int argsMode;
    public String selectedRuntime;
    public String controlLayout;
    public String icon;
    public boolean sharedData;

    protected Instance() {
    }

    protected void sanitize() {
        sanitizeArgs();
        sanitizeIcon();
    }

    private void sanitizeArgs() {
        if(argsMode > ARGS_MODE_LAST) {
            argsMode = 0;
            jvmArgs = null;
        }
    }

    private void sanitizeIcon() {
        if(!InstanceIconProvider.hasStaticIcon(icon)) {
            icon = InstanceIconProvider.FALLBACK_ICON_NAME;
        }
    }

    /**
     * Write the current contents of the instance to persistent storage.
     * @throws IOException in case of write errors
     */
    public void write() throws IOException {
        JSONUtils.writeToFile(InstanceManager.metadataLocation(mInstanceRoot), this);
    }

    /**
     * Try to write the contents of the instance, ignore any exceptions
     */
    public void maybeWrite() {
        try {
            write();
        }catch (IOException e) {
            Log.e("Instance", "Failed to write",e);
        }
    }

    /**
     * Encode the Bitmap as the new profile icon with required encoding settings.
     * @param bitmap the target bitmap
     * @throws IOException in case of errors while storing the icon
     */
    public void encodeNewIcon(Bitmap bitmap) throws IOException {
        try(FileOutputStream fileOutputStream = new FileOutputStream(getInstanceIconLocation())) {
            bitmap.compress(
                    Build.VERSION.SDK_INT < Build.VERSION_CODES.R ?
                            // On Android < 30, there was no distinction between "lossy" and "lossless",
                            // and the type is picked by the quality parameter. We set the quality to 60.
                            // so it should be lossy,
                            Bitmap.CompressFormat.WEBP:
                            // On Android >= 30, we can explicitly specify that we want lossy compression
                            // with the visual quality of 60.
                            Bitmap.CompressFormat.WEBP_LOSSY,
                    60,
                    fileOutputStream
            );
        }
    }

    public String getLaunchRenderer() {
        if(Tools.isValidString(renderer)) return renderer;
        return LauncherPreferences.PREF_RENDERER;
    }

    public String getLaunchArgs() {
        if(!Tools.isValidString(jvmArgs)) return LauncherPreferences.PREF_CUSTOM_JAVA_ARGS;
        switch (argsMode) {
            case ARGS_MODE_REPLACE:
                return jvmArgs;
            case ARGS_MODE_MERGE_DEFAULT_FIRST:
                return LauncherPreferences.PREF_CUSTOM_JAVA_ARGS + " " + jvmArgs;
            case ARGS_MODE_MERGE_INSTANCE_FIRST:
                return jvmArgs + " " + LauncherPreferences.PREF_CUSTOM_JAVA_ARGS;
            default:
                throw new RuntimeException("Unknown value for argsMode: "+argsMode);
        }
    }

    public String getLaunchControls() {
        if(!Tools.isValidString(controlLayout)) return LauncherPreferences.PREF_DEFAULTCTRL_PATH;
        return Tools.CTRLMAP_PATH + "/" + controlLayout;
    }

    public File getGameDirectory() {
        if(sharedData) return InstanceManager.SHARED_DATA_DIRECTORY;
        return mInstanceRoot;
    }

    protected File getInstanceIconLocation() {
        return new File(mInstanceRoot, "icon.webp");
    }

    public boolean isSelected() {
        return this == InstanceManager.getSelectedListedInstance();
    }
}
