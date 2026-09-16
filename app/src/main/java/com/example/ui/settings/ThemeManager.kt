package com.example.ui.settings

import android.app.Activity
import android.graphics.Color
import android.view.View
import androidx.annotation.DrawableRes
import com.example.R

/**
 * İlmNet - 5 Farklı Atmosfer Sunan Dinamik Tema Motoru (Faz 6).
 */
enum class AppTheme(
    val id: String,
    val title: String,
    val subtitle: String,
    @DrawableRes val backgroundDrawableRes: Int,
    val primaryColorHex: String,
    val accentColorHex: String
) {
    GLASSMORPHISM(
        id = "glassmorphism",
        title = "Glassmorphism",
        subtitle = "Zümrüt Yeşili & Altın Sarısı (Varsayılan)",
        backgroundDrawableRes = R.drawable.bg_auth_gradient,
        primaryColorHex = "#D4AF37",
        accentColorHex = "#FFD700"
    ),
    ISLAMIC(
        id = "islamic",
        title = "İslamic",
        subtitle = "Turkuaz, Çini Mavisi ve Gümüş",
        backgroundDrawableRes = R.drawable.bg_theme_islamic,
        primaryColorHex = "#0D3B36",
        accentColorHex = "#14B8A6"
    ),
    TECHNO(
        id = "techno",
        title = "Techno",
        subtitle = "Neon Mavi ve Koyu Gri (Siberpunk)",
        backgroundDrawableRes = R.drawable.bg_theme_techno,
        primaryColorHex = "#0F172A",
        accentColorHex = "#00E5FF"
    ),
    ANTIQUE(
        id = "antique",
        title = "Antique",
        subtitle = "Eski Kağıt, Sepya ve Koyu Kahve",
        backgroundDrawableRes = R.drawable.bg_theme_antique,
        primaryColorHex = "#2C1D13",
        accentColorHex = "#D4A373"
    ),
    SIMPLE(
        id = "simple",
        title = "Simple",
        subtitle = "Düz Siyah / Beyaz, Minimalist",
        backgroundDrawableRes = R.drawable.bg_theme_simple,
        primaryColorHex = "#18181B",
        accentColorHex = "#E4E4E7"
    );

    companion object {
        fun fromId(id: String): AppTheme {
            return values().firstOrNull { it.id.equals(id, ignoreCase = true) } ?: GLASSMORPHISM
        }
    }
}

object ThemeManager {

    /**
     * Aktif temayı SharedPreferences'tan okur veya varsayılanı döndürür.
     */
    fun getActiveTheme(context: android.content.Context): AppTheme {
        val pref = AppSettingsPreferences.getInstance(context)
        return AppTheme.fromId(pref.currentThemeId)
    }

    /**
     * Yeni temayı kaydeder ve hedeflenen View'lara uygular.
     */
    fun applyTheme(activity: Activity, theme: AppTheme, vararg views: View?) {
        AppSettingsPreferences.getInstance(activity).currentThemeId = theme.id
        for (v in views) {
            v?.setBackgroundResource(theme.backgroundDrawableRes)
        }
    }
}
