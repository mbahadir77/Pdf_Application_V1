package com.example.ui.reader

import android.animation.ValueAnimator
import android.content.Context
import android.util.AttributeSet
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.animation.DecelerateInterpolator
import android.widget.FrameLayout

/**
 * İlmNet - Yakınlaştırılabilir PDF Sayfa Kapsayıcısı (Pinch-to-Zoom & Pan).
 * PDF sayfası bitmap'i ile şeffaf fosforlu çizim katmanını (HighlighterDrawingView)
 * senkronize biçimde yakınlaştırır, uzaklaştırır ve kaydırır.
 *
 * - 2 Parmakla Kıstırma (Pinch): 1.0x ile 3.5x arası akıcı yakınlaştırma.
 * - Çift Dokunma: 1.0x ile 2.0x arasında akıllı yakınlaştırma / sıfırlama animasyonu.
 * - Kalem modunda: Tek parmak çizim yapar, iki parmak serbestçe yakınlaştırır ve kaydırır.
 * - Okuma modunda: Sayfa 1.0x iken normal dikey kaydırma, yakınlaştırılmışken iki eksenli serbest gezinme.
 */
class ZoomablePageLayout @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    private var scaleFactor = 1.0f
    private val minScale = 1.0f
    private val maxScale = 3.5f

    private var lastTouchX = 0f
    private var lastTouchY = 0f
    private var isDragging = false

    var isDrawingMode: Boolean = false

    private val scaleDetector = ScaleGestureDetector(context, object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
        override fun onScale(detector: ScaleGestureDetector): Boolean {
            val previousScale = scaleFactor
            scaleFactor = (scaleFactor * detector.scaleFactor).coerceIn(minScale, maxScale)

            if (scaleFactor > 1.05f) {
                parent?.requestDisallowInterceptTouchEvent(true)
            }

            // Odak noktasına göre orantılı yakınlaştır
            val focusX = detector.focusX
            val focusY = detector.focusY

            pivotX = focusX
            pivotY = focusY

            scaleX = scaleFactor
            scaleY = scaleFactor

            constrainTranslation()
            return true
        }
    })

    private val gestureDetector = GestureDetector(context, object : GestureDetector.SimpleOnGestureListener() {
        override fun onDoubleTap(e: MotionEvent): Boolean {
            val targetScale = if (scaleFactor > 1.2f) 1.0f else 2.0f
            animateToScale(targetScale, e.x, e.y)
            return true
        }
    })

    override fun onInterceptTouchEvent(ev: MotionEvent): Boolean {
        // Akıllı kalem (S-Pen / Stylus) teması varsa sayfa kaydırma / sürüklemeyi kapat, çizim katmanına devret
        val toolType = if (ev.pointerCount > 0) ev.getToolType(0) else MotionEvent.TOOL_TYPE_UNKNOWN
        if (toolType == MotionEvent.TOOL_TYPE_STYLUS || toolType == MotionEvent.TOOL_TYPE_ERASER) {
            parent?.requestDisallowInterceptTouchEvent(true)
            return false
        }

        // İki veya daha fazla parmak varsa her zaman pinch-zoom yakala
        if (ev.pointerCount >= 2) {
            parent?.requestDisallowInterceptTouchEvent(true)
            return true
        }

        // Okuma modunda ve yakınlaştırılmışsa tek parmak kaydırmayı da yakala
        if (!isDrawingMode && scaleFactor > 1.05f) {
            when (ev.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    lastTouchX = ev.x
                    lastTouchY = ev.y
                    isDragging = false
                    parent?.requestDisallowInterceptTouchEvent(true)
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = Math.abs(ev.x - lastTouchX)
                    val dy = Math.abs(ev.y - lastTouchY)
                    if (dx > 8f || dy > 8f) {
                        isDragging = true
                        parent?.requestDisallowInterceptTouchEvent(true)
                        return true
                    }
                }
            }
        }

        return super.onInterceptTouchEvent(ev)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        gestureDetector.onTouchEvent(event)
        scaleDetector.onTouchEvent(event)

        val action = event.actionMasked
        when (action) {
            MotionEvent.ACTION_DOWN -> {
                lastTouchX = event.x
                lastTouchY = event.y
                if (scaleFactor > 1.05f) {
                    parent?.requestDisallowInterceptTouchEvent(true)
                }
            }
            MotionEvent.ACTION_MOVE -> {
                if (event.pointerCount >= 2) {
                    parent?.requestDisallowInterceptTouchEvent(true)
                } else if (!isDrawingMode && scaleFactor > 1.05f) {
                    val dx = event.x - lastTouchX
                    val dy = event.y - lastTouchY

                    translationX += dx
                    translationY += dy
                    constrainTranslation()

                    lastTouchX = event.x
                    lastTouchY = event.y
                    parent?.requestDisallowInterceptTouchEvent(true)
                }
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                if (scaleFactor <= 1.05f) {
                    resetZoomSmoothly()
                } else {
                    constrainTranslation()
                }
            }
        }

        return true
    }

    private fun constrainTranslation() {
        if (scaleFactor <= 1.0f) {
            translationX = 0f
            translationY = 0f
            return
        }

        val maxTransX = (width * (scaleFactor - 1f)) / 2f
        val maxTransY = (height * (scaleFactor - 1f)) / 2f

        translationX = translationX.coerceIn(-maxTransX, maxTransX)
        translationY = translationY.coerceIn(-maxTransY, maxTransY)
    }

    private fun animateToScale(targetScale: Float, focusX: Float, focusY: Float) {
        val startScale = scaleFactor
        pivotX = focusX
        pivotY = focusY

        val animator = ValueAnimator.ofFloat(startScale, targetScale).apply {
            duration = 260
            interpolator = DecelerateInterpolator()
            addUpdateListener { animation ->
                val value = animation.animatedValue as Float
                scaleFactor = value
                scaleX = value
                scaleY = value
                if (value <= 1.0f) {
                    translationX = 0f
                    translationY = 0f
                } else {
                    constrainTranslation()
                }
            }
        }
        animator.start()
    }

    fun resetZoomSmoothly() {
        if (scaleFactor == 1.0f && translationX == 0f && translationY == 0f) return

        val startScale = scaleFactor
        val startTransX = translationX
        val startTransY = translationY

        val animator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 200
            interpolator = DecelerateInterpolator()
            addUpdateListener { animation ->
                val fraction = animation.animatedFraction
                scaleFactor = startScale + (1.0f - startScale) * fraction
                scaleX = scaleFactor
                scaleY = scaleFactor
                translationX = startTransX * (1f - fraction)
                translationY = startTransY * (1f - fraction)
            }
        }
        animator.start()
    }
}
