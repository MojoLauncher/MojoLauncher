package net.kdt.pojavlaunch.prefs;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.preference.ListPreference;
import androidx.preference.PreferenceViewHolder;
import net.ashmeet.hyperlauncher.R;

public class MaterialListPreference extends ListPreference {
    public MaterialListPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        setLayoutResource(R.layout.app_preference_layout);
        setWidgetLayoutResource(R.layout.preference_material_list);
    }

    public MaterialListPreference(Context context) {
        this(context, null);
    }

    @Override
    public void onBindViewHolder(@NonNull PreferenceViewHolder holder) {
        super.onBindViewHolder(holder);
        TextView valueView = (TextView) holder.findViewById(R.id.preference_value);
        if (valueView != null) {
            CharSequence entry = getEntry();
            valueView.setText(entry != null ? entry : "");
        }
    }
}
