package net.kdt.pojavlaunch.game.renderer;

import android.content.Context;
import android.content.res.Resources;
import android.util.Log;

import net.kdt.pojavlaunch.Tools;
import net.kdt.pojavlaunch.game.renderer.impl.GLESRenderSpec;
import net.kdt.pojavlaunch.game.renderer.impl.MesaRenderSpec;
import net.kdt.pojavlaunch.plugins.LibraryPlugin;
import net.kdt.pojavlaunch.prefs.LauncherPreferences;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import git.artdeell.mojoexec.MojoExec;

public class GameRenderer {
    public static final String LTW_RENDERER = "opengles3_ltw";
    public static final String GL4ES_RENDERER = "opengles2";
    public static final String ZINK_RENDERER = "vulkan_zink";
    public static final String FREEDRENO_RENDERER = "freedreno_kgsl";
    public static final String MESA_RENDERER = "mesa_desktop";
    private final static String TAG = "Renderer";
    private final static String FALLBACK_RENDERER = GL4ES_RENDERER;
    private static RenderersList sCompatibleRenderers;

    private final Context context;
    private RenderSpec currentRenderer;
    private String additionalLibraryPath = null;

    public GameRenderer(Context context, String currentRenderer) {
        this.context = context;
        this.currentRenderer = getKnownRenderer(currentRenderer);
    }

    public static RenderSpec getKnownRenderer(String renderer) {
        switch (renderer) {
            // For compatibility
            case "opengles2_4":
            case "opengles2_5":
            case GL4ES_RENDERER: return new GLESRenderSpec.GL4ESRenderSpec();
            case LTW_RENDERER: return new GLESRenderSpec.LTWRenderSpec();
            case ZINK_RENDERER: return new MesaRenderSpec.ZinkRenderSpec();
            case FREEDRENO_RENDERER: return new MesaRenderSpec.FreedrenoRenderSpec();
            default: return null;
        }
    }

    public static boolean isCompatibleRenderer(String renderer) {
        if(sCompatibleRenderers != null) {
            return sCompatibleRenderers.rendererIds.contains(renderer);
        }
        Log.w(TAG, "Tried checking renderer compatibility through cache, but it was already released or wasn't initialized at all");
        return false;
    }

    /**
     * Set renderer library path
     *
     * @param mainPath       base library path
     * @param additionalPath additional library path to search libs at
     */
    public static void setRendererLibraryPath(String mainPath, String additionalPath) {
        if (additionalPath != null)
            mainPath = additionalPath + ":" + mainPath;
        MojoExec.setNativeLibraryDir(mainPath);
    }

    /**
     * Return a list of renderers compatible with the current device
     *
     * @param context application context
     * @return RenderersList containing all compatible renderers
     */
    public static RenderersList getCompatibleRenderers(Context context) {
        if (sCompatibleRenderers != null) return sCompatibleRenderers;
        Resources resources = context.getResources();
        String[] renderers = {
                GL4ES_RENDERER, LTW_RENDERER, ZINK_RENDERER, FREEDRENO_RENDERER
        };
        List<String> rendererIds = new ArrayList<>(renderers.length);
        List<String> rendererNames = new ArrayList<>(rendererIds);
        for (String renderer : renderers) {
            RenderSpec r = getKnownRenderer(renderer);
            assert r != null;
            if (!r.compatibleDevice(context)) continue;
            rendererIds.add(renderer);
            rendererNames.add(resources.getString(r.displayName()));
        }
        return (sCompatibleRenderers = new RenderersList(rendererIds, rendererNames.toArray(new String[0])));
    }

    /**
     * Destroy compatible renderers cache
     */
    public static void releaseCache() {
        sCompatibleRenderers = null;
    }

    /**
     * Setup current selected renderer environment. Call before using {@link GameRenderer#maybeSetupRenderer()}
     *
     * @param context application context
     * @param envMap  environment map
     */
    public void setupEnvironment(Context context, Map<String, String> envMap) {
        if (LauncherPreferences.PREF_FREEDRENO_SYSMEM && currentRenderer instanceof MesaRenderSpec.FreedrenoRenderSpec) {
            envMap.put("FD_MESA_DEBUG", "sysmem");
        }
        if (LauncherPreferences.PREF_FREEDRENO_SYSMEM && !LauncherPreferences.PREF_ZINK_PREFER_SYSTEM_DRIVER) {
            envMap.put("TU_DEBUG", "sysmem");
        }
        currentRenderer.setupEnvironment(context, envMap);
    }

    /**
     * Enable legacy Mesa ZINK (23.0.4) usage if ZinkPlugin is installed
     */
    public void enableLegacyZink() {
        if (!(currentRenderer instanceof MesaRenderSpec.ZinkRenderSpec)) return;
        LibraryPlugin zink = LibraryPlugin.discoverPlugin(context, LibraryPlugin.ID_ZINK_PLUGIN);
        if (zink == null) return;
        if (!zink.checkLibraries("libEGL_legacy.so")) return;
        ((MesaRenderSpec) currentRenderer).overrideEGL(zink.resolveAbsolutePath("libEGL_legacy.so"));
        this.additionalLibraryPath = zink.getLibraryPath();
        Log.i(TAG, "Using legacy Mesa ZINK!");
    }

    /**
     * Get current selected renderer in this GameRenderer instance
     *
     * @return renderer
     */
    public RenderSpec getCurrentRenderer() {
        return currentRenderer;
    }

    // This will be used for AdrenoTools in the far future

    /**
     * Set current selected renderer. Call this before {@link GameRenderer#setupEnvironment} or bad things may happen
     *
     * @param renderer renderer
     */
    public void setCurrentRenderer(String renderer) {
        RenderSpec spec = getKnownRenderer(renderer);
        if(spec == null) throw new IllegalArgumentException("Invalid renderer " + renderer);
        Log.i(TAG, "Replacing default renderer with the new: " + spec.name());
        currentRenderer = spec;
    }

    /**
     * Set up the current renderer or fallback to {@link GameRenderer#FALLBACK_RENDERER} if failed
     *
     * @return whether the renderer setup was successful
     */
    public boolean maybeSetupRenderer() {
        setRendererLibraryPath(Tools.NATIVE_LIB_DIR, additionalLibraryPath);
        if (!currentRenderer.setupRenderer()) {
            Log.e(TAG, "Failed to setup renderer " + currentRenderer.name() + ", falling back to " + FALLBACK_RENDERER);
            // Hopefully
            return getKnownRenderer(FALLBACK_RENDERER).setupRenderer();
        }
        return true;
    }

    /**
     * Enable custom Vulkan driver (Turnip) usage
     */
    public void overrideVulkanDriver() {
        MojoExec.setUseTurnip(true);
    }

    /**
     * Compatible renderers list
     */
    public static class RenderersList {
        public final List<String> rendererIds;
        public final String[] rendererDisplayNames;

        public RenderersList(List<String> rendererIds, String[] rendererDisplayNames) {
            this.rendererIds = rendererIds;
            this.rendererDisplayNames = rendererDisplayNames;
        }
    }
}
