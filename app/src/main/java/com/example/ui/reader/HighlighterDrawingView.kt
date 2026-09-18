package com.example.ui.reader

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Rect
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import java.io.File
import java.io.FileOutputStream

/**
 * İlim Diyârı - Çok Renkli Fosforlu Kalem, S-Pen Akıllı Kalem ve Mürekkep Kalıcılığı (FAZ 12 & 13).
 * - Donanım Sensör Ayrımı (Hardware Tool Type Distinction):
 *   * Parmak (TOOL_TYPE_FINGER): Okuma modunda sayfa serbestçe kaydırılır / swipe yapılır.
 *   * Akıllı Kalem (TOOL_TYPE_STYLUS): PDF kaydırma anında durdurulur ve kırmızı mürekkeple
 *     (Color.RED, Stroke) hassas not/çizgi alma katmanı devreye girer.
 * - Mürekkep Kalıcılığı (Ink Persistence):
 *   * Çizimleri Bitmap olarak dışa aktarma (exportToBitmap).
 *   * Disk üzerine sayfa bazlı PNG kaydetme (context.filesDir/annotations/annotation_postId_pX.png).
 *   * Sayfa yeniden açıldığında kaydedilen PNG'yi otomatik olarak 'overlay' olarak çizme.
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
    private var isCurrentStrokeStylus: Boolean = false

    var overlayBitmap: Bitmap? = null
        private set

    var currentPostId: String? = null
    var currentPageNumber: Int = 1

    var onDrawingChangedListener: (() -> Unit)? = null

    companion object {
        val COLOR_YELLOW = 0x80FFE600.toInt() // Fosforlu Sarı (#80FFE600)
        val COLOR_BLUE = 0x802979FF.toInt()   // Fosforlu Mavi (#802979FF)
        val COLOR_GREEN = 0x8000E676.toInt()  // Fosforlu Yeşil (#8000E676)
        val COLOR_RED = 0x80FF5252.toInt()    // Fosforlu Kırmızı (#80FF5252)

        val STYLUS_INK_RED = Color.RED        // S-Pen Kırmızı Mürekkebi (Opaque Red)
        const val STYLUS_STROKE_WIDTH = 7f    // Akıllı Kalem İnce Uçlu Mürekkep Çizgisi
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

        // 1. Önceki oturumdan geri yüklenen kalıcı çizim katmanı (Restore Overlay)
        overlayBitmap?.let { bitmap ->
            if (!bitmap.isRecycled) {
                val destRect = Rect(0, 0, width, height)
                canvas.drawBitmap(bitmap, null, destRect, null)
            }
        }

        // 2. Mevcut oturumdaki kaydedilmemiş vuruşlar (Strokes)
        for (stroke in strokes) {
            highlighterPaint.color = stroke.color
            highlighterPaint.strokeWidth = stroke.strokeWidth
            canvas.drawPath(stroke.path, highlighterPaint)
        }

        // 3. Şu anda ekranda devam eden çizim
        currentPath?.let {
            if (isCurrentStrokeStylus) {
                highlighterPaint.color = STYLUS_INK_RED
                highlighterPaint.strokeWidth = STYLUS_STROKE_WIDTH
            } else {
                highlighterPaint.color = activeColor
                highlighterPaint.strokeWidth = 32f
            }
            canvas.drawPath(it, highlighterPaint)
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        val toolType = if (event.pointerCount > 0) event.getToolType(0) else MotionEvent.TOOL_TYPE_UNKNOWN
        val isStylus = (toolType == MotionEvent.TOOL_TYPE_STYLUS || toolType == MotionEvent.TOOL_TYPE_ERASER)

        // Kural 1: Eğer parmak ise (TOOL_TYPE_FINGER) ve çizim modu açık değilse,
        // dokunmayı üst bileşenlere devret (Sayfa serbestçe kaydırılsın / Scroll / Swipe)
        if (!isStylus && !isDrawingEnabled) {
            return false
        }

        // 2 parmak ve üzeri parmak teması varsa yakınlaştırma (pinch-zoom) için bırak
        if (!isStylus && event.pointerCount >= 2) {
            currentPath?.let {
                strokes.add(HighlightStroke(it, activeColor, 32f))
            }
            currentPath = null
            invalidate()
            return false
        }

        val x = event.x
        val y = event.y

        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                // Kural 2: Akıllı kalem temas ettiğinde (TOOL_TYPE_STYLUS), PDF kaydırmayı durdur!
                parent?.requestDisallowInterceptTouchEvent(true)
                isCurrentStrokeStylus = isStylus
                val path = Path()
                path.moveTo(x, y)
                currentPath = path
                invalidate()
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                parent?.requestDisallowInterceptTouchEvent(true)
                currentPath?.lineTo(x, y)
                invalidate()
                return true
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                currentPath?.let {
                    val color = if (isCurrentStrokeStylus) STYLUS_INK_RED else activeColor
                    val width = if (isCurrentStrokeStylus) STYLUS_STROKE_WIDTH else 32f
                    strokes.add(HighlightStroke(it, color, width))
                }
                currentPath = null
                isCurrentStrokeStylus = false
                parent?.requestDisallowInterceptTouchEvent(false)
                invalidate()
                onDrawingChangedListener?.invoke()
                return true
            }
        }
        return super.onTouchEvent(event)
    }

    /**
     * Daha önce kaydedilmiş çizim overlay'ini diskteki dosya yolundan yükler.
     */
    fun loadOverlayFromPath(filePath: String?) {
        if (filePath.isNullOrBlank()) {
            overlayBitmap = null
            invalidate()
            return
        }
        val file = File(filePath)
        if (file.exists() && file.length() > 0) {
            try {
                overlayBitmap = BitmapFactory.decodeFile(file.absolutePath)
                invalidate()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        } else {
            overlayBitmap = null
            invalidate()
        }
    }

    fun loadOverlayBitmap(bitmap: Bitmap?) {
        overlayBitmap = bitmap
        invalidate()
    }

    /**
     * Kanvastaki tüm çizimleri ve overlay'i şeffaf bir Bitmap olarak dışa aktarır.
     */
    fun exportToBitmap(): Bitmap? {
        val w = if (width > 0) width else layoutParams?.width ?: 0
        val h = if (height > 0) height else layoutParams?.height ?: 0
        if (w <= 0 || h <= 0) return null

        val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        overlayBitmap?.let {
            if (!it.isRecycled) {
                canvas.drawBitmap(it, null, Rect(0, 0, w, h), null)
            }
        }
        for (stroke in strokes) {
            highlighterPaint.color = stroke.color
            highlighterPaint.strokeWidth = stroke.strokeWidth
            canvas.drawPath(stroke.path, highlighterPaint)
        }
        return bitmap
    }

    /**
     * Çizimleri context.filesDir/annotations/annotation_{postId}_p{pageNumber}.png olarak kaydeder.
     */
    fun saveDrawingToDisk(postId: String, pageNumber: Int): File? {
        if (strokes.isEmpty() && (overlayBitmap == null || overlayBitmap?.isRecycled == true)) {
            return null
        }
        val bitmap = exportToBitmap() ?: return null
        return try {
            val annotationsDir = File(context.filesDir, "annotations").apply {
                if (!exists()) mkdirs()
            }
            val sanitizedPostId = postId.replace("[^a-zA-Z0-9_]".toRegex(), "_")
            val targetFile = File(annotationsDir, "annotation_${sanitizedPostId}_p${pageNumber}.png")
            FileOutputStream(targetFile).use { out ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
                out.flush()
            }
            overlayBitmap = bitmap
            strokes.clear()
            targetFile
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun hasDrawings(): Boolean = strokes.isNotEmpty() || (overlayBitmap != null && overlayBitmap?.isRecycled == false)

    fun clearDrawings() {
        strokes.clear()
        currentPath = null
        overlayBitmap = null
        invalidate()
        onDrawingChangedListener?.invoke()
    }
}
