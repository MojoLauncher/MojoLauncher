package net.kdt.pojavlaunch.game.runner;

import android.content.Intent;

import androidx.appcompat.app.AppCompatActivity;

import net.kdt.pojavlaunch.game.renderer.GameRenderer;
import net.kdt.pojavlaunch.instances.Instance;

public interface GameRunner {
    void init(AppCompatActivity activity, Instance instance);
    boolean ensureRendererCompatible(GameRenderer gameRenderer) throws Exception;
    void checkRendererQuirks(GameRenderer gameRenderer) throws Exception;
    void launchGame() throws Throwable;
}
