package net.kdt.pojavlaunch.game.renderer.impl;

import android.content.Context;

import net.kdt.pojavlaunch.extra.ExtraConstants;
import net.kdt.pojavlaunch.extra.ExtraCore;
import net.kdt.pojavlaunch.game.renderer.GameRenderer;
import net.kdt.pojavlaunch.game.renderer.Renderer;

import java.util.Map;

import git.artdeell.mojo.R;
import git.artdeell.mojoexec.MojoExec;

public class GL4ESRenderer implements Renderer {

    @Override
    public boolean compatibleDevice(Context context) {
        return true; // always compatible
    }

    @Override
    public String name() {
        return "GL4ES";
    }

    @Override
    public int displayName() {
        return R.string.mcl_setting_renderer_gles2_4;
    }

    @Override
    public String tag() {
        return GameRenderer.GL4ES_RENDERER;
    }

    @Override
    public String library() {
        return "libgl4es_114.so";
    }

    @Override
    public void setupEnvironment(Context context, Map<String, String> envMap) {
        // Prevent OptiFine (and other error-reporting stuff in Minecraft) from balooning the log
        envMap.put("LIBGL_NOERROR", "1");
        // On certain GLES drivers, overloading default functions shader hack fails, so disable it
        envMap.put("LIBGL_NOINTOVLHACK", "1");
        // Fix white color on banner and sheep, since GL4ES 1.1.5
        envMap.put("LIBGL_NORMALIZE", "1");
        envMap.put("LIBGL_MIPMAP", "3");
        // The OPEN GL version is changed according
        envMap.put("LIBGL_ES", (String) ExtraCore.getValue(ExtraConstants.OPEN_GL_VERSION));
    }

    @Override
    public boolean setupRenderer(boolean namespaceBypass) {
        return MojoExec.prepareEgl(library(), namespaceBypass, true, Integer.parseInt((String) ExtraCore.getValue(ExtraConstants.OPEN_GL_VERSION)));
    }
}