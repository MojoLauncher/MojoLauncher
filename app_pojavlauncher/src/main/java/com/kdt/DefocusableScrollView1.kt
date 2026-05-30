package com.kdt

import android.content.Context
import android.graphics.Rect
import android.util.AttributeSet
import android.widget.ScrollView

/**
 * Class allowing to ignore the focusing from an item such an EditText within it.
 * Ignoring it will stop the scrollView from refocusing on the view
 */
class DefocusableScrollView : ScrollView {
    var isKeepFocusing: Boolean = false


    constructor(context: Context?) : super(context)

    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs)

    constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    )

    override fun computeScrollDeltaToGetChildRectOnScreen(rect: Rect?): Int {
        if (!this.isKeepFocusing) return 0
        return super.computeScrollDeltaToGetChildRectOnScreen(rect)
    }
}
