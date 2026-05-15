package net.kdt.pojavlaunch.fragments;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Rect;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.util.LruCache;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.OvershootInterpolator;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.kdt.mcgui.ProgressLayout;

import net.ashmeet.hyperlauncher.R;
import net.kdt.pojavlaunch.PojavApplication;
import net.kdt.pojavlaunch.Tools;
import net.kdt.pojavlaunch.instances.Instance;
import net.kdt.pojavlaunch.instances.Instances;
import net.kdt.pojavlaunch.modloaders.modpacks.api.ApiHandler;
import net.kdt.pojavlaunch.utils.DownloadUtils;
import net.kdt.pojavlaunch.utils.FileUtils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;



public class ContentInstallerFragment extends Fragment {
    public static final String TAG = "ContentInstallerFrag";

    private final ApiHandler mModrinthApi = new ApiHandler("https://api.modrinth.com/v2");
    private final AtomicInteger mSearchToken = new AtomicInteger(0);
    private final LruCache<String, Bitmap> mIconCache = new LruCache<>(64);

    private ContentType mContentType = ContentType.MODS;
    private EditText mSearch;
    private ProgressBar mLoading;
    private TextView mStatus;

    private View mMainUi;
    private View mFilterVersion;
    private View mFilterLoader;
    private TextView mFilterVersionLabel;
    private TextView mFilterLoaderLabel;

    private ProjectAdapter mProjectAdapter;
    private RecyclerView mList;
    private List<ProjectItem> mLastSearchResults = new ArrayList<>();

    // Versions UI
    private View mVersionsUi;
    private TextView mVersionsTitle;
    private TextView mVersionsDesc;
    private ImageView mVersionsIcon;
    private EditText mVersionsFilter;
    @Nullable private VersionAdapter mVersionsAdapter;

    // Search filters
    @Nullable private String mVersionFilter;
    @Nullable private String mLoaderFilter;

    // Instance info for highlighting
    @Nullable private String mInstanceVersion;
    @Nullable private String mInstanceLoader;

    private boolean mShowingVersions = false;

    // Highlight colors
    private int mColorCompatibleBg;
    private int mColorCompatibleText;
    private int mColorCompatibleBadge;

    private final ActivityResultLauncher<String> mPickLocalContent =
            registerForActivityResult(new ActivityResultContracts.GetContent(), this::installLocalUriToInstance);

