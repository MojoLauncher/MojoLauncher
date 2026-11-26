package git.artdeell.mojo.fragments.modding;

import android.content.ContentResolver;
import android.content.Context;
import android.net.Uri;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;

import com.kdt.mcgui.ProgressLayout;

import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import git.artdeell.mojo.MojoApplication;
import git.artdeell.mojo.R;
import git.artdeell.mojo.Tools;
import git.artdeell.mojo.modding.modplatforms.ModItemAdapter;
import git.artdeell.mojo.modding.modplatforms.models.SearchFilters;
import git.artdeell.mojo.utils.profiles.VersionSelectorDialog;

public class ModpackSearchFragment extends BaseSearchFragment {

    public static final String TAG = "ModpackSearchFragment";

    ActivityResultLauncher<String> mImportLauncher = registerForActivityResult(new ActivityResultContracts.GetContent(),
            uri -> {
                if (uri == null) return;
                Context context = getContext();
                ContentResolver contentResolver = getContext().getContentResolver();
                MojoApplication.sExecutorService.execute(() -> {
                    performLocalInstall(uri, context, contentResolver);
                });
            });

    public void performLocalInstall(Uri uri, Context context, ContentResolver contentResolver) {
        String fileName = Tools.getFileName(context, uri);
        if (fileName == null) return;
        File outFile = new File(Tools.DIR_CACHE, fileName + ".cf");
        ProgressLayout.setProgress(ProgressLayout.INSTALL_MODPACK, R.string.multirt_progress_caching);
        try (InputStream inputStream = contentResolver.openInputStream(uri);
             OutputStream outputStream = new FileOutputStream(outFile)) {
            if (inputStream == null) return;
            IOUtils.copy(inputStream, outputStream);
            outputStream.flush();
        } catch (IOException e) {
            Tools.showErrorRemote("Error", e);
            ProgressLayout.clearProgress(ProgressLayout.INSTALL_MODPACK);
            return;
        }
        try {
            moddingApi.installLocalModpack(fileName, outFile, null);
        } catch (IOException e) {
            Tools.showErrorRemote("Error", e);
        } finally {
            outFile.delete();
            ProgressLayout.clearProgress(ProgressLayout.INSTALL_MODPACK);
        }
    }

    @Override
    protected void prepareFilters(SearchFilters filters) {
        filters.isModpack = true;
    }

    @Override
    protected int getSearchHint() {
        return R.string.hint_search_modpack;
    }

    @Override
    protected int getNoResultsString() {
        return R.string.search_modpack_no_result;
    }

    @Override
    protected int getErrorString() {
        return R.string.search_modpack_error;
    }

    @Override
    protected ModItemAdapter.ModInstallHandler createInstallHandler() {
        return moddingApi::handleModpackInstallation;
    }

    @Override
    protected void setupImportButton(Button button) {
        button.setText(R.string.import_local_modpack);
        button.setOnClickListener(v -> mImportLauncher.launch("*/*"));
    }
    private void displayFilterDialog() {
        AlertDialog dialog = new AlertDialog.Builder(requireContext())
                .setView(R.layout.dialog_mod_filters)
                .create();

        // setup the view behavior
        dialog.setOnShowListener(dialogInterface -> {
            TextView mSelectedVersion = dialog.findViewById(R.id.search_mod_selected_mc_version_textview);
            Button mSelectVersionButton = dialog.findViewById(R.id.search_mod_mc_version_button);
            Button mApplyButton = dialog.findViewById(R.id.search_mod_apply_filters);

            assert mSelectVersionButton != null;
            assert mSelectedVersion != null;
            assert mApplyButton != null;

            // Setup the expendable list behavior
            mSelectVersionButton.setOnClickListener(v -> VersionSelectorDialog.open(v.getContext(), true, (id, snapshot)-> mSelectedVersion.setText(id)));

            // Apply visually all the current settings
            mSelectedVersion.setText(searchFilters.mcVersion);

            // Apply the new settings
            mApplyButton.setOnClickListener(v -> {
                searchFilters.mcVersion = mSelectedVersion.getText().toString().trim();
                if(searchFilters.mcVersion.isEmpty()) searchFilters.mcVersion = null;
                searchMods(searchEditText.getText().toString());
                dialogInterface.dismiss();
            });
        });

        dialog.show();
    }

    @Override
    protected void setupFilterButton(ImageButton button) {
        button.setOnClickListener(v -> displayFilterDialog());
    }
}
