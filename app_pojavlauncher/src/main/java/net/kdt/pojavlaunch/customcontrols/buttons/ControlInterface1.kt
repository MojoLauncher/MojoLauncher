package net.kdt.pojavlaunch.customcontrols.buttons

import android.annotation.SuppressLint
import android.graphics.Color
import android.graphics.Paint
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.LayerDrawable
import android.view.MotionEvent
import android.view.View
import android.view.View.OnLayoutChangeListener
import android.view.View.OnLongClickListener
import androidx.core.math.MathUtils
import net.kdt.pojavlaunch.Tools.dpToPx
import net.kdt.pojavlaunch.customcontrols.ControlData
import net.kdt.pojavlaunch.customcontrols.ControlLayout
import net.kdt.pojavlaunch.customcontrols.handleview.EditControlSideDialog
import net.kdt.pojavlaunch.prefs.LauncherPreferences
import net.kdt.pojavlaunch.utils.MathUtils.calculateConstraint
import kotlin.math.abs

interface ControlInterface : OnLongClickListener {
    val controlView: View?
    val properties: ControlData?

    val controlLayoutParent: ControlLayout?
        get() = controlView?.parent as? ControlLayout

    fun setProperties(properties: ControlData?, changePos: Boolean) {
        if (properties == null) return
        val v = controlView ?: return
        if (changePos) updateProperties()
        v.alpha = properties.opacity
        setBackground()
    }

    fun setBackground() {
        val v = controlView ?: return
        val props = properties ?: return

        val drawable = GradientDrawable()
        drawable.setColor(props.bgColor)
        drawable.setStroke(dpToPx(props.strokeWidth).toInt(), props.strokeColor)
        drawable.cornerRadius = computeCornerRadius(props.cornerRadius)

        val layers = arrayOf<Drawable>(drawable)
        val layerDrawable = LayerDrawable(layers)
        v.background = layerDrawable
    }

    fun updateProperties() {
        val v = controlView ?: return
        val props = properties ?: return
        val parent = controlLayoutParent ?: return

        if (parent.width > 0 && parent.height > 0) {
            v.x = props.insertDynamicPos(props.dynamicX!!, parent.width, parent.height)
            v.y = props.insertDynamicPos(props.dynamicY!!, parent.width, parent.height)
        }
        v.layoutParams.width = props.width.toInt()
        v.layoutParams.height = props.height.toInt()
        v.requestLayout()
    }

    fun computeCornerRadius(cornerRadius: Float): Float {
        val v = controlView ?: return 0f
        return cornerRadius * (if (v.width < v.height) v.width else v.height) / 200f
    }

    fun setVisible(visible: Boolean) {
        controlView?.visibility = if (visible) View.VISIBLE else View.GONE
    }

    fun onGrabState(isGrabbing: Boolean) {}

    fun loadEditValues(editControlDialog: EditControlSideDialog?)
    fun cloneButton()
    fun removeButton()
    fun handlePressed()
    fun handleReleased()

    fun preProcessProperties(properties: ControlData, parent: ControlLayout): ControlData {
        if (properties.dynamicX == null || properties.dynamicY == null) {
            properties.dynamicX = properties.insertDynamicPos(properties.dynamicX!!, parent.width, parent.height).toString()
            properties.dynamicY = properties.insertDynamicPos(properties.dynamicY!!, parent.width, parent.height).toString()
        }
        return properties
    }

