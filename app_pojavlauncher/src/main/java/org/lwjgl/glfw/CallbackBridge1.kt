package org.lwjgl.glfw

import android.content.ClipData
import android.content.ClipDescription
import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.util.Log
import android.view.Choreographer
import androidx.annotation.Keep
import androidx.core.graphics.createBitmap
import androidx.core.graphics.drawable.toDrawable
import androidx.core.util.Consumer
import dalvik.annotation.optimization.CriticalNative
import net.kdt.pojavlaunch.GrabListener
import net.kdt.pojavlaunch.LwjglGlfwKeycode
import net.kdt.pojavlaunch.MainActivity
import net.kdt.pojavlaunch.Tools
import net.kdt.pojavlaunch.customcontrols.gamepad.direct.DirectGamepadEnableHandler
import net.kdt.pojavlaunch.customcontrols.mouse.CursorContainer
import java.lang.ref.WeakReference
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.CopyOnWriteArraySet
import kotlin.concurrent.Volatile

object CallbackBridge {
    val sChoreographer: Choreographer = Choreographer.getInstance()

    // Avoid going through the JNI each time.
    @JvmStatic
    @Volatile
    var isGrabbing: Boolean = false
        private set
    private val grabListeners: MutableList<GrabListener?> = CopyOnWriteArrayList()

    // Use a weak reference here to avoid possibly statically referencing a Context.
    private var sDirectGamepadEnableHandler: WeakReference<DirectGamepadEnableHandler?>? = null

    const val CLIPBOARD_COPY: Int = 2000
    const val CLIPBOARD_PASTE: Int = 2001
    const val CLIPBOARD_OPEN: Int = 2002

    @JvmField
    @Volatile
    var windowWidth: Int = 0

    @JvmField
    @Volatile
    var windowHeight: Int = 0
    @JvmField
    var mouseX: Float = 0f
    @JvmField
    var mouseY: Float = 0f

    @JvmField
    @Volatile
    var holdingAlt: Boolean = false

    @JvmField
    @Volatile
    var holdingCapslock: Boolean = false

    @JvmField
    @Volatile
    var holdingCtrl: Boolean = false

    @JvmField
    @Volatile
    var holdingNumlock: Boolean = false

    @JvmField
    @Volatile
    var holdingShift: Boolean = false

    @JvmField
    val sGamepadButtonBuffer: ByteBuffer?
    @JvmField
    val sGamepadAxisBuffer: FloatBuffer
    @JvmField
    var sGamepadDirectInput: Boolean = false

    private var sCursor: CursorContainer? = null
    private val cursorChangeListeners: MutableSet<Consumer<CursorContainer?>> =
        CopyOnWriteArraySet()

    @JvmStatic
    fun putMouseEventWithCoords(button: Int, x: Float, y: Float) {
        putMouseEventWithCoords(button, true, x, y)
        sChoreographer.postFrameCallbackDelayed({
            putMouseEventWithCoords(
                button,
                false,
                x,
                y
            )
        }, 33)
    }

    @JvmStatic
    fun putMouseEventWithCoords(
        button: Int,
        isDown: Boolean,
        x: Float,
        y: Float /* , int dz, long nanos */
    ) {
        sendCursorPos(x, y)
        sendMouseKeycode(
            button,
            currentMods, isDown
        )
    }


    @JvmStatic
    fun sendCursorPos(x: Float, y: Float) {
        mouseX = x
        mouseY = y
        nativeSendCursorPos(mouseX, mouseY)
    }

    @JvmStatic
    fun sendKeycode(keycode: Int, keychar: Char, scancode: Int, modifiers: Int, isDown: Boolean) {
        // TODO CHECK: This may cause input issue, not receive input!
        if (keycode != 0) nativeSendKey(keycode, scancode, if (isDown) 1 else 0, modifiers)
        if (isDown && keychar != '\u0000') {
            nativeSendCharMods(keychar, modifiers)
            nativeSendChar(keychar)
        }
    }

    @JvmStatic
    fun sendChar(keychar: Char, modifiers: Int) {
        nativeSendCharMods(keychar, modifiers)
        nativeSendChar(keychar)
    }

    @JvmStatic
    fun sendKeyPress(keyCode: Int, modifiers: Int, status: Boolean) {
        sendKeyPress(keyCode, 0, modifiers, status)
    }

    @JvmStatic
    fun sendKeyPress(keyCode: Int, scancode: Int, modifiers: Int, status: Boolean) {
        sendKeyPress(keyCode, '\u0000', scancode, modifiers, status)
    }

