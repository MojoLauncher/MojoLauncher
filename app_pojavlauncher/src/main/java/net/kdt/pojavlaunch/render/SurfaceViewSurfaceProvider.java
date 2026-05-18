package net.kdt.pojavlaunch.render;

import android.content.Context;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import androidx.annotation.NonNull;

public class SurfaceViewSurfaceProvider implements SurfaceProvider<SurfaceView> {
    @Override
    public SurfaceView create(Context context, SurfaceCallback callback) {
        SurfaceView surfaceView = new SurfaceView(context);
        surfaceView.getHolder().addCallback(new CallbackAdapter(callback));
        return surfaceView;
    }
    private static class CallbackAdapter implements SurfaceHolder.Callback {
        private final SurfaceCallback mCallback;

        private CallbackAdapter(SurfaceCallback mCallback) {
            this.mCallback = mCallback;
        }

        @Override
        public void surfaceChanged(@NonNull SurfaceHolder surfaceHolder, int fmt, int width, int height) {
            mCallback.onSurfaceResized();
        }

        @Override
        public void surfaceCreated(@NonNull SurfaceHolder surfaceHolder) {
            mCallback.onSurfaceAvailable(surfaceHolder.getSurface());
        }

        @Override
        public void surfaceDestroyed(@NonNull SurfaceHolder surfaceHolder) {
            mCallback.onSurfaceDestroyed();
        }
    }
}
