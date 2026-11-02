package git.artdeell.mojo.prefs;

import android.content.Context;
import android.util.AttributeSet;

import androidx.preference.Preference;

import git.artdeell.mojo.R;

/** Title card at the top of the settings: not clickable, only shows the title, a tagline and an icon. */
public class HeaderPreference extends Preference {
    public HeaderPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    @SuppressWarnings("unused") public HeaderPreference(Context context) {
        this(context, null);
    }

    private void init() {
        setLayoutResource(R.layout.pref_header);
        setSelectable(false);
        setIconSpaceReserved(false);
    }
}
