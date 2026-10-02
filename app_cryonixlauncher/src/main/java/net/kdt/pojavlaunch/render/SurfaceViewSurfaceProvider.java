package net.kdt.pojavlaunch.render;

import static net.kdt.pojavlaunch.CallbackBridge.windowHeight;
import static net.kdt.pojavlaunch.CallbackBridge.windowWidth;

import android.content.Context;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;

import androidx.annotation.NonNull;

import net.kdt.pojavlaunch.CallbackBridge;

public class SurfaceViewSurfaceProvider implements SurfaceProvider {
    private SurfaceView mSurfaceView;
    @Override
    public View create(Context context, SurfaceCallback callback) {
        mSurfaceView = new SurfaceView(context);
        mSurfaceView.getHolder().addCallback(new CallbackAdapter(callback));
        if(windowWidth != 0 && windowHeight != 0)
            mSurfaceView.getHolder().setFixedSize(windowWidth, windowHeight);
        return mSurfaceView;
    }

    @Override
    public void updateSize() {
        mSurfaceView.getHolder().setFixedSize(windowWidth, windowHeight);
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
