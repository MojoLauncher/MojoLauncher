package net.kdt.pojavlaunch.game.platform;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.util.Log;
import android.view.InputDevice;
import android.view.Surface;
import android.view.View;

import net.kdt.pojavlaunch.awt.AWTBridge;
import net.kdt.pojavlaunch.game.GameView;
import net.kdt.pojavlaunch.game.GameActivity;
import net.kdt.pojavlaunch.Tools;
import net.kdt.pojavlaunch.customcontrols.gamepad.DefaultDataProvider;
import net.kdt.pojavlaunch.customcontrols.gamepad.Gamepad;
import net.kdt.pojavlaunch.game.platform.backend.AWTBackend;
import net.kdt.pojavlaunch.game.platform.backend.DummyBackend;
import net.kdt.pojavlaunch.lifecycle.ContextExecutor;
import net.kdt.pojavlaunch.game.platform.backend.GLFWBackend;
import net.kdt.pojavlaunch.game.platform.backend.PlatformBackend;
import net.kdt.pojavlaunch.game.platform.backend.SDLBackend;
import net.kdt.pojavlaunch.game.platform.clipboard.AndroidClipboard;
import net.kdt.pojavlaunch.game.platform.cursor.PlatformCursor;
import net.kdt.pojavlaunch.game.platform.cursor.PlatformCursorImplementor;
import net.kdt.pojavlaunch.game.platform.input.PlatformGamepad;
import net.kdt.pojavlaunch.game.platform.input.PlatformGrabListener;
import net.kdt.pojavlaunch.game.platform.input.gamepad.GLFWGamepad;
import net.kdt.pojavlaunch.game.platform.input.gamepad.GenericGamepad;
import net.kdt.pojavlaunch.game.platform.input.gamepad.SDLGamepad;

import java.util.ArrayList;
import java.util.List;

import fr.spse.gamepad_remapper.RemapperManager;
import fr.spse.gamepad_remapper.RemapperView;
import git.artdeell.dnbootstrap.glfw.GLFW;
import git.mojo.sdl.SDLActivity;
import git.mojo.sdl.SDLControllerManager;

/**
 * Launcher Platform frontend used to manage different window system & input implementations. Currently supports SDL&GLFW
 */
public class Platform {
    // Always reset cursor on grab lost - makes it move to the center as should if the game didn't move it
    private static final boolean RESET_CURSOR_UNGRAB = true;
    public static PlatformBackend PLATFORM = new DummyBackend();
    public static double cursorX;
    public static double cursorY;
    private static final List<PlatformGrabListener> grabListeners = new ArrayList<>();
    private static PlatformCursorImplementor mCursorImplementor = null;
    private static boolean isGrabbing = false;
    private static Surface mPendingSurface;
    private static PlatformGamepad mPlatformGamepad = null;
    private static PlatformCursor mPlatformCursor = null;
    private static AndroidClipboard mClipboard;
    private static GameView mHostView;
    private static RemapperManager mInputManager;

    /**
     * Initialize Platform, set platform implementations' init callbacks and fire early initializers
     *
     * @param activity an activity to bind to
     * @param view a host view used for input handling
     */
    public static void initialize(Activity activity, GameView view) {
        Platform.mHostView = view;
        Platform.mInputManager = createRemapperManager(view);
        mClipboard = new AndroidClipboard(activity.getApplicationContext());
        GLFW.setInitCallback(() -> onInit(new GLFWBackend()));
        SDLActivity.setInitCallback(() -> onInit(new SDLBackend()));
        AWTBridge.setEnableCallback(() -> onInit(new AWTBackend()));
        SDLActivity.setClipboard(mClipboard);
        GLFW.setClipboardImpl(mClipboard);
        // SDL can handle gamepads on its own, so route all events through it
        // if SDL was detected of course (the check is based on detectDevices)
        // Vanilla SDL client shouldn't touch input system and thus cause emulated input to break
        SDLControllerManager.setEnabledCallback(() -> setPlatformGamepad(new SDLGamepad()));
        // GLFW also has equivalent "onDirectGamepadEnable". Hook it up
        GLFW.setGamepadEnableHandler(() -> setPlatformGamepad(new GLFWGamepad(view.getContext(), mInputManager)));
        SDLBackend.initialize(activity);
    }

    public static void initializeMinimal(Context appContext) {
        Platform.mHostView = null;
        Platform.mInputManager = null;
        mClipboard = new AndroidClipboard(appContext);
        // Do not set backend init callbacks here, as this will be functioning in single-platform mode
    }

    private static void onInit(PlatformBackend impl) {
        // We probably already initialized at this point. Don't try to initialize again
        Platform.setPlatformLibrary(impl);
        Log.i("Platform", "Init backend : " + impl.backendName());
        ContextExecutor.executeActivity(activity -> ((GameActivity) activity).hideLoadingScreen());
        resetCursorPosition();
    }

    /**
     * Is current platform implementation grabbed the cursor
     *
     * @return grab state
     */
    public static boolean isGrabbing() {
        return isGrabbing;
    }