    @JvmStatic
    fun sendKeyPress(keyCode: Int, keyChar: Char, scancode: Int, modifiers: Int, status: Boolean) {
        sendKeycode(keyCode, keyChar, scancode, modifiers, status)
    }

    @JvmStatic
    fun sendKeyPress(keyCode: Int) {
        sendKeyPress(
            keyCode,
            currentMods, true
        )
        sendKeyPress(
            keyCode,
            currentMods, false
        )
    }

    @JvmStatic
    fun sendMouseButton(button: Int, status: Boolean) {
        sendMouseKeycode(
            button,
            currentMods, status
        )
    }

    @JvmStatic
    fun sendMouseKeycode(button: Int, modifiers: Int, isDown: Boolean) {
        // if (isGrabbing()) DEBUG_STRING.append("MouseGrabStrace: " + android.util.Log.getStackTraceString(new Throwable()) + "\n");
        nativeSendMouseButton(button, if (isDown) 1 else 0, modifiers)
    }

    @JvmStatic
    fun sendScroll(xoffset: Double, yoffset: Double) {
        nativeSendScroll(xoffset, yoffset)
    }

    @JvmStatic
    fun sendUpdateWindowSize(w: Int, h: Int) {
        nativeSendScreenSize(w, h)
    }

    // Called from JRE side
    @JvmStatic
    @Suppress("unused")
    @Keep
    fun accessAndroidClipboard(type: Int, copy: String?): String? {
        return when (type) {
            CLIPBOARD_COPY -> {
                MainActivity.GLOBAL_CLIPBOARD?.setPrimaryClip(ClipData.newPlainText("Copy", copy))
                null
            }

            CLIPBOARD_PASTE -> if (MainActivity.GLOBAL_CLIPBOARD?.hasPrimaryClip() == true
                && MainActivity.GLOBAL_CLIPBOARD?.primaryClipDescription?.hasMimeType(ClipDescription.MIMETYPE_TEXT_PLAIN) == true
            ) {
                MainActivity.GLOBAL_CLIPBOARD?.primaryClip?.getItemAt(0)?.text
                    .toString()
            } else {
                ""
            }

            CLIPBOARD_OPEN -> {
                if (!copy.isNullOrEmpty()) {
                    MainActivity.openLink(copy)
                }
                null
            }

            else -> null
        }
    }


    @JvmStatic
    val currentMods: Int
        get() {
            var currMods = 0
            if (holdingAlt) {
                currMods = currMods or LwjglGlfwKeycode.GLFW_MOD_ALT
            }
            if (holdingCapslock) {
                currMods = currMods or LwjglGlfwKeycode.GLFW_MOD_CAPS_LOCK
            }
            if (holdingCtrl) {
                currMods = currMods or LwjglGlfwKeycode.GLFW_MOD_CONTROL
            }
            if (holdingNumlock) {
                currMods = currMods or LwjglGlfwKeycode.GLFW_MOD_NUM_LOCK
            }
            if (holdingShift) {
                currMods = currMods or LwjglGlfwKeycode.GLFW_MOD_SHIFT
            }
            return currMods
        }

    @JvmStatic
    fun setModifiers(keyCode: Int, isDown: Boolean) {
        when (keyCode) {
            LwjglGlfwKeycode.GLFW_KEY_LEFT_SHIFT.toInt() -> {
                holdingShift = isDown
                return
            }

            LwjglGlfwKeycode.GLFW_KEY_LEFT_CONTROL.toInt() -> {
                holdingCtrl = isDown
                return
            }

            LwjglGlfwKeycode.GLFW_KEY_LEFT_ALT.toInt() -> {
                holdingAlt = isDown
                return
            }

            LwjglGlfwKeycode.GLFW_KEY_CAPS_LOCK.toInt() -> {
                holdingCapslock = isDown
                return
            }

            LwjglGlfwKeycode.GLFW_KEY_NUM_LOCK.toInt() -> holdingNumlock = isDown
        }
    }

    //Called from JRE side
    @JvmStatic
    @Suppress("unused")
    @Keep
    private fun onDirectInputEnable() {
        Log.i("CallbackBridge", "onDirectInputEnable()")
        val enableHandler = Tools.getWeakReference<DirectGamepadEnableHandler?>(
            sDirectGamepadEnableHandler
        )
        enableHandler?.onDirectGamepadEnabled()
        sGamepadDirectInput = true
    }

    //Called from JRE side
    @JvmStatic
    @Suppress("unused")
    @Keep
    private fun onGrabStateChanged(grabbing: Boolean) {
        isGrabbing = grabbing
        sChoreographer.postFrameCallbackDelayed({
            // If the grab re-changed, skip notify process
            if (isGrabbing != grabbing) return@postFrameCallbackDelayed

            // Log the change
            Log.i("CallbackBridge", "Grab changed : $grabbing")

            // Iterate over the listeners.
            // Using CopyOnWriteArrayList makes iteration thread-safe and CME-free.
            for (g in grabListeners) {
                g?.onGrabState(grabbing)
            }
        }, 16)
    }

