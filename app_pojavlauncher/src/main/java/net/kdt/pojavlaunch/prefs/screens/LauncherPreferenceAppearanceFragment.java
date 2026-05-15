package net.kdt.pojavlaunch.prefs.screens;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.content.res.Configuration;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.appcompat.app.AlertDialog;
import androidx.preference.ListPreference;
import androidx.preference.Preference;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import net.kdt.pojavlaunch.prefs.CustomSeekBarPreference;
import net.kdt.pojavlaunch.prefs.LauncherPreferences;
import net.kdt.pojavlaunch.prefs.MaterialSwitchPreference;
import net.kdt.pojavlaunch.utils.CropperUtils;

import java.io.File;
import java.io.FileOutputStream;

import net.ashmeet.hyperlauncher.R;

public class LauncherPreferenceAppearanceFragment extends LauncherPreferenceFragment implements CropperUtils.CropperReceiver {

    private final ActivityResultLauncher<?> mCropperLauncher = CropperUtils.registerCropper(this, this);
    private boolean mIsPickingForDrawer = false;

    @Override
    public void onCreatePreferences(Bundle b, String str) {
        addPreferencesFromResource(R.xml.pref_appearance);

        requirePreference("mouse_cursor_picker").setOnPreferenceClickListener(preference -> {
            mIsPickingForDrawer = false;
            CropperUtils.startCropper(mCropperLauncher);
            return true;
        });

        requirePreference("mouse_hotspot_picker").setOnPreferenceClickListener(preference -> {
            showHotspotDialog();
            return true;
        });

        requirePreference("mouse_cursor_reset").setOnPreferenceClickListener(preference -> {
            LauncherPreferences.DEFAULT_PREF.edit()
                    .putString("mouseCursorPath", null)
                    .putInt("mouseHotspotX", 0)
                    .putInt("mouseHotspotY", 0)
                    .apply();
            LauncherPreferences.loadPreferences(getContext());
            Toast.makeText(getContext(), "Mouse cursor reset to default", Toast.LENGTH_SHORT).show();
            return true;
        });

        requirePreference("drawer_button_image_picker").setOnPreferenceClickListener(preference -> {
            mIsPickingForDrawer = true;
            CropperUtils.startCropper(mCropperLauncher);
            return true;
        });

        requirePreference("drawer_button_reset").setOnPreferenceClickListener(preference -> {
            LauncherPreferences.DEFAULT_PREF.edit()
                    .putInt("drawerButtonX", 50)
                    .putInt("drawerButtonY", 0)
                    .putInt("drawerButtonBgOpacity", 33)
                    .putInt("drawerButtonIconOpacity", 100)
                    .putInt("drawerButtonSize", 40)
                    .putInt("drawerButtonCornerRadius", 8)
                    .putBoolean("drawerButtonMovable", false)
                    .putString("drawerButtonImagePath", null)
                    .putString("drawerButtonPreset", "top_center")
                    .putInt("drawerListOpacity", 100)
                    .apply();
            LauncherPreferences.loadPreferences(getContext());
            
            // Sync UI
            ((ListPreference)requirePreference("drawerButtonPreset")).setValue("top_center");
            ((CustomSeekBarPreference)requirePreference("drawerButtonX")).setValue(50);
            ((CustomSeekBarPreference)requirePreference("drawerButtonY")).setValue(0);
            ((CustomSeekBarPreference)requirePreference("drawerButtonSize")).setValue(40);
            ((CustomSeekBarPreference)requirePreference("drawerButtonCornerRadius")).setValue(8);
            ((CustomSeekBarPreference)requirePreference("drawerButtonBgOpacity")).setValue(33);
            ((CustomSeekBarPreference)requirePreference("drawerButtonIconOpacity")).setValue(100);
            ((MaterialSwitchPreference)requirePreference("drawerButtonMovable")).setChecked(false);
            ((CustomSeekBarPreference)requirePreference("drawerListOpacity")).setValue(100);
            
            Toast.makeText(getContext(), "Drawer button reset to default", Toast.LENGTH_SHORT).show();
            return true;
        });

        ListPreference presetPref = (ListPreference) requirePreference("drawerButtonPreset");
        CustomSeekBarPreference xPref = (CustomSeekBarPreference) requirePreference("drawerButtonX");
        CustomSeekBarPreference yPref = (CustomSeekBarPreference) requirePreference("drawerButtonY");
        CustomSeekBarPreference sizePref = (CustomSeekBarPreference) requirePreference("drawerButtonSize");
        CustomSeekBarPreference radiusPref = (CustomSeekBarPreference) requirePreference("drawerButtonCornerRadius");
        CustomSeekBarPreference bgOpacityPref = (CustomSeekBarPreference) requirePreference("drawerButtonBgOpacity");
        CustomSeekBarPreference iconOpacityPref = (CustomSeekBarPreference) requirePreference("drawerButtonIconOpacity");
        MaterialSwitchPreference movablePref = (MaterialSwitchPreference) requirePreference("drawerButtonMovable");
        CustomSeekBarPreference listOpacityPref = (CustomSeekBarPreference) requirePreference("drawerListOpacity");

        presetPref.setOnPreferenceChangeListener((preference, newValue) -> {
            String val = (String) newValue;
            if (val.equals("custom")) return true;

            int x = 50, y = 50;
            switch (val) {
                case "top_left": x = 0; y = 0; break;
                case "top_center": x = 50; y = 0; break;
                case "top_right": x = 100; y = 0; break;
                case "bottom_left": x = 0; y = 100; break;
                case "bottom_center": x = 50; y = 100; break;
                case "bottom_right": x = 100; y = 100; break;
                case "center_left": x = 0; y = 50; break;
                case "center_right": x = 100; y = 50; break;
                case "center": x = 50; y = 50; break;
            }

            xPref.setValue(x);
            yPref.setValue(y);
            LauncherPreferences.DEFAULT_PREF.edit()
                    .putInt("drawerButtonX", x)
                    .putInt("drawerButtonY", y)
                    .apply();
            LauncherPreferences.loadPreferences(getContext());
            return true;
        });

        Preference.OnPreferenceChangeListener manualPosListener = (preference, newValue) -> {
            presetPref.setValue("custom");
            LauncherPreferences.DEFAULT_PREF.edit().putString("drawerButtonPreset", "custom").apply();
            return true;
        };

        xPref.setOnPreferenceChangeListener(manualPosListener);
        yPref.setOnPreferenceChangeListener(manualPosListener);

        sizePref.setOnPreferenceChangeListener((preference, newValue) -> {
            int val = (int) newValue;
            LauncherPreferences.DEFAULT_PREF.edit().putInt("drawerButtonSize", val).apply();
            LauncherPreferences.loadPreferences(getContext());
            return true;
        });

        radiusPref.setOnPreferenceChangeListener((preference, newValue) -> {
            int val = (int) newValue;
            LauncherPreferences.DEFAULT_PREF.edit().putInt("drawerButtonCornerRadius", val).apply();
            LauncherPreferences.loadPreferences(getContext());
            return true;
        });

        bgOpacityPref.setOnPreferenceChangeListener((preference, newValue) -> {
            int val = (int) newValue;
            LauncherPreferences.DEFAULT_PREF.edit().putInt("drawerButtonBgOpacity", val).apply();
            LauncherPreferences.loadPreferences(getContext());
            return true;
        });

        iconOpacityPref.setOnPreferenceChangeListener((preference, newValue) -> {
            int val = (int) newValue;
            LauncherPreferences.DEFAULT_PREF.edit().putInt("drawerButtonIconOpacity", val).apply();
            LauncherPreferences.loadPreferences(getContext());
            return true;
        });

        movablePref.setOnPreferenceChangeListener((preference, newValue) -> {
            boolean val = (boolean) newValue;
            LauncherPreferences.DEFAULT_PREF.edit().putBoolean("drawerButtonMovable", val).apply();
            LauncherPreferences.loadPreferences(getContext());
            return true;
        });

        listOpacityPref.setOnPreferenceChangeListener((preference, newValue) -> {
            int val = (int) newValue;
            LauncherPreferences.DEFAULT_PREF.edit().putInt("drawerListOpacity", val).apply();
            LauncherPreferences.loadPreferences(getContext());
            return true;
        });
    }

