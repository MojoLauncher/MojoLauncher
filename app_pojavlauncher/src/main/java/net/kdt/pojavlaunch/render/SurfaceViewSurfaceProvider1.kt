package net.kdt.pojavlaunch.render

import android.content.Context
import android.view.SurfaceHolder
import android.view.SurfaceView
import net.kdt.pojavlaunch.render.SurfaceProvider.SurfaceCallback

class SurfaceViewSurfaceProvider : SurfaceProvider<SurfaceView?> {
    override fun create(context: Context?, callback: SurfaceCallback?): SurfaceView {
        val surfaceView = SurfaceView(context)
        surfaceView.getHolder().addCallback(CallbackAdapter(callback))
        return surfaceView
    }

    private class CallbackAdapter(private val mCallback: SurfaceCallback?) : SurfaceHolder.Callback {
        override fun surfaceChanged(
            surfaceHolder: SurfaceHolder,
            fmt: Int,
            width: Int,
            height: Int
        ) {
            mCallback?.onSurfaceResized()
        }

        override fun surfaceCreated(surfaceHolder: SurfaceHolder) {
            mCallback?.onSurfaceAvailable(surfaceHolder.getSurface())
        }

        override fun surfaceDestroyed(surfaceHolder: SurfaceHolder) {
            mCallback?.onSurfaceDestroyed()
        }
    }
}
