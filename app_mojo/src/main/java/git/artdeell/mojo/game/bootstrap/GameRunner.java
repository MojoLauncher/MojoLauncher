package git.artdeell.mojo.game.bootstrap;

import androidx.appcompat.app.AppCompatActivity;

import git.artdeell.mojo.game.GameType;
import git.artdeell.mojo.game.renderer.GameRenderer;
import git.artdeell.mojo.instances.Instance;

public interface GameRunner {

    static GameRunner pickGameRunner(GameType type) {
        switch (type) {
            case MC:
                return new MCRunner();
            case VINTAGE_STORY:
                return new VintageRunner();
            default:
                throw new IllegalArgumentException("Unsupported game");
        }
    }

    void init(AppCompatActivity activity, Instance instance);
    boolean ensureRendererCompatible(GameRenderer gameRenderer) throws Exception;
    void checkRendererQuirks(GameRenderer gameRenderer) throws Exception;
    void launchGame() throws Throwable;
}
