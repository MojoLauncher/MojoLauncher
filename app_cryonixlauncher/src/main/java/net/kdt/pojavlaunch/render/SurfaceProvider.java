package net.kdt.pojavlaunch.render;

import android.content.Context;
import android.view.Surface;
import android.view.View;

public interface SurfaceProvider{
    View create(Context context, SurfaceCallback callback);
    void updateSize();

    interface SurfaceCallback {
        void onSurfaceAvailable(Surface surface);
        void onSurfaceResized();
        void onSurfaceDestroyed();
    }
}
