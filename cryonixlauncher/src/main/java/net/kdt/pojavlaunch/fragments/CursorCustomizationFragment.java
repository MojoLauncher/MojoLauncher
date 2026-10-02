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
    private TextView sizeValue;
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
        sizeValue = view.findViewById(R.id.cursor_size_value);
        customName = view.findViewById(R.id.custom_cursor_name);

        bindMotion(view);
        bindSize();
        bindTemplates(view.findViewById(R.id.cursor_templates));
        bindColors(view.findViewById(R.id.cursor_colors));
        bindSidebar(view);
        bindCustomPng(view);
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
        float saved = Math.max(0.5f, Math.min(2f,
                prefs.getFloat(CursorStyleUtils.SCALE_KEY, 1f)));
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

    private void updateSizeText(float scale) {
        sizeValue.setText(Math.round(scale * 100f) + "%");
    }

    private void bindTemplates(LinearLayout container) {
        String selected = prefs.getString(CursorStyleUtils.TEMPLATE_KEY, "Classic");
        for (String template : CursorStyleUtils.TEMPLATES) {
            TextView card = new TextView(requireContext());
            card.setText(template);
            card.setTextColor(Color.WHITE);
            card.setTextSize(12);
            card.setGravity(android.view.Gravity.CENTER);
            card.setPadding(20, 0, 20, 0);
            card.setMinWidth(92);
            card.setBackgroundResource(template.equals(selected)
                    ? R.drawable.cursor_selected_bg : R.drawable.cursor_section_bg);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(92, 70);
            lp.setMargins(0, 0, 8, 0);
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
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(40, 40);
            lp.setMargins(5, 0, 12, 0);
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

    private void bindReset(View root) {
        Button reset = root.findViewById(R.id.cursor_reset);
        applyJellyTouch(reset);
        reset.setOnClickListener(v -> {
            prefs.edit()
                    .putFloat(CursorStyleUtils.SCALE_KEY, 1f)
                    .putBoolean(CursorStyleUtils.MOTION_KEY, true)
                    .putInt(CursorStyleUtils.COLOR_KEY, CursorStyleUtils.COLORS[0])
                    .putString(CursorStyleUtils.TEMPLATE_KEY, "Classic")
                    .remove(CursorStyleUtils.CUSTOM_PATH_KEY)
                    .apply();
            sizeBar.setProgress(50);
            updateSizeText(1f);
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
                view.animate().scaleX(.96f).scaleY(.96f).setDuration(90).start();
            } else if (event.getAction() == MotionEvent.ACTION_UP || event.getAction() == MotionEvent.ACTION_CANCEL) {
                view.animate().scaleX(1.02f).scaleY(1.02f).setDuration(110)
                        .withEndAction(() -> view.animate().scaleX(1f).scaleY(1f).setDuration(80).start()).start();
            }
            return false;
        });
    }
}