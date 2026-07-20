package net.kdt.pojavlaunch;

import static net.kdt.pojavlaunch.platform.Platform.PLATFORM;

import android.content.Intent;
import android.net.Uri;
import android.view.Choreographer;
import android.view.KeyEvent;

import androidx.annotation.Keep;

import net.kdt.pojavlaunch.lifecycle.ContextExecutor;
import net.kdt.pojavlaunch.platform.Platform;

import java.io.File;

public class CallbackBridge {
    public static final Choreographer sChoreographer = Choreographer.getInstance();

    public static volatile int windowWidth, windowHeight;
    public static volatile float windowRate;
    public volatile static boolean holdingAlt, holdingCapslock, holdingCtrl,
            holdingNumlock, holdingShift;

    public static void performClick(int button) {
        double ox = Platform.cursorX, oy = Platform.cursorY;
        PLATFORM.sendMouseEvent(button, 1, CallbackBridge.getCurrentMods());
        sChoreographer.postFrameCallbackDelayed(l -> {
            Platform.cursorX = ox;
            Platform.cursorY = oy;
            PLATFORM.sendMouseEvent(button, 0, CallbackBridge.getCurrentMods());
        }, 33);
    }


    public static void sendKeyPress(int keyCode) {
        PLATFORM.sendKeyEvent(keyCode, true, getCurrentMods());
        PLATFORM.sendKeyEvent(keyCode, false, getCurrentMods());
    }

    public static void sendMouseButton(int button, boolean status) {
        CallbackBridge.sendMouseKeycode(button, CallbackBridge.getCurrentMods(), status);
    }

    public static void sendMouseKeycode(int button, int modifiers, boolean isDown) {
        PLATFORM.sendMouseEvent(button, isDown ? 1 : 0, modifiers);
    }

    public static void sendScroll(double xoffset, double yoffset) {
        PLATFORM.sendScrollEvent(xoffset, yoffset);
    }

    public static int getCurrentMods() {
        int currMods = 0;
        if (holdingAlt) {
            currMods |= LwjglGlfwKeycode.GLFW_MOD_ALT;
        } if (holdingCapslock) {
            currMods |= LwjglGlfwKeycode.GLFW_MOD_CAPS_LOCK;
        } if (holdingCtrl) {
            currMods |= LwjglGlfwKeycode.GLFW_MOD_CONTROL;
        } if (holdingNumlock) {
            currMods |= LwjglGlfwKeycode.GLFW_MOD_NUM_LOCK;
        } if (holdingShift) {
            currMods |= LwjglGlfwKeycode.GLFW_MOD_SHIFT;
        }
        return currMods;
    }

    public static void setModifiers(KeyEvent keyEvent) {
        CallbackBridge.holdingAlt = keyEvent.isAltPressed();
        CallbackBridge.holdingCapslock = keyEvent.isCapsLockOn();
        CallbackBridge.holdingCtrl = keyEvent.isCtrlPressed();
        CallbackBridge.holdingNumlock = keyEvent.isNumLockOn();
        CallbackBridge.holdingShift = keyEvent.isShiftPressed();
    }

    public static void setModifiers(int keyCode, boolean isDown){
        switch (keyCode){
            case KeyEvent.KEYCODE_SHIFT_LEFT:
                CallbackBridge.holdingShift = isDown;
                return;

            case KeyEvent.KEYCODE_CTRL_LEFT:
                CallbackBridge.holdingCtrl = isDown;
                return;

            case KeyEvent.KEYCODE_ALT_LEFT:
                CallbackBridge.holdingAlt = isDown;
                return;

            case KeyEvent.KEYCODE_CAPS_LOCK:
                CallbackBridge.holdingCapslock = isDown;
                return;

            case KeyEvent.KEYCODE_NUM_LOCK:
                CallbackBridge.holdingNumlock = isDown;
        }
    }

    @Keep
    public static void openLink(String link) {
        ContextExecutor.executeActivity(ctx->{
            try {
                if(link.startsWith("file:")) {
                    int truncLength = 5;
                    if(link.startsWith("file://")) truncLength = 7;
                    String path = link.substring(truncLength);
                    Tools.openPath(ctx, new File(path), false);
                }else {
                    Intent intent = new Intent(Intent.ACTION_VIEW);
                    intent.setDataAndType(Uri.parse(link), "*/*");
                    ctx.startActivity(intent);
                }
            } catch (Throwable th) {
                Tools.showError(ctx, th);
            }
        });
    }

    @SuppressWarnings("unused") //TODO: actually use it
    public static void openPath(String path) {
        ContextExecutor.executeActivity(ctx->{
            try {
                Tools.openPath(ctx, new File(path), false);
            } catch (Throwable th) {
                Tools.showError(ctx, th);
            }
        });
    }

    public static native void minibridgeInit();

    static {
        System.loadLibrary("pojavexec");
        minibridgeInit();
    }
}

