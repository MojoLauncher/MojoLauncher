package net.kdt.pojavlaunch.render

import android.content.Context
import android.view.Surface
import android.view.View

interface SurfaceProvider<T : View?> {
    fun create(context: Context?, callback: SurfaceCallback?): T?

    interface SurfaceCallback {
        fun onSurfaceAvailable(surface: Surface?)
        fun onSurfaceResized()
        fun onSurfaceDestroyed()
    }
}
