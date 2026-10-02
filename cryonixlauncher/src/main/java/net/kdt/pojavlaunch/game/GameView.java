package net.kdt.pojavlaunch.game;

import static net.kdt.pojavlaunch.CallbackBridge.windowRate;
import static net.kdt.pojavlaunch.game.GameActivity.touchCharInput;
import static net.kdt.pojavlaunch.utils.MCOptionUtils.getMcScale;
import static net.kdt.pojavlaunch.CallbackBridge.sendMouseButton;
import static net.kdt.pojavlaunch.CallbackBridge.windowHeight;
import static net.kdt.pojavlaunch.CallbackBridge.windowWidth;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.os.Build;
import android.util.AttributeSet;
import android.util.Log;
import android.view.InputDevice;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import androidx.annotation.RequiresApi;

import net.kdt.pojavlaunch.CallbackBridge;
import net.kdt.pojavlaunch.Tools;
import net.kdt.pojavlaunch.customcontrols.ControlLayout;
import net.kdt.pojavlaunch.customcontrols.gamepad.Gamepad;
import net.kdt.pojavlaunch.customcontrols.mouse.AndroidPointerCapture;
import net.kdt.pojavlaunch.game.platform.input.PlatformGrabListener;
import net.kdt.pojavlaunch.game.platform.Platform;

import net.kdt.pojavlaunch.prefs.LauncherPreferences;
import net.kdt.pojavlaunch.render.SurfaceProvider;
import net.kdt.pojavlaunch.render.SurfaceViewSurfaceProvider;
import net.kdt.pojavlaunch.render.TextureViewSurfaceProvider;
import net.kdt.pojavlaunch.utils.MCOptionUtils;

import git.artdeell.mojo.R;
import git.artdeell.mojoexec.MojoExec;

import static net.kdt.pojavlaunch.game.platform.Platform.PLATFORM;

import java.util.Objects;

/**
 * Class dealing with showing minecraft surface and taking inputs to dispatch them to minecraft
 */
public class GameView extends FrameLayout implements PlatformGrabListener, SurfaceProvider.SurfaceCallback {

    /* Sensitivity, adjusted according to screen size */
    private final double mSensitivityFactor = (1.4 * (1080f/ Tools.getDisplayMetrics((Activity) getContext()).heightPixels));

    private final SurfaceProvider mSurfaceProvider = LauncherPreferences.PREF_USE_ALTERNATE_SURFACE ? new SurfaceViewSurfaceProvider() : new TextureViewSurfaceProvider();
    private boolean mRefreshOnly = true;
    /* Surface ready listener, used by the activity to launch minecraft */
    SurfaceReadyListener mSurfaceReadyListener = null;
    final Object mSurfaceReadyListenerLock = new Object();
    /* View holding the surface, either a SurfaceView or a TextureView */
    View mSurface;
    GameCursorView mCursorView;

    private final InGameEventProcessor mIngameProcessor = new InGameEventProcessor(this, mSensitivityFactor);
    private final InGUIEventProcessor mInGUIProcessor = new InGUIEventProcessor(this);
    private TouchEventProcessor mCurrentTouchProcessor = mInGUIProcessor;
    private AndroidPointerCapture mPointerCapture;
    private boolean mLastGrabState = false;
    double cursorRatioX = 0;
    double cursorRatioY = 0;

    public GameView(Context context) {
        this(context, null);
    }

    public GameView(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
        setFocusable(true);
        setFocusableInTouchMode(true);
        Platform.addGrabListener(this);
    }

