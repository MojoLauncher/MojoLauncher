package com.kdt.mcgui;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.LightingColorFilter;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.util.TypedValue;

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
		Resources.Theme theme = getContext().getTheme();
		setTypeface(ResourcesCompat.getFont(getContext(), Tools.getReferenceAttr(theme, R.attr.fontMineButton)));
        setTextSize(TypedValue.COMPLEX_UNIT_PX, Tools.getDimensionSizeAttr(theme, R.attr.fontSizeMineButton));

		Drawable bg = Tools.getDrawableAttr(theme, R.attr.drawableMineButtonBackground);
		if (bg != null) {
			bg = DrawableCompat.wrap(bg);
			bg.setColorFilter(new LightingColorFilter(
					Tools.getColorAttr(theme, R.attr.colorMineButton),
					Tools.getColorAttr(theme, R.attr.colorMineButtonAdd)
			));
			setBackground(bg);
		}
	}
}
