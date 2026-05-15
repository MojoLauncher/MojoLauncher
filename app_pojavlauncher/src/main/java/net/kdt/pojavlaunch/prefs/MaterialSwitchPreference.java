package net.kdt.pojavlaunch.prefs;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.Checkable;

import androidx.annotation.NonNull;
import androidx.preference.PreferenceViewHolder;
import androidx.preference.SwitchPreferenceCompat;

import net.ashmeet.hyperlauncher.R;

public class MaterialSwitchPreference extends SwitchPreferenceCompat {
    public MaterialSwitchPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        setLayoutResource(R.layout.app_preference_layout);
        setWidgetLayoutResource(R.layout.preference_material_switch);
    }

    public MaterialSwitchPreference(Context context) {
        this(context, null);
    }

    @Override
    public void onBindViewHolder(@NonNull PreferenceViewHolder holder) {
        super.onBindViewHolder(holder);

        View switchWidget = holder.findViewById(android.R.id.switch_widget);

        if (switchWidget instanceof ToggleView) {

            ToggleView toggle = (ToggleView) switchWidget;

            // Sync state from preference
            toggle.setChecked(isChecked());

            // Handle clicks
            toggle.setOnClickListener(v -> {
                boolean newValue = !isChecked();

                if (callChangeListener(newValue)) {
                    setChecked(newValue);
                    toggle.setChecked(newValue);
                }
            });
        }

        if (getSwitchTextOn() == null) setSwitchTextOn("");
        if (getSwitchTextOff() == null) setSwitchTextOff("");
    }
}
