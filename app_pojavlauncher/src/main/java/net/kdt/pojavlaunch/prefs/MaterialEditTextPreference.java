package net.kdt.pojavlaunch.prefs;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.preference.EditTextPreference;
import androidx.preference.PreferenceViewHolder;
import net.ashmeet.hyperlauncher.R;

public class MaterialEditTextPreference extends EditTextPreference {
    public MaterialEditTextPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        setLayoutResource(R.layout.app_preference_layout);
    }

    public MaterialEditTextPreference(Context context) {
        this(context, null);
    }

    @Override
    public void onBindViewHolder(@NonNull PreferenceViewHolder holder) {
        super.onBindViewHolder(holder);
        TextView valueView = (TextView) holder.findViewById(R.id.preference_value);
        if (valueView != null) {
            valueView.setText(getText());
        }
    }
}
