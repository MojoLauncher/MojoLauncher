package net.kdt.pojavlaunch.customcontrols.buttons

import android.annotation.SuppressLint
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import net.kdt.pojavlaunch.customcontrols.ControlData
import net.kdt.pojavlaunch.customcontrols.ControlDrawerData
import net.kdt.pojavlaunch.customcontrols.ControlLayout
import net.kdt.pojavlaunch.customcontrols.handleview.EditControlSideDialog

@SuppressLint("ViewConstructor")
class ControlSubButton(
    layout: ControlLayout,
    properties: ControlData?,
    val parentDrawer: ControlDrawer?
) : ControlButton(layout, properties ?: ControlData("SubButton")) {
    init {
        filterProperties()
    }

    private fun filterProperties() {
        if (parentDrawer != null && parentDrawer.drawerData.orientation != ControlDrawerData.Orientation.FREE) {
            properties.setHeight(parentDrawer.properties.getHeight())
            properties.setWidth(parentDrawer.properties.getWidth())
        }

        setProperties(properties, false)
    }

    override fun setVisible(isVisible: Boolean) {
        // STUB, visibility handled by the ControlDrawer
    }

    override fun onGrabState(isGrabbing: Boolean) {
        // STUB, visibility lifecycle handled by the ControlDrawer
    }

    override fun setLayoutParams(params: ViewGroup.LayoutParams?) {
        if (params != null && parentDrawer != null && parentDrawer.drawerData.orientation != ControlDrawerData.Orientation.FREE) {
            params.width = parentDrawer.properties.getWidth().toInt()
            params.height = parentDrawer.properties.getHeight().toInt()
        }
        super.setLayoutParams(params)
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (controlLayoutParent?.modifiable == false || parentDrawer?.drawerData?.orientation == ControlDrawerData.Orientation.FREE) {
            return super.onTouchEvent(event)
        }

        if (event.actionMasked == MotionEvent.ACTION_UP) {
            onLongClick(this)
        }
        return true
    }

    override fun cloneButton() {
        val cloneData = ControlData(properties)
        cloneData.dynamicX = "0.5 * \${screen_width}"
        cloneData.dynamicY = "0.5 * \${screen_height}"
        controlLayoutParent?.addSubButton(parentDrawer!!, cloneData)
    }

    override fun removeButton() {
        parentDrawer?.drawerData?.buttonProperties?.remove(properties)
        parentDrawer?.buttons?.remove(this)

        parentDrawer?.syncButtons()

        super.removeButton()
    }

    override fun snapAndAlign(x: Float, y: Float) {
        if (parentDrawer?.drawerData?.orientation == ControlDrawerData.Orientation.FREE) {
            super.snapAndAlign(x, y)
        }
        // Else the button is forced into place
    }

    override fun loadEditValues(editControlDialog: EditControlSideDialog?) {
        if (parentDrawer != null) {
            editControlDialog?.loadSubButtonValues(properties, parentDrawer.drawerData.orientation!!)
        }
    }
}
