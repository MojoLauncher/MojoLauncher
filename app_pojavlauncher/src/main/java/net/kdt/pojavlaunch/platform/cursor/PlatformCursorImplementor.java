package net.kdt.pojavlaunch.platform.cursor;

import net.kdt.pojavlaunch.platform.input.PlatformGrabListener;

public interface PlatformCursorImplementor extends PlatformGrabListener {
    void onCursorPosition();
    void onCursorChanged();
}
