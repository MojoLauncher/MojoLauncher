package net.kdt.pojavlaunch.game.renderer;

import static net.kdt.pojavlaunch.prefs.LauncherPreferences.PREF_DUMP_SHADERS;

import android.content.Context;
import android.content.res.Resources;
import android.util.Log;

import net.kdt.pojavlaunch.Tools;
import net.kdt.pojavlaunch.game.renderer.angle.AngleDescriptor;
import net.kdt.pojavlaunch.game.renderer.impl.GL4ESRenderer;
import net.kdt.pojavlaunch.game.renderer.impl.LTWRenderer;
import net.kdt.pojavlaunch.game.renderer.impl.MesaRenderer;
import net.kdt.pojavlaunch.plugins.LibraryPlugin;
import net.kdt.pojavlaunch.prefs.LauncherPreferences;

import java.util.ArrayList;
import java.util.LinkedHashMap;
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
    private static final Map<String, Class<? extends Renderer>> KNOWN_RENDERERS = new LinkedHashMap<>();
    private final static String FALLBACK_RENDERER = GL4ES_RENDERER;
    private static RenderersList sCompatibleRenderers;

    static {
        KNOWN_RENDERERS.put(GL4ES_RENDERER, GL4ESRenderer.class);
        KNOWN_RENDERERS.put(LTW_RENDERER, LTWRenderer.class);
        KNOWN_RENDERERS.put(ZINK_RENDERER, MesaRenderer.ZinkRenderer.class);
        KNOWN_RENDERERS.put(FREEDRENO_RENDERER, MesaRenderer.FreedrenoRenderer.class);
    }

    private final Context context;
    private Renderer currentRenderer;
    private AngleDescriptor angleDescriptor = null;
    private boolean forceNsBypass = false;

    public GameRenderer(Context context, String currentRenderer) {
        this.context = context;
        this.currentRenderer = internalCreateRenderer(currentRenderer);
    }

    private static Renderer internalCreateRenderer(String renderer) {
        Class<? extends Renderer> clazz = KNOWN_RENDERERS.containsKey(renderer) ?
                KNOWN_RENDERERS.get(renderer) :
                KNOWN_RENDERERS.get(FALLBACK_RENDERER);
        try {
            return clazz.newInstance();
        } catch (IllegalAccessException | InstantiationException e) {
            throw new RuntimeException(e);
        }
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
        List<String> rendererIds = new ArrayList<>(KNOWN_RENDERERS.size());
        List<String> rendererNames = new ArrayList<>(KNOWN_RENDERERS.size());
        for (String renderer : KNOWN_RENDERERS.keySet()) {
            Renderer r = internalCreateRenderer(renderer);
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
        if (angleDescriptor != null)
            angleDescriptor.setEnvironment(envMap);
        if (PREF_DUMP_SHADERS)
            envMap.put("LIBGL_VGPU_DUMP", "1");
        envMap.put("force_glsl_extensions_warn", "true");
        envMap.put("allow_higher_compat_version", "true");
        envMap.put("allow_glsl_extension_directive_midshader", "true");
        if (LauncherPreferences.PREF_FREEDRENO_SYSMEM && currentRenderer instanceof MesaRenderer.FreedrenoRenderer) {
            envMap.put("FD_MESA_DEBUG", "sysmem");
        }
        if (LauncherPreferences.PREF_FREEDRENO_SYSMEM && !LauncherPreferences.PREF_ZINK_PREFER_SYSTEM_DRIVER) {
            envMap.put("TU_DEBUG", "sysmem");
        }
        currentRenderer.setupEnvironment(context, envMap);
    }

    /**
     * Enable ANGLE usage. Will pick AnglePlugin source if possible and fallback to the system libraries (if present) otherwise while enabling namespace bypass.
     */
    public void enableAngle() {
        if (angleDescriptor != null) return;
        AngleDescriptor descriptor;
        LibraryPlugin anglePlugin = LibraryPlugin.discoverPlugin(context, LibraryPlugin.ID_ANGLE_PLUGIN);
        descriptor = new AngleDescriptor.ExtAngleDescriptor(anglePlugin);
        if (descriptor.supported()) {
            this.angleDescriptor = descriptor;
            Log.i(TAG, "Enabled ANGLE through AnglePlugin");
            return;
        }
        descriptor = new AngleDescriptor.SysAngleDescriptor();
        if (descriptor.supported()) {
            this.angleDescriptor = descriptor;
            // We can't access ANGLE libraries through classloader namespace
            this.forceNsBypass = true;
            Log.i(TAG, "Enabled ANGLE through system libraries");
            return;
        }
        Log.e(TAG, "Unable to setup neither system ANGLE nor external");
    }

    /**
     * Enable legacy Mesa ZINK (23.0.4) usage if ZinkPlugin is installed
     */
    public void enableLegacyZink() {
        if (!(currentRenderer instanceof MesaRenderer.ZinkRenderer)) return;
        LibraryPlugin zink = LibraryPlugin.discoverPlugin(context, LibraryPlugin.ID_ZINK_PLUGIN);
        if (zink == null) return;
        if (!zink.checkLibraries("libEGL_legacy.so")) return;
        ((MesaRenderer) currentRenderer).overrideEGL(zink.resolveAbsolutePath("libEGL_legacy.so"));
        setRendererLibraryPath(Tools.NATIVE_LIB_DIR, zink.getLibraryPath());
        Log.i(TAG, "Using legacy Mesa ZINK!");
    }

    /**
     * Get current selected renderer in this GameRenderer instance
     *
     * @return renderer
     */
    public Renderer getCurrentRenderer() {
        return currentRenderer;
    }

    // This will be used for AdrenoTools in the far future

    /**
     * Set current selected renderer. Call this before {@link GameRenderer#setupEnvironment} or bad things may happen
     *
     * @param renderer renderer
     */
    public void setCurrentRenderer(String renderer) {
        if (!KNOWN_RENDERERS.containsKey(renderer)) return;
        Log.i(TAG, "Replacing default renderer with the new: " + renderer);
        currentRenderer = internalCreateRenderer(renderer);
    }

    /**
     * Setup the current renderer or fallback to {@link GameRenderer#FALLBACK_RENDERER} if failed
     *
     * @return whether the renderer setup was successful
     */
    public boolean maybeSetupRenderer() {
        if (!currentRenderer.setupRenderer(forceNsBypass)) {
            Log.e(TAG, "Failed to setup renderer " + currentRenderer.name() + ", falling back to " + FALLBACK_RENDERER);
            // Hopefully
            return internalCreateRenderer(FALLBACK_RENDERER).setupRenderer(false);
        }
        return true;
    }

    /**
     * Resolve known renderer name from the tag
     *
     * @param renderer renderer tag
     * @return a renderer
     * @throws RuntimeException if the renderer is unknown
     */
    public Renderer getKnownRenderer(String renderer) throws RuntimeException {
        return internalCreateRenderer(renderer);
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
