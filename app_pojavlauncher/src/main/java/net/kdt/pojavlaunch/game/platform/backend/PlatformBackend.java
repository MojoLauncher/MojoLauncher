package net.kdt.pojavlaunch.game.platform.backend;

import android.view.Surface;

/**
 * Platform abstraction to send events to the running app
 */
public interface PlatformBackend {
    /**
     * Send a surface object to the implementation. Call this whenever the surface was recreated/became invalid
     *
     * @param surface Surface
     */
    void surfaceCreated(Surface surface);

    /**
     * Update implementation surface parameters. Call this when the surface has been resized
     */
    void surfaceUpdated();

    /**
     * Destroy implementation surface
     */
    void surfaceDestroyed();

    /**
     * Send current mouse position set in Platform to an implementation
     */
    void sendMousePosition();

    /**
     * Send mouse event to an implementation
     *
     * @param button Android mouse button to send
     * @param state  State (down/up)
     * @param mods   Modifier keys
     */
    void sendMouseEvent(int button, int state, int mods);

    /**
     * Send keyboard key press event to an implementation
     *
     * @param key       Android keycode to send
     * @param state     State (down/up)
     * @param mods      Modifier keys
     * @param codepoint Unicode symbol tied to the sent keycode
     * @return True if succeeded, false if unknown/unsupported keycode
     */
    boolean sendKeyEvent(int key, int state, int mods, char codepoint);

    /**
     * Send keyboard key press event to an implementation
     *
     * @param key   Android keycode to send
     * @param state State (down/up)
     * @param mods  Modifier keys
     * @return True if succeeded, false if unknown/unsupported keycode
     */
    boolean sendKeyEvent(int key, int state, int mods);

    /**
     * Send keyboard key press event to an implementation
     *
     * @param key   Android keycode to send
     * @param state State (down/up)
     * @param mods  Modifier keys
     * @return True if succeeded, false if unknown/unsupported keycode
     */
    boolean sendKeyEvent(int key, boolean state, int mods);

    /**
     * Send mouse wheel/touchpad scroll event to an implementation
     *
     * @param x X axis scroll
     * @param y Y axis scroll
     */
    void sendScrollEvent(double x, double y);

    /**
     * Send a text bulk to an implementation
     *
     * @param text String (text) to send
     * @param mods Modifier keys
     */
    void sendBulkUnicodeEvent(String text, int mods);

    /**
     * Get current implementation backend name
     *
     * @return Backend name
     */
    String backendName();

    /**
     * Set window hover state on an implementation
     *
     * @param hovered Hover state
     */
    void setHovered(boolean hovered);

    /**
     * Set visibility state on an implementation
     *
     * @param visible Visibility state
     */
    void setVisible(boolean visible);
}
