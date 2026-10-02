package net.kdt.pojavlaunch.instances.profcompat;

import android.content.res.AssetManager;

import net.kdt.pojavlaunch.Tools;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Iterator;
import java.util.Map;

public class ProfileWatcher {
    private static final File sLauncherProfiles = new File(Tools.DIR_GAME_NEW, "launcher_profiles.json");
    public static String consumePendingVersion(AssetManager assetManager) throws IOException {
        Profiles store;
        try(FileReader fileReader = new FileReader(sLauncherProfiles)) {
            store = Tools.GLOBAL_GSON.fromJson(fileReader, Profiles.class);
        }
        Map<String, ProfileBody> profiles = store.profiles;
        String versionId = null;
        for (Map.Entry<String, ProfileBody> entry : profiles.entrySet()) {
            if ("(Default)".equals(entry.getKey())) continue;
            versionId = entry.getValue().lastVersionId;
            if(versionId != null) break;
        }
        installDefaultProfiles(assetManager);
        return versionId;
    }
    public static void installDefaultProfiles(AssetManager assetManager) throws IOException {
        Tools.copyAssetFile(assetManager, "launcher_profiles.json", sLauncherProfiles, true);
    }
}
