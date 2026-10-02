package com.kdt.mcgui;

import android.content.*;
import android.graphics.*;
import android.util.*;

import androidx.core.content.res.ResourcesCompat;

import git.artdeell.mojo.R;

public class MineButton extends androidx.appcompat.widget.AppCompatButton {
	
	public MineButton(Context ctx) {
		this(ctx, null);
	}
	
	public MineButton(Context ctx, AttributeSet attrs) {
		super(ctx, attrs);
		init(attrs != null && attrs.getAttributeValue("http://schemas.android.com/apk/res/android", "background") != null);
	}

	public void init() {
		init(false);
	}

	public void init(boolean hasCustomBackground) {
		setTypeface(ResourcesCompat.getFont(getContext(), R.font.noto_sans_bold));
		if (!hasCustomBackground) {
			setBackground(ResourcesCompat.getDrawable(getResources(), R.drawable.mine_button_background, null));
		}
		setTextSize(TypedValue.COMPLEX_UNIT_PX, getResources().getDimensionPixelSize(R.dimen._13ssp));
	}

}
