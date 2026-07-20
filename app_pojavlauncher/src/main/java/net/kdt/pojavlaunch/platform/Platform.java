package net.kdt.pojavlaunch.platform;

import android.app.Activity;
import android.view.Surface;

import net.kdt.pojavlaunch.MainActivity;
import net.kdt.pojavlaunch.lifecycle.ContextExecutor;
import net.kdt.pojavlaunch.platform.backend.GLFWBackend;
import net.kdt.pojavlaunch.platform.backend.DummyBackend;
import net.kdt.pojavlaunch.platform.backend.PlatformBackend;
import net.kdt.pojavlaunch.platform.backend.SDLBackend;
import net.kdt.pojavlaunch.platform.cursor.PlatformCursor;
import net.kdt.pojavlaunch.platform.cursor.PlatformCursorImplementor;
import net.kdt.pojavlaunch.platform.input.PlatformGamepad;
import net.kdt.pojavlaunch.platform.input.PlatformGrabListener;
import net.kdt.pojavlaunch.platform.input.SDLGamepad;

import java.util.ArrayList;
import java.util.List;

import git.artdeell.dnbootstrap.glfw.GLFW;
import git.artdeell.dnbootstrap.glfw.GamepadEnableHandler;
import git.mojo.sdl.SDLActivity;
import git.mojo.sdl.SDLControllerManager;

public class Platform {
    public static void initializeCallbacks(Activity activity){
        GLFW.setInitCallback(() -> onInit(new GLFWBackend()));
        SDLActivity.setInitCallback(() -> onInit(new SDLBackend()));
        // SDL can handle gamepads on its own, so route all events through it
        // if SDL was detected of course (the check is based on detectDevices)
        // Vanilla SDL client shouldn't touch input system and thus cause emulated input to break
        SDLControllerManager.setEnabledCallback(() -> mPlatformGamepad = new SDLGamepad());
        SDLBackend.initialize(activity);
    }

    public static PlatformBackend PLATFORM = new DummyBackend(); // Initialize a dummy platform - the game will initialize correct one later
    private static List<PlatformGrabListener> grabListeners = new ArrayList<>();
    private static PlatformCursorImplementor mCursorImplementor = null;
    static {
        grabListeners.add(grabbing -> isGrabbing = grabbing);
        grabListeners.add(grabbing -> { if(mCursorImplementor != null) mCursorImplementor.onGrabState(grabbing); });
    }
    private static boolean isGrabbing = false;
    public static double cursorX;
    public static double cursorY;
    private static Surface mPendingSurface;
    private static PlatformGamepad mPlatformGamepad = null;
    private static PlatformCursor mPlatformCursor = null;
    private static GamepadEnableHandler mGamepadEnabler;

    private static void onInit(PlatformBackend impl){
        Platform.setPlatformLibrary(impl);
        ContextExecutor.executeActivity(activity -> ((MainActivity) activity).hideLoadingScreen());
    }
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
    public static PlatformCursor getCursor(){
        return mPlatformCursor;
    }
    public static void setCursorImplementor(PlatformCursorImplementor implementor){
        mCursorImplementor = implementor;
    }
    public static PlatformCursorImplementor getCursorImplementor(){
        return mCursorImplementor;
    }

    // To be picked by GLFW
    public static void setGamepadEnableHandler(GamepadEnableHandler handler){
        mGamepadEnabler = handler;
    }
    public static GamepadEnableHandler getGamepadEnableHandler(){
        return mGamepadEnabler;
    }
    public static void addGrabListener(PlatformGrabListener pgl){
        grabListeners.add(pgl);
    }

    public static void setPlatformLibrary(PlatformBackend backend){
        PLATFORM = backend;
        // To be picked by platform library
        if(mPendingSurface != null)
            PLATFORM.surfaceCreated(mPendingSurface);
    }
}
