package net.kdt.pojavlaunch.fragments;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.preference.PreferenceManager;

import net.kdt.pojavlaunch.CustomControlsActivity;
import net.kdt.pojavlaunch.LauncherActivity;
import net.kdt.pojavlaunch.Tools;
import net.kdt.pojavlaunch.game.CursorPreviewView;
import net.kdt.pojavlaunch.prefs.screens.LauncherPreferenceFragment;
import net.kdt.pojavlaunch.utils.CursorStyleUtils;
import net.kdt.pojavlaunch.utils.JellyAnimations;

import git.artdeell.mojo.R;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;

public class CursorCustomizationFragment extends Fragment {
    public static final String TAG = "CursorCustomizationFragment";

    private SharedPreferences prefs;
    private SeekBar sizeBar;
    private SeekBar opacityBar;
    private SeekBar rotationBar;
    private TextView sizeValue;
    private TextView opacityValue;
    private TextView rotationValue;
    private TextView previewSize;
    private TextView customName;
    private CursorPreviewView preview;

    private final ActivityResultLauncher<String[]> pngPicker =
            registerForActivityResult(new ActivityResultContracts.OpenDocument(), uri -> {
                if (uri != null) importPng(uri);
            });

    public CursorCustomizationFragment() {
        super(R.layout.fragment_cursor_customization);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        prefs = PreferenceManager.getDefaultSharedPreferences(requireContext());
        preview = view.findViewById(R.id.cursor_preview);
        sizeBar = view.findViewById(R.id.cursor_size);
        opacityBar = view.findViewById(R.id.cursor_opacity);
        rotationBar = view.findViewById(R.id.cursor_rotation);
        sizeValue = view.findViewById(R.id.cursor_size_value);
        opacityValue = view.findViewById(R.id.cursor_opacity_value);
        rotationValue = view.findViewById(R.id.cursor_rotation_value);
        previewSize = view.findViewById(R.id.cursor_preview_size);
        customName = view.findViewById(R.id.custom_cursor_name);

        bindMotion(view);
        bindSize();
        bindEffects(view);
        bindTemplates(view.findViewById(R.id.cursor_templates));
        bindColors(view.findViewById(R.id.cursor_colors));
        bindSidebar(view);
        bindCustomPng(view);
        bindPresets(view);
        bindReset(view);

        JellyAnimations.animateScreen(view);
    }

    private void bindMotion(View root) {
        Switch motion = root.findViewById(R.id.cursor_motion_switch);
        motion.setChecked(prefs.getBoolean(CursorStyleUtils.MOTION_KEY, true));
        motion.setOnCheckedChangeListener((button, checked) ->
                prefs.edit().putBoolean(CursorStyleUtils.MOTION_KEY, checked).apply());
    }

