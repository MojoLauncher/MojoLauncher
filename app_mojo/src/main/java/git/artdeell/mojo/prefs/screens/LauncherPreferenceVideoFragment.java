package git.artdeell.mojo.prefs.screens;

import android.app.Activity;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.net.Uri;
import android.os.Bundle;
import android.text.InputType;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;
import android.widget.LinearLayout;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import java.util.List;
import git.artdeell.mojo.game.renderer.extra.ExtraRenderers;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.SwitchPreferenceCompat;

import git.artdeell.mojo.R;

import git.artdeell.mojo.game.renderer.RendererCache;
import git.artdeell.mojo.game.renderer.extra.GLESProvider;
import git.artdeell.mojo.prefs.CustomSeekBarPreference;
import git.artdeell.mojo.prefs.LauncherPreferences;
import git.artdeell.mojo.utils.GpuUtils;

/**
 * Fragment for any settings video related
 */
public class LauncherPreferenceVideoFragment extends LauncherPreferenceFragment {
    private Boolean hasAngle = null;
    private final ActivityResultLauncher<String[]> rendererPicker =
            registerForActivityResult(new ActivityResultContracts.OpenDocument(), this::onRendererPicked);
    @Override
    public void onCreatePreferences(Bundle b, String str) {
        addPreferencesFromResource(R.xml.pref_video);
        int resolution = (int) (LauncherPreferences.PREF_SCALE_FACTOR * 100);

        CustomSeekBarPreference resolutionSeekbar = requirePreference("resolutionRatio",
                CustomSeekBarPreference.class);
        resolutionSeekbar.setSuffix(" %");

        // #724 bug fix
        if (resolution < 25) {
            resolutionSeekbar.setValue(100);
        } else {
            resolutionSeekbar.setValue(resolution);
        }

        // Sustained performance is only available since Nougat
        SwitchPreferenceCompat sustainedPerfSwitch = requirePreference("sustainedPerformance",
                SwitchPreferenceCompat.class);
        sustainedPerfSwitch.setVisible(Build.VERSION.SDK_INT >= Build.VERSION_CODES.N);
        sustainedPerfSwitch.setChecked(LauncherPreferences.PREF_SUSTAINED_PERFORMANCE);

        requirePreference("alternate_surface", SwitchPreferenceCompat.class).setChecked(LauncherPreferences.PREF_USE_ALTERNATE_SURFACE);
        requirePreference("force_vsync", SwitchPreferenceCompat.class).setChecked(LauncherPreferences.PREF_FORCE_VSYNC);

        Preference driverPreference = requirePreference("zinkPreferSystemDriver");
        PackageManager packageManager = driverPreference.getContext().getPackageManager();
        boolean supportsTurnip = GpuUtils.checkVulkanSupport(packageManager) && GpuUtils.getGlInfo().isAdreno();
        driverPreference.setVisible(supportsTurnip);

        // Show ANGLE switch only if AnglePlugin is available
        if(hasAngle == null) {
            GLESProvider provider = GLESProvider.getGlesProvider(getContext(), true);
            hasAngle = provider instanceof GLESProvider.ExternalAngleProvider || provider instanceof GLESProvider.SystemAngleProvider;
        }
        SwitchPreferenceCompat angleSwitch = requirePreference("use_angle", SwitchPreferenceCompat.class);
        angleSwitch.setVisible(hasAngle);
        angleSwitch.setChecked(LauncherPreferences.PREF_USE_ANGLE);

        refreshRenderers();
        requirePreference("import_renderer").setOnPreferenceClickListener(p -> {
            rendererPicker.launch(new String[]{"*/*"});
            return true;
        });
        requirePreference("remove_renderer").setOnPreferenceClickListener(p -> {
            showRemoveDialog();
            return true;
        });

        computeVisibility();
    }

    private void refreshRenderers() {
        ListPreference rendererListPreference = requirePreference("renderer",
                ListPreference.class);
        RendererCache.releaseRendererCache();
        RendererCache list = RendererCache.getCompatibleRenderers(requireContext());
        rendererListPreference.setEntries(list.rendererDisplayNames);
        rendererListPreference.setEntryValues(list.rendererIds.toArray(new String[0]));
    }

