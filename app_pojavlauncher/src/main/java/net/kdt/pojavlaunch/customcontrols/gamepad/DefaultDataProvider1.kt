package net.kdt.pojavlaunch.customcontrols.gamepad

import net.kdt.pojavlaunch.GrabListener
import org.lwjgl.glfw.CallbackBridge
import org.lwjgl.glfw.CallbackBridge.addGrabListener
import org.lwjgl.glfw.CallbackBridge.removeGrabListener

class DefaultDataProvider  // Cannot instantiate this class publicly
private constructor() : GamepadDataProvider {
    override val gameMap: GamepadMap?
        get() = GamepadMapStore.gameMap

    override val menuMap: GamepadMap?
        get() = GamepadMapStore.menuMap

    override val isGrabbing: Boolean
        get() = CallbackBridge.isGrabbing

    override fun attachGrabListener(grabListener: GrabListener?) {
        addGrabListener(grabListener)
    }

    override fun detachGrabListener(grabListener: GrabListener?) {
        removeGrabListener(grabListener)
    }

    companion object {
        val INSTANCE: DefaultDataProvider = DefaultDataProvider()
    }
}
