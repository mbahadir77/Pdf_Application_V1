package com.example.data.pref

import android.content.Context
import android.content.SharedPreferences
import com.example.data.repository.MotivationRepository
import java.util.Calendar

/**
 * İlmNet - Kullanıcı Giriş Serisi (Streak) ve Widget Durum Yöneticisi (FAZ 8).
 * SharedPreferences tabanlı olarak günlük aktiflik ve seriyi (streak) hesaplar.
 */
class StreakManager(context: Context) {

    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

    companion object {
        private const val PREF_NAME = "ilmnet_streak_prefs"
        private const val KEY_LAST_LOGIN_DAY = "key_last_login_day"
        private const val KEY_STREAK_COUNT = "key_streak_count"
        private const val KEY_LAST_LOGIN_TIMESTAMP = "key_last_login_timestamp"

        @Volatile
        private var INSTANCE: StreakManager? = null

        fun getInstance(context: Context): StreakManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: StreakManager(context).also { INSTANCE = it }
            }
        }
    }

    /**
     * Kullanıcı uygulamayı her açtığında çağrılır.
     * Günlük seri (streak) durumunu günceller.
     */
    fun recordDailyLogin(): Int {
        val now = System.currentTimeMillis()
        val currentDay = getDayOfYear(now)
        val currentYear = getYear(now)

        val lastDay = prefs.getInt(KEY_LAST_LOGIN_DAY, -1)
        val lastYear = prefs.getInt("key_last_login_year", -1)
        var currentStreak = prefs.getInt(KEY_STREAK_COUNT, 0)

        if (lastDay == -1) {
            // İlk giriş
            currentStreak = 1
        } else if (lastYear == currentYear && lastDay == currentDay) {
            // Aynı gün içinde tekrar giriş, streak değişmez
            return currentStreak
        } else if ((lastYear == currentYear && currentDay - lastDay == 1) ||
            (currentYear - lastYear == 1 && lastDay >= 365 && currentDay == 1)
        ) {
            // Tam ertesi gün giriş (Streak devam ediyor)
            currentStreak += 1
        } else {
            // Ara verilmiş, seri sıfırlanır ve 1'den başlar
            currentStreak = 1
        }

        prefs.edit()
            .putInt(KEY_LAST_LOGIN_DAY, currentDay)
            .putInt("key_last_login_year", currentYear)
            .putLong(KEY_LAST_LOGIN_TIMESTAMP, now)
            .putInt(KEY_STREAK_COUNT, currentStreak)
            .apply()

        return currentStreak
    }

    fun getStreakCount(): Int {
        return prefs.getInt(KEY_STREAK_COUNT, 1)
    }

    /**
     * Durum Makinesi (State Machine):
     * - ACTIVE: Son giriş bugün veya dün (0-1 gün)
     * - WARNING: 2-3 gün girilmemiş (Rölanti)
     * - DANGER: 4-6 gün girilmemiş (Tehlike)
     * - ABANDONED: 7+ gün girilmemiş (Terk Edilmiş)
     */
    fun getWidgetState(): MotivationRepository.StreakState {
        val lastTimestamp = prefs.getLong(KEY_LAST_LOGIN_TIMESTAMP, 0L)
        if (lastTimestamp == 0L) {
            return MotivationRepository.StreakState.ACTIVE
        }

        val diffMillis = System.currentTimeMillis() - lastTimestamp
        val daysDiff = (diffMillis / (1000L * 60 * 60 * 24)).toInt()

        return when {
            daysDiff <= 1 -> MotivationRepository.StreakState.ACTIVE
            daysDiff in 2..3 -> MotivationRepository.StreakState.WARNING
            daysDiff in 4..6 -> MotivationRepository.StreakState.DANGER
            else -> MotivationRepository.StreakState.ABANDONED
        }
    }

    fun getLastLoginTimestamp(): Long {
        return prefs.getLong(KEY_LAST_LOGIN_TIMESTAMP, 0L)
    }

    private fun getDayOfYear(millis: Long): Int {
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = millis
        return calendar.get(Calendar.DAY_OF_YEAR)
    }

    private fun getYear(millis: Long): Int {
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = millis
        return calendar.get(Calendar.YEAR)
    }
}