    private void bindSize() {
        float saved = clamp(prefs.getFloat(CursorStyleUtils.SCALE_KEY, 1f), 0.5f, 2f);
        sizeBar.setProgress(Math.round((saved - 0.5f) * 100f));
        updateSizeText(saved);

        sizeBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar bar, int progress, boolean fromUser) {
                float scale = 0.5f + progress / 100f;
                prefs.edit().putFloat(CursorStyleUtils.SCALE_KEY, scale).apply();
                updateSizeText(scale);
            }
            @Override public void onStartTrackingTouch(SeekBar bar) {}
            @Override public void onStopTrackingTouch(SeekBar bar) {}
        });
    }

    private void bindEffects(View root) {
        bindSwitch(root, R.id.cursor_outline_switch, CursorStyleUtils.OUTLINE_KEY, false);
        bindSwitch(root, R.id.cursor_trail_switch, CursorStyleUtils.TRAIL_KEY, false);
        bindSwitch(root, R.id.cursor_shadow_switch, CursorStyleUtils.SHADOW_KEY, true);

        int opacity = Math.max(20, Math.min(100,
                prefs.getInt(CursorStyleUtils.OPACITY_KEY, 100)));
        opacityBar.setProgress(opacity);
        updateOpacityText(opacity);
        opacityBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar bar, int progress, boolean fromUser) {
                int value = Math.max(20, progress);
                prefs.edit().putInt(CursorStyleUtils.OPACITY_KEY, value).apply();
                updateOpacityText(value);
            }
            @Override public void onStartTrackingTouch(SeekBar bar) {}
            @Override public void onStopTrackingTouch(SeekBar bar) {}
        });

        int rotation = ((prefs.getInt(CursorStyleUtils.ROTATION_KEY, 0) % 360) + 360) % 360;
        rotationBar.setProgress(rotation);
        updateRotationText(rotation);
        rotationBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar bar, int progress, boolean fromUser) {
                prefs.edit().putInt(CursorStyleUtils.ROTATION_KEY, progress).apply();
                updateRotationText(progress);
            }
            @Override public void onStartTrackingTouch(SeekBar bar) {}
            @Override public void onStopTrackingTouch(SeekBar bar) {}
        });
    }

    private void bindSwitch(View root, int id, String key, boolean defaultValue) {
        Switch sw = root.findViewById(id);
        sw.setChecked(prefs.getBoolean(key, defaultValue));
        sw.setOnCheckedChangeListener((button, checked) ->
                prefs.edit().putBoolean(key, checked).apply());
    }

    private void updateSizeText(float scale) {
        int value = Math.round(scale * 100f);
        sizeValue.setText(value + "%");
        if (previewSize != null) {
            String label = value <= 75 ? "Small" : value >= 140 ? "Large" : "Medium";
            previewSize.setText("Cursor size: " + label);
        }
    }

    private void updateOpacityText(int value) {
        opacityValue.setText(value + "%");
    }

    private void updateRotationText(int value) {
        rotationValue.setText(value + "°");
    }

    private float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }

    private void bindTemplates(LinearLayout container) {
        String selected = prefs.getString(CursorStyleUtils.TEMPLATE_KEY, "Classic");
        for (String template : CursorStyleUtils.TEMPLATES) {
            TextView card = new TextView(requireContext());
            card.setText(template);
            card.setTextColor(Color.WHITE);
            card.setTextSize(11);
            card.setGravity(android.view.Gravity.CENTER);
            card.setPadding(14, 0, 14, 0);
            card.setMinWidth(88);
            card.setBackgroundResource(template.equals(selected)
                    ? R.drawable.cursor_selected_bg : R.drawable.cursor_section_bg);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(88, 70);
            lp.setMargins(0, 0, 7, 0);
            container.addView(card, lp);
            applyJellyTouch(card);

            card.setOnClickListener(v -> {
                prefs.edit()
                        .putString(CursorStyleUtils.TEMPLATE_KEY, template)
                        .remove(CursorStyleUtils.CUSTOM_PATH_KEY)
                        .apply();
                customName.setText("Not selected");
                refreshTemplateSelection(container, template);
            });
        }
    }

    private void refreshTemplateSelection(LinearLayout container, String selected) {
        for (int i = 0; i < container.getChildCount(); i++) {
            TextView card = (TextView) container.getChildAt(i);
            card.setBackgroundResource(card.getText().toString().equals(selected)
                    ? R.drawable.cursor_selected_bg : R.drawable.cursor_section_bg);
        }
    }

    private void bindColors(LinearLayout container) {
        int selected = prefs.getInt(CursorStyleUtils.COLOR_KEY, CursorStyleUtils.COLORS[0]);
        for (int color : CursorStyleUtils.COLORS) {
            View swatch = new View(requireContext());
            GradientDrawable bg = new GradientDrawable();
            bg.setShape(GradientDrawable.OVAL);
            bg.setColor(color);
            bg.setStroke(color == selected ? 5 : 2, Color.WHITE);
            swatch.setBackground(bg);
            swatch.setContentDescription("Cursor color");
            swatch.setTag(color);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(38, 38);
            lp.setMargins(4, 0, 9, 0);
            container.addView(swatch, lp);
            applyJellyTouch(swatch);

            swatch.setOnClickListener(v -> {
                prefs.edit().putInt(CursorStyleUtils.COLOR_KEY, color).apply();
                updateColorSelection(container, color);
            });
        }
    }

    private void updateColorSelection(LinearLayout container, int selected) {
        for (int i = 0; i < container.getChildCount(); i++) {
            View swatch = container.getChildAt(i);
            Object tag = swatch.getTag();
            if (tag instanceof Integer) {
                int color = (Integer) tag;
                GradientDrawable bg = new GradientDrawable();
                bg.setShape(GradientDrawable.OVAL);
                bg.setColor(color);
                bg.setStroke(color == selected ? 5 : 2, Color.WHITE);
                swatch.setBackground(bg);
            }
        }
    }

    private void bindSidebar(View root) {
        ImageButton home = root.findViewById(R.id.cursor_sidebar_home);
        ImageButton pointer = root.findViewById(R.id.cursor_sidebar_pointer);
        ImageButton controls = root.findViewById(R.id.cursor_sidebar_controls);
        ImageButton info = root.findViewById(R.id.cursor_sidebar_info);
        ImageButton settings = root.findViewById(R.id.cursor_sidebar_settings);
        ImageButton back = root.findViewById(R.id.cursor_back);

        setNav(home, () -> Tools.backToMainMenu(requireActivity()));
        setNav(back, () -> Tools.backToMainMenu(requireActivity()));
        setNav(pointer, () -> {});
        setNav(controls, () -> requireContext().startActivity(new Intent(requireContext(), CustomControlsActivity.class)));
        setNav(info, () -> Tools.openURL(requireActivity(), Tools.URL_HOME));
        setNav(settings, () -> Tools.swapFragment(requireActivity(), LauncherPreferenceFragment.class, LauncherActivity.SETTING_FRAGMENT_TAG, null));
    }

    private void setNav(View v, Runnable action) {
        if (v == null) return;
        applyJellyTouch(v);
        v.setOnClickListener(view -> {
            Tools.jellyClick(view);
            action.run();
        });
    }

    private void bindCustomPng(View root) {
        Button button = root.findViewById(R.id.custom_cursor_button);
        updateCustomName();
        applyJellyTouch(button);
        button.setOnClickListener(v -> pngPicker.launch(new String[]{"image/png"}));
    }

    private void updateCustomName() {
        String path = prefs.getString(CursorStyleUtils.CUSTOM_PATH_KEY, "");
        customName.setText(path.isEmpty() ? "Not selected" : new File(path).getName());
    }

    private void importPng(Uri uri) {
        try {
            File out = new File(requireContext().getFilesDir(), "cryonix_cursor.png");
            try (InputStream in = requireContext().getContentResolver().openInputStream(uri);
                 FileOutputStream stream = new FileOutputStream(out)) {
                if (in == null) throw new IllegalStateException("Cannot open PNG");
                byte[] buffer = new byte[8192];
                int read;
                while ((read = in.read(buffer)) != -1) stream.write(buffer, 0, read);
            }
            prefs.edit()
                    .putString(CursorStyleUtils.CUSTOM_PATH_KEY, out.getAbsolutePath())
                    .putString(CursorStyleUtils.TEMPLATE_KEY, "Classic")
                    .apply();
            updateCustomName();
            Toast.makeText(requireContext(), "Custom PNG applied", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Toast.makeText(requireContext(), "Could not import PNG", Toast.LENGTH_SHORT).show();
        }
    }

    private void bindPresets(View root) {
        bindPreset(root, R.id.cursor_preset_clean, "Classic", CursorStyleUtils.COLORS[0], 1f, false, false, true);
        bindPreset(root, R.id.cursor_preset_neon, "Dot", 0xFF39FF88, 1.15f, true, true, true);
        bindPreset(root, R.id.cursor_preset_gamer, "Triangle", 0xFF00E5FF, 1.2f, true, true, true);
        bindPreset(root, R.id.cursor_preset_minimal, "Ring", 0xFFFFFFFF, 0.9f, false, false, false);
    }

    private void bindPreset(View root, int id, String template, int color, float scale,
                            boolean outline, boolean trail, boolean shadow) {
        Button button = root.findViewById(id);
        if (button == null) return;
        applyJellyTouch(button);
        button.setOnClickListener(v -> {
            prefs.edit()
                    .putString(CursorStyleUtils.TEMPLATE_KEY, template)
                    .putInt(CursorStyleUtils.COLOR_KEY, color)
                    .putFloat(CursorStyleUtils.SCALE_KEY, scale)
                    .putBoolean(CursorStyleUtils.OUTLINE_KEY, outline)
                    .putBoolean(CursorStyleUtils.TRAIL_KEY, trail)
                    .putBoolean(CursorStyleUtils.SHADOW_KEY, shadow)
                    .putInt(CursorStyleUtils.OPACITY_KEY, 100)
                    .putInt(CursorStyleUtils.ROTATION_KEY, 0)
                    .remove(CursorStyleUtils.CUSTOM_PATH_KEY)
                    .apply();
            sizeBar.setProgress(Math.round((scale - 0.5f) * 100f));
            opacityBar.setProgress(100);
            rotationBar.setProgress(0);
            updateSizeText(scale);
            updateOpacityText(100);
            updateRotationText(0);
            customName.setText("Not selected");
            refreshTemplateSelection(root.findViewById(R.id.cursor_templates), template);
            Toast.makeText(requireContext(), template + " preset applied", Toast.LENGTH_SHORT).show();
        });
    }

    private void bindReset(View root) {
        Button reset = root.findViewById(R.id.cursor_reset);
        applyJellyTouch(reset);
        reset.setOnClickListener(v -> {
            prefs.edit()
                    .putFloat(CursorStyleUtils.SCALE_KEY, 1f)
                    .putBoolean(CursorStyleUtils.MOTION_KEY, true)
                    .putBoolean(CursorStyleUtils.OUTLINE_KEY, false)
                    .putBoolean(CursorStyleUtils.TRAIL_KEY, false)
                    .putBoolean(CursorStyleUtils.SHADOW_KEY, true)
                    .putInt(CursorStyleUtils.OPACITY_KEY, 100)
                    .putInt(CursorStyleUtils.ROTATION_KEY, 0)
                    .putInt(CursorStyleUtils.COLOR_KEY, CursorStyleUtils.COLORS[0])
                    .putString(CursorStyleUtils.TEMPLATE_KEY, "Classic")
                    .remove(CursorStyleUtils.CUSTOM_PATH_KEY)
                    .apply();
            sizeBar.setProgress(50);
            opacityBar.setProgress(100);
            rotationBar.setProgress(0);
            updateSizeText(1f);
            updateOpacityText(100);
            updateRotationText(0);
            customName.setText("Not selected");
            LinearLayout templates = root.findViewById(R.id.cursor_templates);
            templates.removeAllViews();
            bindTemplates(templates);
        });
    }

    private void applyJellyTouch(View v) {
        if (v == null) return;
        v.setOnTouchListener((view, event) -> {
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                view.animate().scaleX(.97f).scaleY(.97f).setDuration(70).start();
            } else if (event.getAction() == MotionEvent.ACTION_UP || event.getAction() == MotionEvent.ACTION_CANCEL) {
                view.animate().scaleX(1.015f).scaleY(1.015f).setDuration(100)
                        .withEndAction(() -> view.animate().scaleX(1f).scaleY(1f).setDuration(75).start()).start();
            }
            return false;
        });
    }
}
