package com.khadka.limitr.ui.home.apps

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.*
import android.provider.Settings
import android.util.AttributeSet
import android.view.View
import android.view.animation.LinearInterpolator
import androidx.core.graphics.withSave

/** One inexpensive shimmer matching item_app's icon, title, duration, bar and percentage. */
class AppListSkeleton @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null) : View(context, attrs) {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val matrix = Matrix()
    private var gradient: LinearGradient? = null
    private var phase = 0f
    private var active = false
    private var animator: ValueAnimator? = null
    private val dp = resources.displayMetrics.density

    fun setActive(value: Boolean) { active = value; updateAnimation() }
    override fun onAttachedToWindow() { super.onAttachedToWindow(); updateAnimation() }
    override fun onDetachedFromWindow() { animator?.cancel(); animator = null; super.onDetachedFromWindow() }
    override fun onVisibilityChanged(changedView: View, visibility: Int) {
        super.onVisibilityChanged(changedView, visibility)
        updateAnimation()
    }
    override fun onWindowVisibilityChanged(visibility: Int) { super.onWindowVisibilityChanged(visibility); updateAnimation() }
    private fun updateAnimation() {
        val animate = active && isAttachedToWindow && isShown && windowVisibility == VISIBLE &&
            Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) > 0f
        if (animate && animator == null) {
            animator = ValueAnimator.ofFloat(0f, 1f).apply {
                duration = 1400
                repeatCount = ValueAnimator.INFINITE
                interpolator = LinearInterpolator()
                addUpdateListener { phase = it.animatedValue as Float; invalidate() }
                start()
            }
        } else if (!animate) { animator?.cancel(); animator = null }
    }
    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        gradient = LinearGradient(0f, 0f, w.coerceAtLeast(1).toFloat(), 0f,
            intArrayOf(0xff35354b.toInt(), 0xff565671.toInt(), 0xff35354b.toInt()),
            floatArrayOf(0f, .5f, 1f), Shader.TileMode.CLAMP)
        paint.shader = gradient
    }
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        matrix.setTranslate((phase * 3 - 1) * width, 0f)
        gradient?.setLocalMatrix(matrix)
        val rowHeight = (40f + 40f * resources.configuration.fontScale.coerceAtLeast(1f)) * dp
        val end = width - 36 * dp
        fun block(left: Float, top: Float, right: Float, bottom: Float, radius: Float = 4 * dp) {
            if (right > left) canvas.drawRoundRect(left, top, right, bottom, radius, radius, paint)
        }
        canvas.withSave {
            if (layoutDirection == LAYOUT_DIRECTION_RTL) { canvas.translate(width.toFloat(), 0f); canvas.scale(-1f, 1f) }
            var y = 36 * dp
            while (y < height) {
                block(36 * dp, y, 76 * dp, y + 40 * dp, 8 * dp)
                val start = 86 * dp
                val span = (end - start).coerceAtLeast(0f)
                block(start, y + 2 * dp, start + span * .52f, y + 14 * dp)
                block(start + span * .72f, y + 2 * dp, end, y + 14 * dp)
                block(start, y + 30 * dp, start + span * .76f, y + 38 * dp)
                block(start + span * .84f, y + 27 * dp, end, y + 39 * dp)
                y += rowHeight
            }
        }
    }
}
