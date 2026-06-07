package net.kdt.pojavlaunch.customcontrols.gamepad;


import git.artdeell.dnbootstrap.glfw.GrabListener;

public interface GamepadDataProvider {
    GamepadMap getMenuMap();
    GamepadMap getGameMap();
    boolean isGrabbing();
    void attachGrabListener(GrabListener grabListener);
}
