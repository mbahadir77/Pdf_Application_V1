package com.example.ui.common

import android.app.Activity
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.OvershootInterpolator
import com.example.databinding.LayoutCustomToastBinding

/**
 * İlmNet - Özel Cam Efektli (Glassmorphism), Altın Vurgulu Animasyonlu Toast & Bildirim Yöneticisi (Faz 5).
 * Varsayılan gri sistem Toast'ları yerine kullanılır.
 */
object IlmToast {

    private var currentToastView: View? = null
    private val handler = Handler(Looper.getMainLooper())
    private var dismissRunnable: Runnable? = null

    fun show(
        activity: Activity,
        message: String,
        title: String = "İlmNet",
        icon: String = "✨",
        durationMs: Long? = null
    ) {
        val finalDuration = durationMs ?: calculateDynamicDuration(message)

        activity.runOnUiThread {
            // Varsa önceki toast'ı kaldır
            dismissRunnable?.let { handler.removeCallbacks(it) }
            currentToastView?.let {
                (it.parent as? ViewGroup)?.removeView(it)
                currentToastView = null
            }

            val decorView = activity.window.decorView as? ViewGroup ?: return@runOnUiThread
            val binding = LayoutCustomToastBinding.inflate(
                LayoutInflater.from(activity),
                decorView,
                false
            )

            binding.tvToastTitle.text = title
            binding.tvToastMessage.text = message
            binding.tvToastIcon.text = icon

            val toastView = binding.root
            toastView.alpha = 0f
            toastView.translationY = -140f

            decorView.addView(toastView)
            currentToastView = toastView

            // Giriş animasyonu (Yukarıdan kayarak ve esneyerek gelme)
            toastView.animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(350)
                .setInterpolator(OvershootInterpolator(1.2f))
                .start()

            // Dokununca hemen kapanma
            toastView.setOnClickListener {
                dismiss(toastView)
            }

            // Dinamik süre dolunca otomatik kaybolma
            dismissRunnable = Runnable {
                dismiss(toastView)
            }
            handler.postDelayed(dismissRunnable!!, finalDuration)
        }
    }

    /**
     * Metin uzunluğuna göre dinamik ekranda kalma süresi:
     * - Kısa metinler: 3000 ms (3 saniye)
     * - Orta metinler: 5000 ms (5 saniye)
     * - Uzun alim sözleri / cümleler: 8000 - 10000 ms (8-10 saniye)
     */
    private fun calculateDynamicDuration(message: String): Long {
        val length = message.length
        return when {
            length <= 45 -> 3000L
            length <= 90 -> 5500L
            length <= 150 -> 8000L
            else -> 10000L
        }
    }

    fun success(activity: Activity, message: String, title: String = "Başarılı ✨") {
        show(activity, message, title = title, icon = "🌟")
    }

    fun info(activity: Activity, message: String, title: String = "İlmNet Bilgi 📚") {
        show(activity, message, title = title, icon = "💡")
    }

    fun witty(activity: Activity, message: String, title: String = "İlim Hatırlatması 🦉") {
        show(activity, message, title = title, icon = "🦉")
    }

    fun warning(activity: Activity, message: String, title: String = "Dikkat ⚠️") {
        show(activity, message, title = title, icon = "⚠️")
    }

    fun error(activity: Activity, message: String, title: String = "Uyarı ⚠️") {
        show(activity, message, title = title, icon = "⚠️")
    }

    private fun dismiss(toastView: View) {
        toastView.animate()
            .alpha(0f)
            .translationY(-100f)
            .setDuration(250)
            .withEndAction {
                (toastView.parent as? ViewGroup)?.removeView(toastView)
                if (currentToastView == toastView) {
                    currentToastView = null
                }
            }
            .start()
    }
}
