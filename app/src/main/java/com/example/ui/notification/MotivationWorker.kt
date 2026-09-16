package com.example.ui.notification

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import java.util.concurrent.TimeUnit

/**
 * İlmNet - 2 Günde Bir Tetiklenen Periyodik Motivasyon Motoru (WorkManager).
 */
class MotivationWorker(
    private val appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            NotificationHelper.createNotificationChannels(appContext)
            NotificationHelper.showPeriodicMotivationNotification(appContext)
            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }

    companion object {
        private const val WORK_TAG = "ilmnet_periodic_motivation_work"

        fun schedule(context: Context) {
            // 2 günde bir periyodik bildirim (PeriodicWork min 15 dk, burada 2 gün = 48 saat)
            val workRequest = PeriodicWorkRequestBuilder<MotivationWorker>(
                2, TimeUnit.DAYS,
                6, TimeUnit.HOURS // flex interval
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
