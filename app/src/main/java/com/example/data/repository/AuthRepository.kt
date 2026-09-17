package com.example.data.repository

import com.example.data.local.dao.UserDao
import com.example.data.local.entity.UserEntity
import com.example.data.pref.SessionManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.util.UUID

/**
 * İlim Diyârı Kimlik Doğrulama ve Kullanıcı Deposu (Offline-First).
 * Room DB ve SharedPreferences ile entegre çalışır.
 */
class AuthRepository(
    private val userDao: UserDao,
    private val sessionManager: SessionManager
) {
    private val _currentUserIdFlow = MutableStateFlow<String?>(sessionManager.getUserId())

    fun isLoggedIn(): Boolean = sessionManager.isLoggedIn()

    fun getCurrentUserFlow(): Flow<UserEntity?> {
        return _currentUserIdFlow.flatMapLatest { id ->
            if (id != null) {
                userDao.getUserById(id).map { dbUser ->
                    dbUser ?: sessionManager.toUserEntity()
                }
            } else {
                flowOf(sessionManager.toUserEntity())
            }
        }
    }

    suspend fun login(email: String, password: String): Result<UserEntity> = withContext(Dispatchers.IO) {
        val normalizedEmail = email.trim().lowercase()
        val existingUser = userDao.findUserByEmailDirect(normalizedEmail)
        if (existingUser != null) {
            val updatedUser = existingUser.copy(lastLoginAt = System.currentTimeMillis())
            userDao.updateUser(updatedUser)
            sessionManager.saveSession(updatedUser.id, updatedUser.email, updatedUser.token)
            sessionManager.saveProfile(
                fullName = updatedUser.fullName,
                academicTitle = updatedUser.academicTitle,
                bio = updatedUser.bio,
                avatarUrl = updatedUser.avatarUrl,
                githubUsername = updatedUser.githubUsername
            )
            _currentUserIdFlow.value = updatedUser.id
            Result.success(updatedUser)
        } else {
            Result.failure(Exception("Kullanıcı kaydı bulunamadı. Lütfen önce 'Kayıt Ol' sekmesinden akademik hesap oluşturunuz."))
        }
    }

    suspend fun register(
        email: String,
        fullName: String,
        password: String,
        githubUsername: String? = null
    ): Result<UserEntity> = withContext(Dispatchers.IO) {
        val normalizedEmail = email.trim().lowercase()
        val existing = userDao.findUserByEmailDirect(normalizedEmail)
        if (existing != null) {
            return@withContext Result.failure(Exception("Bu e-posta adresiyle zaten kayıtlı bir akademik hesap mevcut."))
        }

        val newUser = UserEntity(
            id = UUID.randomUUID().toString(),
            email = normalizedEmail,
            fullName = fullName.trim(),
            githubUsername = githubUsername?.trim()?.ifEmpty { null },
            avatarUrl = null,
            token = "academic_tok_${UUID.randomUUID()}",
            createdAt = System.currentTimeMillis(),
            lastLoginAt = System.currentTimeMillis()
        )

        userDao.insertUser(newUser)
        sessionManager.saveSession(newUser.id, newUser.email, newUser.token)
        sessionManager.saveProfile(
            fullName = newUser.fullName,
            academicTitle = null,
            bio = null,
            avatarUrl = null,
            githubUsername = null
        )
        _currentUserIdFlow.value = newUser.id
        Result.success(newUser)
    }

    suspend fun updateProfile(
        userId: String,
        fullName: String,
        academicTitle: String?,
        bio: String?,
        avatarUrl: String?,
        githubUsername: String? = null
    ): Result<UserEntity> = withContext(Dispatchers.IO) {
        val user = userDao.getUserByIdDirect(userId)
            ?: userDao.findUserByEmailDirect(sessionManager.getUserEmail().orEmpty())
            ?: return@withContext Result.failure(Exception("Kullanıcı kaydı bulunamadı."))

        val updated = user.copy(
            fullName = fullName.trim(),
            academicTitle = academicTitle?.trim()?.ifEmpty { null },
            bio = bio?.trim()?.ifEmpty { null },
            avatarUrl = avatarUrl ?: user.avatarUrl,
            githubUsername = githubUsername?.trim()?.ifEmpty { null },
            lastLoginAt = System.currentTimeMillis()
        )
        userDao.updateUser(updated)
        sessionManager.saveProfile(
            fullName = updated.fullName,
            academicTitle = updated.academicTitle,
            bio = updated.bio,
            avatarUrl = updated.avatarUrl,
            githubUsername = updated.githubUsername
        )
        _currentUserIdFlow.value = updated.id
        Result.success(updated)
    }

    suspend fun logout() = withContext(Dispatchers.IO) {
        sessionManager.clearSession()
        _currentUserIdFlow.value = null
    }
}
