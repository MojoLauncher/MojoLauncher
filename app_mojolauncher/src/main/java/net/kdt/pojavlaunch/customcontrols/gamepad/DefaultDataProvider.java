package net.kdt.pojavlaunch.customcontrols.gamepad;


import git.artdeell.dnbootstrap.glfw.GLFW;
import git.artdeell.dnbootstrap.glfw.GrabListener;

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
        return GLFW.isGrabbing();
    }

    @Override
    public void attachGrabListener(GrabListener grabListener) {
        GLFW.addGrabListener(grabListener);
    }
}
