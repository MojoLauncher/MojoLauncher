package net.kdt.pojavlaunch.prefs;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.Checkable;

import androidx.annotation.Nullable;

public class ToggleView extends View implements Checkable {

    private boolean checked = false;
    private ToggleDrawable drawable;

    public ToggleView(Context context) {
        this(context, null);
    }

    public ToggleView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);

        drawable = new ToggleDrawable(false);
        setBackground(drawable);

        setClickable(true);
        setFocusable(true);

        setOnClickListener(v -> toggle());
    }

    @Override
    public void setChecked(boolean checked) {
        if (this.checked == checked) return;

        this.checked = checked;

        drawable.setChecked(checked, true);

        refreshDrawableState();
        invalidate();
    }

    @Override
    public boolean isChecked() {
        return checked;
    }

    @Override
    public void toggle() {
        setChecked(!checked);
    }
}