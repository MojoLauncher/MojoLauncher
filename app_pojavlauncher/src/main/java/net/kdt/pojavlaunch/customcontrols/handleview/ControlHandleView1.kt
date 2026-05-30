package net.kdt.pojavlaunch.customcontrols.handleview

import android.annotation.SuppressLint
import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import androidx.core.content.res.ResourcesCompat
import net.ashmeet.hyperlauncher.R
import net.kdt.pojavlaunch.customcontrols.buttons.ControlInterface

class ControlHandleView : View {
    constructor(context: Context?) : super(context) {
        init()
    }

    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs) {
        init()
    }

    private val mDrawable = ResourcesCompat.getDrawable(
        getResources(),
        R.drawable.ic_view_handle,
        getContext().getTheme()
    )
    private var mView: ControlInterface? = null
    private var mXOffset = 0f
    private var mYOffset = 0f
    private val mPositionListener: ViewTreeObserver.OnPreDrawListener =
        object : ViewTreeObserver.OnPreDrawListener {
            override fun onPreDraw(): Boolean {
                if (mView == null || mView!!.controlView?.isShown != true) {
                    hide()
                    return true
                }

                setX(mView!!.controlView!!.x + mView!!.controlView!!.width)
                setY(mView!!.controlView!!.y + mView!!.controlView!!.height)
                return true
            }
        }

    private fun init() {
        val size = getResources().getDimensionPixelOffset(R.dimen._22sdp)
        mDrawable!!.setBounds(0, 0, size, size)
        val params = ViewGroup.LayoutParams(size, size)
        setLayoutParams(params)
        setBackground(mDrawable)
        setTranslationZ(10.5f)
    }

    fun setControlButton(controlInterface: ControlInterface?) {
        if (mView != null) mView!!.controlView!!.viewTreeObserver
            .removeOnPreDrawListener(mPositionListener)

        if (controlInterface == null || controlInterface.controlView == null) return
        setVisibility(VISIBLE)
        mView = controlInterface
        mView!!.controlView!!.viewTreeObserver.addOnPreDrawListener(mPositionListener)

        setX(
            controlInterface.controlView!!.x + controlInterface.controlView!!.width
        )
        setY(
            controlInterface.controlView!!.y + controlInterface.controlView!!.height
        )
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.getActionMasked()) {
            MotionEvent.ACTION_DOWN -> {
                mXOffset = event.getX()
                mYOffset = event.getY()
            }

            MotionEvent.ACTION_MOVE -> {
                setX(getX() + event.getX() - mXOffset)
                setY(getY() + event.getY() - mYOffset)

                println(getX() - mView!!.controlView!!.x)
                println(getY() - mView!!.controlView!!.y)


                mView!!.properties!!.setWidth(getX() - mView!!.controlView!!.x)
                mView!!.properties!!.setHeight(getY() - mView!!.controlView!!.y)
                mView!!.regenerateDynamicCoordinates()
            }
        }

        return true
    }

    fun hide() {
        if (mView != null && mView!!.controlView != null) mView!!.controlView!!.viewTreeObserver
            .removeOnPreDrawListener(mPositionListener)
        setVisibility(GONE)
    }
}
