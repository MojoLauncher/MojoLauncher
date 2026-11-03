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
import git.artdeell.mojo.utils.SkinFigure;
import git.artdeell.mojo.authenticator.AuthType;
import git.artdeell.mojo.MojoApplication;
import git.artdeell.mojo.utils.SkinLibraryDialog;
import git.artdeell.mojo.utils.SkinModelView;

/**
 * Skin changer laid out like the official launcher: the current skin as a 3D model on the left and the
 * library (New skin + saved skins, each with a Use button when selected) on the right.
 */
public class SkinsFragment extends Fragment {
    public static final String TAG = "SkinsFragment";
    private static final String PREF_CURRENT = "current_skin_file";
    private static final String PREF_TYPE = "skin_type_";
    private static final String PREF_FAVORITES = "skin_favorites";
    private static final int PANEL = 0xFF2B2B2B;
    private static final int LINE = 0xFF4A4A4A;
    private static final int COLUMNS = 4;

    private SkinModelView mModel;
    private LinearLayout mGrid;
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
        LinearLayout root = new LinearLayout(c);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(PANEL);
        root.addView(ForkUi.pageHeader(c, requireActivity(), ForkUi.TAB_SKINS));

        LinearLayout main = new LinearLayout(c);
        main.setOrientation(LinearLayout.HORIZONTAL);
        root.addView(main, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));

        // --- left: Current ---
        LinearLayout left = new LinearLayout(c);
        left.setOrientation(LinearLayout.VERTICAL);
        left.setGravity(Gravity.CENTER_HORIZONTAL);
        left.setPadding(ForkUi.dp(c, 8), ForkUi.dp(c, 10), ForkUi.dp(c, 8), ForkUi.dp(c, 8));
        main.addView(left, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1f));
        TextView current = ForkUi.text(c, getString(R.string.fork_skins_current), 14, false);
        current.setGravity(Gravity.CENTER);
        left.addView(current, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        mModel = new SkinModelView(c);
        LinearLayout.LayoutParams modelLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f);
        modelLp.topMargin = ForkUi.dp(c, 6);
        left.addView(mModel, modelLp);

        View divider = new View(c);
        divider.setBackgroundColor(LINE);
        main.addView(divider, new LinearLayout.LayoutParams(ForkUi.dp(c, 1), ViewGroup.LayoutParams.MATCH_PARENT));

        // --- right: Library ---
        LinearLayout right = new LinearLayout(c);
        right.setOrientation(LinearLayout.VERTICAL);
        right.setPadding(ForkUi.dp(c, 10), ForkUi.dp(c, 10), ForkUi.dp(c, 10), ForkUi.dp(c, 4));
        main.addView(right, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 2.4f));

        LinearLayout top = ForkUi.row(c);
        TextView library = ForkUi.text(c, getString(R.string.fork_skins_library), 14, false);
        library.setGravity(Gravity.CENTER);
        top.addView(library, ForkUi.weight(1f));
        Button online = ForkUi.button(c, getString(R.string.fork_skins_online), false);
        online.setMinHeight(ForkUi.dp(c, 30));
        online.setMinimumHeight(ForkUi.dp(c, 30));
        online.setTextSize(12);
        online.setOnClickListener(v -> askOnlineName());
        Button namemc = ForkUi.button(c, getString(R.string.fork_skins_namemc), false);
        namemc.setMinHeight(ForkUi.dp(c, 30));
        namemc.setMinimumHeight(ForkUi.dp(c, 30));
        namemc.setTextSize(12);
        namemc.setOnClickListener(v -> Tools.openURL(requireActivity(), "https://namemc.com/minecraft-skins"));
        LinearLayout.LayoutParams btn = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        btn.leftMargin = ForkUi.dp(c, 6);
        top.addView(online, btn);
        top.addView(namemc, btn);
        right.addView(top);

        ScrollView scroll = new ScrollView(c);
        mGrid = new LinearLayout(c);
        mGrid.setOrientation(LinearLayout.VERTICAL);
        scroll.addView(mGrid, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        LinearLayout.LayoutParams scrollLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f);
        scrollLp.topMargin = ForkUi.dp(c, 8);
        right.addView(scroll, scrollLp);
        return root;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        refresh();
    }

    private SkinLibrary.Entry currentEntry(List<SkinLibrary.Entry> skins) {
        for(SkinLibrary.Entry e : skins) if(e.file.getName().equals(currentName())) return e;
        return null;
    }

    private void refresh() {
        Context c = getContext();
        if(c == null || mGrid == null) return;
        List<SkinLibrary.Entry> skins = SkinLibrary.list(c);
        SkinLibrary.Entry keep = null;
        for(SkinLibrary.Entry e : skins) {
            if(mSelected != null && e.file.equals(mSelected.file)) keep = e;
        }
        mSelected = keep;

        // The model on the left is the skin in use (or the selected one if none was applied yet)
        SkinLibrary.Entry shown = currentEntry(skins);
        if(shown == null) shown = mSelected;
        if(shown == null && !skins.isEmpty()) shown = skins.get(0);
        if(shown != null) {
            Bitmap bmp = SkinLibrary.load(shown);
            if(bmp != null) mModel.setSkin(bmp, isSlim(shown, bmp));
        }
        fillGrid(c, skins);
    }

    private void fillGrid(Context c, List<SkinLibrary.Entry> skins) {
        Set<String> favs = favorites();
        List<SkinLibrary.Entry> ordered = new ArrayList<>();
        Collections.sort(skins, (a, b) -> Long.compare(b.file.lastModified(), a.file.lastModified()));
        for(SkinLibrary.Entry e : skins) if(favs.contains(e.file.getName())) ordered.add(e);
        for(SkinLibrary.Entry e : skins) if(!favs.contains(e.file.getName())) ordered.add(e);

        List<View> cards = new ArrayList<>();
        cards.add(newSkinCard(c));
        for(SkinLibrary.Entry e : ordered) cards.add(card(c, e, favs.contains(e.file.getName())));

        mGrid.removeAllViews();
        for(int i = 0; i < cards.size(); i += COLUMNS) {
            LinearLayout row = new LinearLayout(c);
            row.setOrientation(LinearLayout.HORIZONTAL);
            for(int k = 0; k < COLUMNS; k++) {
                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
                if(i + k < cards.size()) row.addView(cards.get(i + k), lp);
                else row.addView(new View(c), lp);
            }
            LinearLayout.LayoutParams rowLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            rowLp.bottomMargin = ForkUi.dp(c, 8);
            mGrid.addView(row, rowLp);
        }
    }

    private View newSkinCard(Context c) {
        LinearLayout card = new LinearLayout(c);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setGravity(Gravity.CENTER_HORIZONTAL);
        card.setPadding(ForkUi.dp(c, 4), ForkUi.dp(c, 4), ForkUi.dp(c, 4), ForkUi.dp(c, 4));
        TextView label = ForkUi.text(c, getString(R.string.fork_skins_new), 11, false);
        label.setGravity(Gravity.CENTER);
        label.setSingleLine(true);
        card.addView(label);
        TextView plus = ForkUi.text(c, "+", 26, false);
        plus.setGravity(Gravity.CENTER);
        plus.setBackground(ForkUi.dashed(c, 0x22000000, 0xFF888888));
        LinearLayout.LayoutParams plusLp = new LinearLayout.LayoutParams(ForkUi.dp(c, 52), ForkUi.dp(c, 104));
        plusLp.topMargin = ForkUi.dp(c, 4);
        card.addView(plus, plusLp);
        card.setOnClickListener(v -> mImport.launch("image/png"));
        return card;
    }

    private View card(Context c, SkinLibrary.Entry entry, boolean favorite) {
        boolean selected = mSelected != null && entry.file.equals(mSelected.file);
        boolean inUse = entry.file.getName().equals(currentName());
        LinearLayout card = new LinearLayout(c);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setGravity(Gravity.CENTER_HORIZONTAL);
        card.setPadding(ForkUi.dp(c, 4), ForkUi.dp(c, 4), ForkUi.dp(c, 4), ForkUi.dp(c, 4));
        if(selected) card.setBackground(ForkUi.box(c, 0xFF353535, 0xFF777777, 1));

        TextView name = ForkUi.text(c, (favorite ? "* " : "") + entry.name, 11, false);
        name.setGravity(Gravity.CENTER);
        name.setSingleLine(true);
        name.setEllipsize(android.text.TextUtils.TruncateAt.END);
        if(favorite) name.setTextColor(0xFFFFD35A);
        card.addView(name, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        Bitmap bmp = SkinLibrary.load(entry);
        ImageView figure = new ImageView(c);
        figure.setScaleType(ImageView.ScaleType.FIT_CENTER);
        if(bmp != null) {
            android.graphics.drawable.BitmapDrawable d = new android.graphics.drawable.BitmapDrawable(
                    c.getResources(), SkinFigure.render(bmp, isSlim(entry, bmp)));
            d.setFilterBitmap(false);
            figure.setImageDrawable(d);
        }
        LinearLayout.LayoutParams figLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ForkUi.dp(c, 104));
        figLp.topMargin = ForkUi.dp(c, 4);
        card.addView(figure, figLp);

        if(selected) {
            Button use = ForkUi.button(c, getString(R.string.fork_skins_apply), true);
            use.setMinHeight(ForkUi.dp(c, 28));
            use.setMinimumHeight(ForkUi.dp(c, 28));
            use.setMinWidth(0);
            use.setMinimumWidth(0);
            use.setTextSize(12);
            use.setPadding(0, 0, 0, 0);
            use.setOnClickListener(v -> onUse());
            LinearLayout.LayoutParams useLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ForkUi.dp(c, 28));
            useLp.topMargin = ForkUi.dp(c, 4);
            card.addView(use, useLp);
            Button more = ForkUi.button(c, "...", false);
            more.setMinHeight(ForkUi.dp(c, 24));
            more.setMinimumHeight(ForkUi.dp(c, 24));
            more.setMinWidth(0);
            more.setMinimumWidth(0);
            more.setTextSize(12);
            more.setPadding(0, 0, 0, 0);
            more.setOnClickListener(v -> showMenu(entry, favorite, isSlim(entry, bmp)));
            LinearLayout.LayoutParams moreLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ForkUi.dp(c, 24));
            moreLp.topMargin = ForkUi.dp(c, 3);
            card.addView(more, moreLp);
        } else if(inUse) {
            TextView mark = ForkUi.text(c, getString(R.string.fork_installs_selected), 11, false);
            mark.setGravity(Gravity.CENTER);
            mark.setTextColor(0xFF7CD65A);
            card.addView(mark);
        }
        card.setOnClickListener(v -> {
            mSelected = entry;
            fillGrid(c, SkinLibrary.list(c));
        });
        return card;
    }

    private void showMenu(SkinLibrary.Entry entry, boolean favorite, boolean slim) {
        String[] items = {
                getString(slim ? R.string.fork_skins_classic : R.string.fork_skins_slim),
                getString(favorite ? R.string.fork_skins_unfavorite : R.string.fork_skins_favorite),
                getString(R.string.fork_skins_rename), getString(R.string.fork_skins_delete)};
        new AlertDialog.Builder(requireContext()).setTitle(entry.name).setItems(items, (d, which) -> {
            if(which == 0) {
                LauncherPreferences.DEFAULT_PREF.edit().putInt(PREF_TYPE + entry.file.getName(), slim ? 1 : 2).apply();
                refresh();
            } else if(which == 1) toggleFavorite(entry);
            else if(which == 2) rename(entry);
            else confirmDelete(entry);
        }).show();
    }

    private void onUse() {
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
    }

    private Set<String> favorites() {
        return new HashSet<>(LauncherPreferences.DEFAULT_PREF.getStringSet(PREF_FAVORITES, new HashSet<>()));
    }

    private void setType(int type) {
        if(mSelected == null) return;
        LauncherPreferences.DEFAULT_PREF.edit().putInt(PREF_TYPE + mSelected.file.getName(), type).apply();
        refresh();
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
        refresh();
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

}
