package com.kdt.mcgui;

import android.content.*;
import android.graphics.*;
import android.graphics.drawable.Drawable;
import android.util.*;

import androidx.core.content.res.ResourcesCompat;
import androidx.core.graphics.drawable.DrawableCompat;

import net.kdt.pojavlaunch.Tools;

import git.artdeell.mojo.R;

public class MineButton extends androidx.appcompat.widget.AppCompatButton {
	
	public MineButton(Context ctx) {
		this(ctx, null);
	}
	
	public MineButton(Context ctx, AttributeSet attrs) {
		super(ctx, attrs);
		init();
	}

	public void init() {
		setTypeface(ResourcesCompat.getFont(getContext(), R.font.noto_sans_bold));
		setTextSize(TypedValue.COMPLEX_UNIT_PX, getResources().getDimensionPixelSize(R.dimen._13ssp));

		Drawable bg = ResourcesCompat.getDrawable(getResources(), R.drawable.mine_button_background, null);
		if (bg != null) {
			bg = DrawableCompat.wrap(bg);
			bg.setColorFilter(new PorterDuffColorFilter(
					Tools.getColorAttr(getContext().getTheme(), R.attr.colorMineButton),
					PorterDuff.Mode.OVERLAY
			));
			setBackground(bg);
		}
	}
}
