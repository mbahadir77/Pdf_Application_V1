package com.example.ui.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.widget.RemoteViews
import com.example.MainActivity
import com.example.R
import com.example.data.pref.StreakManager
import com.example.data.repository.MotivationRepository

/**
 * İlmNet - İlim Aynası (Durum Bazlı App Widget - FAZ 11).
 * RemoteViews arka plan filtre kısıtlamalarını aşmak için Canvas & Paint ile
 * %60 karartılmış Bitmap üretip doğrudan ImageView'a aktarır.
 */
class IlmMirrorWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == ACTION_REFRESH_WIDGET) {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val thisWidget = ComponentName(context, IlmMirrorWidgetProvider::class.java)
            val appWidgetIds = appWidgetManager.getAppWidgetIds(thisWidget)
            onUpdate(context, appWidgetManager, appWidgetIds)
        }
    }

    companion object {
        const val ACTION_REFRESH_WIDGET = "com.example.ilmnet.ACTION_REFRESH_WIDGET"

        /**
         * %60 siyah filtreli (#99000000) ve altın kenarlıklı güvenli Bitmap üretir.
         */
        private fun createDimmedBackgroundBitmap(context: Context, width: Int = 800, height: Int = 450): Bitmap {
            return try {
                val srcBitmap = BitmapFactory.decodeResource(context.resources, R.drawable.widget_ilm_bg)
                val output = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                val canvas = Canvas(output)
                val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)

                if (srcBitmap != null) {
                    val srcRect = Rect(0, 0, srcBitmap.width, srcBitmap.height)
                    val destRect = Rect(0, 0, width, height)
                    canvas.drawBitmap(srcBitmap, srcRect, destRect, paint)
                } else {
                    canvas.drawColor(Color.parseColor("#120B04"))
                }

                // %60 Siyah Filtre Katmanı (#99000000)
                paint.color = Color.parseColor("#99000000")
                paint.style = Paint.Style.FILL
                canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paint)

                // İnce Altın Kenarlık
                paint.color = Color.parseColor("#44D4AF37")
                paint.style = Paint.Style.STROKE
                paint.strokeWidth = 4f
                canvas.drawRect(2f, 2f, (width - 2).toFloat(), (height - 2).toFloat(), paint)

                output
            } catch (e: Exception) {
                val fallback = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                val canvas = Canvas(fallback)
                canvas.drawColor(Color.parseColor("#99120B04"))
                fallback
            }
        }

        fun updateAppWidget(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetId: Int
        ) {
            val streakManager = StreakManager.getInstance(context)
            val state = streakManager.getWidgetState()
            val streakCount = streakManager.getStreakCount()
            val quote = MotivationRepository.getRandomQuote(state)

            val views = RemoteViews(context.packageName, R.layout.widget_ilm_mirror)

            // Duruma göre ikon ve başlık belirleme
            val (iconRes, stateBadge) = when (state) {
                MotivationRepository.StreakState.ACTIVE -> {
                    Pair(R.drawable.ic_widget_active, "🔥 $streakCount Gün İstikrar")
                }
                MotivationRepository.StreakState.WARNING -> {
                    Pair(R.drawable.ic_widget_warning, "⏳ Rölanti • $streakCount Gün")
                }
                MotivationRepository.StreakState.DANGER -> {
                    Pair(R.drawable.ic_widget_warning, "⚠️ Tehlike • $streakCount Gün")
                }
                MotivationRepository.StreakState.ABANDONED -> {
                    Pair(R.drawable.ic_widget_abandoned, "💨 Terk Edilmiş")
                }
            }

            // RemoteViews arka plan filtresi kısıtını çözen karartılmış Bitmap aktarımı
            val dimmedBg = createDimmedBackgroundBitmap(context)
            views.setImageViewBitmap(R.id.iv_widget_background, dimmedBg)

            views.setImageViewResource(R.id.iv_widget_status_icon, iconRes)
            views.setTextViewText(R.id.tv_widget_streak_badge, stateBadge)
            views.setTextViewText(R.id.tv_widget_quote, quote)

            val subtitle = when (state) {
                MotivationRepository.StreakState.ACTIVE -> "📜 Kesintisiz ilim halkasındasın"
                MotivationRepository.StreakState.WARNING -> "⏳ Mütalaa vaktini kaçırma"
                MotivationRepository.StreakState.DANGER -> "⚠️ Zinciri kırma, bir risale oku"
                MotivationRepository.StreakState.ABANDONED -> "💨 İlim meclisi seni bekler, geri dön"
            }
            views.setTextViewText(R.id.tv_widget_status_subtitle, subtitle)

            // Widget'a tıklandığında MainActivity'yi aç
            val clickIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val pendingIntent = PendingIntent.getActivity(
                context,
                0,
                clickIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_root, pendingIntent)

            appWidgetManager.updateAppWidget(appWidgetId, views)
        }

        /**
         * Uygulama içinden veya arka plandan tüm aktif widget'ları tek dokunuşla günceller.
         */
        fun requestWidgetUpdate(context: Context) {
            try {
                val appWidgetManager = AppWidgetManager.getInstance(context)
                val thisWidget = ComponentName(context, IlmMirrorWidgetProvider::class.java)
                val appWidgetIds = appWidgetManager.getAppWidgetIds(thisWidget)
                if (appWidgetIds.isNotEmpty()) {
                    for (widgetId in appWidgetIds) {
                        updateAppWidget(context, appWidgetManager, widgetId)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
