package com.example.ui.reader

import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.databinding.ItemPdfPageBinding
import java.io.File

/**
 * İlim Diyârı - Yerleşik PDF Sayfa Adaptörü ve S-Pen Mürekkep Katmanı (FAZ 12 & 13).
 * android.graphics.pdf.PdfRenderer ile PDF sayfalarını işler ve HighlighterDrawingView
 * üzerinden sayfa bazlı el yazısı notları PNG overlay olarak kalıcı kılar.
 */
class PdfPageAdapter(
    private val pdfFile: File,
    private val postId: String? = null,
    private val onAnnotationSaved: ((pageIndex: Int, filePath: String) -> Unit)? = null
) : RecyclerView.Adapter<PdfPageAdapter.PdfPageViewHolder>() {

    private var fileDescriptor: ParcelFileDescriptor? = null
    private var pdfRenderer: PdfRenderer? = null
    private val pageBitmaps = mutableMapOf<Int, Bitmap>()
    private val activeHighlighters = mutableListOf<HighlighterDrawingView>()
    private val activeZoomContainers = mutableListOf<ZoomablePageLayout>()

    var activeColor: Int = HighlighterDrawingView.COLOR_YELLOW
        set(value) {
            field = value
            activeHighlighters.forEach { it.activeColor = value }
        }

    var isDrawingMode: Boolean = false
        set(value) {
            field = value
            activeHighlighters.forEach { it.isDrawingEnabled = value }
            activeZoomContainers.forEach { it.isDrawingMode = value }
        }

    init {
        try {
            fileDescriptor = ParcelFileDescriptor.open(pdfFile, ParcelFileDescriptor.MODE_READ_ONLY)
            fileDescriptor?.let {
                pdfRenderer = PdfRenderer(it)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun getItemCount(): Int {
        return pdfRenderer?.pageCount ?: 0
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PdfPageViewHolder {
        val binding = ItemPdfPageBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return PdfPageViewHolder(binding)
    }

    override fun onBindViewHolder(holder: PdfPageViewHolder, position: Int) {
        holder.bind(position)
    }

    override fun onViewAttachedToWindow(holder: PdfPageViewHolder) {
        super.onViewAttachedToWindow(holder)
        val highlighter = holder.binding.viewHighlighterOverlay
        if (!activeHighlighters.contains(highlighter)) {
            activeHighlighters.add(highlighter)
        }
        highlighter.isDrawingEnabled = isDrawingMode
        highlighter.activeColor = activeColor

        val zoomContainer = holder.binding.containerPageCanvas
        if (!activeZoomContainers.contains(zoomContainer)) {
            activeZoomContainers.add(zoomContainer)
        }
        zoomContainer.isDrawingMode = isDrawingMode
    }

    override fun onViewDetachedFromWindow(holder: PdfPageViewHolder) {
        super.onViewDetachedFromWindow(holder)
        val highlighter = holder.binding.viewHighlighterOverlay
        val currentPid = postId ?: highlighter.currentPostId
        if (!currentPid.isNullOrEmpty() && highlighter.hasDrawings()) {
            val savedFile = highlighter.saveDrawingToDisk(currentPid, highlighter.currentPageNumber)
            savedFile?.let { file ->
                onAnnotationSaved?.invoke(highlighter.currentPageNumber - 1, file.absolutePath)
            }
        }
        activeHighlighters.remove(highlighter)
        activeZoomContainers.remove(holder.binding.containerPageCanvas)
    }

    /**
     * Açık olan tüm sayfalardaki bekleyen S-Pen çizimlerini diske kaydeder.
     */
    fun saveAllPendingDrawings() {
        val pid = postId ?: return
        activeHighlighters.forEach { highlighter ->
            if (highlighter.hasDrawings()) {
                val file = highlighter.saveDrawingToDisk(pid, highlighter.currentPageNumber)
                file?.let {
                    onAnnotationSaved?.invoke(highlighter.currentPageNumber - 1, it.absolutePath)
                }
            }
        }
    }

    fun clearAllDrawings() {
        activeHighlighters.forEach { it.clearDrawings() }
    }

    fun close() {
        try {
            saveAllPendingDrawings()
            pageBitmaps.values.forEach { it.recycle() }
            pageBitmaps.clear()
            pdfRenderer?.close()
            fileDescriptor?.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    inner class PdfPageViewHolder(
        val binding: ItemPdfPageBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(position: Int) {
            val totalPages = pdfRenderer?.pageCount ?: 1
            val pageNum = position + 1
            binding.tvPageIndicator.text = "Sayfa $pageNum / $totalPages"

            var bitmap = pageBitmaps[position]
            if (bitmap == null && pdfRenderer != null) {
                try {
                    val page = pdfRenderer!!.openPage(position)
                    val scale = 2
                    val width = page.width * scale
                    val height = page.height * scale
                    val renderedBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                    renderedBitmap.eraseColor(Color.WHITE)

                    page.render(renderedBitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                    page.close()

                    pageBitmaps[position] = renderedBitmap
                    bitmap = renderedBitmap
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            if (bitmap != null) {
                binding.ivPdfPage.setImageBitmap(bitmap)
            }

            val highlighter = binding.viewHighlighterOverlay
            val currentPid = postId ?: "default_pdf"
            highlighter.currentPostId = currentPid
            highlighter.currentPageNumber = pageNum
            highlighter.isDrawingEnabled = isDrawingMode
            highlighter.activeColor = activeColor
            binding.containerPageCanvas.isDrawingMode = isDrawingMode

            // FAZ 13: S-Pen Mürekkep Katmanını Geri Yükle (Restore Overlay)
            val context = binding.root.context
            val annotationsDir = File(context.filesDir, "annotations")
            val sanitizedPid = currentPid.replace("[^a-zA-Z0-9_]".toRegex(), "_")
            val targetFile = File(annotationsDir, "annotation_${sanitizedPid}_p${pageNum}.png")

            if (targetFile.exists() && targetFile.length() > 0) {
                highlighter.loadOverlayFromPath(targetFile.absolutePath)
            } else {
                highlighter.loadOverlayFromPath(null)
            }

            // Çizim değiştiğinde veya tamamlandığında otomatik diske ve DB'ye kaydet
            highlighter.onDrawingChangedListener = {
                val saved = highlighter.saveDrawingToDisk(currentPid, pageNum)
                saved?.let { file ->
                    onAnnotationSaved?.invoke(position, file.absolutePath)
                }
            }
        }
    }
}
