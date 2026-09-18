package com.example.ui.common

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.text.TextPaint
import android.text.TextUtils
import android.util.AttributeSet
import android.view.Gravity
import androidx.appcompat.widget.AppCompatTextView

/**
 * İlim Diyârı - Dikey Metin Görünümü (Book Spine Vertical Text).
 * Kütüphane rafındaki 'Kitap Sırtı' formatında, başlığı yukarıdan aşağıya (90 derece)
 * kırpılma olmadan, altın yaldız zarafetiyle yazar.
 */
class VerticalTextView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : AppCompatTextView(context, attrs, defStyleAttr) {

    private val textBounds = Rect()

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        // Genişlik ve yükseklik ölçümlerini dikey eksene göre takas et
        super.onMeasure(heightMeasureSpec, widthMeasureSpec)
        setMeasuredDimension(measuredHeight, measuredWidth)
    }

    override fun onDraw(canvas: Canvas) {
        val textPaint: TextPaint = paint
        textPaint.color = currentTextColor

        canvas.save()

        // Sağa ötele ve 90 derece döndür
        canvas.translate(width.toFloat(), 0f)
        canvas.rotate(90f)

        val displayText = text.toString()
        val availableWidth = height.toFloat() - (compoundPaddingTop + compoundPaddingBottom)
        
        // Uzun metinleri sığdırmak için akıllı kırpma (ellipsize)
        val ellipsizeText = TextUtils.ellipsize(
            displayText,
            textPaint,
            availableWidth,
            TextUtils.TruncateAt.END
        ).toString()

        textPaint.getTextBounds(ellipsizeText, 0, ellipsizeText.length, textBounds)

        // Dikey eksende ortala
        val xPos = compoundPaddingTop.toFloat()
        val yPos = (width.toFloat() / 2f) + (textBounds.height() / 2f) - compoundPaddingStart

        canvas.drawText(ellipsizeText, xPos, yPos, textPaint)

        canvas.restore()
    }
}
