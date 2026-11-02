package git.artdeell.mojo.utils;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.StateListDrawable;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;

import git.artdeell.mojo.R;
import git.artdeell.mojo.Tools;
import git.artdeell.mojo.fragments.InstallationsFragment;
import git.artdeell.mojo.fragments.PatchNotesFragment;
import git.artdeell.mojo.fragments.SkinsFragment;

/** Small helpers to build the launcher-style (dark + green) screens in code. */
public class ForkUi {
    public static final int GREEN = 0xFF3C8527;
    public static final int CARD = 0xFF3A3A3A;
    public static final int CARD_SELECTED = 0xFF4E4E4E;
    public static final int PANEL = 0xFF2B2B2B;
    public static final int BG = 0xFF1E1E1E;
    public static final int SIDEBAR = 0xFF262626;
    public static final int TAB_IDLE = 0xFFB8B8B8;

    public static final int TAB_PLAY = 0, TAB_INSTALLS = 1, TAB_SKINS = 2, TAB_PATCH = 3;

    public static int dp(Context c, int v) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, v, c.getResources().getDisplayMetrics());
    }

    public static GradientDrawable box(Context c, int fill, int stroke, int strokeDp) {
        GradientDrawable g = new GradientDrawable();
        g.setColor(fill);
        if(strokeDp > 0) g.setStroke(dp(c, strokeDp), stroke);
        return g;
    }

    public static GradientDrawable dashed(Context c, int fill, int stroke) {
        GradientDrawable g = new GradientDrawable();
        g.setColor(fill);
        g.setStroke(dp(c, 2), stroke, dp(c, 6), dp(c, 4));
        return g;
    }

    public static TextView text(Context c, CharSequence content, int sp, boolean bold) {
        TextView t = new TextView(c);
        t.setText(content);
        t.setTextColor(Color.WHITE);
        t.setTextSize(TypedValue.COMPLEX_UNIT_SP, sp);
        McStyle.text(t);
        return t;
    }

    /** In-game style button. green = the main action (Play color), otherwise gray. */
    public static Button button(Context c, CharSequence label, boolean green) {
        Button b = new Button(c);
        b.setText(label);
        McStyle.button(b, green);
        b.setMinHeight(dp(c, 40));
        b.setMinimumHeight(dp(c, 40));
        return b;
    }

    public static LinearLayout.LayoutParams match(int w, int h) {
        return new LinearLayout.LayoutParams(w, h);
    }

    public static LinearLayout.LayoutParams weight(float w) {
        return new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, w);
    }

    public static LinearLayout row(Context c) {
        LinearLayout l = new LinearLayout(c);
        l.setOrientation(LinearLayout.HORIZONTAL);
        l.setGravity(Gravity.CENTER_VERTICAL);
        return l;
    }

    /** Moves to a tab of the launcher header, replacing the current tab screen instead of stacking them. */
    private static void openTab(FragmentActivity act, int tab) {
        Class<? extends Fragment> cls;
        String tag;
        switch (tab) {
            case TAB_INSTALLS: cls = InstallationsFragment.class; tag = InstallationsFragment.TAG; break;
            case TAB_SKINS: cls = SkinsFragment.class; tag = SkinsFragment.TAG; break;
            case TAB_PATCH: cls = PatchNotesFragment.class; tag = PatchNotesFragment.TAG; break;
            default: Tools.backToMainMenu(act); return;
        }
        Tools.backToMainMenu(act);
        Tools.swapFragment(act, cls, tag, null);
    }

    private static View tab(Context c, FragmentActivity act, int label, int index, int selected) {
        LinearLayout box = new LinearLayout(c);
        box.setOrientation(LinearLayout.VERTICAL);
        TextView t = text(c, c.getString(label), 13, index == selected);
        t.setTextColor(index == selected ? Color.WHITE : TAB_IDLE);
        t.setPadding(dp(c, 12), dp(c, 3), dp(c, 12), dp(c, 3));
        t.setGravity(Gravity.CENTER);
        box.addView(t);
        View underline = new View(c);
        underline.setBackgroundColor(index == selected ? AccentColor.get() : Color.TRANSPARENT);
        LinearLayout.LayoutParams ulp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(c, 3));
        ulp.leftMargin = dp(c, 6);
        ulp.rightMargin = dp(c, 6);
        box.addView(underline, ulp);
        if(index != selected) box.setOnClickListener(v -> openTab(act, index));
        return box;
    }

    /** Title "MINECRAFT: JAVA EDITION" and the Play / Installations / Skins / Patch notes tabs, for the top bar. */
    public static LinearLayout pageHeader(Context c, FragmentActivity act, int selected) {
        LinearLayout box = new LinearLayout(c);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setGravity(Gravity.CENTER_VERTICAL);
        TextView title = text(c, c.getString(R.string.fork_header_java), 10, true);
        title.setAllCaps(true);
        title.setLetterSpacing(0.06f);
        title.setPadding(dp(c, 12), 0, 0, 0);
        box.addView(title);
        LinearLayout tabs = new LinearLayout(c);
        tabs.setOrientation(LinearLayout.HORIZONTAL);
        tabs.addView(tab(c, act, R.string.fork_tab_play, TAB_PLAY, selected));
        tabs.addView(tab(c, act, R.string.fork_installs_title, TAB_INSTALLS, selected));
        tabs.addView(tab(c, act, R.string.fork_skin_title, TAB_SKINS, selected));
        tabs.addView(tab(c, act, R.string.fork_patch_title, TAB_PATCH, selected));
        box.addView(tabs);
        return box;
    }
}
