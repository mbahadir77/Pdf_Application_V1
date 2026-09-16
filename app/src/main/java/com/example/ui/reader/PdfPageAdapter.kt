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
 * İlmNet - Yerleşik PDF Sayfa Adaptörü.
 * android.graphics.pdf.PdfRenderer ile PDF sayfalarını pürüzsüz vektörel bitmap olarak işler.
 */
class PdfPageAdapter(
    private val pdfFile: File
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
        activeHighlighters.remove(holder.binding.viewHighlighterOverlay)
        activeZoomContainers.remove(holder.binding.containerPageCanvas)
    }

    fun clearAllDrawings() {
        activeHighlighters.forEach { it.clearDrawings() }
    }

    fun close() {
        try {
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
            binding.tvPageIndicator.text = "Sayfa ${position + 1} / $totalPages"

            var bitmap = pageBitmaps[position]
            if (bitmap == null && pdfRenderer != null) {
                try {
                    val page = pdfRenderer!!.openPage(position)
                    // 2x Ölçek ile kristal netliğinde akademik metin okuma deneyimi
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

            binding.viewHighlighterOverlay.isDrawingEnabled = isDrawingMode
            binding.viewHighlighterOverlay.activeColor = activeColor
            binding.containerPageCanvas.isDrawingMode = isDrawingMode
        }
    }
}
