package git.artdeell.mojo.utils;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.LayerDrawable;
import android.graphics.drawable.StateListDrawable;
import android.graphics.drawable.Drawable;

import androidx.appcompat.app.AlertDialog;

import git.artdeell.mojo.R;
import git.artdeell.mojo.prefs.LauncherPreferences;

/** Color of the big Play button, chosen by the user. Default is Bedrock green. */
public class PlayColor {
    public static final String PREF = "play_color";
    private static final int[] PRESETS = {
            0xFF3C8527, // Bedrock green
            0xFF2E6FD1, // blue
            0xFFD14B2E, // red
            0xFF8B4FD6, // purple
            0xFFE0A526, // gold
            0xFF2AA8B5  // teal
    };
    private static final int[] NAME_RES = {R.string.fork_color_green, R.string.fork_color_blue, R.string.fork_color_red,
            R.string.fork_color_purple, R.string.fork_color_gold, R.string.fork_color_teal};

    public static int get() {
        return LauncherPreferences.DEFAULT_PREF.getInt(PREF, PRESETS[0]);
    }

    private static int shade(int color, float factor) {
        int r = Math.round(Color.red(color) * factor);
        int g = Math.round(Color.green(color) * factor);
        int b = Math.round(Color.blue(color) * factor);
        return Color.argb(255, Math.min(255, r), Math.min(255, g), Math.min(255, b));
    }

    private static Drawable face(int color, int border, int top, int bottom, Context c) {
        GradientDrawable outer = new GradientDrawable();
        outer.setColor(border);
        GradientDrawable inner = new GradientDrawable();
        inner.setColor(color);
        LayerDrawable layers = new LayerDrawable(new Drawable[]{outer, inner});
        int d2 = (int) (2 * c.getResources().getDisplayMetrics().density);
        int d6 = (int) (6 * c.getResources().getDisplayMetrics().density);
        layers.setLayerInset(1, d2, top, d2, bottom);
        return layers;
    }

    /** Builds the Play background (normal and pressed) from the chosen color. */
    public static Drawable background(Context c) {
        int base = get();
        int border = shade(base, 0.35f);
        int light = shade(base, 1.35f);
        int dark = shade(base, 0.8f);
        int d2 = (int) (2 * c.getResources().getDisplayMetrics().density);
        int d4 = (int) (4 * c.getResources().getDisplayMetrics().density);
        int d6 = (int) (6 * c.getResources().getDisplayMetrics().density);

        LayerDrawable normal = new LayerDrawable(new Drawable[]{
                solid(border), solid(dark), solid(light), solid(base)});
        normal.setLayerInset(1, d2, d2, d2, d2);
        normal.setLayerInset(2, d2, d2, d2, d6);
        normal.setLayerInset(3, d4, d4, d2, d6);

        LayerDrawable pressed = new LayerDrawable(new Drawable[]{
                solid(border), solid(border), solid(dark)});
        pressed.setLayerInset(1, d2, d2, d2, d2);
        pressed.setLayerInset(2, d2, d4, d2, d2);

        StateListDrawable s = new StateListDrawable();
        s.addState(new int[]{android.R.attr.state_pressed}, pressed);
        s.addState(new int[]{}, normal);
        return s;
    }

    private static String[] names(Context c) {
        String[] out = new String[NAME_RES.length];
        for(int i = 0; i < out.length; i++) out[i] = c.getString(NAME_RES[i]);
        return out;
    }

    private static Drawable solid(int color) {
        GradientDrawable g = new GradientDrawable();
        g.setColor(color);
        return g;
    }

    /** Shows the color picker. Calls onChanged after the user picks one. */
    public static void choose(Context c, Runnable onChanged) {
        new AlertDialog.Builder(c)
                .setTitle(R.string.fork_play_color_title)
                .setItems(names(c), (d, which) -> {
                    LauncherPreferences.DEFAULT_PREF.edit().putInt(PREF, PRESETS[which]).apply();
                    onChanged.run();
                })
                .show();
    }
}
