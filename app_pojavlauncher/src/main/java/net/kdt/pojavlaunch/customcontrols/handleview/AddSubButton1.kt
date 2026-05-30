package net.kdt.pojavlaunch.customcontrols.handleview

import android.annotation.SuppressLint
import android.content.Context
import android.util.AttributeSet
import android.widget.Button
import net.ashmeet.hyperlauncher.R
import net.kdt.pojavlaunch.customcontrols.ControlData
import net.kdt.pojavlaunch.customcontrols.buttons.ControlDrawer
import net.kdt.pojavlaunch.customcontrols.buttons.ControlInterface

@SuppressLint("AppCompatCustomView")
class AddSubButton : Button, ActionButtonInterface {
    constructor(context: Context?) : super(context) {
        init()
    }

    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs) {
        init()
    }

    override fun init() {
        setText(R.string.customctrl_addsubbutton)
        setOnClickListener(this)
    }

    private var mCurrentlySelectedButton: ControlInterface? = null

    override fun shouldBeVisible(): Boolean {
        return mCurrentlySelectedButton != null && mCurrentlySelectedButton is ControlDrawer
    }

    override fun setFollowedView(view: ControlInterface?) {
        mCurrentlySelectedButton = view
    }

    override fun onClick() {
        if (mCurrentlySelectedButton is ControlDrawer) {
            val drawer = mCurrentlySelectedButton as ControlDrawer
            drawer.controlLayoutParent?.addSubButton(drawer, ControlData())
        }
    }
}
