package net.kdt.pojavlaunch.game.platform.backend;

import static net.kdt.pojavlaunch.awt.AWTInput.EVENT_TYPE_CHAR;
import static net.kdt.pojavlaunch.awt.AWTInput.EVENT_TYPE_CURSOR_POS;
import static net.kdt.pojavlaunch.awt.AWTInput.EVENT_TYPE_KEY;
import static net.kdt.pojavlaunch.awt.AWTInput.EVENT_TYPE_MOUSE_BUTTON;

import android.util.Log;
import android.view.Surface;

import net.kdt.pojavlaunch.CallbackBridge;
import net.kdt.pojavlaunch.PojavApplication;
import net.kdt.pojavlaunch.Tools;
import net.kdt.pojavlaunch.awt.AWTInput;
import net.kdt.pojavlaunch.awt.AWTView;
import net.kdt.pojavlaunch.awt.AWTWindow;
import net.kdt.pojavlaunch.game.GameView;
import net.kdt.pojavlaunch.game.platform.Platform;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

public class AWTBackend implements PlatformBackend {
    private volatile boolean rendering = false;
    private Surface surface;
    private Future<?> task;
    @Override
    public void surfaceCreated(Surface surface) {
        this.rendering = true;
        this.surface = surface;
        Platform.grabStateChanged(false);
        AWTWindow.setNativeSize(CallbackBridge.windowWidth, CallbackBridge.windowHeight);
        AWTWindow.setNativeSurface(surface);
        // AWT requires us to manually draw on the screen
        task = PojavApplication.sExecutorService.submit(AWTWindow::beginRendering);
    }

    @Override
    public void surfaceUpdated() {
        // There's no need of updating AWT Surface... for now
    }

    @Override
    public void surfaceDestroyed() {
        AWTWindow.endRendering();
        AWTWindow.destroySurface();
        this.surface = null;
    }

    @Override
    public void sendMousePosition() {
        AWTInput.nativeSendData(EVENT_TYPE_CURSOR_POS, (int) Platform.cursorX, (int) Platform.cursorY, 0, 0);
    }

    @Override
    public void sendMouseEvent(int button, int state, int mods) {
        AWTInput.nativeSendData(EVENT_TYPE_MOUSE_BUTTON, button, state, 0, 0);
    }

    @Override
    public boolean sendKeyEvent(int key, int state, int mods, char codepoint) {
        AWTInput.nativeSendData(EVENT_TYPE_KEY, codepoint, key, state, 0);
        return true;
    }

    @Override
    public boolean sendKeyEvent(int key, int state, int mods) {
        return true;
    }

    @Override
    public boolean sendKeyEvent(int key, boolean state, int mods) {
        return true;
    }

    @Override
    public void sendScrollEvent(double x, double y) {
        // Unsupported
    }

    @Override
    public void sendBulkUnicodeEvent(String text, int mods) {

    }

    @Override
    public String backendName() {
        return "AWT";
    }

    @Override
    public void setHovered(boolean hovered) {
        // Unsupported
    }

    @Override
    public void setVisible(boolean visible) {
        // Unsupported
    }
}
