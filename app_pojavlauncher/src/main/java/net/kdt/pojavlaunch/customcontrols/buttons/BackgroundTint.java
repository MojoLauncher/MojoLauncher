package net.kdt.pojavlaunch.customcontrols.buttons;

import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.Resources;
import android.graphics.Color;
import android.util.TypedValue;

import androidx.core.graphics.ColorUtils;

public class BackgroundTint {
    public static final int BACKGROUND_DEFAULT_TINT_ALPHA = 60;
    public static final int BACKGROUND_TOGGLE_TINT_ALPHA = 128;

    private static int lastTheme = System.identityHashCode(BackgroundTint.class);

    private static final int[][] sState = new int[][] {
            new int[] {android.R.attr.state_activated}
    };
    private static final int[] sDefaultTint = new int[] {
            ColorUtils.setAlphaComponent(Color.WHITE, BACKGROUND_DEFAULT_TINT_ALPHA)
    };
    private static final int[] sToggleableTint = new int[] {
            ColorUtils.setAlphaComponent(Color.WHITE, BACKGROUND_TOGGLE_TINT_ALPHA)
    };

    public static final ColorStateList DEFAULT_TINT_LIST = new ColorStateList(
            sState, sDefaultTint
    );
    public static final ColorStateList TOGGLE_TINT_LIST = new ColorStateList(
            sState, sToggleableTint
    );

    public static void applyToggleTint(Context context) {
        Resources.Theme theme = context.getTheme();
        int themeHash = theme.hashCode();
        if(themeHash == lastTheme) return;
        final TypedValue value = new TypedValue();
        theme.resolveAttribute(android.R.attr.colorAccent, value, true);
        sToggleableTint[0] = ColorUtils.setAlphaComponent(value.data, BACKGROUND_TOGGLE_TINT_ALPHA);
        lastTheme = themeHash;
    }
}
