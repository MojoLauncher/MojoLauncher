package com.kdt.mcgui;

import android.content.Context;
import android.util.AttributeSet;

import androidx.appcompat.widget.AppCompatTextView;

import git.artdeell.mojo.utils.AccentColor;

/** Text drawn with a light shade of the accent color chosen in Appearance. */
public class AccentText extends AppCompatTextView {
    public AccentText(Context context) {
        this(context, null);
    }

    public AccentText(Context context, AttributeSet attrs) {
        super(context, attrs);
        setTextColor(AccentColor.light());
    }
}
