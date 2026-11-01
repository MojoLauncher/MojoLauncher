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
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import java.util.List;

import git.artdeell.mojo.R;
import git.artdeell.mojo.authenticator.accounts.SkinLibrary;
import git.artdeell.mojo.prefs.LauncherPreferences;
import git.artdeell.mojo.utils.ForkUi;
import git.artdeell.mojo.utils.SkinLibraryDialog;
import git.artdeell.mojo.utils.SkinModelView;

/**
 * Skins screen in the style of the official launcher: the current skin in 3D on the left and the
 * library as a grid of cards on the right (the first card imports a new skin).
 */
public class SkinsFragment extends Fragment {
    public static final String TAG = "SkinsFragment";
    private static final String PREF_CURRENT = "current_skin_file";
    private static final int COLUMNS = 4;

    private SkinModelView mModel;
    private TextView mName;
    private LinearLayout mGrid;
    private Button mUse, mDelete;
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

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        Context c = requireContext();
        LinearLayout root = new LinearLayout(c);
        root.setOrientation(LinearLayout.HORIZONTAL);
        root.setBackgroundColor(ContextCompat.getColor(c, R.color.background_app));
        root.setPadding(ForkUi.dp(c, 12), ForkUi.dp(c, 12), ForkUi.dp(c, 12), ForkUi.dp(c, 12));

