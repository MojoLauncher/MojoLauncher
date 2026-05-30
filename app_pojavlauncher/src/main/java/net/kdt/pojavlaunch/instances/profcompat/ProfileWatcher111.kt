package net.kdt.pojavlaunch.instances.profcompat

import android.content.res.AssetManager
import net.kdt.pojavlaunch.Tools
import net.kdt.pojavlaunch.Tools.copyAssetFile
import java.io.File
import java.io.FileReader
import java.io.IOException

object ProfileWatcher {
    private val sLauncherProfiles = File(Tools.DIR_GAME_NEW, "launcher_profiles.json")

    @Throws(IOException::class)
    fun consumePendingVersion(assetManager: AssetManager): String? {
        val store: Profiles
        FileReader(sLauncherProfiles).use { fileReader ->
            store = Tools.GLOBAL_GSON.fromJson<Profiles>(fileReader, Profiles::class.java)
        }
        val profiles = store.profiles
        var versionId: String? = null
        if (profiles != null) {
            for (entry in profiles.entries) {
                if ("(Default)" == entry.key) continue
                versionId = entry.value?.lastVersionId
                if (versionId != null) break
            }
        }
        installDefaultProfiles(assetManager)
        return versionId
    }

    @Throws(IOException::class)
    fun installDefaultProfiles(assetManager: AssetManager) {
        copyAssetFile(assetManager, "launcher_profiles.json", sLauncherProfiles, true)
    }
}
