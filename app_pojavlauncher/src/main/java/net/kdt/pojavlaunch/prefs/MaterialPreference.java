package net.kdt.pojavlaunch.prefs;

import android.content.Context;
import android.util.AttributeSet;
import androidx.preference.Preference;

import net.ashmeet.hyperlauncher.R;


public class MaterialPreference extends Preference {
    public MaterialPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        setLayoutResource(R.layout.app_preference_layout);
    }

    public MaterialPreference(Context context) {
        this(context, null);
    }
}
