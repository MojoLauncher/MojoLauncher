package net.kdt.pojavlaunch.render;

import static net.kdt.pojavlaunch.CallbackBridge.windowHeight;
import static net.kdt.pojavlaunch.CallbackBridge.windowWidth;

import android.content.Context;
import android.graphics.SurfaceTexture;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;

import androidx.annotation.NonNull;

import net.kdt.pojavlaunch.Tools;

public class TextureViewSurfaceProvider implements SurfaceProvider {
    private TextureView mTextureView;
    private SurfaceCallback mCallback;

    @Override
    public View create(Context context, SurfaceCallback callback) {
        mCallback = callback;
        mTextureView = new TextureView(context);
        mTextureView.setOpaque(true);
        mTextureView.setAlpha(1.0f);
        mTextureView.setSurfaceTextureListener(new CallbackAdapter());
        return mTextureView;
    }

    @Override
    public void updateSize() {
        SurfaceTexture surfaceTexture = mTextureView.getSurfaceTexture();
        if(surfaceTexture != null) {
            surfaceTexture.setDefaultBufferSize(windowWidth, windowHeight);
            Tools.runOnUiThread(()->mCallback.onSurfaceResized());
        }
    }

    private class CallbackAdapter implements TextureView.SurfaceTextureListener {

        @Override
        public void onSurfaceTextureAvailable(@NonNull SurfaceTexture surfaceTexture, int i, int i1) {
            if(windowWidth != 0 && windowHeight != 0)
                surfaceTexture.setDefaultBufferSize(windowWidth, windowHeight);
            mCallback.onSurfaceAvailable(new Surface(surfaceTexture));
        }

        @Override
        public boolean onSurfaceTextureDestroyed(@NonNull SurfaceTexture surfaceTexture) {
            mCallback.onSurfaceDestroyed();
            return true;
        }

        @Override
        public void onSurfaceTextureSizeChanged(@NonNull SurfaceTexture surfaceTexture, int i, int i1) {
            mCallback.onSurfaceResized();
        }

        @Override
        public void onSurfaceTextureUpdated(@NonNull SurfaceTexture surfaceTexture) {

        }
    }
}
