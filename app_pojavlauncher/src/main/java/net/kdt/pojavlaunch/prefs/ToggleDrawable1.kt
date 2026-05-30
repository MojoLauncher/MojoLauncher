package net.kdt.pojavlaunch.prefs

import android.animation.ValueAnimator
import android.animation.ValueAnimator.AnimatorUpdateListener
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorFilter
import android.graphics.Paint
import android.graphics.PixelFormat
import android.graphics.RectF
import android.graphics.drawable.Drawable
import android.view.animation.OvershootInterpolator
import androidx.core.content.ContextCompat
import net.ashmeet.hyperlauncher.R
import kotlin.math.max
import kotlin.math.min

/**
 * Fully custom toggle drawable.
 * - Sliding knob between left (OFF) and right (ON)
 * - Tick mark (✓) on knob when ON, cross (✕) when OFF
 * - Smooth color and position animation
 */
class ToggleDrawable(context: Context, initial: Boolean) : Drawable() {
    private val trackOn: Int
    private val trackOff: Int
    private val knobOn: Int
    private val knobOff: Int
    private val iconOn: Int
    private val iconOff: Int

    private val trackPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val knobPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val iconPaint = Paint(Paint.ANTI_ALIAS_FLAG)

    private val trackRect = RectF()
    private val knobRect = RectF()

    private var progress = 0f
    private var checked = false

    private var animator: ValueAnimator? = null

    init {
        checked = initial
        progress = if (initial) 1f else 0f

        trackOn = ContextCompat.getColor(context, R.color.switch_track_on)
        trackOff = ContextCompat.getColor(context, R.color.switch_track_off)
        knobOn = ContextCompat.getColor(context, R.color.switch_knob_on)
        knobOff = ContextCompat.getColor(context, R.color.switch_knob_off)
        iconOn = ContextCompat.getColor(context, R.color.switch_icon_on)
        iconOff = ContextCompat.getColor(context, R.color.switch_icon_off)

        trackPaint.setStyle(Paint.Style.FILL)
        knobPaint.setStyle(Paint.Style.FILL)

        iconPaint.setStyle(Paint.Style.STROKE)
        iconPaint.setStrokeCap(Paint.Cap.ROUND)
        iconPaint.setStrokeJoin(Paint.Join.ROUND)
    }

    fun setChecked(value: Boolean, animate: Boolean) {
        if (checked == value && animator == null) return
        checked = value

        if (animator != null) animator!!.cancel()

        val start = progress
        val end = if (value) 1f else 0f

        if (!animate) {
            progress = end
            invalidateSelf()
            return
        }

        animator = ValueAnimator.ofFloat(start, end)
        animator!!.setDuration(240)
        animator!!.setInterpolator(OvershootInterpolator(1.15f))

        animator!!.addUpdateListener(AnimatorUpdateListener { a: ValueAnimator? ->
            progress = a!!.getAnimatedValue() as Float
            invalidateSelf()
        })

        animator!!.start()
    }

    override fun draw(canvas: Canvas) {
        val w = getBounds().width()
        val h = getBounds().height()

        // Track (pill)
        val trackHeight = h * 0.55f
        val top = (h - trackHeight) / 2f
        val left = w * 0.04f
        val right = w * 0.96f

        trackRect.set(left, top, right, top + trackHeight)

        trackPaint.setColor(interpolate(trackOff, trackOn, progress))
        canvas.drawRoundRect(trackRect, trackHeight / 2f, trackHeight / 2f, trackPaint)

        // Knob with consistent gap to track (top/bottom + left/right)
        val knobRadius = trackHeight * 0.38f
        val gap = max(2f, (trackHeight / 2f) - knobRadius)

        val startX = left + knobRadius + gap
        val endX = right - knobRadius - gap

        val cx = startX + (endX - startX) * progress
        val cy = h / 2f

        knobRect.set(cx - knobRadius, cy - knobRadius, cx + knobRadius, cy + knobRadius)
        knobPaint.setColor(interpolate(knobOff, knobOn, progress))
        canvas.drawOval(knobRect, knobPaint)

        // Icon bounce + fade
        iconPaint.setStrokeWidth(knobRadius * 0.18f)

        val fade = (if (progress > 0.5f) progress else 1f - progress)
        // Clamp fade to 1.0 to avoid alpha overflow/flicker
        val clampedFade = max(0f, min(1f, fade))
        val alpha = (255f * clampedFade).toInt()

        iconPaint.setColor(if (progress > 0.5f) iconOn else iconOff)
        iconPaint.setAlpha(alpha)

        val s = knobRadius * 0.45f
        val scale = 0.85f + (0.25f * clampedFade)


        // Icon rotation: 360 degrees over the full progress
        val rotation = progress * 360f

        canvas.save()
        canvas.scale(scale, scale, cx, cy)
        canvas.rotate(rotation, cx, cy)

        if (progress > 0.5f) {
            drawTick(canvas, cx, cy, s)
        } else {
            drawCross(canvas, cx, cy, s)
        }

        canvas.restore()
        iconPaint.setAlpha(255)
    }

    private fun drawTick(canvas: Canvas, cx: Float, cy: Float, size: Float) {
        val s = size

        val x1 = cx - s * 0.6f
        val y1 = cy + s * 0.1f

        val x2 = cx - s * 0.1f
        val y2 = cy + s * 0.6f

        val x3 = cx + s * 0.7f
        val y3 = cy - s * 0.6f

        iconPaint.setStrokeWidth(size * 0.22f)

        canvas.drawLine(x1, y1, x2, y2, iconPaint)
        canvas.drawLine(x2, y2, x3, y3, iconPaint)
    }

    private fun drawCross(canvas: Canvas, cx: Float, cy: Float, size: Float) {
        val s = size * 0.85f

        iconPaint.setStrokeWidth(size * 0.22f)

        canvas.drawLine(cx - s, cy - s, cx + s, cy + s, iconPaint)
        canvas.drawLine(cx + s, cy - s, cx - s, cy + s, iconPaint)
    }

    private fun interpolate(a: Int, b: Int, t: Float): Int {
        // Clamp t to [0, 1] to prevent Color.rgb from overflowing and causing flickers
        // during animation overshoot.
        val clampedT = max(0f, min(1f, t))
        return Color.rgb(
            (Color.red(a) + (Color.red(b) - Color.red(a)) * clampedT).toInt(),
            (Color.green(a) + (Color.green(b) - Color.green(a)) * clampedT).toInt(),
            (Color.blue(a) + (Color.blue(b) - Color.blue(a)) * clampedT).toInt()
        )
    }

    override fun setAlpha(alpha: Int) {}
    override fun setColorFilter(colorFilter: ColorFilter?) {}
    override fun getOpacity(): Int {
        return PixelFormat.TRANSLUCENT
    }
}
