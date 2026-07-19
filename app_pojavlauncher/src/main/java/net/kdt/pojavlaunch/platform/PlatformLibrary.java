package net.kdt.pojavlaunch.platform;

import android.view.Surface;

import net.kdt.pojavlaunch.MainActivity;
import net.kdt.pojavlaunch.lifecycle.ContextExecutor;

import java.util.ArrayList;
import java.util.List;

import git.artdeell.dnbootstrap.glfw.GLFW;
import git.artdeell.dnbootstrap.glfw.GamepadEnableHandler;
import git.mojo.sdl.SDLActivity;

public abstract class PlatformLibrary {
    public static void initializeCallbacks(){
        GLFW.setInitCallback(() -> onInit(new GLFWImpl()));
        SDLActivity.setInitCallback(() -> onInit(new SDLImpl()));
        GLFWImpl.initialize();
        SDLImpl.initialize();
    }

    public static PlatformLibrary PLATFORM = new NullImpl(); // Initialize a dummy platform - the game will initialize correct one later
    private static List<PlatformGrabListener> grabListeners = new ArrayList<>();
    static {
        grabListeners.add(grabbing -> isGrabbing = grabbing);
    }
    private static boolean isGrabbing = false;
    public static double cursorX;
    public static double cursorY;
    private static Surface mPendingSurface;

    private static void onInit(PlatformLibrary impl){
        PlatformLibrary.setPlatformLibrary(impl);
        ContextExecutor.executeActivity(activity -> ((MainActivity) activity).hideLoadingScreen());
    }



    public abstract void surfaceCreated(Surface surface);
    public abstract void surfaceUpdated();
    public abstract void surfaceDestroyed();

    public abstract void sendMousePosition();
    public abstract void sendMouseEvent(int key, int state, int mods);
    public abstract void sendKeyEvent(int key, int state, int mods, char codepoint);
    public abstract void sendKeyEvent(int key, int state, int mods);
    public abstract void sendKeyEvent(int key, boolean state, int mods);
    public abstract void sendScrollEvent(double x, double y);
    public abstract void sendBulkUnicodeEvent(String text, int mods);
    public static boolean isGrabbing(){
        return isGrabbing;
    }
    public static void setPendingSurface(Surface surface){
        mPendingSurface = surface;
    }

    static void executeGrabbingListeners(boolean grabbing){
        for(PlatformGrabListener listener : grabListeners){
            listener.onGrabState(grabbing);
        }
    }
    public abstract void setGamepadEnableHandler(GamepadEnableHandler handler);
    public static void addGrabListener(PlatformGrabListener pgl){
        grabListeners.add(pgl);
    }

    public static void setPlatformLibrary(PlatformLibrary library){
        PLATFORM = library;
        if(mPendingSurface != null)
            PLATFORM.surfaceCreated(mPendingSurface);
    }

}