    /**
     * Change grab state of a platform. Called from implementation-specific grab listeners. Safe to call from non-UI threads.
     *
     * @param grabbing new grab state
     */
    public static void grabStateChanged(boolean grabbing) {
        boolean wasGrabbing = isGrabbing;
        isGrabbing = grabbing;
        Tools.runOnUiThread(() -> {
            if (RESET_CURSOR_UNGRAB && wasGrabbing && !isGrabbing) resetCursorPosition();
            if (mCursorImplementor != null) mCursorImplementor.onGrabState(grabbing);
            for (PlatformGrabListener listener : grabListeners) {
                listener.onGrabState(grabbing);
            }
        });
    }

    /**
     * Get Platform gamepad implementation
     *
     * @return Platform gamepad object
     */
    public static PlatformGamepad getPlatformGamepad() {
        return mPlatformGamepad;
    }

    /**
     * Get Platform custom cursor
     *
     * @return cursor object
     */
    public static PlatformCursor getCursor() {
        return mPlatformCursor;
    }

    /**
     * Set Platform custom cursor
     *
     * @param bitmap Custom cursor bitmap
     * @param xhot   x offset of the cursor hotspot
     * @param yhot   y offset of the cursor hotspot
     */
    public static void setCursor(Bitmap bitmap, int xhot, int yhot) {
        mPlatformCursor = bitmap == null ? null : new PlatformCursor(bitmap, xhot, yhot);
        mCursorImplementor.onCursorChanged();
    }

    /**
     * Get currently used cursor implementor
     *
     * @return Cursor implementor
     */
    public static PlatformCursorImplementor getCursorImplementor() {
        return mCursorImplementor;
    }

    /**
     * Set cursor implementor for Platform
     *
     * @param implementor cursor implementor
     */
    public static void setCursorImplementor(PlatformCursorImplementor implementor) {
        mCursorImplementor = implementor;
    }

    private static void setPlatformGamepad(PlatformGamepad gamepad){
        if(mPlatformGamepad != null)
            mPlatformGamepad.onDestroy();
        mPlatformGamepad = gamepad;
    }

    /**
     * Create a generic gamepad implementation
     *
     * @param device Input device to accept events from
     * @param touchpadView A view representing on-screen "trackpad"
     */
    public static void createGenericGamepad(InputDevice device, View touchpadView){
        if(mHostView == null || mInputManager == null) return; // Running in minimal mode
        Gamepad gamepad = new Gamepad(device, DefaultDataProvider.INSTANCE, touchpadView);
        setPlatformGamepad(new GenericGamepad(mHostView.getContext(), mInputManager, gamepad));
    }

    /**
     * Set current cursor position
     *
     * @param x Cursor X
     * @param y Cursor Y
     */
    public static void setCursorPosition(double x, double y) {
        cursorX = x;
        cursorY = y;
        clampCursorPosition();
        mCursorImplementor.onCursorPosition();
    }

    /**
     * Clamp cursor position on the screen. Prevents the cursor from moving outside the game window
     */
    public static void clampCursorPosition() {
        cursorX = Math.clamp(cursorX, 0, GameView.getWindowWidth());
        cursorY = Math.clamp(cursorY, 0f, GameView.getWindowHeight());
    }

    /**
     * Reset current cursor position and set it to the center of a window
     */
    public static void resetCursorPosition() {
        cursorX = (double) GameView.getWindowWidth() / 2;
        cursorY = (double) GameView.getWindowHeight() / 2;
    }

    /**
     * Floor current cursor position to stop anticheats from triggering for no reason
     *
     */
    public static void floorCursorPosition(){
        cursorX = Math.floor(cursorX);
        cursorY = Math.floor(cursorY);
    }

    /**
     * Send current cursor position to the implementation after clamping and updating its view position.
     * Prefer using this over {@link PlatformBackend#sendMousePosition()}
     *
     */
    public static void sendCursorPosition() {
        if(mCursorImplementor != null) mCursorImplementor.onCursorPosition();
        if (!isGrabbing) clampCursorPosition();
        else floorCursorPosition();
        PLATFORM.sendMousePosition();
    }

    /**
     * Register Platform grab listener
     *
     * @param pgl Grab listener
     */
    public static void addGrabListener(PlatformGrabListener pgl) {
        grabListeners.add(pgl);
    }

    /**
     * Trigger surface recreate on implementation. Needs to be called each time a surface object becomes invalid
     *
     * @param surface Surface object
     */
    public static void updateSurface(Surface surface) {
        mPendingSurface = surface;
        PLATFORM.surfaceCreated(surface);
    }

    /**
     * Set platform implementation backend
     *
     * @param backend implementation backend
     */
    public static void setPlatformLibrary(PlatformBackend backend) {
        if(PLATFORM != null) PLATFORM.surfaceDestroyed();
        PLATFORM = backend;
        // To be picked by platform library
        if (mPendingSurface != null)
            PLATFORM.surfaceCreated(mPendingSurface);
    }

    /**
     * Get Platform clipboard
     * @return clipboard object
     */
    public static AndroidClipboard getClipboard() {
        return mClipboard;
    }

    private static RemapperManager createRemapperManager(View view){
        return new RemapperManager(view.getContext(), new RemapperView.Builder(null)
                .remapA(true)
                .remapB(true)
                .remapX(true)
                .remapY(true)
                .remapLeftJoystick(true)
                .remapRightJoystick(true)
                .remapStart(true)
                .remapSelect(true)
                .remapLeftShoulder(true)
                .remapRightShoulder(true)
                .remapLeftTrigger(true)
                .remapRightTrigger(true)
                .remapDpad(true));
    }
}
