package net.kdt.pojavlaunch.customcontrols.gamepad

import net.kdt.pojavlaunch.GrabListener

interface GamepadDataProvider {
    val menuMap: GamepadMap?
    val gameMap: GamepadMap?
    val isGrabbing: Boolean
    fun attachGrabListener(grabListener: GrabListener?)
    fun detachGrabListener(grabListener: GrabListener?)
}
