package git.artdeell.mojo.fragments;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import git.artdeell.mojo.R;
import git.artdeell.mojo.Tools;
import git.artdeell.mojo.authenticator.accounts.SkinLibrary;
import git.artdeell.mojo.prefs.LauncherPreferences;
import git.artdeell.mojo.authenticator.accounts.Account;
import git.artdeell.mojo.authenticator.accounts.Accounts;
import git.artdeell.mojo.utils.FileUtils;
import git.artdeell.mojo.utils.ForkUi;
import git.artdeell.mojo.utils.McStyle;
import git.artdeell.mojo.utils.OnlineSkins;
import git.artdeell.mojo.authenticator.AuthType;
import git.artdeell.mojo.MojoApplication;
import git.artdeell.mojo.utils.SkinLibraryDialog;
import git.artdeell.mojo.utils.SkinModelView;

/**
 * Skin changer laid out like the Quick Skin screen: the skin list and an import area on the left,
 * the 3D model with Classic / Slim on the right, and the Import / Laby.net / Delete / Done row below.
 */
public class SkinsFragment extends Fragment {
    public static final String TAG = "SkinsFragment";
    private static final String PREF_CURRENT = "current_skin_file";
    private static final String PREF_TYPE = "skin_type_";
    private static final String PREF_SORT = "skin_sort";
    private static final String PREF_FAVORITES = "skin_favorites";
    private static final int SELECTED_BLUE = 0xFF2F66C8;
    private static final int MODEL_PANEL = 0xFF444444;

    private SkinModelView mModel;
    private EditText mSearch;
    private Button mSort;
    private String mQuery = "";
    private LinearLayout mList;
    private Button mClassic, mSlim, mUse, mDelete;
    private SkinLibrary.Entry mSelected;

    private final ActivityResultLauncher<String> mImport =
            registerForActivityResult(new ActivityResultContracts.GetContent(), (Uri uri) -> {
                if(uri == null) return;
                try {
                    mSelected = SkinLibrary.importSkin(requireContext(), uri);
                    refresh();
                } catch (Exception e) {
                    Toast.makeText(requireContext(), R.string.fork_skin_error, Toast.LENGTH_LONG).show();
                }
            });

    private String currentName() {
        return LauncherPreferences.DEFAULT_PREF.getString(PREF_CURRENT, null);
    }

    /** 0 = detect from the PNG, 1 = Classic, 2 = Slim */
    private int typeOverride(SkinLibrary.Entry e) {
        return LauncherPreferences.DEFAULT_PREF.getInt(PREF_TYPE + e.file.getName(), 0);
    }

