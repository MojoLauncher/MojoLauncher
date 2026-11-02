package git.artdeell.mojo.utils;

import android.content.Context;
import android.graphics.Color;
import android.text.InputType;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;

import git.artdeell.mojo.R;
import git.artdeell.mojo.prefs.LauncherPreferences;

/** The color of the lines that used to be fixed green (top bar line, category lines, tab underline). */
public class AccentColor {
    public static final String PREF = "accent_color";
    public static final int DEFAULT = 0xFF52A535;
    private static final int[] PRESETS = {DEFAULT, 0xFF2E8BE6, 0xFFE04646, 0xFF9B59E8, 0xFFE8B02E, 0xFF29B6C5, 0xFFFFFFFF};
    private static final int[] NAME_RES = {R.string.fork_color_green, R.string.fork_color_blue, R.string.fork_color_red,
            R.string.fork_color_purple, R.string.fork_color_gold, R.string.fork_color_teal, R.string.fork_color_white};

    public static int get() {
        try {
            return LauncherPreferences.DEFAULT_PREF.getInt(PREF, DEFAULT);
        } catch (Exception e) {
            return DEFAULT;
        }
    }

    /** A lighter version of the accent, used for text next to the lines. */
    public static int light() {
        int c = get();
        return Color.argb(255, Math.min(255, Math.round(Color.red(c) * 1.25f) + 12),
                Math.min(255, Math.round(Color.green(c) * 1.25f) + 12), Math.min(255, Math.round(Color.blue(c) * 1.25f) + 12));
    }

    private static void set(int color) {
        LauncherPreferences.DEFAULT_PREF.edit().putInt(PREF, color).apply();
    }

    /** Shows the choices. onChanged runs after a new color is saved. */
    public static void choose(Context c, Runnable onChanged) {
        String[] items = new String[NAME_RES.length + 2];
        for(int i = 0; i < NAME_RES.length; i++) items[i] = c.getString(NAME_RES[i]);
        items[NAME_RES.length] = c.getString(R.string.fork_accent_custom);
        items[NAME_RES.length + 1] = c.getString(R.string.fork_accent_reset);
        new AlertDialog.Builder(c)
                .setTitle(R.string.fork_accent_title)
                .setItems(items, (d, which) -> {
                    if(which < PRESETS.length) {
                        set(PRESETS[which]);
                        onChanged.run();
                    } else if(which == PRESETS.length) {
                        askHex(c, onChanged);
                    } else {
                        set(DEFAULT);
                        onChanged.run();
                    }
                })
                .show();
    }

    private static void askHex(Context c, Runnable onChanged) {
        EditText input = new EditText(c);
        input.setHint(R.string.fork_accent_hex_hint);
        input.setSingleLine(true);
        input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
        new AlertDialog.Builder(c)
                .setTitle(R.string.fork_accent_custom)
                .setView(input)
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton(android.R.string.ok, (d, w) -> {
                    String text = input.getText().toString().trim();
                    if(!text.startsWith("#")) text = "#" + text;
                    try {
                        set(Color.parseColor(text) | 0xFF000000);
                        onChanged.run();
                    } catch (IllegalArgumentException e) {
                        Toast.makeText(c, R.string.fork_accent_invalid, Toast.LENGTH_LONG).show();
                    }
                })
                .show();
    }
}
