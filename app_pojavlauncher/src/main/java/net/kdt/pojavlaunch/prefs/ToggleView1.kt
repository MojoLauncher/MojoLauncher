package net.kdt.pojavlaunch.prefs

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.Checkable

class ToggleView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null) :
    View(context, attrs), Checkable {
    private var checked = false
    private val drawable: ToggleDrawable

    init {
        drawable = ToggleDrawable(context, false)
        setBackground(drawable)

        setClickable(true)
        setFocusable(true)

        setOnClickListener(OnClickListener { v: View? -> toggle() })
    }

    override fun setChecked(checked: Boolean) {
        if (this.checked == checked) return

        this.checked = checked

        drawable.setChecked(checked, true)

        refreshDrawableState()
        invalidate()
    }

    override fun isChecked(): Boolean {
        return checked
    }

    override fun toggle() {
        setChecked(!checked)
    }
}