package net.kdt.pojavlaunch.awt;

import java.nio.ByteBuffer;

public class AWTWindow {
    public static native void nativeMoveWindow(int xoff, int yoff);

    // Obtain AWT screen pixels to render on Android SurfaceView
    public static native boolean nativeRenderFrame(ByteBuffer tempBuffer);

}
