package git.artdeell.mojo.fragments.modding;

import android.content.Context;
import android.net.Uri;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;

import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import git.artdeell.mojo.MojoApplication;
import git.artdeell.mojo.R;
import git.artdeell.mojo.Tools;
import git.artdeell.mojo.contracts.OpenDocumentWithExtension;
import git.artdeell.mojo.modding.modplatforms.ModItemAdapter;
import git.artdeell.mojo.modding.modplatforms.api.ModInstaller;
import git.artdeell.mojo.modding.modplatforms.models.SearchFilters;
import git.artdeell.mojo.utils.FileUtils;
import me.andreasmelone.basicmodinfoparser.platform.Platform;

public class ModSearchFragment extends BaseSearchFragment {

    public static final String TAG = "ModSearchFragment";

    @Override
    protected int getSearchHint() {
        return R.string.hint_search_mods;
    }

    @Override
    protected int getNoResultsString() {
        return R.string.search_mod_no_result;
    }

    @Override
    protected int getErrorString() {
        return R.string.search_mod_error;
    }

    private static String loaderName(Platform platform) {
        switch (platform) {
            case FABRIC: return "fabric";
            case QUILT: return "quilt";
            case NEOFORGE: return "neoforge";
            // There's of course a question how will the filter handle that
            case FORGE:
            case FORGE_LEGACY: return "forge";
            default: return null;
        }
    }

    @Override
    protected void prepareFilters(SearchFilters filters) {
        filters.isModpack = false;
        if(instance == null || instance.moddingData == null) return;
        filters.mcVersion = instance.moddingData.gameVersion;
        filters.modLoader = loaderName(instance.moddingData.platform);
    }

    private void importMod(String fileName, Uri modUri, Context context) {
        File modsFolder = new File(instance.getGameDirectory(), "mods");
        FileUtils.ensureDirectorySilently(modsFolder);
        try (InputStream inputStream = context.getContentResolver().openInputStream(modUri);
             OutputStream outputStream = new FileOutputStream(new File(modsFolder, fileName))){
            if(inputStream == null) throw new IOException("Failed to open mod input stream");
            IOUtils.copy(inputStream, outputStream);
        }catch (IOException e) {
            Tools.showErrorRemote(e);
        }
        Tools.runOnUiThread(() -> onModInstalled(context));
    }

    private final ActivityResultLauncher<Object> mModImportLauncher =
            registerForActivityResult(new OpenDocumentWithExtension("jar"), (data)->{
                if(data == null) return;
                Context context = getContext();
                if(context == null) return;
                String fileName = Tools.getFileName(context, data);
                MojoApplication.sExecutorService.execute(() -> importMod(fileName, data, context));
            });

    @Override
    protected void setupImportButton(Button button) {
        button.setText(R.string.mod_local_install);
        button.setOnClickListener(v -> mModImportLauncher.launch(null));
    }

    @Override
    protected void setupFilterButton(ImageButton button) {
        button.setVisibility(View.GONE);
    }

    @Override
    protected ModItemAdapter.ModInstallHandler createInstallHandler() {
        return ((context, detail, version) -> {
            try {
                ModInstaller.install(context, instance, detail, version, f -> onModInstalled(context));
            } catch (IOException e) {
                Tools.showError(context, e);
            }
        });
    }

    private void onModInstalled(Context context) {
        Toast.makeText(context, R.string.mod_install_toast_success,
                Toast.LENGTH_SHORT).show();
    }

}
