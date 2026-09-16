package com.example.data.pref

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

/**
 * İlmNet Kalıcı Oturum Yöneticisi (EncryptedSharedPreferences).
 * Kullanıcı kimlik doğrulama oturumunu cihaz donanım destekli AES-256 ile şifreli saklar.
 */
class SessionManager(context: Context) {

    private val masterKey: MasterKey = MasterKey.Builder(context.applicationContext)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val prefs: SharedPreferences = EncryptedSharedPreferences.create(
        context.applicationContext,
        PREF_NAME,
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    companion object {
        private const val PREF_NAME = "ilmnet_secure_session"
        private const val KEY_IS_LOGGED_IN = "key_is_logged_in"
        private const val KEY_USER_ID = "key_user_id"
        private const val KEY_USER_EMAIL = "key_user_email"
        private const val KEY_AUTH_TOKEN = "key_auth_token"
        private const val KEY_FULL_NAME = "key_full_name"
        private const val KEY_ACADEMIC_TITLE = "key_academic_title"
        private const val KEY_BIO = "key_bio"
        private const val KEY_AVATAR_URL = "key_avatar_url"
        private const val KEY_GITHUB_USERNAME = "key_github_username"
    }

    fun saveSession(userId: String, email: String, token: String? = null) {
        prefs.edit()
            .putBoolean(KEY_IS_LOGGED_IN, true)
            .putString(KEY_USER_ID, userId)
            .putString(KEY_USER_EMAIL, email)
            .putString(KEY_AUTH_TOKEN, token)
            .apply()
    }

    fun saveProfile(
        fullName: String,
        academicTitle: String?,
        bio: String?,
        avatarUrl: String?,
        githubUsername: String?
    ) {
        prefs.edit()
            .putString(KEY_FULL_NAME, fullName)
            .putString(KEY_ACADEMIC_TITLE, academicTitle)
            .putString(KEY_BIO, bio)
            .putString(KEY_AVATAR_URL, avatarUrl)
            .putString(KEY_GITHUB_USERNAME, githubUsername)
            .apply()
    }

    fun getFullName(): String? = prefs.getString(KEY_FULL_NAME, null)
    fun getAcademicTitle(): String? = prefs.getString(KEY_ACADEMIC_TITLE, null)
    fun getBio(): String? = prefs.getString(KEY_BIO, null)
    fun getAvatarUrl(): String? = prefs.getString(KEY_AVATAR_URL, null)

    fun isLoggedIn(): Boolean {
        return prefs.getBoolean(KEY_IS_LOGGED_IN, false)
    }

    fun getUserId(): String? {
        return prefs.getString(KEY_USER_ID, null)
    }

    fun getUserEmail(): String? {
        return prefs.getString(KEY_USER_EMAIL, null)
    }

    fun getAuthToken(): String? {
        return prefs.getString(KEY_AUTH_TOKEN, null)
    }

    fun clearSession() {
        prefs.edit().clear().apply()
    }
}
