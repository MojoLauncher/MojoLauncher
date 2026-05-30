package net.kdt.pojavlaunch.customcontrols.mouse

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.view.View.OnLayoutChangeListener
import android.view.ViewGroup
import android.view.ViewGroup.MarginLayoutParams
import net.kdt.pojavlaunch.GrabListener
import net.kdt.pojavlaunch.LwjglGlfwKeycode
import net.kdt.pojavlaunch.prefs.LauncherPreferences
import net.kdt.pojavlaunch.utils.MCOptionUtils.MCOptionListener
import net.kdt.pojavlaunch.utils.MCOptionUtils.addMCOptionListener
import net.kdt.pojavlaunch.utils.MCOptionUtils.mcScale
import net.kdt.pojavlaunch.utils.MathUtils.map
import org.lwjgl.glfw.CallbackBridge.addGrabListener
import org.lwjgl.glfw.CallbackBridge.isGrabbing
import org.lwjgl.glfw.CallbackBridge.removeGrabListener
import org.lwjgl.glfw.CallbackBridge.sendKeyPress

class HotbarView : View, MCOptionListener, OnLayoutChangeListener, Runnable {
    private val mDoubleTapDetector = TapDetector(2, TapDetector.Companion.DETECTION_METHOD_DOWN)
    private var mParentView: View? = null
    private val mDropGesture = DropGesture(Handler(Looper.getMainLooper()))
    private val mGrabListener: GrabListener = object : GrabListener {
        override fun onGrabState(isGrabbing: Boolean) {
            mLastIndex = -1
            mDropGesture.cancel()
        }
    }

    private var mWidth = 0
    private var mLastIndex = -1
    private var mGuiScale = 0

    constructor(context: Context?) : super(context) {
        initialize()
    }

    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs) {
        initialize()
    }

    constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    ) {
        initialize()
    }

    @Suppress("unused") // You suggested me this constructor, Android
    constructor(
        context: Context?,
        attrs: AttributeSet?,
        defStyleAttr: Int,
        defStyleRes: Int
    ) : super(context, attrs, defStyleAttr, defStyleRes) {
        initialize()
    }

    private fun initialize() {
        addMCOptionListener(this)
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        val parent = getParent()
        if (parent == null) return
        if (parent is View) {
            mParentView = parent as View
            mParentView!!.addOnLayoutChangeListener(this)
        }
        mGuiScale = mcScale()
        repositionView()
        addGrabListener(mGrabListener)
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        removeGrabListener(mGrabListener)
    }

    private fun repositionView() {
        val layoutParams = getLayoutParams()
        if (layoutParams !is MarginLayoutParams) throw RuntimeException("Incorrect LayoutParams type, expected ViewGroup.MarginLayoutParams")
        val parent = getParent() as ViewGroup
        val marginLayoutParams = layoutParams
        val height: Int
        mWidth = mcScale(180)
        marginLayoutParams.width = mWidth
        height = mcScale(20)
        marginLayoutParams.height = height
        marginLayoutParams.leftMargin = (parent.getWidth() / 2) - (mWidth / 2)
        marginLayoutParams.topMargin = parent.getHeight() - height
        setLayoutParams(marginLayoutParams)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (!isGrabbing) return false
        val hasDoubleTapped = mDoubleTapDetector.onTouchEvent(event)

        // Check if we need to cancel the drop event
        val actionMasked = event.getActionMasked()
        if (isLastEventInGesture(actionMasked)) mDropGesture.cancel()
        else mDropGesture.submit()
        // Determine the hotbar slot
        val x = event.getX()
        // Ignore positions equal to mWidth because they would translate into an out-of-bounds hotbar index
        if (x < 0 || x >= mWidth) {
            // If out of bounds, cancel the hotbar gesture to avoid dropping items on last hotbar slots
            mDropGesture.cancel()
            return true
        }
        val hotbarIndex = map(x, 0f, mWidth.toFloat(), 0f, HOTBAR_KEYS.size.toFloat()).toInt()
        // Check if the slot changed and we need to make a key press
        if (hotbarIndex == mLastIndex) {
            // Only check for doubletapping if the slot has not changed
            if (hasDoubleTapped && !LauncherPreferences.PREF_DISABLE_SWAP_HAND) sendKeyPress(
                LwjglGlfwKeycode.GLFW_KEY_F.toInt()
            )
            return true
        }
        mLastIndex = hotbarIndex
        val hotbarKey: Int = HOTBAR_KEYS[hotbarIndex]
        sendKeyPress(hotbarKey)
        // Cancel the event since we changed hotbar slots.
        mDropGesture.cancel()
        // Only resubmit the gesture only if it isn't the last event we will receive.
        if (!isLastEventInGesture(actionMasked)) mDropGesture.submit()
        return true
    }

    private fun isLastEventInGesture(actionMasked: Int): Boolean {
        return actionMasked == MotionEvent.ACTION_UP || actionMasked == MotionEvent.ACTION_CANCEL
    }

    private fun mcScale(input: Int): Int {
        return ((mGuiScale * input) / LauncherPreferences.PREF_SCALE_FACTOR).toInt()
    }

    /** Forces the view to reposition itself.  */
    fun onResolutionChanged() {
        if (getParent() == null) return
        mGuiScale = mcScale()
        post(Runnable { this.repositionView() })
    }

    override fun onOptionChanged() {
        post(this)
    }

    override fun run() {
        if (getParent() == null) return
        val scale = mcScale()
        if (scale == mGuiScale) return
        mGuiScale = scale
        repositionView()
    }

    override fun onLayoutChange(
        v: View,
        left: Int,
        top: Int,
        right: Int,
        bottom: Int,
        oldLeft: Int,
        oldTop: Int,
        oldRight: Int,
        oldBottom: Int
    ) {
        // We need to check whether dimensions match or not because here we are looking specifically for changes of dimensions
        // and Android keeps calling this without dimensions actually changing for some reason.
        if (v == mParentView && (left != oldLeft || right != oldRight || top != oldTop || bottom != oldBottom)) {
            // Need to post this, because it is not correct to resize the view
            // during a layout pass.
            post(Runnable { this.repositionView() })
        }
    }

    companion object {
        private val HOTBAR_KEYS = intArrayOf(
            LwjglGlfwKeycode.GLFW_KEY_1.toInt(),
            LwjglGlfwKeycode.GLFW_KEY_2.toInt(),
            LwjglGlfwKeycode.GLFW_KEY_3.toInt(),
            LwjglGlfwKeycode.GLFW_KEY_4.toInt(),
            LwjglGlfwKeycode.GLFW_KEY_5.toInt(),
            LwjglGlfwKeycode.GLFW_KEY_6.toInt(),
            LwjglGlfwKeycode.GLFW_KEY_7.toInt(),
            LwjglGlfwKeycode.GLFW_KEY_8.toInt(),
            LwjglGlfwKeycode.GLFW_KEY_9.toInt()
        )
    }
}
