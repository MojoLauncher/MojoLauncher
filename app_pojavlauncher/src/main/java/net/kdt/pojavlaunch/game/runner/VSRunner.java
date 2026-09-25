package net.kdt.pojavlaunch.game.runner;

import androidx.appcompat.app.AppCompatActivity;

import net.kdt.pojavlaunch.Tools;
import net.kdt.pojavlaunch.game.renderer.GameRenderer;
import net.kdt.pojavlaunch.game.renderer.RenderSpec;
import net.kdt.pojavlaunch.game.renderer.def.Renderers;
import net.kdt.pojavlaunch.game.renderer.impl.GLESRenderSpec;
import net.kdt.pojavlaunch.instances.Instance;
import net.kdt.pojavlaunch.utils.GpuUtils;

import java.io.IOException;

import git.artdeell.mojo.R;

public class VSRunner implements GameRunner {
    private AppCompatActivity activity;
    private Instance instance;

    // Autoswitch to provided renderer if supported, otherwise - crash with resId dialog message
    // TODO: my brain isn't braining, but this clearly needs to be deduplicated with LwjglRunner
    private boolean switchRendererIfSupported(GameRenderer gameRenderer, boolean support, RenderSpec renderer, int resId) throws InterruptedException, IOException {
        if(support) {
            instance.renderer = renderer.tag();
            instance.write();
            gameRenderer.setCurrentRenderer(renderer);
        }else {
            Tools.showDialogAndHalt(activity, resId);
        }
        return support;
    }
    @Override
    public void init(AppCompatActivity activity, Instance instance) {
        this.activity = activity;
        this.instance = instance;
    }

    @Override
    public boolean ensureRendererCompatible(GameRenderer gameRenderer) throws Exception {
        RenderSpec ltw = GameRenderer.getKnownRenderer(Renderers.LTW_RENDERER);
        // Vintage Story does not support GL4ES
        if(gameRenderer.getCurrentRenderer() instanceof GLESRenderSpec.GL4ESRenderSpec) {
            // TODO: what if LTW isn't present in the app?
            return switchRendererIfSupported(gameRenderer, ltw.compatibleDevice(activity), ltw, R.string.vintagestory_unsupported_device);
        }
        return true;
    }

    // None for now
    @Override
    public void checkRendererQuirks(GameRenderer gameRenderer) throws Exception {

    }

    @Override
    public void launchGame() throws Throwable {
        // TODO: Actually bootstrap .NET
    }
}
