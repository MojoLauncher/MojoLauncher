package com.kdt.mcgui;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;

import git.artdeell.mojo.utils.AccentColor;

/** A plain line or block filled with the accent color chosen in Appearance. */
public class AccentLine extends View {
    public AccentLine(Context context) {
        this(context, null);
    }

    public AccentLine(Context context, AttributeSet attrs) {
        super(context, attrs);
        setBackgroundColor(AccentColor.get());
    }
}
