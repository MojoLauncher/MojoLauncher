package net.kdt.pojavlaunch.customcontrols.handleview

import android.annotation.SuppressLint
import android.content.Context
import android.util.AttributeSet
import android.widget.Button
import net.ashmeet.hyperlauncher.R
import net.kdt.pojavlaunch.customcontrols.buttons.ControlInterface

@SuppressLint("AppCompatCustomView")
class DeleteButton : Button, ActionButtonInterface {
    constructor(context: Context?) : super(context) {
        init()
    }

    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs) {
        init()
    }

    override fun init() {
        setOnClickListener(this)
        setAllCaps(true)
        setText(R.string.global_delete)
    }

    private var mCurrentlySelectedButton: ControlInterface? = null

    override fun shouldBeVisible(): Boolean {
        return mCurrentlySelectedButton != null
    }

    override fun setFollowedView(view: ControlInterface?) {
        mCurrentlySelectedButton = view
    }

    override fun onClick() {
        if (mCurrentlySelectedButton == null) return

        mCurrentlySelectedButton!!.removeButton()
    }
}
