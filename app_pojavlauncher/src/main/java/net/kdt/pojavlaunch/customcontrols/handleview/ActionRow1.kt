package net.kdt.pojavlaunch.customcontrols.handleview

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import android.widget.LinearLayout
import androidx.core.math.MathUtils
import net.ashmeet.hyperlauncher.R
import net.kdt.pojavlaunch.customcontrols.buttons.ControlInterface

/**
 * Layout floating around a Control Button, displaying contextual actions
 */
class ActionRow : LinearLayout {
    constructor(context: Context?) : super(context) {
        init()
    }

    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs) {
        init()
    }

    val mFollowedViewListener: ViewTreeObserver.OnPreDrawListener =
        object : ViewTreeObserver.OnPreDrawListener {
            override fun onPreDraw(): Boolean {
                if (mFollowedView == null || !mFollowedView!!.isShown()) {
                    hide()
                    return true
                }

                setNewPosition()
                return true
            }
        }
    private val actionButtons: Array<ActionButtonInterface>
    private var mFollowedView: View? = null
    private val mSide: Int = SIDE_AUTO

    /** Add action buttons and configure them  */
    private fun init() {
        setTranslationZ(11f)
        setVisibility(GONE)
        setOrientation(HORIZONTAL)
        setLayoutParams(
            LayoutParams(
                LayoutParams.WRAP_CONTENT,
                getResources().getDimensionPixelOffset(R.dimen._40sdp)
            )
        )

        // Initialized here to avoid nullability issues with arrayOfNulls
        actionButtons[0] = DeleteButton(getContext())
        actionButtons[1] = CloneButton(getContext())
        actionButtons[2] = AddSubButton(getContext())

        // This is not pretty code, don't do this.
        for (buttonInterface in actionButtons) {
            val button = ((buttonInterface) as View?)
            addView(button, LayoutParams(0, LayoutParams.MATCH_PARENT, 1f))
        }

        setElevation(5f)
    }

    init {
        actionButtons = arrayOf(
            DeleteButton(context),
            CloneButton(context),
            AddSubButton(context)
        )
    }

    fun setFollowedButton(controlInterface: ControlInterface?) {
        if (mFollowedView != null) mFollowedView!!.getViewTreeObserver()
            .removeOnPreDrawListener(mFollowedViewListener)

        for (buttonInterface in actionButtons) {
            buttonInterface.setFollowedView(controlInterface)
            ((buttonInterface) as View).setVisibility(if (buttonInterface.shouldBeVisible()) VISIBLE else GONE)
        }

        setVisibility(VISIBLE)
        mFollowedView = controlInterface as View?
        if (mFollowedView != null) mFollowedView!!.getViewTreeObserver()
            .addOnPreDrawListener(mFollowedViewListener)
    }

    private fun getXPosition(side: Int): Float {
        if (side == SIDE_LEFT) {
            return mFollowedView!!.getX() - getWidth()
        } else if (side == SIDE_RIGHT) {
            return mFollowedView!!.getX() + mFollowedView!!.getWidth()
        } else {
            return mFollowedView!!.getX() + mFollowedView!!.getWidth() / 2f - getWidth() / 2f
        }
    }

    private fun getYPosition(side: Int): Float {
        if (side == SIDE_TOP) {
            return mFollowedView!!.getY() - getHeight()
        } else if (side == SIDE_BOTTOM) {
            return mFollowedView!!.getY() + mFollowedView!!.getHeight()
        } else {
            return mFollowedView!!.getY() + mFollowedView!!.getHeight() / 2f - getHeight() / 2f
        }
    }

    private fun setNewPosition() {
        if (mFollowedView == null) return
        val side = pickSide()

        setX(
            MathUtils.clamp(
                getXPosition(side),
                0f,
                ((getParent() as ViewGroup).getWidth() - getWidth()).toFloat()
            )
        )
        setY(getYPosition(side))
    }

    private fun pickSide(): Int {
        if (mFollowedView == null) return mSide //Value should not matter


        if (mSide != SIDE_AUTO) return mSide
        //TODO improve the "algo"
        val parent = (mFollowedView!!.getParent() as ViewGroup?)
        if (parent == null) return mSide //Value should not matter


        var side: Int = SIDE_TOP
        val futurePos = getYPosition(side)
        if (futurePos + getHeight() > (parent.getHeight() + getHeight() / 2f)) {
            side = SIDE_TOP
        } else if (futurePos < -getHeight() / 2f) {
            side = SIDE_BOTTOM
        }

        return side
    }

    fun hide() {
        if (mFollowedView != null) mFollowedView!!.getViewTreeObserver()
            .removeOnPreDrawListener(mFollowedViewListener)
        setVisibility(GONE)
    }

    companion object {
        const val SIDE_LEFT: Int = 0x0
        const val SIDE_TOP: Int = 0x1
        const val SIDE_RIGHT: Int = 0x2
        const val SIDE_BOTTOM: Int = 0x3
        const val SIDE_AUTO: Int = 0x4
    }
}
