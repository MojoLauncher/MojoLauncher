package net.kdt.pojavlaunch.platform.backend;


import android.view.MotionEvent;
import android.view.Surface;

import net.kdt.pojavlaunch.LwjglGlfwKeycode;
import net.kdt.pojavlaunch.platform.Platform;

import git.artdeell.dnbootstrap.glfw.GLFW;
import git.artdeell.dnbootstrap.glfw.GrabListener;

/*
Static provider for GLFW
 */
public class GLFWBackend extends Platform {
    private static final GrabListener BASE_GRAB_LISTENER = Platform::executeGrabbingListeners;

    public GLFWBackend(){
        GLFW.addGrabListener(BASE_GRAB_LISTENER);
        GLFW.setGamepadEnableHandler(Platform.getGamepadEnableHandler());
    }
    public static void initialize() {}

    @Override
    public void surfaceCreated(Surface surface) {
        GLFW.nativeSurfaceCreated(surface);
    }

    @Override
    public void surfaceUpdated() {
        GLFW.nativeSurfaceUpdated();
    }

    @Override
    public void surfaceDestroyed() {
        GLFW.nativeSurfaceDestroyed();
    }

    @Override
    public void sendMousePosition() {
        GLFW.cursorX = Platform.cursorX;
        GLFW.cursorY = Platform.cursorY;
        GLFW.sendMousePos();
    }

    @Override
    public void sendMouseEvent(int button, int action, int mods) {
        int glfwButton;
        switch (button) {
            case MotionEvent.BUTTON_PRIMARY:    glfwButton = LwjglGlfwKeycode.GLFW_MOUSE_BUTTON_LEFT; break;
            case MotionEvent.BUTTON_SECONDARY:  glfwButton = LwjglGlfwKeycode.GLFW_MOUSE_BUTTON_RIGHT; break;
            case MotionEvent.BUTTON_TERTIARY:   glfwButton = LwjglGlfwKeycode.GLFW_MOUSE_BUTTON_MIDDLE; break;
            // TODO: back/forward buttons from MotionEvent, are they even used?
            default:
                glfwButton = 0;
        }
        GLFW.sendMouseEvent(glfwButton, action, mods);
    }

    @Override
    public void sendKeyEvent(int key, int state, int mods, char codepoint) {
        GLFW.sendRawKeyEvent(key, state, mods, codepoint);
    }
    @Override
    public void sendKeyEvent(int key, int state, int mods) {
        GLFW.sendRawKeyEvent(key, state, mods, (char)0);
    }
    @Override
    public void sendKeyEvent(int key, boolean state, int mods) {
        GLFW.sendRawKeyEvent(key, state ? 1 : 0, mods, (char)0);
    }

    @Override
    public void sendScrollEvent(double x, double y) {
        GLFW.sendScrollEvent(x, y);
    }

    @Override
    public void sendBulkUnicodeEvent(String text, int mods) {
        GLFW.sendBulkUnicodeEvent(text, mods);
    }
}
