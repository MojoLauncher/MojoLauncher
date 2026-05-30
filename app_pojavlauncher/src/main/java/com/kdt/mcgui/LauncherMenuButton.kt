package com.kdt.mcgui

import android.content.Context
import android.util.AttributeSet
import android.util.TypedValue
import android.view.Gravity
import androidx.core.content.res.ResourcesCompat
import fr.spse.extended_view.ExtendedButton
import net.ashmeet.hyperlauncher.R

class LauncherMenuButton : ExtendedButton {
    constructor(context: Context) : super(context) {
        setSettings()
    }

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
        setSettings()
    }


    /** Set style stuff  */
    private fun setSettings() {
        val resources = getContext().getResources()

        val padding = resources.getDimensionPixelSize(R.dimen._22sdp)
        setCompoundDrawablePadding(padding)
        setPaddingRelative(padding, 0, 0, 0)
        setGravity(Gravity.CENTER_VERTICAL)
        setTextSize(
            TypedValue.COMPLEX_UNIT_PX,
            getResources().getDimensionPixelSize(R.dimen._12ssp).toFloat()
        )
        setBackground(
            ResourcesCompat.getDrawable(
                getResources(),
                R.drawable.menu_background,
                getContext().getTheme()
            )
        )
        // Set drawable size
        val sizes = getExtendedViewData().getSizeCompounds()
        sizes[0] = resources.getDimensionPixelSize(R.dimen._30sdp)
        getExtendedViewData().setSizeCompounds(sizes)
        postProcessDrawables()
    }
}
