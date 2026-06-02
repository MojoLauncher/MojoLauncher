package net.kdt.pojavlaunch.customcontrols.gamepad.direct;

/**
 * Interface that is called once when the GLFW implementation requests to switch from
 * the default gamepad implementation to the direct one. The implementor of this interface
 * must take the necessary steps to disable the default gamepad implementation and replace
 * it with an instance of DirectGamepad.

 * This is useful for switching from default to direct input after the user has already
 * interacted with the gamepad.
 */
public interface DirectGamepadEnableHandler {
    /**
     * Called once when GLFW requests switching the gamepad mode from default to direct.
     */
    void onDirectGamepadEnabled();
}
