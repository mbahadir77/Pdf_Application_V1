package com.example.ui.profile

import android.content.Context
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.Shader
import android.view.View
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView

/**
 * İlim Diyârı - Rozet Sergi Rafları Çizim Motoru (Shelf Item Decoration).
 * Rozetlerin havada uçuşmasını önler; her satırın (2'li, 3'lü veya 4'lü ızgara) altına
 * zengin ahşap dokusu, altın yaldız kenarlık ve cam gölgesiyle şık bir sergi rafı çizer.
 * Rozetler doğrudan bu rafların üzerine oturur.
 */
class ShelfItemDecoration(context: Context) : RecyclerView.ItemDecoration() {

    private val density = context.resources.displayMetrics.density

    // Raf ölçüleri (dp -> px)
    private val shelfHeight = 12 * density
    private val shelfTrimHeight = 2.5f * density
    private val shadowHeight = 8 * density
    private val bottomSpacing = (shelfHeight + shadowHeight + 6 * density).toInt()

    // 1. Üst Altın Yaldızlı Kenarlık / Işık Çıtası
    private val trimPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFFD4AF37.toInt() // Altın Sarısı (#D4AF37)
        style = Paint.Style.FILL
    }

    // 2. Ahşap Raf Gövdesi
    private val woodPlankPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFF422415.toInt() // Klasik Ceviz / Maun Ahşap
        style = Paint.Style.FILL
    }

    // 3. Raf Altı Yumuşak Gölge
    private val shadowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    override fun getItemOffsets(
        outRect: Rect,
        view: View,
        parent: RecyclerView,
        state: RecyclerView.State
    ) {
        // Her elemanın altına raf kalınlığı ve gölgesi kadar boşluk bırak
        outRect.bottom = bottomSpacing
    }

    override fun onDraw(c: Canvas, parent: RecyclerView, state: RecyclerView.State) {
        super.onDraw(c, parent, state)

        val childCount = parent.childCount
        if (childCount == 0) return

        val layoutManager = parent.layoutManager as? GridLayoutManager
        val spanCount = layoutManager?.spanCount ?: 2

        // Aynı satırdaki öğelerin en alt Y koordinatlarını topla
        val rowBottoms = mutableListOf<Float>()

        for (i in 0 until childCount) {
            val child = parent.getChildAt(i)
            val childBottom = child.bottom.toFloat()

            // Bu öğe mevcut satırların sonuncusuna yakın mı?
            var matchedExisting = false
            for (index in rowBottoms.indices) {
                if (Math.abs(rowBottoms[index] - childBottom) < 30 * density) {
                    if (childBottom > rowBottoms[index]) {
                        rowBottoms[index] = childBottom
                    }
                    matchedExisting = true
                    break
                }
            }
            if (!matchedExisting) {
                rowBottoms.add(childBottom)
            }
        }

        val left = parent.paddingLeft.toFloat()
        val right = (parent.width - parent.paddingRight).toFloat()

        for (bottomY in rowBottoms) {
            val shelfTop = bottomY + (2 * density)
            val shelfBottom = shelfTop + shelfHeight

            // A) Üst Altın Yaldızlı Çıta
            c.drawRect(left, shelfTop, right, shelfTop + shelfTrimHeight, trimPaint)

            // B) Masif Ahşap Raf Gövdesi
            c.drawRect(left, shelfTop + shelfTrimHeight, right, shelfBottom, woodPlankPaint)

            // C) Raf Altı Gradyan Gölge
            shadowPaint.shader = LinearGradient(
                left, shelfBottom,
                left, shelfBottom + shadowHeight,
                0x77000000.toInt(),
                0x00000000,
                Shader.TileMode.CLAMP
            )
            c.drawRect(left, shelfBottom, right, shelfBottom + shadowHeight, shadowPaint)
        }
    }
}
