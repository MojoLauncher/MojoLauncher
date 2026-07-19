package net.kdt.pojavlaunch.platform;

import android.app.Activity;
import android.view.Surface;

import net.kdt.pojavlaunch.MainActivity;
import net.kdt.pojavlaunch.lifecycle.ContextExecutor;
import net.kdt.pojavlaunch.platform.backend.GLFWBackend;
import net.kdt.pojavlaunch.platform.backend.DummyBackend;
import net.kdt.pojavlaunch.platform.backend.SDLBackend;
import net.kdt.pojavlaunch.platform.input.PlatformGamepad;
import net.kdt.pojavlaunch.platform.input.PlatformGrabListener;
import net.kdt.pojavlaunch.platform.input.SDLGamepad;

import java.util.ArrayList;
import java.util.List;

import git.artdeell.dnbootstrap.glfw.GLFW;
import git.artdeell.dnbootstrap.glfw.GamepadEnableHandler;
import git.mojo.sdl.SDLActivity;
import git.mojo.sdl.SDLControllerManager;

public abstract class Platform {
    public static void initializeCallbacks(Activity activity){
        GLFW.setInitCallback(() -> onInit(new GLFWBackend()));
        SDLActivity.setInitCallback(() -> onInit(new SDLBackend()));
        // SDL can handle gamepads on its own, so route all events through it
        // if SDL was detected of course (the check is based on detectDevices)
        // Vanilla SDL client shouldn't touch input system and thus cause emulated input to break
        SDLControllerManager.setEnabledCallback(() -> mPlatformGamepad = new SDLGamepad());
        SDLBackend.initialize(activity);
    }

    public static Platform PLATFORM = new DummyBackend(); // Initialize a dummy platform - the game will initialize correct one later
    private static List<PlatformGrabListener> grabListeners = new ArrayList<>();
    static {
        grabListeners.add(grabbing -> isGrabbing = grabbing);
    }
    private static boolean isGrabbing = false;
    public static double cursorX;
    public static double cursorY;
    private static Surface mPendingSurface;
    private static PlatformGamepad mPlatformGamepad = null;
    private static GamepadEnableHandler mGamepadEnabler;

    private static void onInit(Platform impl){
        Platform.setPlatformLibrary(impl);
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

    public static void executeGrabbingListeners(boolean grabbing){
        for(PlatformGrabListener listener : grabListeners){
            listener.onGrabState(grabbing);
        }
    }

    public static PlatformGamepad getPlatformGamepad() {
        return mPlatformGamepad;
    }

    // To be picked by GLFW
    public static void setGamepadEnableHandler(GamepadEnableHandler handler){
        mGamepadEnabler = handler;
    }
    static GamepadEnableHandler getGamepadEnableHandler(){
        return mGamepadEnabler;
    }
    public static void addGrabListener(PlatformGrabListener pgl){
        grabListeners.add(pgl);
    }

    public static void setPlatformLibrary(Platform library){
        PLATFORM = library;
        // To be picked by platform library
        if(mPendingSurface != null)
            PLATFORM.surfaceCreated(mPendingSurface);
    }

}
