package net.kdt.pojavlaunch.modloaders.modpacks.api

import android.content.Context
import com.kdt.mcgui.ProgressLayout
import net.ashmeet.hyperlauncher.R
import net.kdt.pojavlaunch.PojavApplication
import net.kdt.pojavlaunch.Tools
import net.kdt.pojavlaunch.modloaders.modpacks.models.ModDetail
import net.kdt.pojavlaunch.modloaders.modpacks.models.ModItem
import net.kdt.pojavlaunch.modloaders.modpacks.models.SearchFilters
import net.kdt.pojavlaunch.modloaders.modpacks.models.SearchResult
import java.io.File
import java.io.IOException


/**
 * 
 */
interface ModpackApi {
    /**
     * @param searchFilters Filters
     * @param previousPageResult The result from the previous page
     * @return the list of mod items from specified offset
     */
    fun searchMod(searchFilters: SearchFilters?, previousPageResult: SearchResult?): SearchResult?

    /**
     * Fetch the mod details
     * @param item The moditem that was selected
     * @return Detailed data about a mod(pack)
     */
    fun getModDetails(item: ModItem?): ModDetail?

    /**
     * Download and install the modpack
     * @param modDetail The mod detail data
     * @param selectedVersion The selected version
     */
    fun handleModpackInstallation(context: Context, modDetail: ModDetail?, selectedVersion: Int) {
        // Doing this here since when starting installation, the progress does not start immediately
        // which may lead to two concurrent installations (very bad)
        ProgressLayout.setProgress(ProgressLayout.INSTALL_MODPACK, 0, R.string.global_waiting)
        PojavApplication.sExecutorService.execute(Runnable {
            try {
                installModpack(modDetail, selectedVersion)
            } catch (e: IOException) {
                Tools.showErrorRemote(context, R.string.modpack_install_download_failed, e)
            }
        })
    }

    @Throws(IOException::class)
    fun installLocalModpack(modpackName: String?, modpackFile: File?, icon: String?): ModLoader?

    /**
     * Install the mod(pack).
     * May require the download of additional files.
     * May requires launching the installation of a modloader
     * @param modDetail The mod detail data
     * @param selectedVersion The selected version
     */
    @Throws(IOException::class)
    fun installModpack(modDetail: ModDetail?, selectedVersion: Int): ModLoader?
}
