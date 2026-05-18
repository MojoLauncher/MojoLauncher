package net.kdt.pojavlaunch.render;

import android.content.Context;
import android.view.Surface;
import android.view.View;

public interface SurfaceProvider<T extends View> {
    T create(Context context, SurfaceCallback callback);

    interface SurfaceCallback {
        void onSurfaceAvailable(Surface surface);
        void onSurfaceResized();
        void onSurfaceDestroyed();
    }
}
