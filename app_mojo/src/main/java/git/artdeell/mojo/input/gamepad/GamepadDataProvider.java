package git.artdeell.mojo.input.gamepad;


import git.artdeell.mojo.game.platform.input.PlatformGrabListener;

public interface GamepadDataProvider {
    GamepadMap getMenuMap();
    GamepadMap getGameMap();
    boolean isGrabbing();
    void attachGrabListener(PlatformGrabListener grabListener);
}
