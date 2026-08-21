package net.kdt.pojavlaunch.awt;

import android.view.Surface;

public class AWTWindow {
    public static native void nativeMoveWindow(int xoff, int yoff);

    public static native void beginRendering();
    public static native void endRendering();
    public static native void destroySurface();
    public static native void setNativeSurface(Surface surface);
    public static native void setNativeSize(int width, int height);

}
