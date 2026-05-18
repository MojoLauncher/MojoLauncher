package net.kdt.pojavlaunch.render;

import android.content.Context;
import android.graphics.SurfaceTexture;
import android.view.Surface;
import android.view.TextureView;

import androidx.annotation.NonNull;

public class TextureViewSurfaceProvider implements SurfaceProvider<TextureView> {
    @Override
    public TextureView create(Context context, SurfaceCallback callback) {
        TextureView textureView = new TextureView(context);
        textureView.setOpaque(true);
        textureView.setAlpha(1.0f);
        textureView.setSurfaceTextureListener(new CallbackAdapter(callback));
        return textureView;
    }
    private static class CallbackAdapter implements TextureView.SurfaceTextureListener {
        private final SurfaceCallback mCallback;

        private CallbackAdapter(SurfaceCallback mCallback) {
            this.mCallback = mCallback;
        }

        @Override
        public void onSurfaceTextureAvailable(@NonNull SurfaceTexture surfaceTexture, int i, int i1) {
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
