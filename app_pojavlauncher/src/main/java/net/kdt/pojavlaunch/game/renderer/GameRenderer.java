package net.kdt.pojavlaunch.game.renderer;

import static net.kdt.pojavlaunch.game.renderer.def.Renderers.FREEDRENO_RENDERER;
import static net.kdt.pojavlaunch.game.renderer.def.Renderers.GL4ES_RENDERER;
import static net.kdt.pojavlaunch.game.renderer.def.Renderers.LEGACYZINK_RENDERER;
import static net.kdt.pojavlaunch.game.renderer.def.Renderers.LTW_RENDERER;
import static net.kdt.pojavlaunch.game.renderer.def.Renderers.MESA_RENDERER;
import static net.kdt.pojavlaunch.game.renderer.def.Renderers.MESA_RENDERER_EXT;
import static net.kdt.pojavlaunch.game.renderer.def.Renderers.ZINK_RENDERER;

import android.content.Context;
import android.system.ErrnoException;
import android.system.Os;
import android.util.Log;

import net.kdt.pojavlaunch.Logger;
import net.kdt.pojavlaunch.Tools;
import net.kdt.pojavlaunch.game.renderer.impl.GLESRenderSpec;
import net.kdt.pojavlaunch.game.renderer.impl.MesaRenderSpec;
import net.kdt.pojavlaunch.prefs.LauncherPreferences;

import java.util.HashMap;
import java.util.Map;

import git.artdeell.mojoexec.MojoExec;

/*
 How to add an extra renderer (guide 2026 mediafire works):

 1. Create RenderSpec for that renderer
 2. Add all requires locale strings and wire them up inside your freshly cooked RenderSpec
 3. Add the renderer tag onto the getKnownRenderer() mapping
 4. (Optional): Add the renderer tag into constant list in Renderers class
 5. Add the renderer tag onto the list of renderers to check for the compatibility (see getCompatibleRenderers())
 6. ???
 7. PROFIT

*/

/**
 * Class for managing game renderers (OpenGL ES & Vulkan)
 */
public class GameRenderer {
    private final static String TAG = "Renderer";
    private final static String FALLBACK_RENDERER = GL4ES_RENDERER;
    private RenderSpec currentRenderer;
    private Map<String, String> environment = new HashMap<>();

    public GameRenderer(String currentRenderer) {
        this.currentRenderer = getKnownRenderer(currentRenderer);
        if(this.currentRenderer == null) this.currentRenderer = getKnownRenderer(GL4ES_RENDERER);
        if(this.currentRenderer == null) throw new IllegalStateException("Failed to create the current renderer!");
    }

    /**
     * Map renderer string to a known RenderSpec
     *
     * @param renderer renderer string
     * @return RenderSpec instance if found, null otherwise
     */
    public static RenderSpec getKnownRenderer(String renderer) {
        switch (renderer) {
            // For compatibility
            case "opengles2_4":
            case "opengles2_5":
            case GL4ES_RENDERER: return new GLESRenderSpec.GL4ESRenderSpec();
            case LTW_RENDERER: return new GLESRenderSpec.LTWRenderSpec();
            case ZINK_RENDERER: return new MesaRenderSpec.ZinkRenderSpec();
            case FREEDRENO_RENDERER: return new MesaRenderSpec.FreedrenoRenderSpec();
            case MESA_RENDERER: return new MesaRenderSpec();
            case MESA_RENDERER_EXT: return new MesaRenderSpec.ExtMesaRenderSpec();
            case LEGACYZINK_RENDERER: return new MesaRenderSpec.LegacyZinkRenderSpec();
            default:
                Log.e(TAG, "Unknown renderer " + renderer);
                return null;
        }
    }

    /**
     * Set renderer library path
     *
     * @param mainPath       base library path
     * @param additionalPath additional library path to search libs at
     */
    public static void setRendererLibraryPath(String mainPath, String additionalPath) {
        if (additionalPath != null) mainPath = additionalPath + ":" + mainPath;
        MojoExec.setNativeLibraryDir(mainPath);
    }



    /**
     * Setup current selected renderer environment. Call before using {@link GameRenderer#maybeSetupRenderer()}
     *
     * @param context application context
     * @throws ErrnoException if underlying Os#setenv call threw an exception
     */
    public void setupEnvironment(Context context) throws ErrnoException {
        if(environment == null) {
            Log.w(TAG, "Tried to call setupEnvironment in already initialized environment");
            return;
        }
        currentRenderer.setupEnvironment(context, environment);
        for(Map.Entry<String, String> e : environment.entrySet()) {
            Logger.appendToLog("Added renderer env: " + e.getKey() + '=' + e.getValue());
            Os.setenv(e.getKey(), e.getValue(), true);
        }
        environment.clear();
        environment = null;
    }

    /**
     * Get current selected renderer in this GameRenderer instance
     *
     * @return renderer
     */
    public RenderSpec getCurrentRenderer() {
        return currentRenderer;
    }

    /**
     * Set current selected renderer. Call this before {@link GameRenderer#setupEnvironment} or bad things may happen
     *
     * @param spec renderer
     */
    public void setCurrentRenderer(RenderSpec spec) {
        Log.i(TAG, "Replacing default renderer with the new: " + spec.name());
        currentRenderer = spec;
    }

    /**
     * Set current selected renderer. Call this before {@link GameRenderer#setupEnvironment} or bad things may happen
     *
     * @param renderer renderer string
     * @throws IllegalArgumentException if incorrect renderer string is given
     */
    public void setCurrentRenderer(String renderer) throws IllegalArgumentException {
        RenderSpec spec = getKnownRenderer(renderer);
        if(spec == null) throw new IllegalArgumentException("Invalid renderer string" + renderer + "!");
        this.setCurrentRenderer(spec);
    }

    /**
     * Set up the current renderer or fallback to {@link GameRenderer#FALLBACK_RENDERER} if failed
     *
     * @return whether the renderer setup was successful
     */
    public boolean maybeSetupRenderer() {
        setRendererLibraryPath(Tools.NATIVE_LIB_DIR, currentRenderer.librarySearchPath());
        if (!currentRenderer.setupRenderer()) {
            Log.e(TAG, "Failed to setup renderer " + currentRenderer.name() + ", falling back to " + FALLBACK_RENDERER);
            // Hopefully (yes, it's going to be fun if it returns null for the fallback renderer. Shouldn't happen though)
            return getKnownRenderer(FALLBACK_RENDERER).setupRenderer();
        }
        return true;
    }

    /**
     * Enable custom Vulkan driver (Turnip) usage
     */
    public void overrideVulkanDriver() {
        if(LauncherPreferences.PREF_FREEDRENO_SYSMEM) environment.put("TU_DEBUG", "sysmem");
        MojoExec.setUseTurnip(true);
    }

    /**
     * Compatible renderers list
     */
    public static class RenderersList {
    }
}
