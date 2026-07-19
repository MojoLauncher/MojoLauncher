package net.kdt.pojavlaunch.customcontrols.gamepad;


import net.kdt.pojavlaunch.platform.input.PlatformGrabListener;

public interface GamepadDataProvider {
    GamepadMap getMenuMap();
    GamepadMap getGameMap();
    boolean isGrabbing();
    void attachGrabListener(PlatformGrabListener grabListener);
}
