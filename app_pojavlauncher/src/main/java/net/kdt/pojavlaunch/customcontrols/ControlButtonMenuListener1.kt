package net.kdt.pojavlaunch.customcontrols

import net.kdt.pojavlaunch.customcontrols.buttons.ControlInterface

interface ControlButtonMenuListener {
    fun onClickedMenu()
    fun editControlButton(button: ControlInterface)
}
