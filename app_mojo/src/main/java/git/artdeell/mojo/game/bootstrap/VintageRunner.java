package git.artdeell.mojo.game.bootstrap;

import android.system.Os;
import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;

import git.artdeell.mojo.Logger;
import git.artdeell.mojo.Tools;
import git.artdeell.mojo.game.renderer.GameRenderer;
import git.artdeell.mojo.game.renderer.RenderSpec;
import git.artdeell.mojo.game.renderer.def.Renderers;
import git.artdeell.mojo.game.renderer.impl.GLESRenderSpec;
import git.artdeell.mojo.instances.Instance;

import java.io.File;
import java.io.IOException;

import git.artdeell.mojo.R;
import git.artdeell.mojo.runtime.dotnet.DotnetStarter;
import git.artdeell.mojo.runtime.multirt.MultiRTUtils;
import git.artdeell.mojo.runtime.multirt.Runtime;
import git.artdeell.mojo.utils.SymlinkUtils;

public class VintageRunner implements GameRunner {
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

    private static File findCertsDir() {
        File certsDir = new File("/apex/com.android.conscrypt/cacerts/");
        if(certsDir.exists()) return certsDir;
        certsDir = new File("/system/etc/security/cacerts");
        if(certsDir.exists()) return certsDir;
        return null;
    }

    @Override
    public void launchGame() throws Throwable {

        File homeDir = new File(instance.getGameDirectory(), "home");
        File certsDir = findCertsDir();

        Runtime dotnet = MultiRTUtils.TEMP_GetDotnet();

        if(certsDir == null) throw new IOException("Cannot start: can't find HTTPS certificate directory");
        File trueVsDir = new File(instance.getGameDirectory(), "vintagestory");

        try {
            Os.setenv("HOME", homeDir.getAbsolutePath(), true);
            // TODO: fontconfig path
            Os.setenv("FONTCONFIG_PATH", null, true);
            Os.setenv("SSL_CERT_DIR", certsDir.getAbsolutePath(), true);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        SymlinkUtils symlinkUtil = new SymlinkUtils(trueVsDir, new File(Tools.NATIVE_LIB_DIR));

        // Well, because...
        // TODO: Discover better way
        symlinkUtil.symlinkLibrary("libopenal.so", "libopenal.so.1");
        symlinkUtil.symlinkLibrary("libcairo.so", "libcairo.so.2");

        Logger.appendToLog("=== BEGIN .NET ===");
        Log.i("VintageRunner", "Running VS in:" + trueVsDir);
        DotnetStarter.runDotnet(dotnet.root, trueVsDir.getAbsolutePath());
    }
}
