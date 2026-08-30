package net.kdt.pojavlaunch.game.renderer.impl;

import android.content.Context;

import net.kdt.pojavlaunch.Tools;
import net.kdt.pojavlaunch.game.renderer.Renderer;
import net.kdt.pojavlaunch.utils.JREUtils;

import java.io.File;
import java.util.Map;

import git.artdeell.mojo.R;
import git.artdeell.mojoexec.MojoExec;

public class LTWRenderer implements Renderer {

    @Override
    public boolean compatibleDevice(Context context) {
        return JREUtils.getDetectedVersion() >= 3 && new File(Tools.NATIVE_LIB_DIR, this.library()).exists();
    }

    @Override
    public String name() {
        return "LTW";
    }

    @Override
    public int displayName() {
        return R.string.mcl_setting_renderer_ltw;
    }

    @Override
    public String tag() {
        return "";
    }

    @Override
    public String library() {
        return "libltw.so";
    }

    @Override
    public void setupEnvironment(Context context, Map<String, String> envMap) {
        // Prevent OptiFine (and other error-reporting stuff in Minecraft) from balooning the log
        envMap.put("LIBGL_NOERROR", "1");
    }

    @Override
    public boolean setupRenderer(boolean namespaceBypass) {
        return MojoExec.prepareEgl(library(), namespaceBypass, true, 3);
    }
}