    @JvmStatic
    fun addGrabListener(listener: GrabListener?) {
        if (listener == null) return
        listener.onGrabState(isGrabbing)
        if (!grabListeners.contains(listener)) {
            grabListeners.add(listener)
        }
    }

    @JvmStatic
    fun removeGrabListener(listener: GrabListener?) {
        grabListeners.remove(listener)
    }

    @JvmStatic
    fun createGamepadAxisBuffer(): FloatBuffer {
        val axisByteBuffer = nativeCreateGamepadAxisBuffer()
        // NOTE: hardcoded order (also in jre_lwjgl3glfw CallbackBridge)
        return axisByteBuffer.order(ByteOrder.LITTLE_ENDIAN).asFloatBuffer()
    }

    @JvmStatic
    fun setDirectGamepadEnableHandler(h: DirectGamepadEnableHandler?) {
        sDirectGamepadEnableHandler = WeakReference(h)
    }

    @JvmStatic
    var cursor: CursorContainer?
        get() = sCursor
        set(cursor) {
            sCursor = cursor
            for (listener in cursorChangeListeners) {
                listener.accept(cursor)
            }
        }

    @JvmStatic
    @Suppress("unused")
    @Keep
    private fun removeCursor(cursor: CursorContainer?) {
        if (cursor == null) return
        if (sCursor === cursor) CallbackBridge.cursor =
            null
    }

    @JvmStatic
    @Suppress("unused")
    @Keep
    private fun createCursor(
        imageBuffer: ByteBuffer,
        width: Int,
        height: Int,
        xHot: Int,
        yHot: Int
    ): CursorContainer {
        val bitmap = createBitmap(width, height, Bitmap.Config.ARGB_8888)
        bitmap.copyPixelsFromBuffer(imageBuffer)
        // using the system resources isn't really a good practice
        // but we do not have access to our context here
        val drawable = bitmap.toDrawable(Resources.getSystem())
        drawable.setBounds(0, 0, width, height)

        // I am not sure why this works, but when this is here
        // the bitmap becomes premultiplied, although this quite literally
        // does nothing
        drawable.colorFilter = ColorMatrixColorFilter(ColorMatrix())

        return CursorContainer(drawable, xHot, yHot)
    }

    @JvmStatic
    fun addCursorChangeListener(listener: Consumer<CursorContainer?>?) {
        cursorChangeListeners.add(listener!!)
    }

    @JvmStatic
    fun removeCursorChangeListener(listener: Consumer<CursorContainer?>?) {
        cursorChangeListeners.remove(listener)
    }

    @JvmStatic
    @Keep
    @CriticalNative
    external fun nativeSetUseInputStackQueue(useInputStackQueue: Boolean)

    @JvmStatic
    @Keep
    @CriticalNative
    private external fun nativeSendChar(codepoint: Char): Boolean

    // GLFW: GLFWCharModsCallback deprecated, but is Minecraft still use?
    @JvmStatic
    @Keep
    @CriticalNative
    private external fun nativeSendCharMods(codepoint: Char, mods: Int): Boolean

    @JvmStatic
    @Keep
    @CriticalNative
    private external fun nativeSendKey(key: Int, scancode: Int, action: Int, mods: Int)

    // private static native void nativeSendCursorEnter(int entered);
    @JvmStatic
    @Keep
    @CriticalNative
    private external fun nativeSendCursorPos(x: Float, y: Float)

    @JvmStatic
    @Keep
    @CriticalNative
    private external fun nativeSendMouseButton(button: Int, action: Int, mods: Int)

    @JvmStatic
    @Keep
    @CriticalNative
    private external fun nativeSendScroll(xoffset: Double, yoffset: Double)

    @JvmStatic
    @Keep
    @CriticalNative
    private external fun nativeSendScreenSize(width: Int, height: Int)

    @JvmStatic
    external fun nativeSetWindowAttrib(attrib: Int, value: Int)

    @JvmStatic
    @Keep
    private external fun nativeCreateGamepadButtonBuffer(): ByteBuffer?

    @JvmStatic
    @Keep
    private external fun nativeCreateGamepadAxisBuffer(): ByteBuffer

    init {
        System.loadLibrary("pojavexec")
        sGamepadButtonBuffer = nativeCreateGamepadButtonBuffer()
        sGamepadAxisBuffer = createGamepadAxisBuffer()
    }
}
