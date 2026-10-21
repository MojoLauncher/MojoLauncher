package git.artdeell.mojo.fragments;

import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;

import androidx.activity.result.ActivityResultLauncher;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import git.artdeell.mojo.MojoApplication;
import git.artdeell.mojo.Tools;
import git.artdeell.mojo.contracts.OpenDocumentWithExtension;
import git.artdeell.mojo.instances.Instance;
import git.artdeell.mojo.instances.Instances;
import git.artdeell.mojo.modding.modmanager.ModAdapter;
import git.artdeell.mojo.modding.modmanager.ModInfo;
import git.artdeell.mojo.modding.modmanager.ModScanner;
import git.artdeell.mojo.utils.FileUtils;

import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;

import git.artdeell.mojo.R;

public class ModManagerFragment extends Fragment {
    public static final String TAG = "ModManagerFragment";
    private RecyclerView mModRecyclerView;
    private ProgressBar mProgressBar;
    private EditText mSearchEditText;
    private File mModsFolder;

    private final ActivityResultLauncher<Object> mModImportLauncher =
            registerForActivityResult(new OpenDocumentWithExtension("jar"), (data)->{
                if(data == null) return;
                Context context = getContext();
                if(context == null) return;
                String fileName = Tools.getFileName(context, data);
                MojoApplication.sExecutorService.execute(()->importMod(fileName, data, context));
            });

    public ModManagerFragment() {
        super(R.layout.fragment_mod_manager);
    }

    private void importMod(String fileName, Uri modUri, Context context) {
        FileUtils.ensureDirectorySilently(mModsFolder);
        try (InputStream inputStream = context.getContentResolver().openInputStream(modUri);
             OutputStream outputStream = new FileOutputStream(new File(mModsFolder, fileName))){
                if(inputStream == null) throw new IOException("Failed to open mod input stream");
                IOUtils.copy(inputStream, outputStream);
        }catch (IOException e) {
            Tools.showErrorRemote(e);
        }
        Tools.runOnUiThread(()->{
            if(!isDetached()) refreshModsList();
        });
    }

    private void refreshModsList() {
        mProgressBar.setVisibility(View.VISIBLE);
        MojoApplication.sExecutorService.execute(()->{
            Instance instance = Instances.loadSelectedInstance();
            List<ModInfo> modInfoList = ModScanner.findMods(mModsFolder, instance.platform);
            Tools.runOnUiThread(()->{
                if(isRemoving() || isDetached()) return;
                ModAdapter modAdapter = new ModAdapter(modInfoList, instance.platform);
                mModRecyclerView.setAdapter(modAdapter);
                modAdapter.performSearch(m->true, mProgressBar);
            });
        });
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        mModsFolder = new File(Instances.loadSelectedInstance().getGameDirectory(), "mods");
        mModRecyclerView = view.findViewById(R.id.mod_manager_recyclerview);
        mProgressBar = view.findViewById(R.id.search_mod_progressbar);
        View mOverlay = view.findViewById(R.id.search_mod_overlay);
        Button mImportButton = view.findViewById(R.id.mod_manager_import_button);
        mImportButton.setOnClickListener(v->{
            mModImportLauncher.launch(null);
        });
        mSearchEditText = view.findViewById(R.id.search_mod_edittext);
        mSearchEditText.setOnEditorActionListener(((textView, i, keyEvent) -> {
            String searchText = mSearchEditText.getText().toString().toLowerCase();
            ModAdapter modAdapter = (ModAdapter) mModRecyclerView.getAdapter();
            if(modAdapter != null) {
                modAdapter.performSearch(m->m.searchTerms.contains(searchText), mProgressBar);
            }
            return true;
        }));
        mModRecyclerView.setLayoutManager(new LinearLayoutManager(view.getContext()));
        Tools.setupOverlayView(mModRecyclerView, mOverlay);
        refreshModsList();
    }
}
