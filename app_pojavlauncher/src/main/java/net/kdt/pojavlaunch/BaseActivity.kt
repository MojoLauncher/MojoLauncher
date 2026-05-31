@file:Suppress("MISSING_DEPENDENCY_SUPERCLASS_WARNING")

package net.kdt.pojavlaunch

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.RenderEffect
import android.graphics.Shader
import android.graphics.drawable.ColorDrawable
import android.os.Build
import android.os.Bundle
import android.util.TypedValue
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.res.ResourcesCompat
import net.ashmeet.hyperlauncher.R
import net.kdt.pojavlaunch.prefs.LauncherPreferences
import net.kdt.pojavlaunch.utils.LocaleUtils.Companion.setLocale
import java.io.File

abstract class BaseActivity : AppCompatActivity() {
    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(setLocale(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Apply insets mode AFTER super.onCreate to ensure android.R.id.content is available
        setLocale(this)
        Tools.setInsetsMode(this, setFullscreen(), ignoreNotch = shouldIgnoreNotch())

        Tools.getDisplayMetrics(this)
    }

    override fun setContentView(layoutResID: Int) {
        super.setContentView(layoutResID)
        applyCustomBackground()
    }

    override fun setContentView(view: View?) {
        super.setContentView(view)
        applyCustomBackground()
    }

    override fun setContentView(view: View?, params: ViewGroup.LayoutParams?) {
        super.setContentView(view, params)
        applyCustomBackground()
    }

    fun applyCustomBackground() {
        val bgView = findViewById<ImageView?>(R.id.activity_background)
        val bgOverlayView = findViewById<View?>(R.id.activity_background_overlay)
        
        val backgroundPath = LauncherPreferences.PREF_BACKGROUND_PATH
        if (backgroundPath != null) {
            val pathChanged = backgroundPath != sBackgroundPathCache
            var currentLastModified = 0L
            try {
                currentLastModified = File(backgroundPath).lastModified()
            } catch (ignored: Exception) {
            }
            
            val contentChanged = !pathChanged && sBackgroundPathCache == backgroundPath
                    && currentLastModified != 0L && currentLastModified != sBackgroundLastModifiedCache

            if (sBackgroundBitmapCache == null || pathChanged || contentChanged) {
                sBackgroundBitmapCache?.recycle()
                try {
                    sBackgroundBitmapCache = BitmapFactory.decodeFile(backgroundPath)
                    sBackgroundPathCache = backgroundPath
                    sBackgroundLastModifiedCache = currentLastModified
                } catch (e: Exception) {
                    sBackgroundBitmapCache = null
                }
            }

            val bitmap = sBackgroundBitmapCache
            if (bitmap != null && bgView != null) {
                if (bgView.visibility != View.VISIBLE || pathChanged || contentChanged) {
                    bgView.setImageBitmap(bitmap)
                    bgView.visibility = View.VISIBLE
                    bgOverlayView?.visibility = View.VISIBLE
                }
                
                if (bgOverlayView != null) {
                    val showOverlay = LauncherPreferences.PREF_BACKGROUND_IMAGE_OVERLAY_ENABLED
                            && LauncherPreferences.PREF_BACKGROUND_IMAGE_OVERLAY_ALPHA > 0f
                    bgOverlayView.visibility = if (showOverlay) View.VISIBLE else View.GONE
                    bgOverlayView.alpha = LauncherPreferences.PREF_BACKGROUND_IMAGE_OVERLAY_ALPHA
                }

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    if (LauncherPreferences.PREF_BACKGROUND_BLUR) {
                        val radius = LauncherPreferences.PREF_BACKGROUND_BLUR_INTENSITY.toFloat()
                        bgView.setRenderEffect(
                            RenderEffect.createBlurEffect(radius, radius, Shader.TileMode.CLAMP)
                        )
                    } else {
                        bgView.setRenderEffect(null)
                    }
                }

                window.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

                val root = findViewById<View?>(android.R.id.content)
                if (root is ViewGroup && root.childCount > 0) {
                    root.getChildAt(0).setBackground(null)
                }
                return
            }
        }

        // Default behavior if no background path or view missing
        if (bgView != null) {
            sBackgroundBitmapCache = null
            sBackgroundPathCache = null
            sBackgroundLastModifiedCache = 0L
            bgView.visibility = View.GONE
            bgView.setImageBitmap(null)
            bgOverlayView?.visibility = View.GONE
        }

        // Restore default background color
        val color = try {
            ResourcesCompat.getColor(resources, R.color.background_app, theme)
        } catch (e: Exception) {
            val typedValue = TypedValue()
            if (theme.resolveAttribute(com.google.android.material.R.attr.colorSurface, typedValue, true)) {
                typedValue.data
            } else {
                Color.BLACK
            }
        }

        window.setBackgroundDrawable(ColorDrawable(color))
        findViewById<View?>(android.R.id.content)?.setBackgroundColor(color)
    }

    open fun setFullscreen(): Boolean = true

    override fun startActivity(i: Intent?) {
        super.startActivity(i)
        applyPendingTransition(true)
    }

    override fun onResume() {
        super.onResume()
        Tools.checkStorageInteractive(this)
        applyCustomBackground()
    }

    override fun finish() {
        super.finish()
        applyPendingTransition(false)
    }

    protected fun applyPendingTransition(opening: Boolean) {
        val enterAnim: Int
        val exitAnim: Int
        
        when (LauncherPreferences.PREF_ANIMATION_TYPE) {
            "none" -> { enterAnim = 0; exitAnim = 0 }
            "jelly" -> { enterAnim = R.anim.jelly_in; exitAnim = R.anim.jelly_out }
            "slide" -> {
                if (opening) {
                    enterAnim = R.anim.slide_in_right
                    exitAnim = R.anim.slide_out_left
                } else {
                    enterAnim = R.anim.slide_in_left
                    exitAnim = R.anim.slide_out_right
                }
            }
            "default" -> { enterAnim = android.R.anim.fade_in; exitAnim = android.R.anim.fade_out }
            else -> return
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            overrideActivityTransition(
                if (opening) OVERRIDE_TRANSITION_OPEN else OVERRIDE_TRANSITION_CLOSE,
                enterAnim, exitAnim
            )
        } else {
            @Suppress("DEPRECATION")
            overridePendingTransition(enterAnim, exitAnim)
        }
    }

    override fun onPostResume() {
        super.onPostResume()
        Tools.getDisplayMetrics(this)
    }

    protected open fun shouldIgnoreNotch(): Boolean = LauncherPreferences.PREF_IGNORE_NOTCH

    companion object {
        @JvmStatic
        protected var sBackgroundBitmapCache: Bitmap? = null
        private var sBackgroundPathCache: String? = null
        private var sBackgroundLastModifiedCache = 0L

        @JvmStatic
        fun getBackgroundBitmap(): Bitmap? = sBackgroundBitmapCache
    }
}
