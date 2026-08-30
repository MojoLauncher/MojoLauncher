package net.kdt.pojavlaunch.game.renderer;

import android.content.Context;

import java.util.Map;

/**
 * Interface representing a renderer
 */
public interface Renderer {
    /**
     * Check if the current device is able to use this renderer
     * @param context application context
     * @return whether the renderer is compatible
     */
    boolean compatibleDevice(Context context);

    /**
     * Renderer name (or tag?)
     * @return name
     */
    String name();

    /**
     * Renderer resource display name
     * @return resource id
     */
    int displayName();

    /**
     * Renderer tag
     * @return tag
     */
    String tag();

    /**
     * Renderer EGL library
     * @return library name or path
     */
    String library();

    /**
     * Setup renderer-specific environment variables
     * @param context application context
     * @param envMap environment map
     */
    void setupEnvironment(Context context, Map<String, String> envMap);

    /**
     * Setup this renderer in MojoExec. Prefer using {@link GameRenderer#maybeSetupRenderer()}
     * @param namespaceBypass if namespace bypass is needed
     * @return whether setup was successful
     */
    boolean setupRenderer(boolean namespaceBypass);
}
