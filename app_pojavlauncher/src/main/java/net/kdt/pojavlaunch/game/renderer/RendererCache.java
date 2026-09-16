package net.kdt.pojavlaunch.game.renderer;

import static net.kdt.pojavlaunch.game.renderer.def.Renderers.FREEDRENO_RENDERER;
import static net.kdt.pojavlaunch.game.renderer.def.Renderers.GL4ES_RENDERER;
import static net.kdt.pojavlaunch.game.renderer.def.Renderers.LEGACYZINK_RENDERER;
import static net.kdt.pojavlaunch.game.renderer.def.Renderers.LTW_RENDERER;
import static net.kdt.pojavlaunch.game.renderer.def.Renderers.MESA_RENDERER;
import static net.kdt.pojavlaunch.game.renderer.def.Renderers.MESA_RENDERER_EXT;
import static net.kdt.pojavlaunch.game.renderer.def.Renderers.ZINK_RENDERER;

import android.content.Context;
import android.content.res.Resources;

import java.util.ArrayList;
import java.util.List;

/**
 * Compatible renderers cache. Used for the UI renderer list
 */
public class RendererCache {
    private static RendererCache sCompatibleRenderers;

    public final List<String> rendererIds;
    public final String[] rendererDisplayNames;

    public RendererCache(List<String> rendererIds, String[] rendererDisplayNames) {
        this.rendererIds = rendererIds;
        this.rendererDisplayNames = rendererDisplayNames;
    }

    /**
     * Return a list of renderers compatible with the current device
     * Don't forget to clean the cache when the list isn't needed anymore (i.e. when starting the game) - {@link GameRenderer#releaseRendererCache()}
     *
     * @param context application context
     * @return RenderersList containing all compatible renderers
     */
    public static RendererCache getCompatibleRenderers(Context context) {
        if (sCompatibleRenderers != null) return sCompatibleRenderers;
        Resources resources = context.getResources();
        // This is the list that controls em all!
        String[] renderers = {
                GL4ES_RENDERER, LTW_RENDERER, ZINK_RENDERER, FREEDRENO_RENDERER, MESA_RENDERER, MESA_RENDERER_EXT, LEGACYZINK_RENDERER
        };
        ArrayList<String> rendererIds = new ArrayList<>(renderers.length);
        ArrayList<String> rendererNames = new ArrayList<>(rendererIds);
        for (String renderer : renderers) {
            RenderSpec r = GameRenderer.getKnownRenderer(renderer);
            assert r != null;
            if (!r.compatibleDevice(context)) continue;
            rendererIds.add(renderer);
            rendererNames.add(resources.getString(r.displayName()));
        }
        rendererIds.trimToSize();
        rendererNames.trimToSize();
        return (sCompatibleRenderers = new RendererCache(rendererIds, rendererNames.toArray(new String[0])));
    }

    /**
     * Destroy compatible renderers cache
     */
    public static void releaseRendererCache() {
        sCompatibleRenderers.rendererIds.clear();
        sCompatibleRenderers = null;
    }
}
