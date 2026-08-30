package net.kdt.pojavlaunch.game.renderer.impl;

import static android.os.Build.VERSION.SDK_INT;

import android.content.Context;

import net.kdt.pojavlaunch.Tools;
import net.kdt.pojavlaunch.game.renderer.GameRenderer;
import net.kdt.pojavlaunch.game.renderer.Renderer;
import net.kdt.pojavlaunch.utils.GpuUtils;

import java.io.File;
import java.util.Map;

import git.artdeell.mojo.R;
import git.artdeell.mojoexec.MojoExec;

public abstract class MesaRenderer implements Renderer {
    private String overrideEGL;

    @Override
    public String library() {
        return overrideEGL != null ? overrideEGL : "libEGL_mesa.so";
    }

    @Override
    public void setupEnvironment(Context context, Map<String, String> envMap) {
        envMap.put("MESA_GLSL_CACHE_DIR", Tools.DIR_CACHE.getAbsolutePath());
    }

    @Override
    public boolean compatibleDevice(Context context) {
        return SDK_INT >= 29 && new File(Tools.NATIVE_LIB_DIR, this.library()).exists();
    }

    @Override
    public String name() {
        return "Mesa";
    }

    @Override
    public String tag() {
        return GameRenderer.MESA_RENDERER;
    }

    // To allow legacy Zink usage
    public void overrideEGL(String overrideEGL) {
        this.overrideEGL = overrideEGL;
    }

    // Namespace bypass param is always active on Mesa
    @Override
    public boolean setupRenderer(boolean namespaceBypass) {
        return MojoExec.prepareEgl(library(), true, false, 3);
    }

    public static class ZinkRenderer extends MesaRenderer {

        @Override
        public String name() {
            return "ZINK";
        }

        @Override
        public String tag() {
            return GameRenderer.ZINK_RENDERER;
        }

        @Override
        public int displayName() {
            return R.string.mcl_setting_renderer_vulkan_zink;
        }

        @Override
        public void setupEnvironment(Context context, Map<String, String> envMap) {
            envMap.put("GALLIUM_DRIVER", "zink");
            envMap.put("MESA_LOADER_DRIVER_OVERRIDE", "zink");
            // HACK: GLSL version override for Mesa-based renderers (i.e. Zink)
            // Required to run the game properly on some mobile Vulkan drivers (Minecraft fails to compile shaders without)
            envMap.put("MESA_GLSL_VERSION_OVERRIDE", "460");
            envMap.put("MESA_GL_VERSION_OVERRIDE", "4.6");
            super.setupEnvironment(context, envMap);
        }

        @Override
        public boolean setupRenderer(boolean n) {
            MojoExec.preloadVulkan();
            return super.setupRenderer(n);
        }

        @Override
        public boolean compatibleDevice(Context context) {
            return super.compatibleDevice(context) && GpuUtils.checkVulkanSupport(context.getPackageManager());
        }
    }

    public static class FreedrenoRenderer extends MesaRenderer {

        @Override
        public String name() {
            return "Freedreno";
        }

        @Override
        public int displayName() {
            return R.string.mcl_setting_renderer_freedreno_kgsl;
        }

        @Override
        public String tag() {
            return GameRenderer.FREEDRENO_RENDERER;
        }

        @Override
        public boolean compatibleDevice(Context context) {
            return super.compatibleDevice(context) && GpuUtils.getGlInfo().isAdreno();
        }

        @Override
        public void setupEnvironment(Context context, Map<String, String> envMap) {
            if (GpuUtils.getGlInfo().isAdreno()) {
                envMap.put("MESA_LOADER_DRIVER_OVERRIDE", "kgsl");
                // On Adreno 5XX and lower only Core 3.1 is exposed by default due to missing hardware extensions.
                // 3.3 is required for modern Minecraft so let's force 3.3 if running on such GPU - it's known to be working.
                if (GpuUtils.getGlInfo().isAdreno500Lower()) {
                    envMap.put("MESA_GL_VERSION_OVERRIDE", "3.3");
                    envMap.put("MESA_GLSL_VERSION_OVERRIDE", "330");
                }
            }
            super.setupEnvironment(context, envMap);
        }
    }
}
