package com.kdt.mcgui

import android.content.Context
import android.util.AttributeSet
import android.view.View

/** DELETED - Functionality moved to Compose TaskProgressItem in LauncherScreen.kt */
class TextProgressBar @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {
    fun setProgress(progress: Int) {}
    fun setText(text: String) {}
    fun setText(resid: Int) {}
    fun setTextPadding(padding: Int) {}
}
