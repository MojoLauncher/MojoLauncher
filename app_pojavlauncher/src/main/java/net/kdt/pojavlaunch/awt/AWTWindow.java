package net.kdt.pojavlaunch.awt;

import android.view.Surface;

import java.nio.ByteBuffer;

public class AWTWindow {
    public static native void nativeMoveWindow(int xoff, int yoff);

    // Obtain AWT screen pixels to render on Android SurfaceView
    public static native void renderFrame();
    public static native void destroySurface();
    public static native void setNativeSurface(Surface surface);
    public static native void setNativeSize(int width, int height);

}
