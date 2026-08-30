package net.kdt.pojavlaunch.game.renderer.angle;

import net.kdt.pojavlaunch.Architecture;
import net.kdt.pojavlaunch.plugins.LibraryPlugin;

import java.io.File;
import java.util.Map;

public interface AngleDescriptor {
    String ANGLE_EGL = "libEGL_angle.so";
    String ANGLE_GLES = "libGLESv2_angle.so";
    String ENV_EGL = "LIBGL_EGL";
    String ENV_GLES = "LIBGL_GLES";

    File egl();
    File gles();
    void setEnvironment(Map<String, String> envMap);
    boolean supported();

    class SysAngleDescriptor implements AngleDescriptor {

        private static final String BASE_PATH = Architecture.is64BitsDevice() ? "/system/lib64/" : "/system/lib";

        @Override
        public File egl() {
            return new File(BASE_PATH, ANGLE_EGL);
        }

        @Override
        public File gles() {
            return new File(BASE_PATH, ANGLE_EGL);
        }

        @Override
        public void setEnvironment(Map<String, String> envMap) {
            envMap.put(ENV_EGL, egl().getAbsolutePath());
            envMap.put(ENV_GLES, gles().getAbsolutePath());
        }

        @Override
        public boolean supported() {
            return egl().exists() && gles().exists();
        }
    }
    class ExtAngleDescriptor implements AngleDescriptor {
        private final LibraryPlugin plugin;
        public ExtAngleDescriptor(LibraryPlugin plugin) {
            this.plugin = plugin;
        }

        @Override
        public File egl() {
            return plugin.resolve(ANGLE_EGL);
        }

        @Override
        public File gles() {
            return plugin.resolve(ANGLE_GLES);
        }

        @Override
        public void setEnvironment(Map<String, String> envMap) {
            envMap.put(ENV_EGL, egl().getAbsolutePath());
            envMap.put(ENV_GLES, gles().getAbsolutePath());
        }

        @Override
        public boolean supported() {
            return plugin != null && plugin.checkLibraries(ANGLE_EGL, ANGLE_GLES);
        }
    }
}
