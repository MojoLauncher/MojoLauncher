package com.kdt

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ObjectAnimator
import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.Button
import android.widget.ImageButton
import android.widget.TextView
import androidx.annotation.CallSuper
import androidx.annotation.LayoutRes
import androidx.annotation.StringRes
import androidx.core.content.res.ResourcesCompat
import net.ashmeet.hyperlauncher.R


/**
 * The base class for side dialog views
 * A side dialog is a dialog appearing from one side of the screen
 */
abstract class SideDialogView(
    context: Context,
    private val mParent: ViewGroup,
    @field:LayoutRes @param:LayoutRes private val mLayoutId: Int
) {
    private var mDialogLayout: ViewGroup? = null
    private var mScrollView: DefocusableScrollView? = null
    protected var mDialogContent: View? = null

    protected val mMargin: Int
    private var mSideDialogAnimator: ObjectAnimator? = null

    /** @return Whether the dialog is currently displaying
     */
    var isDisplaying: Boolean = false
        protected set

    /* UI elements */
    private var mStartButton: Button? = null
    private var mEndButton: Button? = null
    private var mCloseButton: ImageButton? = null
    private var mTitleTextview: TextView? = null
    private var mTitleDivider: View? = null

    /* Data to store when the UI element has yet to be inflated */
    @StringRes
    private var mStartButtonStringId = 0

    @StringRes
    private var mEndButtonStringId = 0

    @StringRes
    private var mTitleStringId = 0
    private var mStartButtonListener: View.OnClickListener? = null
    private var mEndButtonListener: View.OnClickListener? = null
    private var mCloseButtonListener: View.OnClickListener? = null


    init {
        mMargin = context.resources.getDimensionPixelOffset(R.dimen._20sdp)
    }

    fun setTitle(@StringRes textId: Int) {
        mTitleStringId = textId
        if (mDialogLayout != null) {
            mTitleTextview?.setText(textId)
            mTitleTextview?.visibility = View.VISIBLE
            mTitleDivider?.visibility = View.VISIBLE
        }
    }

    fun setStartButtonListener(@StringRes textId: Int, listener: View.OnClickListener?) {
        mStartButtonStringId = textId
        mStartButtonListener = listener
        if (mDialogLayout != null) mStartButton?.let { setButton(it, textId, listener) }
    }

    fun setEndButtonListener(@StringRes textId: Int, listener: View.OnClickListener?) {
        mEndButtonStringId = textId
        mEndButtonListener = listener
        if (mDialogLayout != null) mEndButton?.let { setButton(it, textId, listener) }
    }

    fun setCloseButtonListener(listener: View.OnClickListener?) {
        mCloseButtonListener = listener
        if (mDialogLayout != null) {
            mCloseButton?.setOnClickListener(listener ?: View.OnClickListener {
                disappear(true)
            })
        }
    }

    private fun setButton(button: Button, @StringRes textId: Int, listener: View.OnClickListener?) {
        button.setText(textId)
        button.setOnClickListener(listener)
        button.visibility = View.VISIBLE
    }


    private fun inflateLayout() {
        if (mDialogLayout != null) {
            Log.w("SideDialogView", "Layout already inflated")
            return
        }

        // Inflate layouts
        mDialogLayout = LayoutInflater.from(mParent.context)
            .inflate(R.layout.dialog_side_dialog, mParent, false) as ViewGroup?
        mScrollView =
            mDialogLayout?.findViewById(R.id.side_dialog_scrollview)
        mStartButton = mDialogLayout?.findViewById(R.id.side_dialog_start_button)
        mEndButton = mDialogLayout?.findViewById(R.id.side_dialog_end_button)
        mCloseButton = mDialogLayout?.findViewById(R.id.side_dialog_close_button)
        mTitleTextview = mDialogLayout?.findViewById(R.id.side_dialog_title_textview)
        mTitleDivider = mDialogLayout?.findViewById(R.id.side_dialog_title_divider)

        mCloseButton?.setOnClickListener(mCloseButtonListener ?: View.OnClickListener {
            disappear(true)
        })

        LayoutInflater.from(mParent.context).inflate(mLayoutId, mScrollView, true)
        mDialogContent = mScrollView?.getChildAt(0)

        // Attach layouts
        mParent.addView(mDialogLayout)

        mSideDialogAnimator = ObjectAnimator.ofFloat(mDialogLayout, "x", 0f).setDuration(600)
        mSideDialogAnimator?.interpolator = AccelerateDecelerateInterpolator()

        mDialogLayout?.elevation = 10f
        mDialogLayout?.translationZ = 10f

        mDialogLayout?.visibility = View.VISIBLE
        mDialogLayout?.let {
            it.background = ResourcesCompat.getDrawable(
                it.resources,
                R.drawable.background_control_editor,
                null
            )
        }

        //TODO offset better according to view width
        mDialogLayout?.let {
            it.x = -it.resources.getDimensionPixelOffset(R.dimen._280sdp).toFloat()
        }

        // Set up UI elements
        if (mTitleStringId != 0) setTitle(mTitleStringId)
        if (mStartButtonStringId != 0) setStartButtonListener(
            mStartButtonStringId,
            mStartButtonListener
        )
        if (mEndButtonStringId != 0) setEndButtonListener(mEndButtonStringId, mEndButtonListener)
    }

    /** Destroy the layout, cleanup variables  */
    private fun deflateLayout() {
        if (mDialogLayout == null) {
            Log.w("SideDialogView", "Layout not inflated")
            return
        }

        mSideDialogAnimator?.removeAllUpdateListeners()
        mSideDialogAnimator?.removeAllListeners()

        mParent.removeView(mDialogLayout)

        mDialogLayout = null
        mScrollView = null
        mSideDialogAnimator = null
        mDialogContent = null
        mTitleTextview = null
        mTitleDivider = null
        mStartButton = null
        mEndButton = null
        mCloseButton = null
    }


    /**
     * Slide the layout into the visible screen area
     */
    @CallSuper
    fun appear(fromRight: Boolean) {
        if (mDialogLayout == null) {
            inflateLayout()
            onInflate()
        }

        // To avoid UI sizing issue when the dialog is not fully inflated
        onAppear()
        val parent = this.parent
        mScrollView?.post {
            if (mDialogLayout == null) return@post
            val animator = mSideDialogAnimator ?: throw RuntimeException("Unexpected side animator state when dialog is inflated")
            if (fromRight) {
                if (!this.isDisplaying || !this.isAtRight) {
                    animator.setFloatValues(
                        parent.width.toFloat(),
                        (parent.width - (mScrollView?.width ?: 0) - mMargin).toFloat()
                    )
                    animator.start()
                    this.isDisplaying = true
                }
            } else {
                if (!this.isDisplaying || this.isAtRight) {
                    animator.setFloatValues(
                        -(mDialogLayout?.width?.toFloat() ?: 0f),
                        mMargin.toFloat()
                    )
                    animator.start()
                    this.isDisplaying = true
                }
            }
        }
    }

    protected val isAtRight: Boolean
        get() {
            val layout = mDialogLayout ?: throw RuntimeException("attempted to check dialog position when deflated")
            return layout.x > this.parent.width / 2f
        }

    /**
     * Slide out the layout
     * @param destroy Whether the layout should be destroyed after disappearing.
     * Recommended to be true if the layout is not going to be used anymore
     */
    @CallSuper
    fun disappear(destroy: Boolean) {
        if (mDialogLayout == null) {
            Log.w("SideDialogView", "Layout not inflated")
            return
        }

        if (!this.isDisplaying) {
            if (destroy) {
                onDisappear()
                onDestroy()
                deflateLayout()
            }
            return
        }

        this.isDisplaying = false
        if (this.isAtRight) mSideDialogAnimator?.setFloatValues(
            (this.parent.width - (mDialogLayout?.width ?: 0) - mMargin).toFloat(),
            this.parent.width.toFloat()
        )
        else mSideDialogAnimator?.setFloatValues(
            mMargin.toFloat(),
            -(mDialogLayout?.width?.toFloat() ?: 0f)
        )

        if (destroy) {
            onDisappear()
            onDestroy()
            mSideDialogAnimator?.addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    deflateLayout()
                }
            })
        }

        mSideDialogAnimator?.start()
    }

    private val parent: ViewGroup
        get() = mDialogLayout?.parent as ViewGroup

    /**
     * Called when the dialog is inflated, ideal for setting up UI elements bindings
     */
    protected open fun onInflate() {}

    /**
     * Called after the dialog has appeared
     */
    protected fun onAppear() {}

    /**
     * Called after the dialog has disappeared
     */
    protected fun onDisappear() {}

    /**
     * Called before the dialog gets destroyed (removing views from parent)
     * Ideal for cleaning up resources
     */
    protected open fun onDestroy() {}
}