    private void onRendererPicked(Uri uri) {
        if (uri == null) return;
        android.content.Context context = requireContext();
        String fileName = ExtraRenderers.fileNameOf(context, uri);
        if (!fileName.toLowerCase().endsWith(".so")) {
            Toast.makeText(context, R.string.fork_renderer_not_so, Toast.LENGTH_LONG).show();
            return;
        }
        ExtraRenderers.Preset preset = ExtraRenderers.presetFor(fileName);
        float d = context.getResources().getDisplayMetrics().density;
        int pad = (int) (16 * d);

        LinearLayout box = new LinearLayout(context);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(pad, pad / 2, pad, 0);
        EditText name = new EditText(context);
        name.setInputType(InputType.TYPE_CLASS_TEXT);
        name.setHint(R.string.fork_renderer_import_name);
        String suggested = preset != null ? preset.name : fileName.substring(0, fileName.length() - 3);
        name.setText(suggested);
        box.addView(name);
        RadioGroup group = new RadioGroup(context);
        RadioButton gles2 = new RadioButton(context);
        gles2.setId(android.view.View.generateViewId());
        gles2.setText(R.string.fork_renderer_type_gles2);
        RadioButton gles3 = new RadioButton(context);
        gles3.setId(android.view.View.generateViewId());
        gles3.setText(R.string.fork_renderer_type_gles3);
        group.addView(gles2);
        group.addView(gles3);
        group.check(preset != null && preset.gles >= 3 ? gles3.getId() : gles2.getId());
        box.addView(group);

        new AlertDialog.Builder(context)
                .setTitle(R.string.fork_renderer_import_type)
                .setView(box)
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton(android.R.string.ok, (dialog, which) -> {
                    String label = name.getText().toString().trim();
                    if (label.isEmpty()) label = suggested;
                    int gles = group.getCheckedRadioButtonId() == gles3.getId() ? 3 : 2;
                    try {
                        ExtraRenderers.importLibrary(context, uri, fileName, label, gles);
                        refreshRenderers();
                        Toast.makeText(context, R.string.fork_renderer_imported_ok, Toast.LENGTH_LONG).show();
                    } catch (Exception e) {
                        Toast.makeText(context, context.getString(R.string.fork_renderer_import_error, e.getMessage()), Toast.LENGTH_LONG).show();
                    }
                })
                .show();
    }

    private void showRemoveDialog() {
        android.content.Context context = requireContext();
        List<ExtraRenderers.Entry> entries = ExtraRenderers.list();
        if (entries.isEmpty()) {
            Toast.makeText(context, R.string.fork_renderer_none_imported, Toast.LENGTH_SHORT).show();
            return;
        }
        String[] names = new String[entries.size()];
        for (int i = 0; i < names.length; i++) names[i] = entries.get(i).name;
        new AlertDialog.Builder(context)
                .setTitle(R.string.fork_renderer_remove_title)
                .setItems(names, (dialog, which) -> {
                    ExtraRenderers.Entry e = entries.get(which);
                    if (e.tag().equals(LauncherPreferences.DEFAULT_PREF.getString("renderer", ""))) {
                        LauncherPreferences.DEFAULT_PREF.edit().remove("renderer").apply();
                    }
                    ExtraRenderers.remove(e);
                    refreshRenderers();
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    @Override
    public void onResume() {
        super.onResume();
        Activity activity = getActivity();
        if(activity != null) {
            requirePreference("ignoreNotch").setVisible(LauncherPreferences.hasNotch(activity));
        }
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences p, String s) {
        super.onSharedPreferenceChanged(p, s);
        computeVisibility();
    }

    private void computeVisibility(){
        requirePreference("force_vsync", SwitchPreferenceCompat.class)
                .setVisible(LauncherPreferences.PREF_USE_ALTERNATE_SURFACE);
    }
}