    private void showHotspotDialog() {
        if (LauncherPreferences.PREF_MOUSE_CURSOR_PATH == null) {
            Toast.makeText(getContext(), "Please select a custom mouse cursor first", Toast.LENGTH_SHORT).show();
            return;
        }

        Bitmap bitmap = BitmapFactory.decodeFile(LauncherPreferences.PREF_MOUSE_CURSOR_PATH);
        if (bitmap == null) {
            Toast.makeText(getContext(), "Failed to load mouse cursor image", Toast.LENGTH_SHORT).show();
            return;
        }

        ScrollView scrollView = new ScrollView(getContext());
        LinearLayout root = new LinearLayout(getContext());
        root.setOrientation(LinearLayout.VERTICAL);
        root.setGravity(Gravity.CENTER_HORIZONTAL);
        root.setPadding(20, 20, 20, 20);
        scrollView.addView(root);

        TextView instructions = new TextView(getContext());
        instructions.setText("Tap on the point that should act as the 'click' point.");
        instructions.setPadding(0, 0, 0, 16);
        root.addView(instructions);

        ImageView imageView = new ImageView(getContext());
        imageView.setImageBitmap(bitmap);
        imageView.setAdjustViewBounds(true);
        imageView.setScaleType(ImageView.ScaleType.FIT_CENTER);
        
        // Ensure content doesn't push dialog buttons off-screen in landscape
        int screenHeight = getResources().getDisplayMetrics().heightPixels;
        boolean isLandscape = getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE;
        int maxContentHeight = (int) (screenHeight * (isLandscape ? 0.45f : 0.60f));
        scrollView.setFillViewport(true);
        scrollView.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                maxContentHeight
        ));
        imageView.setMaxHeight((int) (maxContentHeight * 0.85f));
        imageView.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        ));

        root.addView(imageView);

        final int[] hotspot = new int[]{LauncherPreferences.PREF_MOUSE_HOTSPOT_X, LauncherPreferences.PREF_MOUSE_HOTSPOT_Y};

        AlertDialog dialog = new MaterialAlertDialogBuilder(getContext())
                .setTitle("Select Click Point")
                .setView(scrollView)
                .setPositiveButton("Done", (d, w) -> {
                    LauncherPreferences.DEFAULT_PREF.edit()
                            .putInt("mouseHotspotX", hotspot[0])
                            .putInt("mouseHotspotY", hotspot[1])
                            .apply();
                    LauncherPreferences.loadPreferences(getContext());
                })
                .setNegativeButton(android.R.string.cancel, null)
                .create();

        imageView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    float touchX = event.getX();
                    float touchY = event.getY();

                    float viewWidth = v.getWidth();
                    float viewHeight = v.getHeight();
                    float bitmapWidth = bitmap.getWidth();
                    float bitmapHeight = bitmap.getHeight();

                    float scale = Math.min(viewWidth / bitmapWidth, viewHeight / bitmapHeight);
                    float dx = (viewWidth - bitmapWidth * scale) / 2f;
                    float dy = (viewHeight - bitmapHeight * scale) / 2f;

                    hotspot[0] = (int) ((touchX - dx) / scale);
                    hotspot[1] = (int) ((touchY - dy) / scale);

                    hotspot[0] = Math.max(0, Math.min(bitmap.getWidth(), hotspot[0]));
                    hotspot[1] = Math.max(0, Math.min(bitmap.getHeight(), hotspot[1]));

                    Toast.makeText(getContext(), "Hotspot set to: " + hotspot[0] + ", " + hotspot[1], Toast.LENGTH_SHORT).show();
                    v.performClick();
                }
                return true;
            }
        });

        dialog.show();
    }

    @Override
    public float getAspectRatio() {
        return 0; // Free aspect ratio
    }

    @Override
    public int getTargetMaxSide() {
        return 512;
    }

    @Override
    public void onCropped(Bitmap contentBitmap) {
        try {
            if (mIsPickingForDrawer) {
                File drawerFile = new File(getContext().getFilesDir(), "custom_drawer_button.png");
                FileOutputStream out = new FileOutputStream(drawerFile);
                contentBitmap.compress(Bitmap.CompressFormat.PNG, 100, out);
                out.close();

                LauncherPreferences.DEFAULT_PREF.edit()
                        .putString("drawerButtonImagePath", drawerFile.getAbsolutePath())
                        .apply();
                LauncherPreferences.loadPreferences(getContext());
                Toast.makeText(getContext(), "Drawer button image updated", Toast.LENGTH_SHORT).show();
            } else {
                File pointerFile = new File(getContext().getFilesDir(), "custom_pointer.png");
                FileOutputStream out = new FileOutputStream(pointerFile);
                contentBitmap.compress(Bitmap.CompressFormat.PNG, 100, out);
                out.close();

                LauncherPreferences.DEFAULT_PREF.edit()
                        .putString("mouseCursorPath", pointerFile.getAbsolutePath())
                        .putInt("mouseHotspotX", 0)
                        .putInt("mouseHotspotY", 0)
                        .apply();
                LauncherPreferences.loadPreferences(getContext());
                Toast.makeText(getContext(), "Mouse cursor updated. Now set the click point.", Toast.LENGTH_LONG).show();
                showHotspotDialog();
            }
        } catch (Exception e) {
            onFailed(e);
        }
    }

    @Override
    public void onFailed(Exception exception) {
        Toast.makeText(getContext(), "Failed: " + exception.getMessage(), Toast.LENGTH_LONG).show();
    }
}
