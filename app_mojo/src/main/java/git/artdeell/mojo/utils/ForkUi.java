package git.artdeell.mojo.utils;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.StateListDrawable;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

/** Small helpers to build the launcher-style (dark + green) screens in code. */
public class ForkUi {
    public static final int GREEN = 0xFF3C8527;
    public static final int CARD = 0xFF3A3A3A;
    public static final int CARD_SELECTED = 0xFF4E4E4E;
    public static final int PANEL = 0xFF2B2B2B;

    public static int dp(Context c, int v) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, v, c.getResources().getDisplayMetrics());
    }

    public static GradientDrawable box(Context c, int fill, int stroke, int strokeDp) {
        GradientDrawable g = new GradientDrawable();
        g.setColor(fill);
        g.setCornerRadius(dp(c, 3));
        if(strokeDp > 0) g.setStroke(dp(c, strokeDp), stroke);
        return g;
    }

    public static TextView text(Context c, CharSequence content, int sp, boolean bold) {
        TextView t = new TextView(c);
        t.setText(content);
        t.setTextColor(Color.WHITE);
        t.setTextSize(TypedValue.COMPLEX_UNIT_SP, sp);
        if(bold) t.setTypeface(Typeface.DEFAULT_BOLD);
        return t;
    }

    /** Flat button. green = the main action (Play style), otherwise gray. */
    public static Button button(Context c, CharSequence label, boolean green) {
        Button b = new Button(c);
        b.setText(label);
        b.setAllCaps(false);
        b.setTextColor(Color.WHITE);
        b.setTypeface(Typeface.DEFAULT_BOLD);
        int base = green ? PlayColor.get() : 0xFF5A5A5A;
        int dark = green ? 0xFF1F4A15 : 0xFF3A3A3A;
        StateListDrawable s = new StateListDrawable();
        s.addState(new int[]{android.R.attr.state_pressed}, box(c, dark, 0xFF111111, 2));
        s.addState(new int[]{}, box(c, base, 0xFF111111, 2));
        b.setBackground(s);
        b.setMinHeight(dp(c, 44));
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
}
