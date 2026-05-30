package com.kdt

import android.content.Context
import android.graphics.Typeface
import android.util.AttributeSet
import android.view.View
import android.widget.CompoundButton
import android.widget.ImageButton
import android.widget.TextView
import android.widget.ToggleButton
import androidx.constraintlayout.widget.ConstraintLayout
import net.ashmeet.hyperlauncher.R
import net.kdt.pojavlaunch.Logger.eventLogListener
import net.kdt.pojavlaunch.Logger.setLogListener

/**
 * A class able to display logs to the user.
 * It has support for the Logger class
 */
class LoggerView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null) :
    ConstraintLayout(context, attrs) {
    private var mLogListener: eventLogListener? = null
    private var mLogToggle: ToggleButton? = null
    private var mScrollView: DefocusableScrollView? = null
    private var mLogTextView: TextView? = null


    init {
        init()
    }

    override fun setVisibility(visibility: Int) {
        super.setVisibility(visibility)
        // Triggers the log view shown state by default when viewing it
        mLogToggle?.setChecked(visibility == VISIBLE)
    }

    /**
     * Inflate the layout, and add component behaviors
     */
    private fun init() {
        inflate(getContext(), R.layout.view_logger, this)
        mLogTextView = findViewById(R.id.content_log_view)
        mLogTextView?.setTypeface(Typeface.MONOSPACE)
        //TODO clamp the max text so it doesn't go oob
        mLogTextView?.setMaxLines(Int.MAX_VALUE)
        mLogTextView?.setEllipsize(null)
        mLogTextView?.setVisibility(GONE)

        // Toggle log visibility
        mLogToggle = findViewById(R.id.content_log_toggle_log)
        mLogToggle?.setOnCheckedChangeListener { _, isChecked ->
            mLogTextView?.setVisibility(if (isChecked) VISIBLE else GONE)
            if (isChecked) {
                setLogListener(mLogListener)
            } else {
                mLogTextView?.setText("")
                setLogListener(null) // Makes the JNI code be able to skip expensive logger callbacks
                // NOTE: was tested by rapidly smashing the log on/off button, no sync issues found :)
            }
        }
        mLogToggle?.setChecked(false)

        // Remove the loggerView from the user View
        val cancelButton = findViewById<ImageButton>(R.id.log_view_cancel)
        cancelButton.setOnClickListener {
            this@LoggerView.setVisibility(GONE)
        }

        // Set the scroll view
        mScrollView = findViewById(R.id.content_log_scroll)
        mScrollView?.isKeepFocusing = true

        //Set up the autoscroll switch
        val autoscrollToggle = findViewById<ToggleButton>(R.id.content_log_toggle_autoscroll)
        autoscrollToggle.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) mScrollView?.fullScroll(View.FOCUS_DOWN)
            mScrollView?.isKeepFocusing = isChecked
        }
        autoscrollToggle.setChecked(true)

        // Listen to logs
        mLogListener = eventLogListener { text ->
            if (mLogTextView?.visibility != VISIBLE) return@eventLogListener
            post {
                mLogTextView?.append(text + '\n')
                if (mScrollView?.isKeepFocusing == true) mScrollView?.fullScroll(View.FOCUS_DOWN)
            }
        }
    }
}
