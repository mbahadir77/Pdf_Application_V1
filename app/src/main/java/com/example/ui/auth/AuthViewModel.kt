package com.example.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.local.entity.UserEntity
import com.example.data.repository.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed class AuthUiState {
    object Idle : AuthUiState()
    object Loading : AuthUiState()
    data class Success(val user: UserEntity) : AuthUiState()
    data class Error(val message: String) : AuthUiState()
}

sealed class EditProfileUiState {
    object Idle : EditProfileUiState()
    object Loading : EditProfileUiState()
    data class Success(val user: UserEntity) : EditProfileUiState()
    data class Error(val message: String) : EditProfileUiState()
}

enum class AuthMode {
    LOGIN,
    REGISTER
}

class AuthViewModel(
    private val repository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<AuthUiState>(AuthUiState.Idle)
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    private val _editProfileState = MutableStateFlow<EditProfileUiState>(EditProfileUiState.Idle)
    val editProfileState: StateFlow<EditProfileUiState> = _editProfileState.asStateFlow()

    private val _authMode = MutableStateFlow(AuthMode.LOGIN)
    val authMode: StateFlow<AuthMode> = _authMode.asStateFlow()

    val currentUser: StateFlow<UserEntity?> = repository.getCurrentUserFlow()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    fun setAuthMode(mode: AuthMode) {
        _authMode.value = mode
        _uiState.value = AuthUiState.Idle
    }

    fun isUserLoggedIn(): Boolean = repository.isLoggedIn()

    fun login(email: String, pass: String) {
        if (email.isBlank() || pass.isBlank()) {
            _uiState.value = AuthUiState.Error("Lütfen e-posta ve şifrenizi giriniz.")
            return
        }
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email.trim()).matches()) {
            _uiState.value = AuthUiState.Error("Lütfen geçerli bir e-posta adresi giriniz.")
            return
        }

        viewModelScope.launch {
            _uiState.value = AuthUiState.Loading
            val result = repository.login(email, pass)
            result.onSuccess { user ->
                _uiState.value = AuthUiState.Success(user)
            }.onFailure { ex ->
                _uiState.value = AuthUiState.Error(ex.localizedMessage ?: "Giriş yapılamadı.")
            }
        }
    }

    fun register(email: String, fullName: String, pass: String, githubUsername: String? = null) {
        if (email.isBlank() || fullName.isBlank() || pass.isBlank()) {
            _uiState.value = AuthUiState.Error("Lütfen ad soyad, e-posta ve şifre alanlarını doldurunuz.")
            return
        }
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email.trim()).matches()) {
            _uiState.value = AuthUiState.Error("Lütfen geçerli bir e-posta adresi giriniz.")
            return
        }
        if (pass.length < 6) {
            _uiState.value = AuthUiState.Error("Şifre en az 6 karakter olmalıdır.")
            return
        }

        viewModelScope.launch {
            _uiState.value = AuthUiState.Loading
            val result = repository.register(email, fullName, pass, githubUsername)
            result.onSuccess { user ->
                _uiState.value = AuthUiState.Success(user)
            }.onFailure { ex ->
                _uiState.value = AuthUiState.Error(ex.localizedMessage ?: "Kayıt işlemi başarısız.")
            }
        }
    }

    fun updateAvatar(avatarUrl: String?) {
        viewModelScope.launch {
            _editProfileState.value = EditProfileUiState.Loading
            val result = repository.updateAvatar(avatarUrl)
            result.onSuccess { updatedUser ->
                _editProfileState.value = EditProfileUiState.Success(updatedUser)
            }.onFailure { ex ->
                _editProfileState.value = EditProfileUiState.Error(ex.localizedMessage ?: "Profil fotoğrafı güncellenemedi.")
            }
        }
    }

    fun updateProfile(
        fullName: String,
        academicTitle: String?,
        bio: String?,
        avatarUrl: String?,
        githubUsername: String? = null
    ) {
        val safeUserId = currentUser.value?.id ?: repository.currentUserIdFlow.value.orEmpty()
        if (fullName.isBlank()) {
            _editProfileState.value = EditProfileUiState.Error("Ad ve soyad alanı boş bırakılamaz.")
            return
        }
        viewModelScope.launch {
            _editProfileState.value = EditProfileUiState.Loading
            val result = repository.updateProfile(
                userId = safeUserId,
                fullName = fullName,
                academicTitle = academicTitle,
                bio = bio,
                avatarUrl = avatarUrl,
                githubUsername = githubUsername
            )
            result.onSuccess { updatedUser ->
                _editProfileState.value = EditProfileUiState.Success(updatedUser)
            }.onFailure { ex ->
                _editProfileState.value = EditProfileUiState.Error(ex.localizedMessage ?: "Profil güncellenemedi.")
            }
        }
    }

    fun resetEditProfileState() {
        _editProfileState.value = EditProfileUiState.Idle
    }

    fun logout() {
        viewModelScope.launch {
            repository.logout()
            _uiState.value = AuthUiState.Idle
        }
    }

    fun resetState() {
        _uiState.value = AuthUiState.Idle
    }

    class Factory(private val repository: AuthRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(AuthViewModel::class.java)) {
                return AuthViewModel(repository) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
