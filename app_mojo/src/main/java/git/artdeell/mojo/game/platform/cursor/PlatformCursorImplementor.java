package git.artdeell.mojo.game.platform.cursor;

import git.artdeell.mojo.game.platform.input.PlatformGrabListener;

/**
 * Platform cursor implementor. Receives cursor updates
 */
public interface PlatformCursorImplementor extends PlatformGrabListener {
    /**
     * Update cursor position on the screen
     */
    void onCursorPosition();

    /**
     * Update cursor drawable on the screen
     */
    void onCursorChanged();
}
