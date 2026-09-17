package com.example.ui.notification

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.example.data.pref.SessionManager
import com.example.data.repository.MotivationRepository
import java.util.concurrent.TimeUnit

/**
 * İlim Diyârı - Hikmet ve 48 Saat İnaktivite Dış Bildirim Motoru (FAZ 9 Mega Update).
 * Kullanıcı 2 günden fazla (48 saat) uygulamaya girmediğinde, MotivationRepository içindeki
 * sitemkâr (Warning/Danger) sözlerden birini seçip cihaza sessiz Dış Bildirim (Push Notification) gönderir.
 */
class WisdomNotificationWorker(
    private val appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            val sessionManager = SessionManager(appContext)
            val lastActiveTime = sessionManager.getLastActiveTime()
            val now = System.currentTimeMillis()
            val diffHours = (now - lastActiveTime) / (1000 * 60 * 60)

            // Kullanıcı 48 saat (2 gün) veya daha fazla süredir uygulamaya girmediyse
            if (diffHours >= 48) {
                val sitemkarQuote = MotivationRepository.getRandomSitemkarQuote()
                NotificationHelper.createNotificationChannels(appContext)
                NotificationHelper.showWisdomNotification(appContext, sitemkarQuote)
            }
            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }

    companion object {
        private const val WORK_TAG = "ilim_diyari_wisdom_notification_work"

        fun schedule(context: Context) {
            // Arka planda 12 saatte bir kontrol eder
            val workRequest = PeriodicWorkRequestBuilder<WisdomNotificationWorker>(
                12, TimeUnit.HOURS,
                2, TimeUnit.HOURS // flex interval
            )
                .addTag(WORK_TAG)
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_TAG,
                ExistingPeriodicWorkPolicy.KEEP,
                workRequest
            )
        }
    }
}
