package com.example.util

import android.os.Build
import android.view.HapticFeedbackConstants
import android.view.View

/**
 * İlmNet - Android Tok Haptik Geri Bildirim Yardımcısı (FAZ 8).
 * Beğen, Takip Et ve Rozet kazanımında CONFIRM / KEYBOARD_TAP tok titreşimini tetikler.
 */
fun View.performTokHaptic() {
    try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            performHapticFeedback(HapticFeedbackConstants.CONFIRM)
        } else {
            performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
        }
    } catch (e: Exception) {
        try {
            performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
        } catch (_: Exception) {}
    }
}
