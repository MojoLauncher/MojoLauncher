package net.kdt.pojavlaunch.game.renderer.def;

public final class GLESConstants {
    // ANGLE library definitions
    public static final String ANGLE_EGL = "libEGL_angle.so";
    public static final String ANGLE_GLES = "libGLESv2_angle.so";

    // System OpenGLES library definitions
    public static final String NATIVE_EGL = "libEGL.so";
    public static final String NATIVE_GLES = "libGLESv2.so";

    // custom GLES environment variables (works on GL4ES/LTW)
    public static final String ENV_EGL = "LIBGL_EGL";
    public static final String ENV_GLES = "LIBGL_GLES";
}
