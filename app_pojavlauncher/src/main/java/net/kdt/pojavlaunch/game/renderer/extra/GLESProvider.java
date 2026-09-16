package net.kdt.pojavlaunch.game.renderer.extra;

import static net.kdt.pojavlaunch.game.renderer.def.GLESConstants.ANGLE_EGL;
import static net.kdt.pojavlaunch.game.renderer.def.GLESConstants.ANGLE_GLES;
import static net.kdt.pojavlaunch.game.renderer.def.GLESConstants.ENV_EGL;
import static net.kdt.pojavlaunch.game.renderer.def.GLESConstants.ENV_GLES;
import static net.kdt.pojavlaunch.game.renderer.def.GLESConstants.NATIVE_EGL;
import static net.kdt.pojavlaunch.game.renderer.def.GLESConstants.NATIVE_GLES;

import android.content.Context;

import net.kdt.pojavlaunch.Architecture;
import net.kdt.pojavlaunch.game.renderer.impl.GLESRenderSpec;
import net.kdt.pojavlaunch.plugins.LibraryPlugin;

import java.io.File;
import java.util.Map;

/**
 * OpenGL ES driver provider for {@link GLESRenderSpec} based renderers (a.k.a. wrappers on-top of OpenGL ES)
 */
public interface GLESProvider {
    /**
     * Get fitting OpenGL ES provider for the current device
     *
     * @param context     Application context
     * @param preferAngle Whether the ANGLE provider should be selected
     * @return OpenGL ES provider
     */
    static GLESProvider getGlesProvider(Context context, boolean preferAngle) {
        if (!preferAngle) return new NativeGLESProvider();
        GLESProvider provider;
        // External ANGLE takes priority over system ANGLE so we can override it easily
        LibraryPlugin anglePlugin = LibraryPlugin.discoverPlugin(context, LibraryPlugin.ID_ANGLE_PLUGIN);
        provider = new GLESProvider.ExternalAngleProvider(anglePlugin);
        if (provider.supported()) {
            return provider;
        }
        provider = new GLESProvider.SystemAngleProvider();
        if (provider.supported()) {
            return provider;
        }
        return new NativeGLESProvider();
    }

    /**
     * Name of the provider
     *
     * @return name
     */
    String type();

    /**
     * OpenGL EGL library name or the absolute path to it
     *
     * @return path
     */

    String eglPath();

    /**
     * OpenGL ES driver library name or the absolute path to it
     *
     * @return path
     */
    String glesPath();

    /**
     * {@link File} of the EGL library. You can use this to check if the library exists
     *
     * @return instance of {@link File}
     */
    File egl();

    /**
     * {@link File} of the OpenGL ES library. ou can use this to check if the library exists
     *
     * @return instance of {@link File}
     */
    File gles();

    /**
     * Set environment needed for this OpenGL ES provider
     *
     * @param envMap environment map
     */
    default void setEnvironment(Map<String, String> envMap) {
        envMap.put(ENV_EGL, eglPath());
        envMap.put(ENV_GLES, glesPath());
    }

    /**
     * Check if the current device supports this OpenGL ES provider
     *
     * @return state
     */
    boolean supported();

    /**
     * Check if the current OpenGL ES provider requires to load its libraries in a global/unrestricted namespace to avoid linker issues
     *
     * @return state
     */
    boolean requiresNamespace();

    /**
     * Native OpenGL ES provider. Doesn't do much as the wrappers already use it automatically if no EGL/GLES override was given, but we still implement this
     * for the correctness
     */
    class NativeGLESProvider implements GLESProvider {
        public String type() {
            return "Native OpenGL ES Driver";
        }
        public String eglPath() {
            return NATIVE_EGL;
        }
        public String glesPath() {
            return NATIVE_GLES;
        }
        public File egl() {
            return null;
        }
        public File gles() {
            return null;
        }
        public void setEnvironment(Map<String, String> envMap) {
        }
        public boolean supported() {
            return true; // Native GLES is always present even in a form of ANGLE (hello Samsung)
        }
        public boolean requiresNamespace() {
            return false;
        }
    }

    /**
     * System ANGLE provider. Android 15+ devices often have ANGLE libraries located in their system partition, so we can take advantage of them
     */
    class SystemAngleProvider implements GLESProvider {
        private static final String BASE_PATH = Architecture.is64BitsDevice() ? "/system/lib64/" : "/system/lib";
        public String type() {
            return "System ANGLE";
        }
        public String eglPath() {
            return gles().getAbsolutePath();
        }
        public String glesPath() {
            return gles().getAbsolutePath();
        }
        public File egl() {
            return new File(BASE_PATH, ANGLE_EGL);
        }
        public File gles() {
            return new File(BASE_PATH, ANGLE_EGL);
        }
        public boolean supported() {
            return egl().exists() && gles().exists();
        }
        public boolean requiresNamespace() {
            return true;
        }
    }

    /**
     * External ANGLE provider. Loads ANGLE libraries through {@link LibraryPlugin} (AnglePlugin) hence requires it to be installed on the device.
     * Useful for using newer ANGLE, patching ANGLE to overcome OpenGL ES restrictions or when nsbypass misbehaves on this device
     */
    class ExternalAngleProvider implements GLESProvider {
        private final LibraryPlugin plugin;
        public ExternalAngleProvider(LibraryPlugin plugin) {
            this.plugin = plugin;
        }
        public String type() {
            return "External ANGLE";
        }
        public String eglPath() {
            return plugin.resolve(ANGLE_EGL).getAbsolutePath();
        }
        public String glesPath() {
            return gles().getAbsolutePath();
        }
        public File egl() {
            return plugin.resolve(ANGLE_EGL);
        }
        public File gles() {
            return plugin.resolve(ANGLE_GLES);
        }
        public boolean supported() {
            return plugin != null && plugin.checkLibraries(ANGLE_EGL, ANGLE_GLES);
        }
        public boolean requiresNamespace() {
            return false;
        }
    }
    // One might add other OpenGLES providers (such as Mesa and/or bundled ANGLE), but this is not something we want right now
}