    private boolean isSlim(SkinLibrary.Entry e, Bitmap bmp) {
        int o = typeOverride(e);
        if(o != 0) return o == 2;
        return bmp != null && SkinLibrary.isSlim(bmp);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        Context c = requireContext();
        FrameLayout frame = new FrameLayout(c);
        frame.setBackground(McStyle.dirt(c));
        View shade = new View(c);
        shade.setBackgroundColor(0x99000000);
        frame.addView(shade, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        LinearLayout root = new LinearLayout(c);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(ForkUi.dp(c, 14), ForkUi.dp(c, 8), ForkUi.dp(c, 14), ForkUi.dp(c, 10));
        frame.addView(root, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        TextView title = ForkUi.text(c, getString(R.string.fork_skins_change_title), 16, false);
        title.setGravity(Gravity.CENTER);
        root.addView(title, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        LinearLayout main = new LinearLayout(c);
        main.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams mainLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f);
        mainLp.topMargin = ForkUi.dp(c, 6);
        root.addView(main, mainLp);

        // --- left: name field, skin list, import area ---
        LinearLayout left = new LinearLayout(c);
        left.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams leftLp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1.5f);
        leftLp.rightMargin = ForkUi.dp(c, 10);
        main.addView(left, leftLp);

        LinearLayout nameRow = ForkUi.row(c);
        mSearch = new EditText(c);
        mSearch.setHint(R.string.fork_skins_search_hint);
        mSearch.setHintTextColor(0xFF777777);
        mSearch.setTextColor(Color.WHITE);
        mSearch.setTextSize(14);
        mSearch.setSingleLine(true);
        McStyle.text(mSearch);
        mSearch.setBackground(ForkUi.box(c, Color.BLACK, 0xFFAAAAAA, 2));
        mSearch.setPadding(ForkUi.dp(c, 8), 0, ForkUi.dp(c, 8), 0);
        mSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int st, int co, int af) {}
            @Override public void onTextChanged(CharSequence s, int st, int be, int co) {}
            @Override public void afterTextChanged(Editable e) {
                mQuery = e.toString().trim().toLowerCase();
                fillList();
            }
        });
        nameRow.addView(mSearch, new LinearLayout.LayoutParams(0, ForkUi.dp(c, 36), 1f));
        Button plus = ForkUi.button(c, "+", false);
        plus.setMinWidth(0);
        plus.setMinimumWidth(0);
        plus.setPadding(0, 0, 0, 0);
        plus.setOnClickListener(v -> mImport.launch("image/png"));
        LinearLayout.LayoutParams plusLp = new LinearLayout.LayoutParams(ForkUi.dp(c, 36), ForkUi.dp(c, 36));
        plusLp.leftMargin = ForkUi.dp(c, 6);
        nameRow.addView(plus, plusLp);
        left.addView(nameRow);

        mSort = ForkUi.button(c, "", false);
        mSort.setMinHeight(ForkUi.dp(c, 30));
        mSort.setMinimumHeight(ForkUi.dp(c, 30));
        mSort.setTextSize(12);
        mSort.setOnClickListener(v -> {
            int next = (sortMode() + 1) % 3;
            LauncherPreferences.DEFAULT_PREF.edit().putInt(PREF_SORT, next).apply();
            fillList();
        });
        LinearLayout.LayoutParams sortLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        sortLp.topMargin = ForkUi.dp(c, 4);
        left.addView(mSort, sortLp);

        ScrollView scroll = new ScrollView(c);
        mList = new LinearLayout(c);
        mList.setOrientation(LinearLayout.VERTICAL);
        scroll.addView(mList);
        LinearLayout.LayoutParams scrollLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f);
        scrollLp.topMargin = ForkUi.dp(c, 6);
        left.addView(scroll, scrollLp);

        TextView drop = ForkUi.text(c, getString(R.string.fork_skins_drop), 13, false);
        drop.setGravity(Gravity.CENTER);
        drop.setTextColor(0xFFCCCCCC);
        drop.setBackground(ForkUi.dashed(c, 0x33000000, 0xFF888888));
        drop.setOnClickListener(v -> mImport.launch("image/png"));
        LinearLayout.LayoutParams dropLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ForkUi.dp(c, 54));
        dropLp.topMargin = ForkUi.dp(c, 6);
        left.addView(drop, dropLp);

        // --- right: 3D model, Classic / Slim, Use ---
        LinearLayout right = new LinearLayout(c);
        right.setOrientation(LinearLayout.VERTICAL);
        right.setBackgroundColor(MODEL_PANEL);
        right.setPadding(ForkUi.dp(c, 6), ForkUi.dp(c, 6), ForkUi.dp(c, 6), ForkUi.dp(c, 6));
        main.addView(right, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1f));
        mModel = new SkinModelView(c);
        right.addView(mModel, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));

        LinearLayout types = ForkUi.row(c);
        mClassic = ForkUi.button(c, getString(R.string.fork_skins_classic), false);
        mSlim = ForkUi.button(c, getString(R.string.fork_skins_slim), false);
        LinearLayout.LayoutParams t1 = ForkUi.weight(1f);
        t1.rightMargin = ForkUi.dp(c, 3);
        LinearLayout.LayoutParams t2 = ForkUi.weight(1f);
        t2.leftMargin = ForkUi.dp(c, 3);
        types.addView(mClassic, t1);
        types.addView(mSlim, t2);
        right.addView(types);
        mUse = ForkUi.button(c, getString(R.string.fork_skins_apply), true);
        LinearLayout.LayoutParams useLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        useLp.topMargin = ForkUi.dp(c, 4);
        right.addView(mUse, useLp);

        // --- bottom row ---
        LinearLayout bottom = ForkUi.row(c);
        Button importBtn = ForkUi.button(c, getString(R.string.fork_skins_import_skin), false);
        Button namemc = ForkUi.button(c, getString(R.string.fork_skins_namemc), false);
        Button online = ForkUi.button(c, getString(R.string.fork_skins_online), false);
        mDelete = ForkUi.button(c, getString(R.string.fork_skins_delete), false);
        Button done = ForkUi.button(c, getString(R.string.fork_skins_done), false);
        Button[] all = {importBtn, online, namemc, mDelete, done};
        for(int i = 0; i < all.length; i++) {
            LinearLayout.LayoutParams lp = ForkUi.weight(1f);
            lp.leftMargin = ForkUi.dp(c, i == 0 ? 0 : 3);
            lp.rightMargin = ForkUi.dp(c, i == all.length - 1 ? 0 : 3);
            bottom.addView(all[i], lp);
        }
        LinearLayout.LayoutParams bottomLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        bottomLp.topMargin = ForkUi.dp(c, 8);
        root.addView(bottom, bottomLp);

        importBtn.setOnClickListener(v -> mImport.launch("image/png"));
        online.setOnClickListener(v -> askOnlineName());
        namemc.setOnClickListener(v -> Tools.openURL(requireActivity(), "https://namemc.com/minecraft-skins"));
        done.setOnClickListener(v -> requireActivity().getOnBackPressedDispatcher().onBackPressed());
        mDelete.setOnClickListener(v -> confirmDelete(mSelected));
        mUse.setOnClickListener(v -> {
            if(mSelected == null) return;
            Account account = Accounts.getCurrent();
            if(account != null && account.authType == AuthType.ELY_BY) {
                exportForEly(mSelected);
            } else if(account != null && account.isMicrosoft) {
                Bitmap bmp = SkinLibrary.load(mSelected);
                String model = getString(isSlim(mSelected, bmp) ? R.string.fork_skins_slim : R.string.fork_skins_classic);
                new AlertDialog.Builder(requireContext())
                        .setTitle(R.string.fork_skins_upload_title)
                        .setMessage(getString(R.string.fork_skins_upload_prompt, mSelected.name, model))
                        .setNegativeButton(android.R.string.cancel, null)
                        .setPositiveButton(R.string.fork_skins_apply, (d, w) -> useSelected())
                        .show();
            } else {
                useSelected();
            }
        });
        mClassic.setOnClickListener(v -> setType(1));
        mSlim.setOnClickListener(v -> setType(2));
        return frame;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        refresh();
    }

    private void setType(int type) {
        if(mSelected == null) return;
        LauncherPreferences.DEFAULT_PREF.edit().putInt(PREF_TYPE + mSelected.file.getName(), type).apply();
        refresh();
    }

    private void refresh() {
        Context c = requireContext();
        List<SkinLibrary.Entry> skins = SkinLibrary.list(c);
        SkinLibrary.Entry keep = null;
        for(SkinLibrary.Entry e : skins) {
            if(mSelected != null && e.file.equals(mSelected.file)) keep = e;
        }
        if(keep == null) {
            for(SkinLibrary.Entry e : skins) {
                if(e.file.getName().equals(currentName())) keep = e;
            }
        }
        if(keep == null && !skins.isEmpty()) keep = skins.get(0);
        mSelected = keep;

        boolean slim = false;
        if(mSelected != null) {
            Bitmap bmp = SkinLibrary.load(mSelected);
            slim = isSlim(mSelected, bmp);
            if(bmp != null) mModel.setSkin(bmp, slim);
        }
        mUse.setEnabled(mSelected != null);
        mDelete.setEnabled(mSelected != null);
        mClassic.setEnabled(mSelected != null);
        mSlim.setEnabled(mSelected != null);
        mClassic.setAlpha(!slim ? 1f : 0.6f);
        mSlim.setAlpha(slim ? 1f : 0.6f);

        fillList();
    }

    private int sortMode() {
        return LauncherPreferences.DEFAULT_PREF.getInt(PREF_SORT, 0);
    }

    private Set<String> favorites() {
        return new HashSet<>(LauncherPreferences.DEFAULT_PREF.getStringSet(PREF_FAVORITES, new HashSet<>()));
    }

    private void fillList() {
        Context c = getContext();
        if(c == null || mList == null) return;
        int mode = sortMode();
        mSort.setText(getString(R.string.fork_skins_sort, getString(
                mode == 0 ? R.string.fork_skins_sort_new : mode == 1 ? R.string.fork_skins_sort_old : R.string.fork_skins_sort_az)));
        Set<String> favs = favorites();
        List<SkinLibrary.Entry> shown = new ArrayList<>();
        for(SkinLibrary.Entry e : SkinLibrary.list(c)) {
            if(mQuery.isEmpty() || e.name.toLowerCase().contains(mQuery)) shown.add(e);
        }
        if(mode == 2) {
            Collections.sort(shown, (a, b) -> a.name.compareToIgnoreCase(b.name));
        } else {
            Collections.sort(shown, (a, b) -> Long.compare(b.file.lastModified(), a.file.lastModified()));
            if(mode == 1) Collections.reverse(shown);
        }
        // Favorites first, keeping the chosen order inside each group
        List<SkinLibrary.Entry> ordered = new ArrayList<>();
        for(SkinLibrary.Entry e : shown) if(favs.contains(e.file.getName())) ordered.add(e);
        for(SkinLibrary.Entry e : shown) if(!favs.contains(e.file.getName())) ordered.add(e);
        mList.removeAllViews();
        for(SkinLibrary.Entry entry : ordered) mList.addView(row(c, entry, favs.contains(entry.file.getName())));
    }

    /** Ely.by has no upload API for apps, so the skin is exported and the upload page is opened. */
    private void exportForEly(SkinLibrary.Entry entry) {
        Context context = requireContext();
        new AlertDialog.Builder(context)
                .setTitle(R.string.fork_skins_ely_title)
                .setMessage(R.string.fork_skins_ely_message)
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton(R.string.fork_skins_ely_go, (d, w) -> {
                    android.os.Handler main = new android.os.Handler(android.os.Looper.getMainLooper());
                    MojoApplication.sExecutorService.execute(() -> {
                        String error = null;
                        String file = null;
                        try {
                            file = OnlineSkins.exportToDownloads(context, entry.file);
                        } catch (Exception e) {
                            error = String.valueOf(e.getMessage());
                        }
                        String finalError = error, finalFile = file;
                        main.post(() -> {
                            if(!isAdded()) return;
                            if(finalError != null) {
                                Toast.makeText(context, getString(R.string.fork_skins_ely_error, finalError), Toast.LENGTH_LONG).show();
                            } else {
                                Toast.makeText(context, getString(R.string.fork_skins_ely_saved, finalFile), Toast.LENGTH_LONG).show();
                                LauncherPreferences.DEFAULT_PREF.edit().putString(PREF_CURRENT, entry.file.getName()).apply();
                                Tools.openURL(requireActivity(), OnlineSkins.ELY_UPLOAD_PAGE);
                            }
                        });
                    });
                })
                .show();
    }

    private void askOnlineName() {
        Context context = requireContext();
        EditText input = new EditText(context);
        input.setSingleLine(true);
        input.setHint(R.string.fork_skins_online_hint);
        Account account = Accounts.getCurrent();
        if(account != null && !account.isLocal()) input.setText(account.username);
        FrameLayout box = new FrameLayout(context);
        int pad = ForkUi.dp(context, 20);
        box.setPadding(pad, ForkUi.dp(context, 8), pad, 0);
        box.addView(input);
        new AlertDialog.Builder(context)
                .setTitle(R.string.fork_skins_online)
                .setMessage(R.string.fork_skins_online_message)
                .setView(box)
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton(R.string.fork_skins_online_get, (d, w) -> {
                    String name = input.getText().toString().trim();
                    if(name.isEmpty()) return;
                    android.os.Handler main = new android.os.Handler(android.os.Looper.getMainLooper());
                    Toast.makeText(context, R.string.fork_skins_online_loading, Toast.LENGTH_SHORT).show();
                    MojoApplication.sExecutorService.execute(() -> {
                        SkinLibrary.Entry entry = null;
                        String error = null;
                        try {
                            entry = OnlineSkins.download(context, name);
                        } catch (Exception e) {
                            error = String.valueOf(e.getMessage());
                        }
                        SkinLibrary.Entry finalEntry = entry;
                        String finalError = error;
                        main.post(() -> {
                            if(!isAdded()) return;
                            if(finalEntry != null) {
                                mSelected = finalEntry;
                                refresh();
                            } else {
                                Toast.makeText(context, getString(R.string.fork_skins_online_error, name, finalError), Toast.LENGTH_LONG).show();
                            }
                        });
                    });
                })
                .show();
    }

    private void useSelected() {
        if(mSelected == null) return;
        Bitmap bmp = SkinLibrary.load(mSelected);
        SkinLibraryDialog.apply(requireContext(), mSelected, isSlim(mSelected, bmp));
        LauncherPreferences.DEFAULT_PREF.edit().putString(PREF_CURRENT, mSelected.file.getName()).apply();
        refresh();
    }

    private void confirmDelete(SkinLibrary.Entry entry) {
        if(entry == null) return;
        new AlertDialog.Builder(requireContext())
                .setTitle(R.string.fork_skins_delete_title)
                .setMessage(getString(R.string.fork_skins_delete_confirm, entry.name))
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton(R.string.fork_skins_delete, (d, w) -> {
                    SkinLibrary.delete(entry);
                    Set<String> favs = favorites();
                    if(favs.remove(entry.file.getName())) {
                        LauncherPreferences.DEFAULT_PREF.edit().putStringSet(PREF_FAVORITES, favs).apply();
                    }
                    if(mSelected != null && mSelected.file.equals(entry.file)) mSelected = null;
                    refresh();
                })
                .show();
    }

    private void toggleFavorite(SkinLibrary.Entry entry) {
        Set<String> favs = favorites();
        if(!favs.remove(entry.file.getName())) favs.add(entry.file.getName());
        LauncherPreferences.DEFAULT_PREF.edit().putStringSet(PREF_FAVORITES, favs).apply();
        fillList();
    }

    private void rename(SkinLibrary.Entry entry) {
        Context c = requireContext();
        EditText input = new EditText(c);
        input.setText(entry.name);
        input.setSingleLine(true);
        new AlertDialog.Builder(c)
                .setTitle(R.string.fork_skins_rename_title)
                .setView(input)
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton(android.R.string.ok, (d, w) -> {
                    String name = FileUtils.escapeFileName(input.getText().toString().trim());
                    if(name.isEmpty()) return;
                    File target = new File(entry.file.getParentFile(), name + ".png");
                    if(target.exists() || !entry.file.renameTo(target)) {
                        Toast.makeText(c, R.string.fork_skins_rename_error, Toast.LENGTH_LONG).show();
                        return;
                    }
                    moveSettings(entry.file.getName(), target.getName());
                    mSelected = null;
                    refresh();
                })
                .show();
    }

    /** Keeps the favorite, type and "in use" marks when a skin file is renamed. */
    private void moveSettings(String from, String to) {
        android.content.SharedPreferences prefs = LauncherPreferences.DEFAULT_PREF;
        android.content.SharedPreferences.Editor ed = prefs.edit();
        if(from.equals(prefs.getString(PREF_CURRENT, null))) ed.putString(PREF_CURRENT, to);
        int type = prefs.getInt(PREF_TYPE + from, 0);
        if(type != 0) ed.putInt(PREF_TYPE + to, type);
        ed.remove(PREF_TYPE + from);
        Set<String> favs = favorites();
        if(favs.remove(from)) {
            favs.add(to);
            ed.putStringSet(PREF_FAVORITES, favs);
        }
        ed.apply();
    }

    private View row(Context c, SkinLibrary.Entry entry, boolean favorite) {
        boolean selected = mSelected != null && entry.file.equals(mSelected.file);
        boolean current = entry.file.getName().equals(currentName());
        LinearLayout row = ForkUi.row(c);
        row.setPadding(ForkUi.dp(c, 8), ForkUi.dp(c, 6), ForkUi.dp(c, 8), ForkUi.dp(c, 6));
        if(selected) row.setBackground(ForkUi.box(c, 0x552F66C8, SELECTED_BLUE, 2));

        Bitmap bmp = SkinLibrary.load(entry);
        ImageView face = new ImageView(c);
        if(bmp != null) face.setImageBitmap(faceOf(bmp));
        row.addView(face, new LinearLayout.LayoutParams(ForkUi.dp(c, 36), ForkUi.dp(c, 36)));

        LinearLayout texts = new LinearLayout(c);
        texts.setOrientation(LinearLayout.VERTICAL);
        texts.setPadding(ForkUi.dp(c, 10), 0, 0, 0);
        TextView name = ForkUi.text(c, (favorite ? "★ " : "") + entry.name, 14, false);
        if(favorite) name.setTextColor(0xFFFFD35A);
        name.setSingleLine(true);
        name.setEllipsize(android.text.TextUtils.TruncateAt.END);
        texts.addView(name);
        TextView sub = ForkUi.text(c, getString(isSlim(entry, bmp) ? R.string.fork_skins_slim : R.string.fork_skins_classic)
                + (current ? "  ·  " + getString(R.string.fork_installs_selected) : ""), 12, false);
        sub.setTextColor(current ? 0xFF7CD65A : 0xFFAAAAAA);
        texts.addView(sub);
        row.addView(texts, ForkUi.weight(1f));

        row.setOnClickListener(v -> {
            mSelected = entry;
            refresh();
        });
        row.setOnLongClickListener(v -> {
            String[] items = {getString(favorite ? R.string.fork_skins_unfavorite : R.string.fork_skins_favorite),
                    getString(R.string.fork_skins_rename), getString(R.string.fork_skins_delete)};
            new AlertDialog.Builder(c).setTitle(entry.name).setItems(items, (d, which) -> {
                if(which == 0) toggleFavorite(entry);
                else if(which == 1) rename(entry);
                else confirmDelete(entry);
            }).show();
            return true;
        });
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.bottomMargin = ForkUi.dp(c, 3);
        row.setLayoutParams(lp);
        return row;
    }

    /** The head (with hat layer) of a 64x64 skin, enlarged without smoothing. */
    private static Bitmap faceOf(Bitmap skin) {
        Bitmap out = Bitmap.createBitmap(64, 64, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(out);
        Paint paint = new Paint();
        paint.setFilterBitmap(false);
        Rect dst = new Rect(0, 0, 64, 64);
        canvas.drawBitmap(skin, new Rect(8, 8, 16, 16), dst, paint);
        canvas.drawBitmap(skin, new Rect(40, 8, 48, 16), dst, paint);
        return out;
    }
}
