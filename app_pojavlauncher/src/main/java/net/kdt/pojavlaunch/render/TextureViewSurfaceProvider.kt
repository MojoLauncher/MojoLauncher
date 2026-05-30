package net.kdt.pojavlaunch.render

import android.content.Context
import android.graphics.SurfaceTexture
import android.view.Surface
import android.view.TextureView
import android.view.TextureView.SurfaceTextureListener
import net.kdt.pojavlaunch.render.SurfaceProvider.SurfaceCallback

class TextureViewSurfaceProvider : SurfaceProvider<TextureView?> {
    override fun create(context: Context?, callback: SurfaceCallback?): TextureView {
        val textureView = TextureView(requireNotNull(context))
        textureView.setOpaque(true)
        textureView.setAlpha(1.0f)
        textureView.setSurfaceTextureListener(CallbackAdapter(callback))
        return textureView
    }

    private class CallbackAdapter(private val mCallback: SurfaceCallback?) : SurfaceTextureListener {
        override fun onSurfaceTextureAvailable(surfaceTexture: SurfaceTexture, i: Int, i1: Int) {
            mCallback?.onSurfaceAvailable(Surface(surfaceTexture))
        }

        override fun onSurfaceTextureDestroyed(surfaceTexture: SurfaceTexture): Boolean {
            mCallback?.onSurfaceDestroyed()
            return true
        }

        override fun onSurfaceTextureSizeChanged(surfaceTexture: SurfaceTexture, i: Int, i1: Int) {
            mCallback?.onSurfaceResized()
        }

        override fun onSurfaceTextureUpdated(surfaceTexture: SurfaceTexture) {
        }
    }
}
