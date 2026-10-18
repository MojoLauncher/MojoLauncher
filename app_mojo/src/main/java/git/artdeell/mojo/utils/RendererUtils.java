package git.artdeell.mojo.utils;

import androidx.appcompat.app.AppCompatActivity;

import java.io.IOException;

import git.artdeell.mojo.Tools;
import git.artdeell.mojo.game.renderer.GameRenderer;
import git.artdeell.mojo.game.renderer.RenderSpec;
import git.artdeell.mojo.instances.Instance;

public class RendererUtils {

    /**
     * Switch the current renderer to the supported one if it's supported or bail out with an error dialog
     * @param activity app activity
     * @param instance instance to write switched renderer to
     * @param gameRenderer renderer manager
     * @param support whether the provided renderer supported
     * @param renderer renderer to switch to
     * @param resId message to show to if the renderer isn't supported
     * @return support boolean passthrough
     * @throws InterruptedException if the dialog was interrupted
     * @throws IOException if failed writing the instance data
     */
    public static boolean switchRendererIfSupported(AppCompatActivity activity,
                                                    Instance instance,
                                                    GameRenderer gameRenderer,
                                                    boolean support,
                                                    RenderSpec renderer,
                                                    int resId) throws InterruptedException, IOException {
        if(support) {
            instance.renderer = renderer.tag();
            instance.write();
            gameRenderer.setCurrentRenderer(renderer);
        }else {
            Tools.showDialogAndHalt(activity, resId);
        }
        return support;
    }
}
