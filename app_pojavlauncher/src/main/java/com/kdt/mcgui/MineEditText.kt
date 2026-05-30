package com.kdt.mcgui

import android.content.Context
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatEditText
import androidx.core.content.ContextCompat
import net.ashmeet.hyperlauncher.R

class MineEditText : AppCompatEditText {
    constructor(ctx: Context) : super(ctx) {
        init()
    }

    constructor(ctx: Context, attrs: AttributeSet?) : super(ctx, attrs) {
        init()
    }

    fun init() {
        setBackgroundColor(ContextCompat.getColor(getContext(), R.color.background_bottom_bar))
        setTextColor(ContextCompat.getColor(getContext(), R.color.primary_text))
        setHintTextColor(ContextCompat.getColor(getContext(), R.color.secondary_text))
        setPadding(20, 10, 20, 10)
    }
}
