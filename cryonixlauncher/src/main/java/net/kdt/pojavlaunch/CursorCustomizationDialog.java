package net.kdt.pojavlaunch;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.preference.PreferenceManager;

import git.artdeell.mojo.R;

public final class CursorCustomizationDialog {
    private static final String SCALE_KEY = "cryonix_cursor_scale";
    private static final String MOTION_KEY = "cryonix_cursor_motion_jelly";

    private CursorCustomizationDialog() {
    }

    public static void show(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        float savedScale = Math.max(0.5f, Math.min(2.0f, prefs.getFloat(SCALE_KEY, 1.0f)));

        LinearLayout root = new LinearLayout(context);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(28, 8, 28, 4);

        TextView value = new TextView(context);
        value.setTextColor(Color.WHITE);
        value.setTextSize(13);
        value.setGravity(Gravity.CENTER_VERTICAL);
        root.addView(value, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 42));

        SeekBar sizeBar = new SeekBar(context);
        sizeBar.setMax(150);
        sizeBar.setProgress(Math.round((savedScale - 0.5f) * 100f));
        root.addView(sizeBar, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        Switch motion = new Switch(context);
        motion.setText("Motion jelly cursor");
        motion.setTextColor(Color.WHITE);
        motion.setChecked(prefs.getBoolean(MOTION_KEY, true));
        root.addView(motion, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 52));

        Runnable updateLabel = () -> {
            float scale = 0.5f + (sizeBar.getProgress() / 100f);
            value.setText("Cursor size: " + Math.round(scale * 100f) + "%");
        };
        updateLabel.run();

        sizeBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                float scale = 0.5f + (progress / 100f);
                prefs.edit().putFloat(SCALE_KEY, scale).apply();
                updateLabel.run();
            }
            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        motion.setOnCheckedChangeListener((buttonView, checked) ->
                prefs.edit().putBoolean(MOTION_KEY, checked).apply());

        AlertDialog dialog = new AlertDialog.Builder(context)
                .setTitle("Cursor customization")
                .setView(root)
                .setPositiveButton("Done", null)
                .setNegativeButton("Reset", (d, which) -> {
                    prefs.edit()
                            .putFloat(SCALE_KEY, 1.0f)
                            .putBoolean(MOTION_KEY, true)
                            .apply();
                })
                .create();

        dialog.setOnShowListener(d -> {
            if (dialog.getButton(AlertDialog.BUTTON_POSITIVE) != null) {
                dialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(
                        context.getResources().getColor(R.color.cryo_blue, context.getTheme()));
            }
        });
        dialog.show();
    }
}
