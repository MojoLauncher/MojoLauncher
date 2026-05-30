package net.kdt.pojavlaunch.utils

import android.opengl.EGL14
import android.opengl.EGLConfig
import android.opengl.EGLContext
import android.opengl.EGLDisplay
import android.opengl.EGLSurface
import android.opengl.GLES20
import android.opengl.GLES30
import android.util.Log
import kotlin.math.min

object GLInfoUtils {
    var GLES_VERSION_PREFIX: String = "OpenGL ES "
    private var info: GLInfo? = null

    private fun getMajorGLVersion(versionString: String): Int {
        var versionString = versionString
        if (versionString.startsWith(GLES_VERSION_PREFIX)) {
            versionString = versionString.substring(GLES_VERSION_PREFIX.length)
        }
        val firstDot = versionString.indexOf('.')
        val majorVersion = versionString.substring(0, firstDot).trim { it <= ' ' }
        return majorVersion.toInt()
    }

    private fun queryInfo(contextGLVersion: Int, forcedMsaa: Boolean): GLInfo {
        val vendor = GLES20.glGetString(GLES20.GL_VENDOR)
        val renderer = GLES20.glGetString(GLES20.GL_RENDERER)
        val versionString = GLES20.glGetString(GLES30.GL_VERSION)
        var version = 2
        try {
            version = getMajorGLVersion(versionString)
        } catch (e: NumberFormatException) {
            Log.w("GLInfoUtils", "Failed to parse GL version number, falling back to 2", e)
        }
        // LTW depends on the ability to create a context with a major version of 3,
        // and even if the string parse returns 3 while EGL can only create 2,
        // it's still a noncompilant implementation
        version = min(version, contextGLVersion)
        return GLInfo(vendor, renderer, version, forcedMsaa)
    }

    private fun initDummyInfo() {
        Log.e(
            "GLInfoUtils",
            "An error happened during info query. Will use dummy info. This should be investigated."
        )
        info = GLInfo("<Unknown>", "<Unknown>", 2, false)
    }

    private fun tryCreateContext(
        eglDisplay: EGLDisplay?,
        config: EGLConfig?,
        majorVersion: Int
    ): EGLContext? {
        val egl_context_attributes =
            intArrayOf(EGL14.EGL_CONTEXT_CLIENT_VERSION, majorVersion, EGL14.EGL_NONE)
        val context = EGL14.eglCreateContext(
            eglDisplay,
            config,
            EGL14.EGL_NO_CONTEXT,
            egl_context_attributes,
            0
        )
        if (context === EGL14.EGL_NO_CONTEXT || context == null) {
            Log.e("GLInfoUtils", "Failed to create a context with major version " + majorVersion)
            return null
        }
        return context
    }

    private fun tryMakeCurrent(
        eglDisplay: EGLDisplay?,
        config: EGLConfig?,
        surface: EGLSurface?,
        majorVersion: Int
    ): EGLContext? {
        val context = tryCreateContext(eglDisplay, config, majorVersion)
        if (context == null) return null
        // Old Mali drivers are broken, and will actually let us create a context with GLES 3
        // But won't let us make it current, which will break the check anyway...
        val makeCurrentResult = EGL14.eglMakeCurrent(eglDisplay, surface, surface, context)
        if (!makeCurrentResult) {
            Log.i("GLInfoUtils", "Failed to make context GL version " + majorVersion + " current")
            EGL14.eglDestroyContext(eglDisplay, context)
            return null
        }
        return context
    }

    private fun isMSAAConfig(eglDisplay: EGLDisplay?, eglConfig: EGLConfig?): Boolean {
        val sampleBuffers = intArrayOf(0)
        EGL14.eglGetConfigAttrib(eglDisplay, eglConfig, EGL14.EGL_SAMPLE_BUFFERS, sampleBuffers, 0)
        return sampleBuffers[0] != 0
    }