    public ContentInstallerFragment() {
        super(R.layout.content_installer_layout);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        mColorCompatibleBg    = ContextCompat.getColor(requireContext(), R.color.compatible_version_bg);
        mColorCompatibleText  = ContextCompat.getColor(requireContext(), R.color.compatible_version_text);
        mColorCompatibleBadge = ContextCompat.getColor(requireContext(), R.color.compatible_version_badge);

        mMainUi  = view.findViewById(R.id.content_main_ui);
        mSearch  = view.findViewById(R.id.content_search);
        mLoading = view.findViewById(R.id.content_loading);
        mStatus  = view.findViewById(R.id.content_status);

        mFilterVersion = view.findViewById(R.id.content_filter_version);
        mFilterLoader  = view.findViewById(R.id.content_filter_loader);

        if (mFilterVersion instanceof TextView) mFilterVersionLabel = (TextView) mFilterVersion;
        if (mFilterLoader instanceof TextView)  mFilterLoaderLabel  = (TextView) mFilterLoader;

        RadioGroup typeGroup = view.findViewById(R.id.content_type_group);
        if (typeGroup != null) {
            typeGroup.check(R.id.content_type_mods);
            typeGroup.setOnCheckedChangeListener((group, checkedId) -> {
                if (checkedId == R.id.content_type_mods)          mContentType = ContentType.MODS;
                else if (checkedId == R.id.content_type_shaders)  mContentType = ContentType.SHADERS;
                else if (checkedId == R.id.content_type_resources) mContentType = ContentType.RESOURCES;
                updateFilterButtons();
                triggerSearch();
            });
        }

        View openDownloads = view.findViewById(R.id.content_open_downloads);

        if (openDownloads != null) {
            openDownloads.setOnClickListener(v -> {
                requireActivity().getSupportFragmentManager()
                        .beginTransaction()
                        .replace(R.id.container_fragment, new DirectoryManagerFragment())
                        .addToBackStack(null)
                        .commit();
            });
        }

        View installLocal = view.findViewById(R.id.content_install_local);
        if (installLocal != null) {
            installLocal.setOnClickListener(v -> mPickLocalContent.launch("*/*"));
        }

        mList = view.findViewById(R.id.content_list);

        if (mList != null) {
            mList.setLayoutManager(new LinearLayoutManager(requireContext()));
            int spacing = dpToPx(6);
            mList.addItemDecoration(new RecyclerView.ItemDecoration() {
                @Override
                public void getItemOffsets(@NonNull Rect outRect,
                                           @NonNull View view,
                                           @NonNull RecyclerView parent,
                                           @NonNull RecyclerView.State state) {
                    outRect.bottom = spacing;
                }
            });
            mProjectAdapter = new ProjectAdapter();
            mList.setAdapter(mProjectAdapter);
        }

        // Versions UI setup
        mVersionsUi     = view.findViewById(R.id.content_versions_ui);
        mVersionsTitle  = view.findViewById(R.id.content_versions_title);
        mVersionsDesc   = view.findViewById(R.id.content_versions_desc);
        mVersionsIcon   = view.findViewById(R.id.content_versions_icon);
        mVersionsFilter = view.findViewById(R.id.content_versions_filter);

        View backButton = view.findViewById(R.id.content_versions_back);
        if (backButton != null) backButton.setOnClickListener(v -> hideVersionsPanel());

        if (mVersionsFilter != null) {
            mVersionsFilter.addTextChangedListener(new TextWatcher() {
                @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
                @Override public void afterTextChanged(Editable s) {
                    if (mVersionsAdapter != null) mVersionsAdapter.filter(String.valueOf(s));
                }
            });
        }

        if (mSearch != null) {
            mSearch.addTextChangedListener(new TextWatcher() {
                @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
                @Override public void afterTextChanged(Editable s) { triggerSearch(); }
            });
        }

        initFilters();
        if (mFilterVersion != null) mFilterVersion.setOnClickListener(v -> showVersionFilterDialog());
        if (mFilterLoader != null) mFilterLoader.setOnClickListener(v -> showLoaderFilterDialog());

        triggerSearch();
    }

    private void setLoading(boolean loading) {
        if (mLoading != null) mLoading.setVisibility(loading ? View.VISIBLE : View.GONE);
        if (loading) setStatusText("");
    }

    private void hideVersionsPanel() {
        mShowingVersions = false;
        if (mVersionsUi != null) mVersionsUi.setVisibility(View.GONE);
        if (mMainUi != null) mMainUi.setVisibility(View.VISIBLE);

        mVersionsAdapter = null;
        if (mList != null) {
            mList.setAdapter(mProjectAdapter);
            mList.setVisibility(View.VISIBLE);
        }
        setStatusText(mLastSearchResults.isEmpty() ? "No results" : "");
        setLoading(false);
    }

    private void applyBounceAnimation(View view) {
        view.setScaleX(0.9f);
        view.setScaleY(0.9f);
        view.setAlpha(0f);
        view.animate()
                .scaleX(1f)
                .scaleY(1f)
                .alpha(1f)
                .setDuration(350)
                .setInterpolator(new OvershootInterpolator(1.4f))
                .start();
    }

