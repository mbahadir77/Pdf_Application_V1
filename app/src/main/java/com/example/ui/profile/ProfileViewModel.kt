package com.example.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.local.dao.UserDao
import com.example.data.local.entity.UserEntity
import com.example.data.pref.SessionManager
import com.example.data.repository.AuthRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

sealed class ProfileUpdateState {
    object Idle : ProfileUpdateState()
    object Loading : ProfileUpdateState()
    data class Success(val user: UserEntity, val message: String) : ProfileUpdateState()
    data class Error(val message: String) : ProfileUpdateState()
}

/**
 * İlim Diyârı - Profil ve Ayarlar Görünüm Modeli (Emir 3).
 * Avatar ve profil güncelleme sırasında Session ID kaybolsa dahi Room DB ve SessionManager
 * üzerinden çok kademeli güvenli yedekten veri çeker ve Room DB'deki avatar_url alanını
 * doğrudan UPDATE sorgusu ile kalıcı olarak günceller.
 */
class ProfileViewModel(
    private val authRepository: AuthRepository,
    private val userDao: UserDao,
    private val sessionManager: SessionManager
) : ViewModel() {

    private val _updateState = MutableStateFlow<ProfileUpdateState>(ProfileUpdateState.Idle)
    val updateState: StateFlow<ProfileUpdateState> = _updateState.asStateFlow()

    /**
     * Avatar Güncelleme (Emir 3):
     * 1. Room DB ve AuthRepository'den currentUserId değerini güvenli bir şekilde Flow ile al.
     * 2. ID null ise işlemi asla iptal etme; SessionManager'dan yedeğini al.
     * 3. Yine bulunamazsa son giriş yapan kullanıcıyı Room DB'den doğrudan fetch et.
     * 4. Room DB'deki avatar_url alanını UPDATE sorgusu ile KESİN olarak güncelle.
     */
    fun updateAvatar(avatarPath: String) {
        viewModelScope.launch {
            _updateState.value = ProfileUpdateState.Loading
            try {
                // 1. Flow'dan güvenli fetch
                var resolvedUserId: String? = authRepository.currentUserIdFlow.value

                // 2. ID null ise SessionManager yedeği
                if (resolvedUserId.isNullOrBlank()) {
                    resolvedUserId = sessionManager.getUserId()
                }

                // 3. Room DB'den son aktif kullanıcı yedeği
                if (resolvedUserId.isNullOrBlank()) {
                    val lastUser = withContext(Dispatchers.IO) {
                        userDao.getLastLoggedInUserDirect()
                    }
                    resolvedUserId = lastUser?.id
                }

                // 4. Veritabanında kullanıcıyı tespit et
                var existingUser: UserEntity? = null
                if (!resolvedUserId.isNullOrBlank()) {
                    existingUser = withContext(Dispatchers.IO) {
                        userDao.getUserByIdDirect(resolvedUserId)
                    }
                }

                if (existingUser == null) {
                    val email = sessionManager.getUserEmail().orEmpty()
                    if (email.isNotBlank()) {
                        existingUser = withContext(Dispatchers.IO) {
                            userDao.findUserByEmailDirect(email)
                        }
                    }
                }

                if (existingUser != null) {
                    // Room DB'deki avatar_url alanını UPDATE sorgusu ile KESİN olarak güncelle
                    withContext(Dispatchers.IO) {
                        userDao.updateAvatarUrl(existingUser.id, avatarPath)
                    }

                    // SessionManager'a kaydet
                    sessionManager.saveProfile(
                        fullName = existingUser.fullName,
                        academicTitle = existingUser.academicTitle,
                        bio = existingUser.bio,
                        avatarUrl = avatarPath,
                        githubUsername = existingUser.githubUsername
                    )

                    val updatedUser = existingUser.copy(avatarUrl = avatarPath)
                    authRepository.notifyUserUpdated(updatedUser)

                    _updateState.value = ProfileUpdateState.Success(
                        user = updatedUser,
                        message = "Profil fotoğrafı başarıyla güncellendi."
                    )
                } else {
                    // SessionManager'dan yeni nesne oluştur ve kaydet
                    val fallbackUser = sessionManager.toUserEntity()?.copy(avatarUrl = avatarPath)
                        ?: UserEntity(
                            id = resolvedUserId ?: "usr_${System.currentTimeMillis()}",
                            email = sessionManager.getUserEmail() ?: "user@ilimdiyari.org",
                            fullName = sessionManager.getFullName() ?: "Akademik Araştırmacı",
                            avatarUrl = avatarPath
                        )

                    withContext(Dispatchers.IO) {
                        userDao.insertUser(fallbackUser)
                        userDao.updateAvatarUrl(fallbackUser.id, avatarPath)
                    }

                    sessionManager.saveProfile(
                        fullName = fallbackUser.fullName,
                        academicTitle = fallbackUser.academicTitle,
                        bio = fallbackUser.bio,
                        avatarUrl = avatarPath,
                        githubUsername = fallbackUser.githubUsername
                    )

                    authRepository.notifyUserUpdated(fallbackUser)
                    _updateState.value = ProfileUpdateState.Success(
                        user = fallbackUser,
                        message = "Profil fotoğrafı kaydedildi."
                    )
                }
            } catch (e: Exception) {
                _updateState.value = ProfileUpdateState.Error(
                    e.localizedMessage ?: "Profil fotoğrafı güncellenemedi."
                )
            }
        }
    }

    /**
     * Tüm profil alanlarını güncelleme
     */
    fun updateProfile(
        fullName: String,
        academicTitle: String?,
        bio: String?,
        avatarUrl: String?,
        githubUsername: String? = null
    ) {
        viewModelScope.launch {
            _updateState.value = ProfileUpdateState.Loading
            try {
                var safeUserId: String? = authRepository.currentUserIdFlow.value
                if (safeUserId.isNullOrBlank()) {
                    safeUserId = sessionManager.getUserId()
                }
                if (safeUserId.isNullOrBlank()) {
                    val lastUser = withContext(Dispatchers.IO) {
                        userDao.getLastLoggedInUserDirect()
                    }
                    safeUserId = lastUser?.id
                }

                val result = authRepository.updateProfile(
                    userId = safeUserId.orEmpty(),
                    fullName = fullName,
                    academicTitle = academicTitle,
                    bio = bio,
                    avatarUrl = avatarUrl,
                    githubUsername = githubUsername
                )

                result.onSuccess { user ->
                    _updateState.value = ProfileUpdateState.Success(user, "Profil güncellendi.")
                }.onFailure { ex ->
                    _updateState.value = ProfileUpdateState.Error(ex.localizedMessage ?: "Profil güncellenemedi.")
                }
            } catch (e: Exception) {
                _updateState.value = ProfileUpdateState.Error(e.localizedMessage ?: "Profil güncellenemedi.")
            }
        }
    }

    fun resetState() {
        _updateState.value = ProfileUpdateState.Idle
    }

    class Factory(
        private val authRepository: AuthRepository,
        private val userDao: UserDao,
        private val sessionManager: SessionManager
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(ProfileViewModel::class.java)) {
                return ProfileViewModel(authRepository, userDao, sessionManager) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
    }
}
