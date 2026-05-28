package git.artdeell.dnbootstrap.glfw;

import android.graphics.Bitmap;
import android.util.Log;
import android.view.Surface;

import java.lang.ref.WeakReference;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.Collections;
import java.util.Set;
import java.util.WeakHashMap;

import git.artdeell.dnbootstrap.utils.Utils;

public class GLFW {
    private static final Set<GrabListener> grabListeners = Collections.newSetFromMap(new WeakHashMap<>());
    private static WeakReference<CursorImplementor> cursorImpl;
    private static WeakReference<ClipboardProvider> clipboardImpl;
    private static WeakReference<GamepadEnableHandler> gamepadEnable;
    private static boolean grabbing = false;
    private static GLFWCursor cursor;
    public static double cursorX = 0.5, cursorY = 0.5;
    public static ByteBuffer gamepadButtonBuffer;
    public static FloatBuffer gamepadAxisBuffer;

    static {
        System.loadLibrary("glfw");
        GLFW.initialize();
    }

    public static void setCursorImpl(CursorImplementor cursorImpl) {
        GLFW.cursorImpl = new WeakReference<>(cursorImpl);
        addGrabListener(cursorImpl);
    }

    public static void setClipboardImpl(ClipboardProvider clipboardImpl) {
        GLFW.clipboardImpl = new WeakReference<>(clipboardImpl);
    }

    public static void setGamepadEnableHandler(GamepadEnableHandler handler) {
        GLFW.gamepadEnable = new WeakReference<>(handler);
    }

    public static void addGrabListener(GrabListener grabListener) {
        grabListeners.add(grabListener);
    }

    public static boolean isGrabbing() {
        return grabbing;
    }

    public static GLFWCursor getCursor() {
        return cursor;
    }

    public static void sendMousePos() {
        if(!grabbing) {
            if(cursorX < 0) cursorX = 0;
            else if(cursorX > 1) cursorX = 1;
            if(cursorY < 0) cursorY = 0;
            else if(cursorY > 1) cursorY = 1;
        }
        CursorImplementor cursor = Utils.getWeakReference(GLFW.cursorImpl);
        if(cursor != null) cursor.onCursorPosition();
        sendMousePosition0(cursorX, cursorY);
    }

    @SuppressWarnings("unused") // Used from native
    private static void receiveGrabState(boolean isGrabbing) {
        boolean wasGrabbing = GLFW.grabbing;
        GLFW.grabbing = isGrabbing;
        Utils.runOnUiThread(() -> {
            for(GrabListener grabListener : grabListeners) grabListener.onGrabState(isGrabbing);
        });
        if(!isGrabbing && wasGrabbing) {
            cursorX = cursorY = 0.5;
            sendMousePos();
        }

    }

    @SuppressWarnings("unused") // Used from native
    private static void receiveCursorPos(double x, double y) {
        cursorX = x;
        cursorY = y;
        CursorImplementor cursor = Utils.getWeakReference(GLFW.cursorImpl);
        if(cursor != null) cursor.onCursorPosition();
    }

    @SuppressWarnings("unused") // Used from native
    private static GLFWCursor loadCursor(ByteBuffer imageBytes, int width, int height, int xhot, int yhot) {
        try {
            Bitmap cursorBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
            cursorBitmap.copyPixelsFromBuffer(imageBytes);
            return new GLFWCursor(cursorBitmap, xhot, yhot);
        }catch (Throwable t) {
            Log.w("GLFW", "Failed to load cursor", t);
            return null;
        }
    }

    @SuppressWarnings("unused") // Used from native
    private static void useCursor(GLFWCursor glfwCursor) {
        GLFW.cursor = glfwCursor;
        CursorImplementor cursor = Utils.getWeakReference(GLFW.cursorImpl);
        if(cursor != null) cursor.onCursorChanged();
    }

    @SuppressWarnings("unused") // Used from native
    private static String getClipboardString() {
        ClipboardProvider clipboardProvider = Utils.getWeakReference(clipboardImpl);
        if(clipboardProvider == null) return null;
        return clipboardProvider.getClipboardString();
    }

    @SuppressWarnings("unused") // Used from native
    private static void setClipboardString(String str) {
        ClipboardProvider clipboardProvider = Utils.getWeakReference(clipboardImpl);
        if(clipboardProvider == null) return;
        clipboardProvider.setClipboardString(str);
    }

    @SuppressWarnings("unused") // Used from native
    private static void enableDirectGamepad(ByteBuffer buttonBuffer, ByteBuffer axisBuffer) {
        buttonBuffer = buttonBuffer.order(ByteOrder.nativeOrder());
        FloatBuffer axisFloatBuffer = axisBuffer.order(ByteOrder.nativeOrder()).asFloatBuffer();
        if(buttonBuffer.capacity() != 14 || axisFloatBuffer.capacity() != 6) {
            Log.i("GLFW", "Not enabling direct gamepad: unexpected buffer capacities ("+buttonBuffer.capacity()+" " + axisFloatBuffer.capacity()+")");
            return;
        }
        gamepadAxisBuffer = axisFloatBuffer;
        gamepadButtonBuffer = buttonBuffer;
        GamepadEnableHandler enableHandler = Utils.getWeakReference(gamepadEnable);
        if(enableHandler != null) enableHandler.onEnableGamepad();
    }

    public static void sendKeyEvent(int glfwCode, boolean state, int mods) {
        sendKeyEvent(glfwCode, state ? 1 : 0, mods);
    }

    public static native void initialize();
    private static native void sendMousePosition0(double x, double y);
    public static native void sendKeyEvent(int glfwCode, int state, int mods);
    public static native void sendRawKeyEvent(int androidCode, int state, int mods, char codepoint);
    public static native void sendMouseEvent(int glfwMouseKey, int state, int mods);
    public static native void sendBulkUnicodeEvent(String input, int mods);
    public static native void sendScrollEvent(double xoffset, double yoffset);
    public static native void nativeSurfaceCreated(Surface surface);
    public static native void nativeSurfaceDestroyed();
    public static native void nativeSurfaceUpdated();
    public static native void nativeNotifyGamepadConnected();
}