    private void showVersionsPanel(@NonNull String title, @NonNull ArrayList<VersionItem> items) {
        mShowingVersions = true;
        if (mMainUi != null) mMainUi.setVisibility(View.GONE);
        if (mVersionsUi != null) {
            mVersionsUi.setVisibility(View.VISIBLE);
            applyBounceAnimation(mVersionsUi);
        }

        if (mVersionsTitle != null) mVersionsTitle.setText(title);
        if (mVersionsFilter != null) mVersionsFilter.setText("");

        // Use instance info for highlighting
        mVersionsAdapter = new VersionAdapter(items, mInstanceVersion, mInstanceLoader);
        if (mList != null) {
            mList.setAdapter(mVersionsAdapter);
            mList.setVisibility(View.VISIBLE);
            applyBounceAnimation(mList);
        }
        setLoading(false);
    }

    private void showVersionPicker(@NonNull ProjectItem item) {
        int token = mSearchToken.incrementAndGet();

        // Immediate UI swap
        mShowingVersions = true;
        if (mMainUi != null) mMainUi.setVisibility(View.GONE);
        if (mVersionsUi != null) {
            mVersionsUi.setVisibility(View.VISIBLE);
        }
        if (mVersionsTitle != null) mVersionsTitle.setText(item.title);
        if (mVersionsDesc != null) mVersionsDesc.setText(item.description);
        if (mVersionsIcon != null) {
            Bitmap icon = item.iconUrl != null ? mIconCache.get(item.iconUrl) : null;
            if (icon != null) mVersionsIcon.setImageBitmap(icon);
            else mVersionsIcon.setImageResource(R.drawable.ic_px_java);
        }
        if (mVersionsFilter != null) mVersionsFilter.setText("");

        // Hide list and show loading
        if (mList != null) mList.setVisibility(View.GONE);
        setLoading(true);
        setStatusText("Loading versions for " + item.title + "…");

        PojavApplication.sExecutorService.execute(() -> {
            try {
                JsonArray raw = mModrinthApi.get("project/" + item.projectId + "/version", JsonArray.class);
                final ArrayList<VersionItem> versionItems = parseVersions(raw);

                Tools.runOnUiThread(() -> {
                    if (token != mSearchToken.get()) return;
                    setLoading(false);
                    if (versionItems == null || versionItems.isEmpty()) {
                        Toast.makeText(requireContext(), "No downloadable versions found", Toast.LENGTH_LONG).show();
                        hideVersionsPanel();
                        return;
                    }
                    showVersionsPanel(item.title, versionItems);
                    setStatusText("");
                });
            } catch (Exception e) {
                Log.e(TAG, "Error fetching versions", e);
                Tools.runOnUiThread(() -> {
                    if (token != mSearchToken.get()) return;
                    setLoading(false);
                    setStatusText("Failed to load versions");
                    Tools.showError(requireContext(), e);
                    hideVersionsPanel();
                });
            }
        });
    }

    private class VersionAdapter extends RecyclerView.Adapter<VersionViewHolder> {
        private final ArrayList<VersionItem> mAll;
        private ArrayList<VersionItem> mFiltered;
        @Nullable private final String mCompatVersion;
        @Nullable private final String mCompatLoader;

        VersionAdapter(@NonNull ArrayList<VersionItem> items, @Nullable String compatVersion, @Nullable String compatLoader) {
            mAll = new ArrayList<>(items);
            mFiltered = new ArrayList<>(items);
            mCompatVersion = compatVersion;
            mCompatLoader = compatLoader;
            Log.d(TAG, "VersionAdapter: compatVersion=" + compatVersion + ", compatLoader=" + compatLoader);
        }

        void filter(@NonNull String query) {
            if (query.isEmpty()) {
                mFiltered = new ArrayList<>(mAll);
            } else {
                String q = query.toLowerCase();
                mFiltered = new ArrayList<>();
                for (VersionItem vi : mAll) {
                    if (vi.label.toLowerCase().contains(q)) mFiltered.add(vi);
                }
            }
            notifyDataSetChanged();
        }

