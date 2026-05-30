package net.kdt.pojavlaunch.customcontrols.buttons

import android.annotation.SuppressLint
import android.view.MotionEvent
import android.view.ViewGroup
import net.kdt.pojavlaunch.customcontrols.ControlData
import net.kdt.pojavlaunch.customcontrols.ControlDrawerData
import net.kdt.pojavlaunch.customcontrols.ControlLayout
import net.kdt.pojavlaunch.customcontrols.handleview.EditControlSideDialog

@SuppressLint("ViewConstructor")
class ControlDrawer(
    val parentLayout: ControlLayout, //Getters
    val drawerData: ControlDrawerData
) : ControlButton(
    parentLayout, drawerData.properties
) {
    val buttons: ArrayList<ControlSubButton>?
    var areButtonsVisible: Boolean


    init {
        buttons = ArrayList<ControlSubButton>(drawerData.buttonProperties.size)
        areButtonsVisible = parentLayout.modifiable
    }


    fun addButton(button: ControlSubButton) {
        buttons!!.add(button)
        syncButtons()
        setControlButtonVisibility(button, areButtonsVisible)
    }

    private fun setControlButtonVisibility(button: ControlButton, isVisible: Boolean) {
        button.controlView?.visibility = if (isVisible) VISIBLE else GONE
    }

    private fun switchButtonVisibility() {
        areButtonsVisible = !areButtonsVisible
        val visibility = if (areButtonsVisible) VISIBLE else GONE
        for (button in buttons!!) {
            button.controlView?.visibility = visibility
        }
    }

    //Syncing stuff
    private fun alignButtons() {
        if (buttons == null) return
        if (drawerData.orientation == ControlDrawerData.Orientation.FREE) return
        val margin = ControlInterface.marginDistance.toInt()

        for (i in buttons.indices) {
            when (drawerData.orientation) {
                ControlDrawerData.Orientation.RIGHT -> {
                    buttons.get(i)
                        .setDynamicX(generateDynamicX(x + (drawerData.properties.width + margin) * (i + 1)))
                    buttons.get(i).setDynamicY(generateDynamicY(y))
                }

                ControlDrawerData.Orientation.LEFT -> {
                    buttons.get(i)
                        .setDynamicX(generateDynamicX(x - (drawerData.properties.width + margin) * (i + 1)))
                    buttons.get(i).setDynamicY(generateDynamicY(y))
                }

                ControlDrawerData.Orientation.UP -> {
                    buttons.get(i)
                        .setDynamicY(generateDynamicY(y - (drawerData.properties.height + margin) * (i + 1)))
                    buttons.get(i).setDynamicX(generateDynamicX(x))
                }

                ControlDrawerData.Orientation.DOWN -> {
                    buttons.get(i)
                        .setDynamicY(generateDynamicY(y + (drawerData.properties.height + margin) * (i + 1)))
                    buttons.get(i).setDynamicX(generateDynamicX(x))
                }

                else -> {}
            }
            buttons.get(i).updateProperties()
        }
    }


    private fun resizeButtons() {
        if (buttons == null || drawerData.orientation == ControlDrawerData.Orientation.FREE) return
        val baseProps = properties
        for (subButton in buttons) {
            subButton.properties.width = baseProps.width
            subButton.properties.height = baseProps.height

            subButton.updateProperties()
        }
    }

    fun syncButtons() {
        alignButtons()
        resizeButtons()
    }

    /**
     * Check whether or not the button passed as a parameter belongs to this drawer.
     * 
     * @param button The button to look for
     * @return Whether the button is in the buttons list of the drawer.
     */
    fun containsChild(button: ControlInterface?): Boolean {
        for (childButton in buttons!!) {
            if (childButton === button) return true
        }
        return false
    }

    override fun preProcessProperties(properties: ControlData, layout: ControlLayout): ControlData {
        val data = super.preProcessProperties(properties, layout)
        data.isHideable = true
        return data
    }

    override fun setVisible(isVisible: Boolean) {
        val visibility = if (isVisible) VISIBLE else GONE
        setVisibility(visibility)
        if (visibility == GONE || areButtonsVisible) {
            for (button in buttons!!) {
                button.controlView?.visibility =
                    if (isVisible) VISIBLE else if (!(properties.isHideable) && visibility == GONE) VISIBLE else GONE
            }
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (controlLayoutParent?.modifiable != true) {
            when (event.getActionMasked()) {
                MotionEvent.ACTION_UP, MotionEvent.ACTION_POINTER_UP -> switchButtonVisibility()
            }
            return true
        }

        return super.onTouchEvent(event)
    }


    override fun setX(x: Float) {
        super.setX(x)
        alignButtons()
    }

    override fun setY(y: Float) {
        super.setY(y)
        alignButtons()
    }

    override fun setLayoutParams(params: ViewGroup.LayoutParams?) {
        super.setLayoutParams(params)
        syncButtons()
    }

    override fun canSnap(button: ControlInterface): Boolean {
        val result = super.canSnap(button)
        return result && !containsChild(button)
    }

    override fun loadEditValues(editControlPopup: EditControlSideDialog?) {
        editControlPopup?.loadValues(drawerData)
    }

    override fun cloneButton() {
        val cloneData = ControlDrawerData(this.drawerData)
        cloneData.properties.dynamicX = "0.5 * \${screen_width}"
        cloneData.properties.dynamicY = "0.5 * \${screen_height}"
        (parent as ControlLayout).addDrawer(cloneData)
    }

    override fun removeButton() {
        val layout = controlLayoutParent ?: return
        for (subButton in buttons!!) {
            layout.removeView(subButton)
        }

        layout.layout?.mDrawerDataList?.remove(this.drawerData)
        layout.removeView(this)
    }
}
