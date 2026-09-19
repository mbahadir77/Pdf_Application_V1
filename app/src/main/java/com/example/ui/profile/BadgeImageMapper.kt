package com.example.ui.profile

import android.content.Context
import android.widget.ImageView
import coil.load
import com.example.R

/**
 * İlim Diyârı - Gerçek Rozet Resim Haritalama Motoru (Emir 4).
 * XML vektörler yerine res/drawable içindeki gerçek PNG resimlerini
 * (badge_1, badge_2, badge_3, badge_4, badge_locked veya badge_1..badge_20)
 * okur ve Coil / standart setImageResource ile yükler.
 * Kullanıcı klasöre badge_X.png resimlerini eklediğinde sistem bunları otomatik tanır.
 */
object BadgeImageMapper {

    /**
     * Verilen seviye ve kademe için res/drawable içindeki gerçek drawable/PNG ID'sini döndürür.
     */
    fun getBadgeDrawableRes(context: Context, level: Int, currentTier: Int, isUnlocked: Boolean): Int {
        if (!isUnlocked) {
            val lockedDynamic = getDynamicDrawableId(context, "badge_locked")
            return if (lockedDynamic != 0) lockedDynamic else R.drawable.badge_locked
        }

        // 1. Öncelik: Doğrudan seviye PNG'si (Örn: badge_1 .. badge_20)
        val levelName = "badge_$level"
        val levelDynamic = getDynamicDrawableId(context, levelName)
        if (levelDynamic != 0) return levelDynamic

        // 2. Öncelik: Kademe PNG'si (badge_1: Bakır, badge_2: Gümüş, badge_3: Altın, badge_4: Elmas)
        val tierName = "badge_$currentTier"
        val tierDynamic = getDynamicDrawableId(context, tierName)
        if (tierDynamic != 0) return tierDynamic

        // 3. Öncelik: Derleme anında tanımlı R.drawable referansı
        return when (currentTier) {
            4 -> R.drawable.badge_4
            3 -> R.drawable.badge_3
            2 -> R.drawable.badge_2
            1 -> R.drawable.badge_1
            else -> R.drawable.badge_1
        }
    }

    /**
     * Seviye veya kademeye göre statik R.drawable kimliği döndürür.
     */
    fun getStaticBadgeRes(currentTier: Int, isUnlocked: Boolean): Int {
        if (!isUnlocked) return R.drawable.badge_locked
        return when (currentTier) {
            4 -> R.drawable.badge_4
            3 -> R.drawable.badge_3
            2 -> R.drawable.badge_2
            1 -> R.drawable.badge_1
            else -> R.drawable.badge_1
        }
    }

    /**
     * Context üzerinden res/drawable'daki dinamik resim ID'sini sorgular.
     */
    private fun getDynamicDrawableId(context: Context, resourceName: String): Int {
        return try {
            context.resources.getIdentifier(resourceName, "drawable", context.packageName)
        } catch (_: Exception) {
            0
        }
    }

    /**
     * Rozet resmini Coil kütüphanesi veya standart setImageResource ile ImageView'a yükler.
     */
    fun loadBadgeImage(imageView: ImageView, badge: AcademicBadge) {
        val context = imageView.context
        val resId = getBadgeDrawableRes(
            context = context,
            level = badge.level,
            currentTier = badge.currentTier,
            isUnlocked = badge.isUnlocked
        )

        try {
            // Coil ile animasyonlu ve performanslı PNG yükleme
            imageView.load(resId) {
                crossfade(true)
                error(resId)
            }
        } catch (_: Throwable) {
            // Standart Android Resource Fallback
            imageView.setImageResource(resId)
        }
    }
}
