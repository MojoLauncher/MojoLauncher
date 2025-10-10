package net.kdt.pojavlaunch.customcontrols.mouse;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.res.ResourcesCompat;
import androidx.core.util.Consumer;

import net.kdt.pojavlaunch.GrabListener;
import net.kdt.pojavlaunch.prefs.LauncherPreferences;

import org.lwjgl.glfw.CallbackBridge;

import git.artdeell.mojo.R;

/**
 * Class dealing with the virtual mouse
 */
public class Touchpad extends View implements GrabListener, AbstractTouchpad, DefaultCursorCreator {
    /* Whether the Touchpad should be displayed */
    private boolean mDisplayState;
    /* Mouse pointer icon used by the touchpad */
    private float mMouseX, mMouseY;
    private boolean mMoveOnLayout;
    private CursorContainer mDefaultContainer;
    private final Consumer<CursorContainer> onCursorChange = cursor->invalidate();
    public Touchpad(@NonNull Context context) {
        this(context, null);
    }

    public Touchpad(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
        CallbackBridge.setCursorCreator(this);
    }

    /** Enable the touchpad */
    private void _enable(){
        setVisibility(VISIBLE);
        mMoveOnLayout = true;
    }

    /** Disable the touchpad and hides the mouse */
    private void _disable(){
        setVisibility(GONE);
    }

    /** @return The new state, enabled or disabled */
    public boolean switchState(){
        mDisplayState = !mDisplayState;
        if(!CallbackBridge.isGrabbing()) {
            if(mDisplayState) _enable();
            else _disable();
        }
        return mDisplayState;
    }

    public void placeMouseAt(float x, float y) {
        mMouseX = x;
        mMouseY = y;
        updateMousePosition();
    }

    private void sendMousePosition() {
        CallbackBridge.sendCursorPos((mMouseX * LauncherPreferences.PREF_SCALE_FACTOR), (mMouseY * LauncherPreferences.PREF_SCALE_FACTOR));
    }

    private void updateMousePosition() {
        sendMousePosition();
        // I wanted to implement a dirty rect for this, but it is ignored since API level 21
        // (which is our min API)
        // Let's hope the "internally calculated area" is good enough.
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        canvas.translate(mMouseX, mMouseY);
        canvas.scale(LauncherPreferences.PREF_MOUSESCALE, LauncherPreferences.PREF_MOUSESCALE);
        if(CallbackBridge.getCursor() != null) {
            CallbackBridge.getCursor().draw(canvas);
        } else {
            mDefaultContainer.draw(canvas);
        }
    }

    private void init() {
        mDefaultContainer = createDefaultCursor(0x00036001);
        CallbackBridge.addCursorChangeListener(onCursorChange);

        setFocusable(false);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            setDefaultFocusHighlightEnabled(false);
        }

        // When the game is grabbing, we should not display the mouse
        disable();
        mDisplayState = false;
    }

    @Override
    public void onGrabState(boolean isGrabbing) {
        post(()->updateGrabState(isGrabbing));
    }
    private void updateGrabState(boolean isGrabbing) {
        if(!isGrabbing) {
            if(mDisplayState && getVisibility() != VISIBLE) _enable();
            if(!mDisplayState && getVisibility() == VISIBLE) _disable();
        }else{
            if(getVisibility() != View.GONE) _disable();
        }
    }

    @Override
    public boolean getDisplayState() {
        return mDisplayState;
    }

    @Override
    public void applyMotionVector(float x, float y) {
        mMouseX = Math.max(0, Math.min(getWidth(), mMouseX + x * LauncherPreferences.PREF_MOUSESPEED));
        mMouseY = Math.max(0, Math.min(getHeight(), mMouseY + y * LauncherPreferences.PREF_MOUSESPEED));
        updateMousePosition();
    }

    @Override
    public void enable(boolean supposed) {
        if(mDisplayState) return;
        mDisplayState = true;
        if(supposed && CallbackBridge.isGrabbing()) return;
        _enable();
    }

    @Override
    public void disable() {
        if(!mDisplayState) return;
        mDisplayState = false;
        _disable();
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        if(!mMoveOnLayout) return;
        int w = getMeasuredWidth();
        int h = getMeasuredHeight();
        if(w == 0) w = getWidth();
        if(h == 0) h = getHeight();
        placeMouseAt(w / 2f, h / 2f);
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        CallbackBridge.addCursorChangeListener(onCursorChange);
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        // if we do not detach the listener
        // it may cause a memory leak due to the object
        // storing an instance of this View
        CallbackBridge.removeCursorChangeListener(onCursorChange);
    }

    @Override
    public CursorContainer createDefaultCursor(int shape) {
        int cursorResource, cursorHotspotX = 0, cursorHotspotY = 0;
        switch (shape) {
            case 0x00036001: // GLFW_ARROW_CURSOR
                cursorResource = R.drawable.cursor_arrow;
                break;
            case 0x00036002: // GLFW_IBEAM_CURSOR
                cursorResource = R.drawable.cursor_ibeam;
                cursorHotspotX = 9;
                cursorHotspotY = 16;
                break;
            case 0x00036004: // GLFW_HAND_CURSOR
                cursorResource = R.drawable.cursor_pointer;
                cursorHotspotY = 3;
                break;
            default:
                return null;
        }
        Drawable drawable = ResourcesCompat.getDrawable(getResources(), cursorResource, getContext().getTheme());
        assert drawable != null;
        if(drawable instanceof BitmapDrawable) {
            BitmapDrawable bitmapDrawable = (BitmapDrawable) drawable;
            bitmapDrawable.setFilterBitmap(false);
            bitmapDrawable.setAntiAlias(false);
            Bitmap bitmap = bitmapDrawable.getBitmap();
            drawable.setBounds(0, 0, bitmap.getWidth() * 2, bitmap.getHeight() * 2);
        }
        return new CursorContainer(drawable, cursorHotspotX, cursorHotspotY);
    }
}
