package net.kdt.pojavlaunch.customcontrols.gamepad;


import static net.kdt.pojavlaunch.platform.PlatformLibrary.PLATFORM;

import net.kdt.pojavlaunch.platform.PlatformGrabListener;
import net.kdt.pojavlaunch.platform.PlatformLibrary;


public class DefaultDataProvider implements GamepadDataProvider {
    public static final DefaultDataProvider INSTANCE = new DefaultDataProvider();

    // Cannot instantiate this class publicly
    private DefaultDataProvider() {}

    @Override
    public GamepadMap getGameMap() {
        return GamepadMapStore.getGameMap();
    }


    @Override
    public GamepadMap getMenuMap() {
        return GamepadMapStore.getMenuMap();
    }

    @Override
    public boolean isGrabbing() {
        // Avoid going through the JNI each time.
        return PLATFORM.isGrabbing();
    }

    @Override
    public void attachGrabListener(PlatformGrabListener grabListener) {
        PlatformLibrary.addGrabListener(grabListener);
    }
}
