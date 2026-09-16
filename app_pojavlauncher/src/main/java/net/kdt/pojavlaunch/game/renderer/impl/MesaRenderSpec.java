package net.kdt.pojavlaunch.game.renderer.impl;

import static android.os.Build.VERSION.SDK_INT;

import android.content.Context;
import android.util.Log;

import net.kdt.pojavlaunch.Tools;
import net.kdt.pojavlaunch.game.renderer.GameRenderer;
import net.kdt.pojavlaunch.game.renderer.RenderSpec;
import net.kdt.pojavlaunch.game.renderer.def.Renderers;
import net.kdt.pojavlaunch.plugins.LibraryPlugin;
import net.kdt.pojavlaunch.prefs.LauncherPreferences;
import net.kdt.pojavlaunch.utils.GpuUtils;

import java.io.File;
import java.util.Map;

import git.artdeell.mojo.R;
import git.artdeell.mojoexec.MojoExec;

/**
 * Mesa3D RenderSpec. Provides desktop Mesa, zink & freedreno
 */
public class MesaRenderSpec implements RenderSpec {
    public String library() {
        return "libEGL_mesa.so";
    }
    public void setupEnvironment(Context context, Map<String, String> envMap) {
        envMap.put("MESA_GLSL_CACHE_DIR", Tools.DIR_CACHE.getAbsolutePath());
    }
    protected boolean hasMesa() {
        return SDK_INT >= 29 && new File(Tools.NATIVE_LIB_DIR, this.library()).exists();
    }
    public boolean compatibleDevice(Context context) {
        return hasMesa() && GpuUtils.checkChromebook(context.getPackageManager());
    }
    public String name() {
        return "Mesa";
    }
    public int displayName() {
        return R.string.mcl_setting_renderer_mesa_desktop;
    }
    public String tag() {
        return Renderers.MESA_RENDERER;
    }
    public boolean setupRenderer() {
        return MojoExec.prepareEgl(library(), true, false, 3);
    }

    public static class ZinkRenderSpec extends MesaRenderSpec {
        public String name() {
            return "ZINK";
        }
        public String tag() {
            return Renderers.ZINK_RENDERER;
        }
        public int displayName() {
            return R.string.mcl_setting_renderer_vulkan_zink;
        }
        public void setupEnvironment(Context context, Map<String, String> envMap) {
            envMap.put("MESA_LOADER_DRIVER_OVERRIDE", "zink");
            // HACK: GLSL version override for Mesa-based renderers (i.e. Zink)
            // Required to run the game properly on some mobile Vulkan drivers (Minecraft fails to compile shaders without)
            envMap.put("MESA_GLSL_VERSION_OVERRIDE", "460");
            envMap.put("MESA_GL_VERSION_OVERRIDE", "4.6");
            super.setupEnvironment(context, envMap);
        }
        public boolean setupRenderer() {
            MojoExec.preloadVulkan();
            return super.setupRenderer();
        }
        public boolean compatibleDevice(Context context) {
            return hasMesa() && GpuUtils.checkVulkanSupport(context.getPackageManager());
        }
    }

    public static class FreedrenoRenderSpec extends MesaRenderSpec {
        public String name() {
            return "Freedreno";
        }
        public int displayName() {
            return R.string.mcl_setting_renderer_freedreno_kgsl;
        }
        public String tag() {
            return Renderers.FREEDRENO_RENDERER;
        }
        public boolean compatibleDevice(Context context) {
            return hasMesa() && GpuUtils.getGlInfo().isAdreno();
        }
        public void setupEnvironment(Context context, Map<String, String> envMap) {
            if (LauncherPreferences.PREF_FREEDRENO_SYSMEM)
                envMap.put("FD_MESA_DEBUG", "sysmem");
            envMap.put("MESA_LOADER_DRIVER_OVERRIDE", "kgsl");
            // On Adreno 5XX and lower only Core 3.1 is exposed by default due to missing hardware extensions.
            // 3.3 is required for modern Minecraft so let's force 3.3 if running on such GPU - it's known to be working.
            if (GpuUtils.getGlInfo().isAdreno500Lower()) {
                envMap.put("MESA_GL_VERSION_OVERRIDE", "3.3");
                envMap.put("MESA_GLSL_VERSION_OVERRIDE", "330");
            }
            super.setupEnvironment(context, envMap);
        }
    }
    public static class ExtMesaRenderSpec extends MesaRenderSpec {
        private LibraryPlugin provider;
        protected String plugin() {
            return LibraryPlugin.ID_MESA_PLUGIN;
        }
        public String name() {
            return "Mesa (external)";
        }
        public String librarySearchPath() {
            return provider.getLibraryPath();
        }
        public String tag() {
            return Renderers.MESA_RENDERER_EXT;
        }
        public int displayName() {
            return R.string.mcl_setting_renderer_mesa_desktop_ext;
        }
        private boolean discover(Context context) {
            if(provider == null) provider = LibraryPlugin.discoverPlugin(context, plugin());
            return provider != null;
        }
        public boolean compatibleDevice(Context context) {
            return discover(context) && provider.checkLibraries(library());
        }
        public void setupEnvironment(Context context, Map<String, String> envMap) {
            discover(context);
            super.setupEnvironment(context, envMap);
        }
        public boolean setupRenderer() {
            if(provider == null) return false;
            return MojoExec.prepareEgl(provider.resolveAbsolutePath(library()), true, false, 0);
        }
    }
    public static class LegacyZinkRenderSpec extends ExtMesaRenderSpec {
        protected String plugin() {
            return LibraryPlugin.ID_ZINK_PLUGIN;
        }
        public String name() {
            return "ZINK (Legacy)";
        }
        public String tag() {
            return Renderers.LEGACYZINK_RENDERER;
        }
        public int displayName() {
            return R.string.mcl_setting_renderer_vulkan_lzink;
        }
        public void setupEnvironment(Context context, Map<String, String> envMap) {
            super.setupEnvironment(context, envMap);
            envMap.put("MESA_GL_VERSION_OVERRIDE", "4.3");
            envMap.put("MESA_GLSL_VERSION_OVERRIDE", "460");
            envMap.put("MESA_LOADER_DRIVER_OVERRIDE", "zink");
        }
        public boolean setupRenderer() {
            MojoExec.preloadVulkan();
            return super.setupRenderer();
        }
        public boolean compatibleDevice(Context context) {
            return GpuUtils.checkVulkanSupport(context.getPackageManager()) && super.compatibleDevice(context);
        }
        public String library() {
            return "libEGL_legacy.so";
        }
    }
}
