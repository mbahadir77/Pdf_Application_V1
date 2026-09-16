package com.example.ui.common

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.util.AttributeSet
import android.view.View
import android.view.animation.LinearInterpolator
import kotlin.random.Random

/**
 * İlmNet - Coşkulu Konfeti & Tebrik Kutlama Animasyonu (Faz 5).
 * Yeni akademik rozet (Bakır, Gümüş, Altın, Elmas) kazanıldığında ekranı kaplayan parçacık patlaması.
 */
class CelebrationConfettiView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private data class Particle(
        var x: Float,
        var y: Float,
        var vx: Float,
        var vy: Float,
        var size: Float,
        var color: Int,
        var rotation: Float,
        var rotationSpeed: Float,
        var alpha: Int,
        var isCircle: Boolean
    )

    private val particles = mutableListOf<Particle>()
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private var animator: ValueAnimator? = null

    private val confettiColors = intArrayOf(
        Color.parseColor("#FFD700"), // Parlak Altın
        Color.parseColor("#00E5FF"), // Elmas Mavi Kristal
        Color.parseColor("#FFFFFF"), // Kristal Beyaz
        Color.parseColor("#CD7F32"), // Bakır
        Color.parseColor("#E0E6ED"), // Metalik Gümüş
        Color.parseColor("#10B981"), // Zümrüt Yeşili
        Color.parseColor("#F59E0B")  // Sıcak Kehribar
    )

    fun startCelebration() {
        vibratePhone()
        particles.clear()
        val width = if (getWidth() > 0) getWidth().toFloat() else 1080f
        val height = if (getHeight() > 0) getHeight().toFloat() else 1920f

        // 140 Parçacık üret
        for (i in 0 until 140) {
            val color = confettiColors[Random.nextInt(confettiColors.size)]
            val p = Particle(
                x = width * 0.5f + (Random.nextFloat() - 0.5f) * 160f,
                y = height * 0.4f + (Random.nextFloat() - 0.5f) * 160f,
                vx = (Random.nextFloat() - 0.5f) * 28f,
                vy = -(Random.nextFloat() * 22f + 10f), // Yukarı fırlatma
                size = Random.nextFloat() * 16f + 10f,
                color = color,
                rotation = Random.nextFloat() * 360f,
                rotationSpeed = (Random.nextFloat() - 0.5f) * 18f,
                alpha = 255,
                isCircle = Random.nextBoolean()
            )
            particles.add(p)
        }

        visibility = VISIBLE
        animator?.cancel()
        animator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 3200
            interpolator = LinearInterpolator()
            addUpdateListener { anim ->
                val progress = anim.animatedFraction
                val gravity = 0.65f

                particles.forEach { p ->
                    p.x += p.vx
                    p.y += p.vy
                    p.vy += gravity
                    p.rotation += p.rotationSpeed
                    // Yavaşça şeffaflaşma
                    p.alpha = ((1f - progress * progress) * 255).toInt().coerceIn(0, 255)
                }
                invalidate()
            }
            start()
        }
    }

    private fun vibratePhone() {
        try {
            val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            if (vibrator != null && vibrator.hasVibrator()) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 80, 60, 120), -1))
                } else {
                    @Suppress("DEPRECATION")
                    vibrator.vibrate(200)
                }
            }
        } catch (_: Exception) {}
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (particles.isEmpty()) return

        particles.forEach { p ->
            if (p.alpha > 0) {
                paint.color = p.color
                paint.alpha = p.alpha
                canvas.save()
                canvas.translate(p.x, p.y)
                canvas.rotate(p.rotation)
                if (p.isCircle) {
                    canvas.drawCircle(0f, 0f, p.size / 2f, paint)
                } else {
                    val half = p.size / 2f
                    canvas.drawRoundRect(
                        RectF(-half, -half * 0.6f, half, half * 0.6f),
                        4f, 4f, paint
                    )
                }
                canvas.restore()
            }
        }
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        animator?.cancel()
    }
}
