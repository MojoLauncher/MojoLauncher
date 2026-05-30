package net.kdt.pojavlaunch.customcontrols.handleview

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import androidx.vectordrawable.graphics.drawable.VectorDrawableCompat
import net.ashmeet.hyperlauncher.R
import net.kdt.pojavlaunch.prefs.LauncherPreferences

class DrawerPullButton : View {
    constructor(context: Context?) : super(context) {
        init()
    }

    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs) {
        init()
    }

    private val mBackgroundPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val mOutlinePaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val mBitmapPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private var mDefaultDrawable: VectorDrawableCompat? = null
    private var mCustomBitmap: Bitmap? = null
    private val mSrcRect = Rect()
    private val mDestRect = RectF()

    private fun init() {
        mDefaultDrawable = VectorDrawableCompat.create(
            getContext().getResources(),
            R.drawable.ic_sharp_settings_24,
            null
        )
        mBackgroundPaint.setColor(Color.BLACK)
        mOutlinePaint.setStyle(Paint.Style.STROKE)
        mOutlinePaint.setStrokeWidth(getResources().getDisplayMetrics().density * 2f)
        updateCustomImage()
    }

    fun updateCustomImage() {
        if (LauncherPreferences.PREF_DRAWER_BUTTON_IMAGE_PATH != null) {
            mCustomBitmap =
                BitmapFactory.decodeFile(LauncherPreferences.PREF_DRAWER_BUTTON_IMAGE_PATH)
        } else {
            mCustomBitmap = null
        }
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        // Draw a rounded square background
        val radius =
            LauncherPreferences.PREF_DRAWER_BUTTON_CORNER_RADIUS * getResources().getDisplayMetrics().density
        mDestRect.set(
            getPaddingLeft().toFloat(),
            getPaddingTop().toFloat(),
            (getWidth() - getPaddingRight()).toFloat(),
            (getHeight() - getPaddingBottom()).toFloat()
        )

        mBackgroundPaint.setAlpha((LauncherPreferences.PREF_DRAWER_BUTTON_BG_OPACITY * 255).toInt())
        canvas.drawRoundRect(mDestRect, radius, radius, mBackgroundPaint)

        // Draw icon outline (around the background or the icon container)
        if (LauncherPreferences.PREF_DRAWER_BUTTON_STROKE_ENABLED) {
            mOutlinePaint.setColor(LauncherPreferences.PREF_ICON_OUTLINE_COLOR)
            mOutlinePaint.setAlpha((LauncherPreferences.PREF_DRAWER_BUTTON_OPACITY * 255).toInt())
            canvas.drawRoundRect(mDestRect, radius, radius, mOutlinePaint)
        }

        val iconAlpha = (LauncherPreferences.PREF_DRAWER_BUTTON_ICON_OPACITY * 255).toInt()

        if (mCustomBitmap != null) {
            mSrcRect.set(0, 0, mCustomBitmap!!.getWidth(), mCustomBitmap!!.getHeight())
            // Center bitmap inside the rounded rect
            val innerPadding = 4f * getResources().getDisplayMetrics().density
            mDestRect.inset(innerPadding, innerPadding)
            mBitmapPaint.setAlpha(iconAlpha)
            canvas.drawBitmap(mCustomBitmap!!, mSrcRect, mDestRect, mBitmapPaint)
        } else {
            val iconPadding = (4f * getResources().getDisplayMetrics().density).toInt()
            mDefaultDrawable!!.setBounds(
                (getPaddingLeft() + iconPadding),
                (getPaddingTop() + iconPadding),
                (getWidth() - getPaddingRight() - iconPadding),
                (getHeight() - getPaddingBottom() - iconPadding)
            )
            mDefaultDrawable!!.setAlpha(iconAlpha)
            mDefaultDrawable!!.draw(canvas)
        }
    }
}
