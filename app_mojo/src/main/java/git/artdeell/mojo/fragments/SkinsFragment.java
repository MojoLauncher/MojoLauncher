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
import android.widget.Button;
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
import androidx.fragment.app.Fragment;

import java.util.List;

import git.artdeell.mojo.R;
import git.artdeell.mojo.Tools;
import git.artdeell.mojo.authenticator.accounts.SkinLibrary;
import git.artdeell.mojo.prefs.LauncherPreferences;
import git.artdeell.mojo.utils.ForkUi;
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
    private static final int SELECTED_BLUE = 0xFF2F66C8;
    private static final int MODEL_PANEL = 0xFF444444;

    private SkinModelView mModel;
    private TextView mNameField;
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
        ImageView back = new ImageView(c);
        back.setScaleType(ImageView.ScaleType.CENTER_CROP);
        back.setImageResource(R.drawable.bedrock_banner_art);
        frame.addView(back, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        View shade = new View(c);
        shade.setBackgroundColor(0xDD111111);
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
        mNameField = ForkUi.text(c, "", 14, false);
        mNameField.setBackground(ForkUi.box(c, Color.BLACK, 0xFFAAAAAA, 2));
        mNameField.setGravity(Gravity.CENTER_VERTICAL);
        mNameField.setPadding(ForkUi.dp(c, 8), 0, ForkUi.dp(c, 8), 0);
        mNameField.setSingleLine(true);
        nameRow.addView(mNameField, new LinearLayout.LayoutParams(0, ForkUi.dp(c, 36), 1f));
        Button plus = ForkUi.button(c, "+", false);
        plus.setMinWidth(0);
        plus.setMinimumWidth(0);
        plus.setPadding(0, 0, 0, 0);
        plus.setOnClickListener(v -> mImport.launch("image/png"));
        LinearLayout.LayoutParams plusLp = new LinearLayout.LayoutParams(ForkUi.dp(c, 36), ForkUi.dp(c, 36));
        plusLp.leftMargin = ForkUi.dp(c, 6);
        nameRow.addView(plus, plusLp);
        left.addView(nameRow);

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
        Button laby = ForkUi.button(c, getString(R.string.fork_skins_laby), false);
        mDelete = ForkUi.button(c, getString(R.string.fork_skins_delete), false);
        Button done = ForkUi.button(c, getString(R.string.fork_skins_done), false);
        Button[] all = {importBtn, laby, mDelete, done};
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
        laby.setOnClickListener(v -> Tools.openURL(requireActivity(), "https://laby.net/skins"));
        done.setOnClickListener(v -> requireActivity().getOnBackPressedDispatcher().onBackPressed());
        mDelete.setOnClickListener(v -> {
            if(mSelected == null) return;
            SkinLibrary.delete(mSelected);
            mSelected = null;
            refresh();
        });
        mUse.setOnClickListener(v -> {
            if(mSelected == null) return;
            Bitmap bmp = SkinLibrary.load(mSelected);
            SkinLibraryDialog.apply(requireContext(), mSelected, isSlim(mSelected, bmp));
            LauncherPreferences.DEFAULT_PREF.edit().putString(PREF_CURRENT, mSelected.file.getName()).apply();
            refresh();
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
            mNameField.setText(mSelected.name);
        } else {
            mNameField.setText(R.string.fork_skins_none);
        }
        mUse.setEnabled(mSelected != null);
        mDelete.setEnabled(mSelected != null);
        mClassic.setEnabled(mSelected != null);
        mSlim.setEnabled(mSelected != null);
        mClassic.setAlpha(!slim ? 1f : 0.6f);
        mSlim.setAlpha(slim ? 1f : 0.6f);

        mList.removeAllViews();
        for(SkinLibrary.Entry entry : skins) {
            mList.addView(row(c, entry));
        }
    }

    private View row(Context c, SkinLibrary.Entry entry) {
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
        TextView name = ForkUi.text(c, entry.name, 14, false);
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
