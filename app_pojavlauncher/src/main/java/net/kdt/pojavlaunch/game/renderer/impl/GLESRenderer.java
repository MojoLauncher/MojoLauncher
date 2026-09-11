package net.kdt.pojavlaunch.game.renderer.impl;

import android.content.Context;
import android.util.Log;

import net.kdt.pojavlaunch.Tools;
import net.kdt.pojavlaunch.extra.ExtraConstants;
import net.kdt.pojavlaunch.extra.ExtraCore;
import net.kdt.pojavlaunch.game.renderer.Renderer;
import net.kdt.pojavlaunch.game.renderer.angle.AngleDescriptor;
import net.kdt.pojavlaunch.plugins.LibraryPlugin;
import net.kdt.pojavlaunch.prefs.LauncherPreferences;
import net.kdt.pojavlaunch.utils.JREUtils;

import java.io.File;
import java.util.Map;

import git.artdeell.mojo.R;
import git.artdeell.mojoexec.MojoExec;

public abstract class GLESRenderer implements Renderer {
    private boolean nsBypass = false;

    private AngleDescriptor getAngleDescriptor(Context context) {
        AngleDescriptor descriptor;
        LibraryPlugin anglePlugin = LibraryPlugin.discoverPlugin(context, LibraryPlugin.ID_ANGLE_PLUGIN);
        descriptor = new AngleDescriptor.ExtAngleDescriptor(anglePlugin);
        if (descriptor.supported()) {
            Log.i("GLESRenderer", "Using ANGLE through AnglePlugin");
            return descriptor;
        }
        descriptor = new AngleDescriptor.SysAngleDescriptor();
        if (descriptor.supported()) {
            // We can't access ANGLE libraries through classloader namespace
            this.nsBypass = true;
            Log.i("GLESRenderer", "Enabled ANGLE through system libraries");
            return descriptor;
        }
        return null;
    }

    @Override
    public void setupEnvironment(Context context, Map<String, String> envMap) {
        AngleDescriptor angle;
        if(LauncherPreferences.PREF_USE_ANGLE && ((angle = getAngleDescriptor(context)) != null))
            angle.setEnvironment(envMap);
        if (LauncherPreferences.PREF_DUMP_SHADERS)
            envMap.put("LIBGL_VGPU_DUMP", "1");
        envMap.put("force_glsl_extensions_warn", "true");
        envMap.put("allow_higher_compat_version", "true");
        envMap.put("allow_glsl_extension_directive_midshader", "true");
        // Prevent OptiFine (and other error-reporting stuff in Minecraft) from balooning the log
        envMap.put("LIBGL_NOERROR", "1");
    }

    @Override
    public boolean setupRenderer() {
        return MojoExec.prepareEgl(library(), nsBypass, true, Integer.parseInt((String) ExtraCore.getValue(ExtraConstants.OPEN_GL_VERSION)));
    }

    public static class LTWRenderer extends GLESRenderer {
        public boolean compatibleDevice(Context context) {
            return JREUtils.getDetectedVersion() >= 3 && new File(Tools.NATIVE_LIB_DIR, this.library()).exists();
        }
        public String name() {
            return "OpenLTW";
        }
        public int displayName() {
            return R.string.mcl_setting_renderer_ltw;
        }
        public String tag() {
            return "opengles3_ltw";
        }
        public String library() {
            return "libltw.so";
        }
    }
    public static class GL4ESRenderer extends GLESRenderer {
        public boolean compatibleDevice(Context context) {
            return true;
        }
        public String name() {
            return "GL4ES";
        }
        public int displayName() {
            return R.string.mcl_setting_renderer_gles2_4;
        }
        public String tag() {
            return "opengles2";
        }
        public String library() {
            return "libltw.so";
        }
    }
}
