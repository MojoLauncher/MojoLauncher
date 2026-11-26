package git.artdeell.mojo.fragments.modding;

import static git.artdeell.mojo.Tools.runOnUiThread;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import git.artdeell.mojo.R;
import git.artdeell.mojo.Tools;
import git.artdeell.mojo.instances.Instance;
import git.artdeell.mojo.instances.Instances;
import git.artdeell.mojo.modding.modplatforms.ModItemAdapter;
import git.artdeell.mojo.modding.modplatforms.api.CommonApi;
import git.artdeell.mojo.modding.modplatforms.api.ModdingApi;
import git.artdeell.mojo.modding.modplatforms.models.SearchFilters;
import git.artdeell.mojo.progresskeeper.ProgressKeeper;
import git.artdeell.mojo.progresskeeper.TaskCountListener;

public abstract class BaseSearchFragment extends Fragment implements ModItemAdapter.SearchResultCallback {
    private View overlay;

    protected Instance instance;
    protected ModdingApi moddingApi;
    protected SearchFilters searchFilters;
    protected ModItemAdapter modItemAdapter;

    protected EditText searchEditText;
    protected ImageButton filterButton;
    protected RecyclerView recyclerView;
    protected ProgressBar searchProgressBar;
    protected TextView statusTextView;
    protected Button importButton;
    private ColorStateList defaultTextColor;
    private TaskCountListener taskCountListener;

    protected BaseSearchFragment() {
        super(R.layout.fragment_mod_search);
    }

    protected abstract int getSearchHint();
    protected abstract int getNoResultsString();
    protected abstract int getErrorString();
    protected abstract ModItemAdapter.ModInstallHandler createInstallHandler();

    protected void prepareFilters(SearchFilters filters) {}
    protected abstract void setupImportButton(Button button);
    protected abstract void setupFilterButton(ImageButton button);

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        searchFilters = new SearchFilters();
        instance = Instances.loadSelectedInstance();
        prepareFilters(searchFilters);
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        moddingApi = new CommonApi(context.getString(R.string.curseforge_api_key));
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        overlay = view.findViewById(R.id.search_mod_overlay);
        // You can only access resources after attaching to current context
        modItemAdapter = new ModItemAdapter(getResources(), moddingApi, this, createInstallHandler());
        ProgressKeeper.addTaskCountListener(modItemAdapter);

        searchEditText = view.findViewById(R.id.search_mod_edittext);
        searchProgressBar = view.findViewById(R.id.search_mod_progressbar);
        recyclerView = view.findViewById(R.id.search_mod_list);
        statusTextView = view.findViewById(R.id.search_mod_status_text);
        filterButton = view.findViewById(R.id.search_mod_filter);

        defaultTextColor = statusTextView.getTextColors();

        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.setAdapter(modItemAdapter);

        searchEditText.setOnEditorActionListener((v, actionId, event) -> {
            searchMods(searchEditText.getText().toString());
            searchEditText.clearFocus();
            return false;
        });
        searchEditText.setHint(getSearchHint());
        Tools.setupOverlayView(recyclerView, overlay);
        importButton = view.findViewById(R.id.mineButton_import_local_modpack);
        setupImportButton(importButton);
        setupFilterButton(filterButton);

        taskCountListener = taskCount -> {
            runOnUiThread(() -> importButton.setEnabled(taskCount == 0));
            return false;
        };
        ProgressKeeper.addTaskCountListener(taskCountListener);

        searchMods(null);
    }

    @Override
    public void onSearchFinished() {
        searchProgressBar.setVisibility(View.GONE);
        statusTextView.setVisibility(View.GONE);
    }

    @Override
    public void onSearchError(int error) {
        searchProgressBar.setVisibility(View.GONE);
        statusTextView.setVisibility(View.VISIBLE);
        switch (error) {
            case ERROR_INTERNAL:
                statusTextView.setTextColor(Color.RED);
                statusTextView.setText(getErrorString());
                break;
            case ERROR_NO_RESULTS:
                statusTextView.setTextColor(defaultTextColor);
                statusTextView.setText(getNoResultsString());
                break;
        }
    }

    void searchMods(String name) {
        searchProgressBar.setVisibility(View.VISIBLE);
        searchFilters.name = name == null ? "" : name;
        modItemAdapter.performSearchQuery(searchFilters);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        ProgressKeeper.removeTaskCountListener(modItemAdapter);
        if (taskCountListener != null) { ProgressKeeper.removeTaskCountListener(taskCountListener); }
    }
}