    // This is required to actually get the CursorView object
    @Override
    public void onFinishInflate(){
        super.onFinishInflate();
        mCursorView = findViewById(R.id.main_cursorview);
        Platform.setCursorImplementor(mCursorView);
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private void setUpPointerCapture() {
        if(mPointerCapture != null) mPointerCapture.detach();
        mPointerCapture = new AndroidPointerCapture(mCursorView, this);
    }

    /** Initialize the view and all its settings
     * @param isAlreadyRunning set to true to tell the view that the game is already running
     *                         (only updates the window without calling the start listener)
     */
    public void start(boolean isAlreadyRunning) {
        if (Tools.isAndroid8OrHigher()) setUpPointerCapture();
        mInGUIProcessor.setAbstractTouchpad(mCursorView);
        mRefreshOnly = isAlreadyRunning;
        mSurface = mSurfaceProvider.create(getContext(), this);
        this.addView(mSurface);
        this.mCursorView.bringToFront();
        this.requestFocus();
    }

    /**
     * The touch event for both grabbed an non-grabbed mouse state on the touch screen
     * Does not cover the virtual mouse touchpad
     */
    @Override
    @SuppressWarnings("accessibility")
    public boolean onTouchEvent(MotionEvent e) {
        // Kinda need to send this back to the layout
        if(((ControlLayout)getParent()).getModifiable()) return false;
        // Looking for a mouse to handle, won't have an effect if no mouse exists.
        for (int i = 0; i < e.getPointerCount(); i++) {
            int toolType = e.getToolType(i);
            if(toolType == MotionEvent.TOOL_TYPE_MOUSE) {
                if(Tools.isAndroid8OrHigher() &&
                        mPointerCapture != null) {
                    mPointerCapture.handleAutomaticCapture();
                    return true;
                }
            }else if(toolType != MotionEvent.TOOL_TYPE_STYLUS) continue;

            // Mouse found
            // Avoid going through the JNI each time.
            if(Platform.isGrabbing()) return false;
            Platform.cursorX = e.getX(i) / cursorRatioX;
            Platform.cursorY = e.getY(i) / cursorRatioY;
            Platform.sendCursorPosition();
            return true; //mouse event handled successfully
        }
        if (mIngameProcessor == null || mInGUIProcessor == null) return true;
        boolean ret = mCurrentTouchProcessor.processTouchEvent(e);
        // Keep cursor on screen if panning with IME inset
        if(LauncherPreferences.PREF_KEYBOARD_AUTOPANNING && GameActivity.mImeHeight > 0){
            int translationY = Tools.getTranslationFromCursorY(
                    (int) (Platform.cursorY * cursorRatioY + 100),
                    getHeight(),
                    GameActivity.mImeHeight,
                    0
            );
            // If the view was force panned (KeyboardPan keycode) apply an animation instead of immediate override
            // This fixes weird jumps when the user moves the cursor first time after pressing that keycode
            if(GameActivity.mForcedPanningHeight != 0) {
                mSurface.animate().setDuration(100).translationY(-translationY).start();
                mCursorView.animate().setDuration(100).translationY(-translationY).start();
                GameActivity.mForcedPanningHeight = 0;
            } else {
                mSurface.setTranslationY(-translationY);
                mCursorView.setTranslationY(-translationY);
            }
        }
        return ret;
    }

    /**
     * The event for mouse/joystick movements
     */
    @SuppressLint("NewApi")
    @Override
    public boolean dispatchGenericMotionEvent(MotionEvent event) {
        int mouseCursorIndex = -1;

        if(Gamepad.isGamepadEvent(event)){
            if(Platform.getPlatformGamepad() == null)
                Platform.createGenericGamepad(event.getDevice(), mCursorView);
            Platform.getPlatformGamepad().sendMotionEvent(event);
            return true;
        }

        for(int i = 0; i < event.getPointerCount(); i++) {
            if(event.getToolType(i) != MotionEvent.TOOL_TYPE_MOUSE && event.getToolType(i) != MotionEvent.TOOL_TYPE_STYLUS ) continue;
            // Mouse found
            mouseCursorIndex = i;
            break;
        }
        if(mouseCursorIndex == -1) return false; // we cant consoom that, theres no mice!

        // Make sure we grabbed the mouse if necessary
        // Avoid going through the JNI each time.
        updateGrabState(Platform.isGrabbing());

        switch(event.getActionMasked()) {
            case MotionEvent.ACTION_HOVER_MOVE:
                Platform.cursorX = event.getX(mouseCursorIndex) / cursorRatioX;
                Platform.cursorY = event.getY(mouseCursorIndex) / cursorRatioY;
                Platform.sendCursorPosition();
                return true;
            case MotionEvent.ACTION_SCROLL:
                CallbackBridge.sendScroll(event.getAxisValue(MotionEvent.AXIS_HSCROLL), event.getAxisValue(MotionEvent.AXIS_VSCROLL));
                return true;
            case MotionEvent.ACTION_BUTTON_PRESS: sendMouseButton(event.getActionButton(),true); return true;
            case MotionEvent.ACTION_BUTTON_RELEASE: sendMouseButton(event.getActionButton(),false); return true;
            default:
                return false;
        }
    }

    /** The event for keyboard/ gamepad button inputs */
    public boolean processKeyEvent(KeyEvent event) {
        //Log.i("KeyEvent", event.toString());

        //Filtering useless events by order of probability
        int eventKeycode = event.getKeyCode();
        if(eventKeycode == KeyEvent.KEYCODE_UNKNOWN) return true;
        if(eventKeycode == KeyEvent.KEYCODE_VOLUME_DOWN) return false;
        if(eventKeycode == KeyEvent.KEYCODE_VOLUME_UP) return false;
        if(event.getRepeatCount() != 0) return true;
        int action = event.getAction();
        if(action == KeyEvent.ACTION_MULTIPLE) return true;
        // Ignore the cancelled up events. They occur when the user switches layouts.
        // In accordance with https://developer.android.com/reference/android/view/KeyEvent#FLAG_CANCELED
        if(action == KeyEvent.ACTION_UP &&
                (event.getFlags() & KeyEvent.FLAG_CANCELED) != 0) return true;

        //Sometimes, key events comes from SOME keys of the software keyboard
        //Even weirder, is is unknown why a key or another is selected to trigger a keyEvent
        if((event.getFlags() & KeyEvent.FLAG_SOFT_KEYBOARD) == KeyEvent.FLAG_SOFT_KEYBOARD){
            if(eventKeycode == KeyEvent.KEYCODE_ENTER) return true; //We already listen to it.
            touchCharInput.dispatchKeyEvent(event);
            return true;
        }

        //Sometimes, key events may come from the mouse
        if(event.getDevice() != null
                && ( (event.getSource() & InputDevice.SOURCE_MOUSE_RELATIVE) == InputDevice.SOURCE_MOUSE_RELATIVE
                ||   (event.getSource() & InputDevice.SOURCE_MOUSE) == InputDevice.SOURCE_MOUSE)  ){

            if(eventKeycode == KeyEvent.KEYCODE_BACK){
                sendMouseButton(MotionEvent.BUTTON_SECONDARY, event.getAction() == KeyEvent.ACTION_DOWN);
                return true;
            }
        }

        if(Gamepad.isGamepadEvent(event)){
            if(Platform.getPlatformGamepad() == null)
                Platform.createGenericGamepad(event.getDevice(), mCursorView);
            Platform.getPlatformGamepad().sendKeyEvent(event);
            return true;
        }

        CallbackBridge.setModifiers(event);
        char codepoint = action == KeyEvent.ACTION_DOWN ? (char) event.getUnicodeChar(event.getMetaState()) : 0;
        if(PLATFORM.sendKeyEvent(eventKeycode, action == KeyEvent.ACTION_DOWN ? 1 : 0, CallbackBridge.getCurrentMods(), codepoint))
            return true;

        // Some events will be generated an infinite number of times when no consumed
        return (event.getFlags() & KeyEvent.FLAG_FALLBACK) == KeyEvent.FLAG_FALLBACK;
    }

    /** Called when the size need to be set at any point during the surface lifecycle **/
    public void refreshSize(){
        refreshSize(false);
    }

    /** Same as refreshSize, but allows you to force an immediate size update **/
    public void refreshSize(boolean immediate) {
        if(isInLayout() && !immediate) {
            post(this::refreshSize);
            return;
        }
        // Use the width and height of the View instead of display dimensions to avoid
        // getting squiched/stretched due to inconsistencies between the layout and
        // screen dimensions.
        int newWidth = Tools.getDisplayFriendlyRes(getWidth(), LauncherPreferences.PREF_SCALE_FACTOR);
        int newHeight = Tools.getDisplayFriendlyRes(getHeight(), LauncherPreferences.PREF_SCALE_FACTOR);
        if (newHeight < 1 || newWidth < 1) {
            Log.e("MGLSurface", String.format("Impossible resolution : %dx%d", newWidth, newHeight));
            return;
        }
        windowWidth = newWidth;
        windowHeight = newHeight;

        // Update cursor ratio values
        // Mouse events are sent in the full view coordinate space, the game accepts them only in the window space
        this.cursorRatioX = (double) getWidth() / windowWidth;
        this.cursorRatioY = (double) getHeight() / windowHeight;

        if(mSurface == null){
            Log.w("MGLSurface", "Attempt to refresh size on null surface");
            return;
        }
        windowRate = mSurface.getDisplay().getRefreshRate();
        MojoExec.setDisplayParams(windowWidth, windowHeight, windowRate);
        mSurfaceProvider.updateSize();
    }

    private void realStart(){
        // Initial size set. Request immedate refresh, otherwise the initial width and height for the game
        // may be broken/unknown.
        refreshSize(true);

        //Load Minecraft options:
        MCOptionUtils.set("fullscreen", "off");
        MCOptionUtils.set("overrideWidth", String.valueOf(windowWidth));
        MCOptionUtils.set("overrideHeight", String.valueOf(windowHeight));
        MCOptionUtils.save();
        getMcScale();

        new Thread(() -> {
            try {
                // Wait until the listener is attached
                synchronized(mSurfaceReadyListenerLock) {
                    if(mSurfaceReadyListener == null) mSurfaceReadyListenerLock.wait();
                }

                mSurfaceReadyListener.isReady();
            } catch (Throwable e) {
                Tools.showError(getContext(), e, true);
            }
        }, "JVM Main thread").start();
    }

    @Override
    public void onGrabState(boolean isGrabbing) {
        if(mLastGrabState != isGrabbing) {
            Log.i("MGLSurface", "Grabbing state changed! " + mLastGrabState + " -> " + isGrabbing);
            mCurrentTouchProcessor.cancelPendingActions();
            mCurrentTouchProcessor = pickEventProcessor(isGrabbing);
            mLastGrabState = isGrabbing;
        }
    }

    private TouchEventProcessor pickEventProcessor(boolean isGrabbing) {
        return isGrabbing ? mIngameProcessor : mInGUIProcessor;
    }

    private void updateGrabState(boolean isGrabbing) {

    }

    @Override
    public void onSurfaceAvailable(Surface surface) {
        Platform.updateSurface(surface);
        if(mRefreshOnly) return;
        realStart();
        mRefreshOnly = true;
    }

    @Override
    public void onSurfaceResized() {
        if(PLATFORM != null)
            PLATFORM.surfaceUpdated();
    }

    @Override
    public void onSurfaceDestroyed() {
        if(PLATFORM != null)
            PLATFORM.surfaceDestroyed();
    }

    /** A small interface called when the listener is ready for the first time */
    public interface SurfaceReadyListener {
        void isReady();
    }

    public void setSurfaceReadyListener(SurfaceReadyListener listener){
        synchronized (mSurfaceReadyListenerLock) {
            mSurfaceReadyListener = listener;
            mSurfaceReadyListenerLock.notifyAll();
        }
    }
    public static int getWindowWidth(){
        return windowWidth;
    }
    public static int getWindowHeight(){
        return windowHeight;
    }
    public static float getWindowRate(){
        return windowRate;
    }
    public double getCursorRatioX(){
        return cursorRatioX;
    }

    public double getCursorRatioY() {
        return cursorRatioY;
    }
    public SurfaceProvider getSurfaceProvider() {
        return mSurfaceProvider;
    }
}
