package com.example.util

import android.graphics.RenderEffect
import android.graphics.Shader
import android.os.Build
import android.view.View

/**
 * İlim Diyârı - Android 12+ (API 31+) Backdrop Blur & Glassmorphism Motoru (FAZ 13).
 * RenderEffect.createBlurEffect(30f, 30f, Shader.TileMode.MIRROR) ile
 * koyu zümrüt mesh gradient zemin üzerine binen kartları, BottomNav ve panelleri
 * gerçek buzlu cam (Frosted Glass) hissiyatına kavuşturur.
 */
object GlassEffectUtil {

    /**
     * View üzerine donanım destekli buzlu cam bulanıklığı (Backdrop Blur) uygular.
     */
    fun applyBlur(view: View, radiusX: Float = 30f, radiusY: Float = 30f) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            try {
                view.setRenderEffect(
                    RenderEffect.createBlurEffect(radiusX, radiusY, Shader.TileMode.MIRROR)
                )
            } catch (_: Exception) {}
        }
    }
}

/**
 * View uzantısı olarak doğrudan kullanım: view.applyGlassmorphismBlur()
 */
fun View.applyGlassmorphismBlur(radiusX: Float = 30f, radiusY: Float = 30f) {
    GlassEffectUtil.applyBlur(this, radiusX, radiusY)
}