        // --- left: current skin, 3D ---
        LinearLayout left = new LinearLayout(c);
        left.setOrientation(LinearLayout.VERTICAL);
        left.setBackground(ForkUi.box(c, ForkUi.PANEL, 0xFF111111, 1));
        left.setPadding(ForkUi.dp(c, 10), ForkUi.dp(c, 10), ForkUi.dp(c, 10), ForkUi.dp(c, 10));
        LinearLayout.LayoutParams leftLp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1f);
        leftLp.rightMargin = ForkUi.dp(c, 10);
        root.addView(left, leftLp);
        left.addView(ForkUi.text(c, getString(R.string.fork_skins_current), 20, true));
        mModel = new SkinModelView(c);
        left.addView(mModel, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));
        mName = ForkUi.text(c, "", 14, false);
        mName.setGravity(Gravity.CENTER);
        left.addView(mName);
        LinearLayout buttons = ForkUi.row(c);
        mUse = ForkUi.button(c, getString(R.string.fork_skins_apply), true);
        mDelete = ForkUi.button(c, getString(R.string.fork_skins_delete), false);
        LinearLayout.LayoutParams b1 = ForkUi.weight(1f);
        b1.rightMargin = ForkUi.dp(c, 4);
        LinearLayout.LayoutParams b2 = ForkUi.weight(1f);
        b2.leftMargin = ForkUi.dp(c, 4);
        buttons.addView(mUse, b1);
        buttons.addView(mDelete, b2);
        left.addView(buttons);

        // --- right: library ---
        LinearLayout right = new LinearLayout(c);
        right.setOrientation(LinearLayout.VERTICAL);
        root.addView(right, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 2.2f));
        right.addView(ForkUi.text(c, getString(R.string.fork_skins_library), 20, true));
        TextView hint = ForkUi.text(c, getString(R.string.fork_skins_hint), 13, false);
        hint.setPadding(0, ForkUi.dp(c, 2), 0, ForkUi.dp(c, 8));
        right.addView(hint);
        ScrollView scroll = new ScrollView(c);
        mGrid = new LinearLayout(c);
        mGrid.setOrientation(LinearLayout.VERTICAL);
        scroll.addView(mGrid);
        right.addView(scroll, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));

        mUse.setOnClickListener(v -> {
            if(mSelected == null) return;
            SkinLibraryDialog.apply(requireContext(), mSelected);
            LauncherPreferences.DEFAULT_PREF.edit().putString(PREF_CURRENT, mSelected.file.getName()).apply();
            refresh();
        });
        mDelete.setOnClickListener(v -> {
            if(mSelected == null) return;
            SkinLibrary.delete(mSelected);
            mSelected = null;
            refresh();
        });
        return root;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        refresh();
    }

    private void refresh() {
        Context c = requireContext();
        List<SkinLibrary.Entry> skins = SkinLibrary.list(c);
        // Keep the selection if it still exists, else fall back to the applied skin, else the first one
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

        if(mSelected != null) {
            Bitmap bmp = SkinLibrary.load(mSelected);
            if(bmp != null) mModel.setSkin(bmp, SkinLibrary.isSlim(bmp));
            mName.setText(mSelected.name);
        } else {
            mName.setText(R.string.fork_skins_empty);
        }
        mUse.setEnabled(mSelected != null);
        mDelete.setEnabled(mSelected != null);

        mGrid.removeAllViews();
        int count = skins.size() + 1; // + "New skin"
        LinearLayout row = null;
        for(int i = 0; i < count; i++) {
            if(i % COLUMNS == 0) {
                row = new LinearLayout(c);
                row.setOrientation(LinearLayout.HORIZONTAL);
                mGrid.addView(row, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
            }
            View card = (i == 0) ? newSkinCard(c) : skinCard(c, skins.get(i - 1));
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, ForkUi.dp(c, 112), 1f);
            lp.setMargins(ForkUi.dp(c, 3), ForkUi.dp(c, 3), ForkUi.dp(c, 3), ForkUi.dp(c, 3));
            row.addView(card, lp);
        }
        // Fill the last row so the cards keep the same width
        int rem = count % COLUMNS;
        if(rem != 0 && row != null) {
            for(int i = rem; i < COLUMNS; i++) {
                View filler = new View(c);
                row.addView(filler, new LinearLayout.LayoutParams(0, ForkUi.dp(c, 112), 1f));
            }
        }
    }

    private View newSkinCard(Context c) {
        LinearLayout card = new LinearLayout(c);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setGravity(Gravity.CENTER);
        card.setBackground(ForkUi.box(c, ForkUi.CARD, ForkUi.GREEN, 2));
        TextView plus = ForkUi.text(c, "+", 34, true);
        plus.setTextColor(ForkUi.GREEN);
        plus.setGravity(Gravity.CENTER);
        card.addView(plus);
        TextView label = ForkUi.text(c, getString(R.string.fork_skins_new), 13, true);
        label.setGravity(Gravity.CENTER);
        card.addView(label);
        card.setOnClickListener(v -> mImport.launch("image/png"));
        return card;
    }

    private View skinCard(Context c, SkinLibrary.Entry entry) {
        LinearLayout card = new LinearLayout(c);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setGravity(Gravity.CENTER);
        boolean selected = mSelected != null && entry.file.equals(mSelected.file);
        boolean current = entry.file.getName().equals(currentName());
        card.setBackground(ForkUi.box(c, selected ? ForkUi.CARD_SELECTED : ForkUi.CARD,
                current ? ForkUi.GREEN : (selected ? Color.WHITE : 0xFF111111), selected || current ? 2 : 1));
        ImageView face = new ImageView(c);
        Bitmap bmp = SkinLibrary.load(entry);
        if(bmp != null) face.setImageBitmap(faceOf(bmp));
        card.addView(face, new LinearLayout.LayoutParams(ForkUi.dp(c, 64), ForkUi.dp(c, 64)));
        TextView label = ForkUi.text(c, entry.name, 12, false);
        label.setGravity(Gravity.CENTER);
        label.setSingleLine(true);
        label.setEllipsize(android.text.TextUtils.TruncateAt.END);
        label.setPadding(ForkUi.dp(c, 4), ForkUi.dp(c, 4), ForkUi.dp(c, 4), 0);
        card.addView(label);
        card.setOnClickListener(v -> {
            mSelected = entry;
            refresh();
        });
        return card;
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