    private fun initAndQueryInfo(): Boolean {
        // This is here just to satisfy Android M which incorrectly null-checks it
        val egl_version = IntArray(2)
        val eglDisplay = EGL14.eglGetDisplay(EGL14.EGL_DEFAULT_DISPLAY)
        if (eglDisplay === EGL14.EGL_NO_DISPLAY || !EGL14.eglInitialize(
                eglDisplay,
                egl_version,
                0,
                egl_version,
                1
            )
        ) return false
        val egl_attributes = intArrayOf(
            EGL14.EGL_BLUE_SIZE, 8,
            EGL14.EGL_GREEN_SIZE, 8,
            EGL14.EGL_RED_SIZE, 8,
            EGL14.EGL_ALPHA_SIZE, 8,
            EGL14.EGL_DEPTH_SIZE, 24,
            EGL14.EGL_SURFACE_TYPE, EGL14.EGL_PBUFFER_BIT,
            EGL14.EGL_RENDERABLE_TYPE, EGL14.EGL_OPENGL_ES2_BIT,
            EGL14.EGL_NONE
        )
        val config = arrayOfNulls<EGLConfig>(1)
        val num_configs = intArrayOf(0)
        if (!EGL14.eglChooseConfig(
                eglDisplay,
                egl_attributes,
                0,
                config,
                0,
                1,
                num_configs,
                0
            ) || num_configs[0] == 0
        ) {
            EGL14.eglTerminate(eglDisplay)
            Log.e("GLInfoUtils", "Failed to choose an EGL config")
            return false
        }

        val forcedMsaa = isMSAAConfig(eglDisplay, config[0])

        // Create PBuffer surface as some devices might actually not support surfaceless.
        val pbuffer_attributes = intArrayOf(
            EGL14.EGL_WIDTH, 16,
            EGL14.EGL_HEIGHT, 16,
            EGL14.EGL_NONE
        )

        val surface = EGL14.eglCreatePbufferSurface(eglDisplay, config[0], pbuffer_attributes, 0)
        if (surface == null || surface === EGL14.EGL_NO_SURFACE) {
            Log.e("GLInfoUtils", "Failed to create pbuffer surface")
            EGL14.eglTerminate(eglDisplay)
            return false
        }

        var contextGLVersion = 3
        var context = tryMakeCurrent(eglDisplay, config[0], surface, contextGLVersion)
        if (context == null) {
            contextGLVersion = 2
            context = tryMakeCurrent(eglDisplay, config[0], surface, contextGLVersion)
        }

        // Creation/currenting failed in both cases
        if (context == null) {
            Log.e("GLInfoUtils", "Failed to create and make context current")
            EGL14.eglDestroySurface(eglDisplay, surface)
            EGL14.eglTerminate(eglDisplay)
            return false
        }

        info = queryInfo(contextGLVersion, forcedMsaa)

        EGL14.eglMakeCurrent(
            eglDisplay,
            EGL14.EGL_NO_SURFACE,
            EGL14.EGL_NO_SURFACE,
            EGL14.EGL_NO_CONTEXT
        )
        EGL14.eglDestroyContext(eglDisplay, context)
        EGL14.eglTerminate(eglDisplay)
        return true
    }

    @JvmStatic
    val glInfo: GLInfo?
        /**
         * Get the information about the current OpenGL ES device, which consists of the vendor,
         * the renderer and the major GLES version
         * @return the info
         */
        get() {
            if (info != null) return info
            Log.i("GLInfoUtils", "Querying graphics device info...")
            var infoQueryResult = false
            try {
                infoQueryResult = initAndQueryInfo()
            } catch (e: Throwable) {
                Log.e("GLInfoUtils", "Throwable when trying to initialize GL info", e)
            }
            if (!infoQueryResult) initDummyInfo()
            return info
        }

    class GLInfo(
        @JvmField val vendor: String,
        @JvmField val renderer: String,
        @JvmField val glesMajorVersion: Int,
        val forcedMsaa: Boolean
    ) {
        val isAdreno: Boolean
            /**
             * Check if this GLInfo belongs to a Qualcomm Adreno graphics adapter
             * @return
             */
            get() = renderer.contains("Adreno") && vendor == "Qualcomm"

        val isArm: Boolean
            /**
             * Check if this GLInfo belongs to a ARM Mali/Immortalis graphics adapter
             * @return
             */
            get() = (renderer.contains("Mali") || renderer.contains("Immortalis")) && vendor == "ARM"
    }
}
