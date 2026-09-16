package com.example.ui.reader

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View

/**
 * İlmNet - Çok Renkli Fosforlu Kalem Çizim Katmanı (Faz 4).
 * Kullanıcı 'Kalem' modundayken PDF sayfaları üzerine Sarı, Mavi, Yeşil ve Kırmızı
 * renklerinde yarı saydam (alpha = 128 / 0x80) fosforlu vurgulama yapmasını sağlar.
 * Çizilen her çizgi kendi seçilen rengini muhafaza eder.
 */
class HighlighterDrawingView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    data class HighlightStroke(
        val path: Path,
        val color: Int,
        val strokeWidth: Float = 32f
    )

    private val strokes = mutableListOf<HighlightStroke>()
    private var currentPath: Path? = null

    companion object {
        val COLOR_YELLOW = 0x80FFE600.toInt() // Fosforlu Sarı (#80FFE600)
        val COLOR_BLUE = 0x802979FF.toInt()   // Fosforlu Mavi (#802979FF)
        val COLOR_GREEN = 0x8000E676.toInt()  // Fosforlu Yeşil (#8000E676)
        val COLOR_RED = 0x80FF5252.toInt()    // Fosforlu Kırmızı (#80FF5252)
    }

    var activeColor: Int = COLOR_YELLOW
        set(value) {
            field = value
            highlighterPaint.color = value
        }

    var isDrawingEnabled: Boolean = false
        set(value) {
            field = value
            invalidate()
        }

    private val highlighterPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = COLOR_YELLOW
        style = Paint.Style.STROKE
        strokeWidth = 32f
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        for (stroke in strokes) {
            highlighterPaint.color = stroke.color
            highlighterPaint.strokeWidth = stroke.strokeWidth
            canvas.drawPath(stroke.path, highlighterPaint)
        }
        currentPath?.let {
            highlighterPaint.color = activeColor
            highlighterPaint.strokeWidth = 32f
            canvas.drawPath(it, highlighterPaint)
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (!isDrawingEnabled) {
            return false // Dokunmayı üst bileşenlere devret (Okuma modu)
        }

        // 2 parmak ve üzeri ise pinch-zoom için bırak
        if (event.pointerCount >= 2) {
            currentPath?.let {
                strokes.add(HighlightStroke(it, activeColor))
            }
            currentPath = null
            invalidate()
            return false
        }

        val x = event.x
        val y = event.y

        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                parent?.requestDisallowInterceptTouchEvent(true)
                val path = Path()
                path.moveTo(x, y)
                currentPath = path
                invalidate()
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                currentPath?.lineTo(x, y)
                invalidate()
                return true
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                currentPath?.let {
                    strokes.add(HighlightStroke(it, activeColor))
                }
                currentPath = null
                parent?.requestDisallowInterceptTouchEvent(false)
                invalidate()
                return true
            }
        }
        return super.onTouchEvent(event)
    }

    fun clearDrawings() {
        strokes.clear()
        currentPath = null
        invalidate()
    }
}