        @NonNull @Override
        public VersionViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View card = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_modrinth_project, parent, false);
            return new VersionViewHolder(card);
        }

        @Override
        public void onBindViewHolder(@NonNull VersionViewHolder holder, int position) {
            VersionItem item = mFiltered.get(position);
            boolean compatible = false;

            // Highlight logic
            if (mCompatVersion != null) {
                for (String gv : item.gameVersions) {
                    if (gv == null) continue;
                    String g = gv.toLowerCase();
                    String compat = mCompatVersion.toLowerCase();
                    if (g.equals(compat) || g.startsWith(compat) || g.contains(compat) || compat.contains(g)) {
                        compatible = true;
                        break;
                    }
                }
            }

            if (compatible && mCompatLoader != null && !item.loaders.isEmpty()) {
                boolean loaderMatch = false;
                for (String l : item.loaders) {
                    if (mCompatLoader.equalsIgnoreCase(l)) {
                        loaderMatch = true;
                        break;
                    }
                }
                compatible = loaderMatch;
            }

            holder.bind(item, compatible);
            holder.itemView.setOnClickListener(v -> {
                hideVersionsPanel();
                downloadVersion(item);
            });
        }

        @Override public int getItemCount() { return mFiltered.size(); }
    }

    private class VersionViewHolder extends RecyclerView.ViewHolder {
        private final ImageView mIcon;
        private final TextView mTitle;
        private final TextView mDesc;
        private final ImageView mDot;

        VersionViewHolder(@NonNull View itemView) {
            super(itemView);
            mIcon  = itemView.findViewById(R.id.modrinth_icon);
            mTitle = itemView.findViewById(R.id.modrinth_title);
            mDesc  = itemView.findViewById(R.id.modrinth_desc);
            mDot   = itemView.findViewById(R.id.modrinth_dot);
        }

        void bind(@NonNull VersionItem item, boolean compatible) {
            mTitle.setText(item.label);

            String info = (item.loaders.isEmpty() ? "" : (String.join(", ", item.loaders) + "  •  "))
                    + (item.gameVersions.isEmpty() ? "Unknown version" : String.join(", ", item.gameVersions));
            mDesc.setText(info);

            if (compatible) {
                GradientDrawable gd = new GradientDrawable();
                gd.setColor(mColorCompatibleBg);
                gd.setCornerRadius(getResources().getDimension(R.dimen._12sdp));
                itemView.setBackground(gd);

                mTitle.setTextColor(mColorCompatibleText);
                mDesc.setTextColor(mColorCompatibleText);

                mIcon.setImageResource(R.drawable.ic_px_download);
                mIcon.setColorFilter(mColorCompatibleText);

                GradientDrawable badge = new GradientDrawable();
                badge.setColor(mColorCompatibleBadge);
                badge.setCornerRadius(dpToPx(8));
                mIcon.setBackground(badge);
            } else {
                itemView.setBackgroundResource(R.drawable.preference_item_background);
                mTitle.setTextColor(ContextCompat.getColor(requireContext(), R.color.primary_text));
                mDesc.setTextColor(ContextCompat.getColor(requireContext(), R.color.secondary_text));
                mIcon.setImageResource(R.drawable.ic_px_download);
                mIcon.setColorFilter(null);
                mIcon.setBackground(null);
            }
        }
    }

    private int dpToPx(int dp) {
        return Math.round(dp * getResources().getDisplayMetrics().density);
    }

    private void initFilters() {
        Instance inst = Instances.loadSelectedInstance();
        if (inst == null) return;
        String iv = inst.versionId;
        if (iv == null) return;

        String[] parts = iv.split("-");

        mInstanceVersion = null;
        mInstanceLoader = null;

        for (int i = parts.length - 1; i >= 0; i--) {
            String part = parts[i];

            if (part.matches("\\d+\\.\\d+(\\.\\d+)?")) {
                mInstanceVersion = part;
                mVersionFilter = part;
                break;
            }
        }

        if (mInstanceVersion == null && parts.length > 0) {
            mInstanceVersion = parts[0];
        }

        String ivLower = iv.toLowerCase();
        if (ivLower.contains("fabric")) mInstanceLoader = "fabric";
        else if (ivLower.contains("forge")) mInstanceLoader = "forge";
        else if (ivLower.contains("quilt")) mInstanceLoader = "quilt";
        else if (ivLower.contains("neoforge")) mInstanceLoader = "neoforge";

        mVersionFilter = mInstanceVersion;
        mLoaderFilter = mInstanceLoader;

        Log.d(TAG, "initFilters: id=" + iv + ", instanceVersion=" + mInstanceVersion + ", instanceLoader=" + mInstanceLoader);
        updateFilterButtons();
    }

    private void updateFilterButtons() {
        String v = (mVersionFilter == null) ? "Any" : mVersionFilter;
        if (mFilterVersionLabel != null) mFilterVersionLabel.setText("Version: " + v);

        String l = (mLoaderFilter == null) ? "Any" : mLoaderFilter;
        if (mFilterLoaderLabel != null) mFilterLoaderLabel.setText("Loader: " + l);

        boolean modsOnly = mContentType == ContentType.MODS;
        if (mFilterLoader != null) mFilterLoader.setEnabled(modsOnly);
    }

    private void showVersionFilterDialog() {
        ArrayList<String> options = new ArrayList<>();
        options.add("Any");

        if (mInstanceVersion != null && !options.contains(mInstanceVersion)) {
            options.add(mInstanceVersion);
        }

        CharSequence[] labels = options.toArray(new CharSequence[0]);
        new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                .setTitle("Minecraft version")
                .setItems(labels, (d, which) -> {
                    String picked = options.get(which);
                    mVersionFilter = "Any".equals(picked) ? null : picked;
                    updateFilterButtons();
                    triggerSearch();
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    private void showLoaderFilterDialog() {
        if (mContentType != ContentType.MODS) return;
        String[] options = {"Any", "fabric", "forge", "quilt", "neoforge"};
        CharSequence[] labels = {"Any", "Fabric", "Forge", "Quilt", "NeoForge"};
        new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                .setTitle("Mod loader")
                .setItems(labels, (d, which) -> {
                    String picked = options[which];
                    mLoaderFilter = "Any".equals(picked) ? null : picked;
                    updateFilterButtons();
                    triggerSearch();
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    private File getDownloadsDir() {
        File base = Tools.DIR_CACHE != null ? Tools.DIR_CACHE : requireContext().getCacheDir();
        File downloads = new File(base, "downloads");
        FileUtils.ensureDirectorySilently(downloads);
        return downloads;
    }

    private File getTargetDir() {
        Instance instance = Instances.loadSelectedInstance();
        if (instance == null) return getDownloadsDir();

        File base;
        if (instance.sharedData) {
            base = new File(Tools.DIR_GAME_NEW);
        } else {
            base = instance.getGameDirectory();
            File dotMc = new File(base, ".minecraft");
            if (dotMc.exists() && dotMc.isDirectory()) base = dotMc;
        }

        String subfolder;
        switch (mContentType) {
            case MODS: subfolder = "mods"; break;
            case SHADERS: subfolder = "shaderpacks"; break;
            case RESOURCES: subfolder = "resourcepacks"; break;
            default: return getDownloadsDir();
        }
        File target = new File(base, subfolder);
        if (!target.exists()) {
            if (!target.mkdirs()) {
                Log.e(TAG, "Failed to create directory: " + target.getAbsolutePath());
                return getDownloadsDir();
            }
        }
        return target;
    }

    private void installLocalUriToInstance(@Nullable Uri uri) {
        if (uri == null) return;
        if (Instances.loadSelectedInstance() == null) {
            Toast.makeText(requireContext(), R.string.no_instance, Toast.LENGTH_LONG).show();
            return;
        }
        // ContentInstallerDialogFragment missing, so we ignore for now or show Toast
        Toast.makeText(requireContext(), "Local install not supported yet", Toast.LENGTH_SHORT).show();
    }

    private void triggerSearch() {
        if (mShowingVersions) hideVersionsPanel();

        int token = mSearchToken.incrementAndGet();
        String query = String.valueOf(mSearch.getText()).trim();
        setLoading(true);

        PojavApplication.sExecutorService.execute(() -> {
            try {
                List<ProjectItem> results = searchProjects(query);
                Tools.runOnUiThread(() -> {
                    if (token != mSearchToken.get()) return;
                    mProjectAdapter.setItems(results);
                    mLastSearchResults = results;
                    setLoading(false);
                    setStatusText(results.isEmpty() ? "No results" : "");
                });
            } catch (Exception e) {
                Tools.runOnUiThread(() -> {
                    if (token != mSearchToken.get()) return;
                    setLoading(false);
                    setStatusText("Failed to load");
                    Tools.showError(requireContext(), e);
                });
            }
        });
    }

    private List<ProjectItem> searchProjects(@NonNull String query) {
        HashMap<String, Object> params = new HashMap<>();
        params.put("query", query);
        params.put("limit", 50);
        params.put("index", "relevance");
        params.put("facets", buildFacets());

        JsonObject response = mModrinthApi.get("search", params, JsonObject.class);
        if (response == null) return new ArrayList<>();
        JsonArray hits = response.getAsJsonArray("hits");
        if (hits == null) return new ArrayList<>();

        ArrayList<ProjectItem> items = new ArrayList<>(hits.size());
        for (int i = 0; i < hits.size(); i++) {
            JsonObject hit = hits.get(i).getAsJsonObject();
            String id = hit.has("project_id") ? hit.get("project_id").getAsString() : null;
            String title = hit.has("title") ? hit.get("title").getAsString() : "(untitled)";
            String desc = hit.has("description") ? hit.get("description").getAsString() : "";
            String iconUrl = (hit.has("icon_url") && !hit.get("icon_url").isJsonNull())
                    ? hit.get("icon_url").getAsString() : null;
            if (id != null) items.add(new ProjectItem(id, title, desc, iconUrl));
        }
        return items;
    }

    private String buildFacets() {
        StringBuilder sb = new StringBuilder("[");
        sb.append(String.format("[\"project_type:%s\"]", mContentType.projectType));
        if (mVersionFilter != null)
            sb.append(String.format(",[\"versions:%s\"]", mVersionFilter));
        if (mContentType == ContentType.MODS && mLoaderFilter != null)
            sb.append(String.format(",[\"categories:%s\"]", mLoaderFilter));
        sb.append("]");
        return sb.toString();
    }

    /** Set status text in the fragment's internal UI */
    private void setStatusText(@Nullable String text) {
        if (mStatus != null) {
            mStatus.setText(text == null ? "" : text);
        }
    }

    /** Set status in the global progress layout for background-capable tasks */
    private void setGlobalStatus(@Nullable String text) {
        if (text == null || text.isEmpty()) {
            ProgressLayout.clearProgress(ProgressLayout.CONTENT_INSTALL);
        } else {
            ProgressLayout.setProgress(ProgressLayout.CONTENT_INSTALL, 0, text);
        }
    }

    private void downloadLatest(@NonNull ProjectItem item) {
        setStatusText("Loading latest…");
        int token = mSearchToken.get();
        PojavApplication.sExecutorService.execute(() -> {
            try {
                JsonArray raw = mModrinthApi.get("project/" + item.projectId + "/version", JsonArray.class);
                ArrayList<VersionItem> versionItems = parseVersions(raw);
                if (versionItems == null || versionItems.isEmpty()) {
                    Tools.runOnUiThread(() -> {
                        if (token != mSearchToken.get()) return;
                        setStatusText("");
                        Toast.makeText(requireContext(), "No downloadable versions found", Toast.LENGTH_LONG).show();
                    });
                    return;
                }
                VersionItem best = versionItems.get(0);
                if (mInstanceVersion != null) {
                    for (VersionItem vi : versionItems) {
                        for (String gv : vi.gameVersions) {
                            if (gv == null) continue;
                            String g = gv.toLowerCase();
                            String compat = mInstanceVersion.toLowerCase();
                            if (g.equals(compat) || g.startsWith(compat) || g.contains(compat) || compat.contains(g)) {
                                best = vi; break;
                            }
                        }
                    }
                }
                final VersionItem toDownload = best;
                Tools.runOnUiThread(() -> {
                    if (token != mSearchToken.get()) return;
                    setStatusText("");
                    downloadVersion(toDownload);
                });
            } catch (Exception e) {
                Tools.runOnUiThread(() -> {
                    if (token != mSearchToken.get()) return;
                    setLoading(false);
                    setStatusText("Failed to load");
                    Tools.showError(requireContext(), e);
                });
            }
        });
    }

    @Nullable
    private static Bitmap downloadIcon(@NonNull String url) {
        try (InputStream in = new URL(url).openStream()) {
            return BitmapFactory.decodeStream(in);
        } catch (IOException e) {
            return null;
        }
    }

    private ArrayList<VersionItem> parseVersions(@Nullable JsonArray versions) {
        ArrayList<VersionItem> items = new ArrayList<>();
        if (versions == null) return items;
        for (int i = 0; i < versions.size(); i++) {
            JsonObject v = versions.get(i).getAsJsonObject();
            if (v == null) continue;
            String name = v.has("name") ? v.get("name").getAsString() : "Version";

            ArrayList<String> gameVersions = new ArrayList<>();
            if (v.has("game_versions") && v.get("game_versions").isJsonArray()) {
                JsonArray arr = v.getAsJsonArray("game_versions");
                for (int j = 0; j < arr.size(); j++) gameVersions.add(arr.get(j).getAsString());
            }

            ArrayList<String> loaders = new ArrayList<>();
            if (v.has("loaders") && v.get("loaders").isJsonArray()) {
                JsonArray arr = v.getAsJsonArray("loaders");
                for (int j = 0; j < arr.size(); j++) loaders.add(arr.get(j).getAsString());
            }

            String url = null, filename = null;
            if (v.has("files") && v.get("files").isJsonArray()) {
                JsonArray files = v.getAsJsonArray("files");
                if (files.size() > 0) {
                    JsonObject f = files.get(0).getAsJsonObject();
                    if (f != null) {
                        if (f.has("url"))      url      = f.get("url").getAsString();
                        if (f.has("filename")) filename = f.get("filename").getAsString();
                    }
                }
            }
            if (url == null) continue;

            items.add(new VersionItem(name, url, filename, gameVersions, loaders));
        }
        return items;
    }

    private void downloadVersion(@NonNull VersionItem version) {
        File targetDir = getTargetDir();
        String fileName = (version.fileName != null && !version.fileName.trim().isEmpty())
                ? version.fileName : "download";
        File target = new File(targetDir, fileName);

        Toast.makeText(requireContext(), "Downloading to " + targetDir.getName() + "...", Toast.LENGTH_SHORT).show();
        setGlobalStatus("Downloading: " + fileName);

        int token = mSearchToken.get();
        PojavApplication.sExecutorService.execute(() -> {
            try {
                DownloadUtils.downloadFile(version.url, target);
                Tools.runOnUiThread(() -> {
                    if (token != mSearchToken.get()) return;
                    setGlobalStatus("");
                    Toast.makeText(requireContext(),
                            "Saved: " + target.getName(), Toast.LENGTH_LONG).show();
                });
            } catch (IOException e) {
                Tools.runOnUiThread(() -> {
                    if (token != mSearchToken.get()) return;
                    setGlobalStatus("Download failed");
                    Tools.showError(requireContext(), e);
                });
            }
        });
    }

    private enum ContentType {
        MODS("mod"), SHADERS("shader"), RESOURCES("resourcepack");
        final String projectType;
        ContentType(String t) { projectType = t; }
    }

    private static class ProjectItem {
        final String projectId, title, description;
        @Nullable final String iconUrl;
        ProjectItem(String id, String title, String desc, @Nullable String icon) {
            projectId = id; this.title = title; description = desc; iconUrl = icon;
        }
    }

    private static class VersionItem {
        final String label, url;
        @Nullable final String fileName;
        final ArrayList<String> gameVersions;
        final ArrayList<String> loaders;
        VersionItem(String label, String url, @Nullable String fileName,
                    @NonNull ArrayList<String> gameVersions, @NonNull ArrayList<String> loaders) {
            this.label = label; this.url = url;
            this.fileName = fileName; this.gameVersions = gameVersions;
            this.loaders = loaders;
        }
    }

    private class ProjectAdapter extends RecyclerView.Adapter<ProjectViewHolder> {
        private final List<ProjectItem> mItems = new ArrayList<>();

        void setItems(@NonNull List<ProjectItem> items) {
            mItems.clear();
            mItems.addAll(items);
            notifyDataSetChanged();
        }

        @NonNull @Override
        public ProjectViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View item = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_modrinth_project, parent, false);
            return new ProjectViewHolder(item);
        }

        @Override
        public void onBindViewHolder(@NonNull ProjectViewHolder holder, int position) {
            ProjectItem item = mItems.get(position);
            Bitmap icon = item.iconUrl != null ? mIconCache.get(item.iconUrl) : null;
            holder.bind(item, icon);
            holder.itemView.setOnClickListener(v -> showVersionPicker(item));
            holder.setIconClickListener(v -> downloadLatest(item));

            if (icon == null && item.iconUrl != null) {
                holder.setIconTag(item.iconUrl);
                PojavApplication.sExecutorService.execute(() -> {
                    Bitmap downloaded = downloadIcon(item.iconUrl);
                    if (downloaded == null) return;
                    mIconCache.put(item.iconUrl, downloaded);
                    Tools.runOnUiThread(() -> {
                        if (!item.iconUrl.equals(holder.getIconTag())) return;
                        holder.bind(item, downloaded);
                    });
                });
            } else {
                holder.setIconTag(null);
            }
        }

        @Override public int getItemCount() { return mItems.size(); }
    }

    private static class ProjectViewHolder extends RecyclerView.ViewHolder {
        private final ImageView mIcon;
        private final TextView mTitle, mDesc;
        private final ImageView mDot;

        ProjectViewHolder(@NonNull View itemView) {
            super(itemView);
            mIcon  = itemView.findViewById(R.id.modrinth_icon);
            mTitle = itemView.findViewById(R.id.modrinth_title);
            mDesc  = itemView.findViewById(R.id.modrinth_desc);
            mDot   = itemView.findViewById(R.id.modrinth_dot);
        }

        void setIconClickListener(@NonNull View.OnClickListener l) { mIcon.setOnClickListener(l); }
        void setIconTag(@Nullable String tag) { mIcon.setTag(tag); }
        @Nullable String getIconTag() {
            Object t = mIcon.getTag(); return t instanceof String ? (String) t : null;
        }

        void bind(@NonNull ProjectItem item, @Nullable Bitmap icon) {
            mTitle.setText(item.title);
            mDesc.setText(item.description);
            if (icon != null) mIcon.setImageBitmap(icon);
            else mIcon.setImageResource(R.drawable.ic_px_java);
            mIcon.clearColorFilter();
            if (mDot != null) mDot.setVisibility(View.GONE);
        }
    }
}
