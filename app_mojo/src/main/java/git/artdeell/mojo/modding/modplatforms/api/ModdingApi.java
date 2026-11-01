package git.artdeell.mojo.modding.modplatforms.api;


import android.content.Context;

import com.kdt.mcgui.ProgressLayout;

import git.artdeell.mojo.MojoApplication;
import git.artdeell.mojo.R;
import git.artdeell.mojo.Tools;
import git.artdeell.mojo.modding.modplatforms.api.modloader.LoaderInstaller;
import git.artdeell.mojo.modding.modplatforms.models.ModDetail;
import git.artdeell.mojo.modding.modplatforms.models.ModDownload;
import git.artdeell.mojo.modding.modplatforms.models.ModItem;
import git.artdeell.mojo.modding.modplatforms.models.ModSource;
import git.artdeell.mojo.modding.modplatforms.models.SearchFilters;
import git.artdeell.mojo.modding.modplatforms.models.SearchResult;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

/**
 *
 */
public interface ModdingApi {

    /**
     * @param searchFilters Filters
     * @param previousPageResult The result from the previous page
     * @return the list of mod items from specified offset
     */
    SearchResult searchMod(SearchFilters searchFilters, SearchResult previousPageResult);

    /**
     * @param searchFilters Filters
     * @return A list of mod items
     */
    default SearchResult searchMod(SearchFilters searchFilters) {
        return searchMod(searchFilters, null);
    }

    /**
     * Fetch the mod details
     * @param item The moditem that was selected
     * @return Detailed data about a mod(pack)
     */
    default ModDetail getModDetails(ModItem item) {
        return new ModDetail(item, getDownloads(item));
    }

    /**
     * Fetch mod metadata
     * @param source source of the mod
     * @return mod metadata array
     */
    ModDownload[] getDownloads(ModSource source);

    default ModDownload[] getDownloads(ModSource source, SearchFilters filters) {
        return getDownloads(source);
    }

    /**
     * Fetch mod details with search filters
     * @param item mod item
     * @param filters search filters
     * @return mod metadata array
     */
    default ModDetail getModDetails(ModItem item, SearchFilters filters) {
        ModDownload[] downloads = getDownloads(item, filters);
        if (downloads != null && filters != null) {
            ArrayList<ModDownload> compatible = new ArrayList<>(downloads.length);
            for (ModDownload download : downloads) {
                if (download.matches(filters.mcVersion, filters.modLoader)) {
                    compatible.add(download);
                }
            }
            downloads = compatible.toArray(new ModDownload[0]);
        }
        return new ModDetail(item, downloads);
    }

    /**
     * Download and install the modpack
     * @param modDetail The mod detail data
     * @param selectedVersion The selected version
     */
    default void handleModpackInstallation(Context context, ModDetail modDetail, int selectedVersion) {
        // Doing this here since when starting installation, the progress does not start immediately
        // which may lead to two concurrent installations (very bad)
        ProgressLayout.setProgress(ProgressLayout.INSTALL_MODPACK, 0, R.string.global_waiting);
        MojoApplication.sExecutorService.execute(() -> {
            try {
                installModpack(modDetail, selectedVersion);
            }catch (IOException e) {
                Tools.showErrorRemote(context, R.string.modpack_install_download_failed, e);
            }
        });
    }

    LoaderInstaller installLocalModpack(String modpackName, File modpackFile, String icon) throws IOException;

    /**
     * Install the mod(pack).
     * May require the download of additional files.
     * May requires launching the installation of a modloader
     * @param modDetail The mod detail data
     * @param selectedVersion The selected version
     */
    LoaderInstaller installModpack(ModDetail modDetail, int selectedVersion) throws IOException;
}
