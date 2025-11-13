package net.kdt.pojavlaunch.fragments;

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

import net.kdt.pojavlaunch.PojavApplication;
import net.kdt.pojavlaunch.Tools;
import net.kdt.pojavlaunch.contracts.OpenDocumentWithExtension;
import net.kdt.pojavlaunch.instances.InstanceManager;
import net.kdt.pojavlaunch.modloaders.modmanager.ModAdapter;
import net.kdt.pojavlaunch.modloaders.modmanager.ModInfo;
import net.kdt.pojavlaunch.modloaders.modmanager.ModScanner;
import net.kdt.pojavlaunch.utils.FileUtils;

import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;

import git.artdeell.mojo.R;
import me.andreasmelone.basicmodinfoparser.modfile.DependencyChecker;
import me.andreasmelone.basicmodinfoparser.platform.Platform;

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
                PojavApplication.sExecutorService.execute(()->importMod(fileName, data, context));
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
        PojavApplication.sExecutorService.execute(()->{
            List<ModInfo> modInfoList = ModScanner.findMods(mModsFolder, Platform.NEOFORGE);
            Tools.runOnUiThread(()->{
                if(isRemoving() || isDetached()) return;
                ModAdapter modAdapter = new ModAdapter(modInfoList, Platform.NEOFORGE);
                mModRecyclerView.setAdapter(modAdapter);
                modAdapter.performSearch(m->true, mProgressBar);
            });
        });
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        mModsFolder = new File(InstanceManager.getSelectedListedInstance().getGameDirectory(), "mods");
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
