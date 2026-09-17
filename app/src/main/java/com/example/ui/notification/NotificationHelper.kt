package com.example.ui.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R
import com.example.data.motivation.MotivationQuotes
import kotlinx.coroutines.launch
import kotlin.random.Random

/**
 * İlim Diyârı - Dış Sistem Bildirim Yöneticisi (OS Push Notifications & WorkManager).
 * Beğeni, yorum, rozet kazanımı ve 48 saatlik hareketsizlikte hikmet bildirimlerini
 * Android İşletim Sistemi bildirimleri (NotificationManager / NotificationCompat) olarak iletir.
 */
object NotificationHelper {

    const val CHANNEL_MOTIVATION_ID = "ilim_diyari_motivation_channel"
    private const val CHANNEL_MOTIVATION_NAME = "İlim Diyârı Hikmet & Motivasyon Bildirimleri"
    const val CHANNEL_SOCIAL_ID = "ilim_diyari_social_channel"
    private const val CHANNEL_SOCIAL_NAME = "İlim Diyârı Sosyal Etkileşim ve Rütbe Bildirimleri"

    const val EXTRA_TARGET_POST_ID = "extra_target_post_id"
    const val EXTRA_TARGET_AUTHOR = "extra_target_author"

    fun createNotificationChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            val motivationChannel = NotificationChannel(
                CHANNEL_MOTIVATION_ID,
                CHANNEL_MOTIVATION_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "48 saat hareketsizlik hikmet bildirimleri ve ilmi tefekkür dürtmeleri"
                enableLights(true)
                lightColor = android.graphics.Color.parseColor("#FFD700")
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 350, 200, 350)
                lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
                setShowBadge(true)
            }

            val socialChannel = NotificationChannel(
                CHANNEL_SOCIAL_ID,
                CHANNEL_SOCIAL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "PDF okuma, yorum, beğeni ve 20 seviyeli rozet kazanımı bildirimleri"
                enableLights(true)
                lightColor = android.graphics.Color.parseColor("#FFD700")
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 250, 150, 250)
                lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
                setShowBadge(true)
            }

            notificationManager.createNotificationChannel(motivationChannel)
            notificationManager.createNotificationChannel(socialChannel)
        }
    }

    /**
     * 48 saatten uzun süre uygulamaya girmeyen kullanıcıya gönderilen Dış Sistem (OS Push) Hikmet Bildirimi.
     */
    fun showWisdomNotification(context: Context, quoteText: String) {
        val title = "📜 İlim Meclisi Seni Özledi! • İlim Diyârı"
        val body = quoteText

        saveNotificationToDatabase(context, title, body, "WISDOM")

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            1002,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_MOTIVATION_ID)
            .setSmallIcon(R.drawable.ic_academic_logo)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setCategory(NotificationCompat.CATEGORY_RECOMMENDATION)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(1002, notification)
    }

    /**
     * WorkManager tarafından tetiklenen periyodik motivasyon bildirimi.
     */
    fun showPeriodicMotivationNotification(context: Context) {
        if (!com.example.ui.settings.AppSettingsPreferences.getInstance(context).isMotivationNotificationsEnabled) {
            return
        }
        val quote = MotivationQuotes.getRandomQuote()
        val title = "İlim Diyârı Vakti 📚 • ${quote.source}"
        val body = quote.text

        saveNotificationToDatabase(context, title, body, "MOTIVATION")

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            1001,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_MOTIVATION_ID)
            .setSmallIcon(R.drawable.ic_academic_logo)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setCategory(NotificationCompat.CATEGORY_RECOMMENDATION)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(1001, notification)
    }

    /**
     * Rozet ve rütbe kazanımı olduğunda cihazın üst panelinden (OS Push Notification) bildirim gönderir.
     */
    fun showBadgeUnlockedNotification(
        context: Context,
        categoryName: String,
        rankTitle: String,
        level: Int,
        icon: String = "🎖️"
    ) {
        val title = "🏆 Yeni Akademik Rütbe! ($icon $categoryName)"
        val message = "Tebrikler! Seviye $level: '$rankTitle' unvanına yükseldiniz. Meclis sizinle iftihar ediyor! ✨"
        val notificationId = 3000 + Math.abs(categoryName.hashCode() % 1000)
        sendSocialNotification(context, title, message, notificationId, type = "BADGE")
    }

    /**
     * Biri kullanıcının PDF'ini okumaya başladığında (Göz / Oku ikonuna bastığında).
     * Yazar kendi PDF'ini okuduğunda (readerId == authorId) bildirim gönderilmez (Self-Spam engelleme).
     */
    fun showPdfReadNotification(
        context: Context,
        pdfTitle: String,
        readerName: String = "Bir araştırmacı",
        readerId: String? = null,
        authorId: String? = null,
        targetPostId: String? = null
    ) {
        if (readerId != null && authorId != null && readerId == authorId) {
            return
        }
        val wittyMessages = listOf(
            "$readerName senin '$pdfTitle' risaleni okumaya başladı. İlmin dalga dalga yayılıyor, durma! ✨",
            "'$pdfTitle' eserin masaya yatırıldı! Zihinler aydınlanıyor, yeni risaleyi ne zaman yüklüyorsun? 🧐",
            "Müjde! Biri az önce '$pdfTitle' eserinden istifade etmeye başladı. Sadaka-i cariyen işliyor! 📖"
        )
        val message = wittyMessages[Random.nextInt(wittyMessages.size)]
        sendSocialNotification(
            context = context,
            title = "Eserin Okunuyor! 👁️",
            message = message,
            notificationId = 2001,
            targetPostId = targetPostId
        )
    }

    /**
     * PDF'e yeni yorum geldiğinde OS Push bildirimi.
     */
    fun showCommentNotification(
        context: Context,
        pdfTitle: String,
        commenterName: String,
        commentText: String,
        targetPostId: String? = null
    ) {
        val wittyMessages = listOf(
            "$commenterName, '$pdfTitle' risalene tahlil bıraktı: \"$commentText\". Hemen cevap ver, meclisi dağıtma! 💬",
            "Tartışma alevlendi! $commenterName eserin hakkında fikrini beyan etti. Alimler müzakereyi sever, haydi katıl! ✍️",
            "Yeni bir akademik münazara başladı! $commenterName yorum yaptı: \"$commentText\""
        )
        val message = wittyMessages[Random.nextInt(wittyMessages.size)]
        sendSocialNotification(
            context = context,
            title = "Akademik Yorum Geldi! 🖋️",
            message = message,
            notificationId = 2002,
            targetPostId = targetPostId
        )
    }

    /**
     * PDF'e beğeni geldiğinde OS Push bildirimi.
     */
    fun showLikeNotification(
        context: Context,
        pdfTitle: String,
        likerName: String,
        targetPostId: String? = null
    ) {
        val message = "$likerName, '$pdfTitle' çalışmana gıpta ile kalp bıraktı. İlmin bereketi daim olsun! ❤️"
        sendSocialNotification(
            context = context,
            title = "Risalen Beğenildi! ✨",
            message = message,
            notificationId = 2003,
            targetPostId = targetPostId
        )
    }

    /**
     * Takip edilen kişi yeni PDF yüklediğinde.
     */
    fun showFollowedAuthorUploadedPdf(
        context: Context,
        authorName: String,
        pdfTitle: String,
        targetPostId: String? = null
    ) {
        val message = "Takip ettiğin $authorName yeni bir risale neşretti: '$pdfTitle'. İlk mütalaa eden sen ol! 🚀"
        sendSocialNotification(
            context = context,
            title = "Takip Ettiğin Hoca Eser Paylaştı! 📜",
            message = message,
            notificationId = 2004,
            targetPostId = targetPostId,
            targetAuthorName = authorName
        )
    }

    /**
     * Takip edilen kişi rozet kazandığında.
     */
    fun showFollowedUserEarnedBadge(
        context: Context,
        authorName: String,
        badgeName: String,
        tierName: String
    ) {
        val wittyMessages = listOf(
            "Rakibin $authorName az önce $badgeName alanında $tierName rozeti aldı, sen hala uyuyor musun? 👀",
            "$authorName ilim basamaklarını tırmanıyor ($tierName rozeti kazandı). Ona tebrik yazmak ister misin? 🎖️",
            "İlim yarışında bayrak el değiştirdi! $authorName $tierName rütbesine yükseldi. Gayret vakti! 💎"
        )
        val message = wittyMessages[Random.nextInt(wittyMessages.size)]
        sendSocialNotification(
            context = context,
            title = "Akademik Rütbe Bildirimi! 🏆",
            message = message,
            notificationId = 2005,
            targetAuthorName = authorName
        )
    }

    private fun sendSocialNotification(
        context: Context,
        title: String,
        message: String,
        notificationId: Int,
        type: String = "SOCIAL",
        targetPostId: String? = null,
        targetAuthorName: String? = null
    ) {
        // Yerel Room DB'ye geçmiş bildirimi kaydet
        saveNotificationToDatabase(context, title, message, type, targetPostId = targetPostId)

        if (!com.example.ui.settings.AppSettingsPreferences.getInstance(context).isSocialNotificationsEnabled) {
            return
        }
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            if (targetPostId != null) {
                putExtra(EXTRA_TARGET_POST_ID, targetPostId)
            }
            if (targetAuthorName != null) {
                putExtra(EXTRA_TARGET_AUTHOR, targetAuthorName)
            }
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_SOCIAL_ID)
            .setSmallIcon(R.drawable.ic_academic_logo)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setCategory(NotificationCompat.CATEGORY_SOCIAL)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(notificationId, notification)
    }

    /**
     * Bildirimleri yerel Room DB'ye asenkron olarak kaydeder.
     */
    fun saveNotificationToDatabase(
        context: Context,
        title: String,
        message: String,
        type: String = "INFO",
        targetPostId: String? = null
    ) {
        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
            try {
                val db = com.example.data.local.AppDatabase.getInstance(context)
                db.notificationDao().insertNotification(
                    com.example.data.local.entity.NotificationEntity(
                        title = title,
                        message = message,
                        type = type,
                        timestamp = System.currentTimeMillis(),
                        isRead = false,
                        targetPostId = targetPostId
                    )
                )
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
