package com.example.data.pref

import android.content.Context
import android.content.SharedPreferences
import com.example.data.local.entity.UserEntity

/**
 * İlim Diyârı Kalıcı Oturum Yöneticisi (SharedPreferences).
 * Kullanıcı kimlik doğrulama oturumunu ve profil verilerini kalıcı olarak saklar.
 */
class SessionManager(context: Context) {

    private val prefs: SharedPreferences = context.applicationContext.getSharedPreferences(
        PREF_NAME,
        Context.MODE_PRIVATE
    )

    companion object {
        private const val PREF_NAME = "ilim_diyari_session_prefs"
        private const val KEY_IS_LOGGED_IN = "key_is_logged_in"
        private const val KEY_USER_ID = "key_user_id"
        private const val KEY_USER_EMAIL = "key_user_email"
        private const val KEY_AUTH_TOKEN = "key_auth_token"
        private const val KEY_FULL_NAME = "key_full_name"
        private const val KEY_ACADEMIC_TITLE = "key_academic_title"
        private const val KEY_BIO = "key_bio"
        private const val KEY_AVATAR_URL = "key_avatar_url"
        private const val KEY_GITHUB_USERNAME = "key_github_username"
        private const val KEY_LAST_ACTIVE_TIME = "key_last_active_time"
    }

    fun updateLastActiveTime() {
        prefs.edit().putLong(KEY_LAST_ACTIVE_TIME, System.currentTimeMillis()).apply()
    }

    fun getLastActiveTime(): Long {
        val time = prefs.getLong(KEY_LAST_ACTIVE_TIME, 0L)
        return if (time == 0L) System.currentTimeMillis() else time
    }

    fun saveSession(userId: String, email: String, token: String? = null) {
        prefs.edit()
            .putBoolean(KEY_IS_LOGGED_IN, true)
            .putString(KEY_USER_ID, userId)
            .putString(KEY_USER_EMAIL, email)
            .putString(KEY_AUTH_TOKEN, token)
            .commit()
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
            .commit()
    }

    fun getFullName(): String? = prefs.getString(KEY_FULL_NAME, null)
    fun getAcademicTitle(): String? = prefs.getString(KEY_ACADEMIC_TITLE, null)
    fun getBio(): String? = prefs.getString(KEY_BIO, null)
    fun getAvatarUrl(): String? = prefs.getString(KEY_AVATAR_URL, null)
    fun getGithubUsername(): String? = prefs.getString(KEY_GITHUB_USERNAME, null)

    fun isLoggedIn(): Boolean {
        return prefs.getBoolean(KEY_IS_LOGGED_IN, false) && !getUserId().isNullOrBlank()
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
        prefs.edit().clear().commit()
    }

    fun toUserEntity(): UserEntity? {
        val uid = getUserId() ?: return null
        return UserEntity(
            id = uid,
            email = getUserEmail().orEmpty(),
            fullName = getFullName() ?: "Araştırmacı",
            academicTitle = getAcademicTitle(),
            bio = getBio(),
            avatarUrl = getAvatarUrl(),
            githubUsername = getGithubUsername(),
            token = getAuthToken().orEmpty(),
            createdAt = System.currentTimeMillis(),
            lastLoginAt = System.currentTimeMillis()
        )
    }
}