    fun snapAndAlign(x: Float, y: Float) {
        val v = controlView ?: return
        val props = properties ?: return
        val parent = controlLayoutParent ?: return

        var finalX = x
        var finalY = y

        val snap = snapDistance
        val margin = marginDistance

        // Snap to parent edges
        if (finalX < margin + snap) finalX = margin
        if (finalX > parent.width - v.width - margin - snap) finalX = parent.width - v.width - margin

        if (finalY < margin + snap) finalY = margin
        if (finalY > parent.height - v.height - margin - snap) finalY = parent.height - v.height - margin

        // Snap to other buttons
        for (other in parent.buttonChildren!!) {
            val otherView = other.controlView
            if (otherView == null || otherView == v || otherView.visibility != View.VISIBLE) continue

            // Vertical alignment
            if (abs(finalX - otherView.x) < snap) finalX = otherView.x
            if (abs(finalX + v.width - (otherView.x + otherView.width)) < snap) finalX = otherView.x + otherView.width - v.width
            if (abs(finalX - (otherView.x + otherView.width + margin)) < snap) finalX = otherView.x + otherView.width + margin
            if (abs(finalX + v.width - (otherView.x - margin)) < snap) finalX = otherView.x - margin - v.width

            // Horizontal alignment
            if (abs(finalY - otherView.y) < snap) finalY = otherView.y
            if (abs(finalY + v.height - (otherView.y + otherView.height)) < snap) finalY = otherView.y + otherView.height - v.height
            if (abs(finalY - (otherView.y + otherView.height + margin)) < snap) finalY = otherView.y + otherView.height + margin
            if (abs(finalY + v.height - (otherView.y - margin)) < snap) finalY = otherView.y - margin - v.height
        }

        v.x = finalX
        v.y = finalY

        regenerateDynamicCoordinates()
    }

    fun regenerateDynamicCoordinates() {
        val v = controlView ?: return
        val props = properties ?: return
        val parent = controlLayoutParent ?: return

        props.dynamicX = calculateConstraint(v.x, parent.width.toFloat(), v.width.toFloat(), true)
        props.dynamicY = calculateConstraint(v.y, parent.height.toFloat(), v.height.toFloat(), false)
    }

    fun generateDynamicX(x: Float): String {
        val parent = controlLayoutParent ?: return "0"
        val v = controlView ?: return "0"
        return calculateConstraint(x, parent.width.toFloat(), v.width.toFloat(), true)
    }

    fun generateDynamicY(y: Float): String {
        val parent = controlLayoutParent ?: return "0"
        val v = controlView ?: return "0"
        return calculateConstraint(y, parent.height.toFloat(), v.height.toFloat(), false)
    }

    fun setDynamicX(dynamicX: String) {
        properties?.dynamicX = dynamicX
    }

    fun setDynamicY(dynamicY: String) {
        properties?.dynamicY = dynamicY
    }

    @SuppressLint("ClickableViewAccessibility")
    fun injectBehaviors() {
        val v = controlView ?: return
        v.setOnLongClickListener(this)
        v.setOnTouchListener(object : View.OnTouchListener {
            var downX = 0f
            var downY = 0f
            var downRawX = 0f
            var downRawY = 0f
            var mCanTriggerLongClick = false

            override fun onTouch(v: View, event: MotionEvent): Boolean {
                val parent = controlLayoutParent ?: return false
                if (!parent.modifiable) return false

                when (event.actionMasked) {
                    MotionEvent.ACTION_DOWN -> {
                        mCanTriggerLongClick = true
                        downRawX = event.rawX
                        downRawY = event.rawY
                        downX = downRawX - v.x
                        downY = downRawY - v.y
                    }

                    MotionEvent.ACTION_MOVE -> {
                        if (abs(event.rawX - downRawX) > 8 || abs(event.rawY - downRawY) > 8) mCanTriggerLongClick =
                            false
                        
                        snapAndAlign(
                            MathUtils.clamp(
                                event.rawX - downX,
                                0f,
                                (parent.width - v.width).toFloat()
                            ),
                            MathUtils.clamp(
                                event.rawY - downY,
                                0f,
                                (parent.height - v.height).toFloat()
                            )
                        )
                    }

                    MotionEvent.ACTION_UP -> {
                        if (mCanTriggerLongClick) onLongClick(v)
                        v.translationX = 0f
                        v.translationY = 0f
                        v.requestLayout()
                    }
                }
                return true
            }
        })
    }

    fun injectLayoutParamBehavior() {
        this.controlView?.addOnLayoutChangeListener(OnLayoutChangeListener { v: View?, l: Int, t: Int, r: Int, b: Int, ol: Int, or: Int, ot: Int, ob: Int -> setBackground() })
    }

    override fun onLongClick(v: View?): Boolean {
        val parent = controlLayoutParent ?: return false
        if (parent.modifiable) {
            parent.editControlButton(this)
        }

        return true
    }

    fun canSnap(button: ControlInterface): Boolean {
        return true
    }

    companion object {
        val snapDistance: Float
            get() = dpToPx(6f)

        val marginDistance: Float
            get() = dpToPx(2f)

    }
}
