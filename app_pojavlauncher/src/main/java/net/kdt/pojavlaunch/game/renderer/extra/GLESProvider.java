package net.kdt.pojavlaunch.game.renderer.extra;

import static net.kdt.pojavlaunch.game.renderer.extra.GLESConstants.ANGLE_EGL;
import static net.kdt.pojavlaunch.game.renderer.extra.GLESConstants.ANGLE_GLES;
import static net.kdt.pojavlaunch.game.renderer.extra.GLESConstants.ENV_EGL;
import static net.kdt.pojavlaunch.game.renderer.extra.GLESConstants.ENV_GLES;
import static net.kdt.pojavlaunch.game.renderer.extra.GLESConstants.NATIVE_EGL;

import android.content.Context;
import android.util.Log;

import net.kdt.pojavlaunch.Architecture;
import net.kdt.pojavlaunch.plugins.LibraryPlugin;

import java.io.File;
import java.util.Map;

public interface GLESProvider {
    String type();

    String eglPath();

    String glesPath();
    File egl();
    File gles();

    default void setEnvironment(Map<String, String> envMap) {
        envMap.put(ENV_EGL, eglPath());
        envMap.put(ENV_GLES, glesPath());
    }

    boolean supported();
    boolean requiresNamespace();

    static GLESProvider getGlesProvider(Context context, boolean preferAngle) {
        if(!preferAngle) return new NativeGLESProvider();
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

    // Stub class providing native OpenGL ES driver for GLES wrappers
    // These wrappers load it automatically if the custom driver wasn't provided
    // hence this class is a simple stub
    class NativeGLESProvider implements GLESProvider {
        public String type() {
            return "Native OpenGL ES Driver";
        }
        public String eglPath() {
            return NATIVE_EGL;
        }
        public String glesPath() {
            return NATIVE_EGL;
        }
        public File egl() {
            return null;
        }
        public File gles() {
            return null;
        }
        public void setEnvironment(Map<String, String> envMap) {}
        public boolean supported() {
            return true;
        }
        public boolean requiresNamespace() {
            return false;
        }
    }
    // System ANGLE provider for devices with Android 15+ (or older if it exists there)
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

    // External ANGLE provider, see AnglePlugin
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
}
