package com.example.ui.splash

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.BuildConfig
import com.example.data.repository.AuthRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

sealed class SplashNavigationEvent {
    object NavigateToAuth : SplashNavigationEvent()
    object NavigateToDashboard : SplashNavigationEvent()
}

sealed class SplashNavigationState {
    object Idle : SplashNavigationState()
    object NavigateToAuth : SplashNavigationState()
    object NavigateToDashboard : SplashNavigationState()
}

data class VersionUpdateInfo(
    val hasUpdate: Boolean,
    val latestVersion: String,
    val currentVersion: String,
    val releaseUrl: String,
    val releaseNotes: String
)

class SplashViewModel(
    private val repository: AuthRepository
) : ViewModel() {

    private val _navigationState = MutableStateFlow<SplashNavigationState>(SplashNavigationState.Idle)
    val navigationState: StateFlow<SplashNavigationState> = _navigationState.asStateFlow()

    private val _navigationEvent = MutableSharedFlow<SplashNavigationEvent>(replay = 1)
    val navigationEvent: SharedFlow<SplashNavigationEvent> = _navigationEvent.asSharedFlow()

    private val _updateInfo = MutableStateFlow<VersionUpdateInfo?>(null)
    val updateInfo: StateFlow<VersionUpdateInfo?> = _updateInfo.asStateFlow()

    init {
        checkSession()
        checkForGitHubReleaseUpdate()
    }

    fun isUserLoggedIn(): Boolean = repository.isLoggedIn()

    private fun checkSession() {
        viewModelScope.launch {
            // Splash ekranı altın logosunu ve degrade zeminini zarif bir süre sunar
            delay(1200)
            if (repository.isLoggedIn()) {
                _navigationState.value = SplashNavigationState.NavigateToDashboard
                _navigationEvent.emit(SplashNavigationEvent.NavigateToDashboard)
            } else {
                _navigationState.value = SplashNavigationState.NavigateToAuth
                _navigationEvent.emit(SplashNavigationEvent.NavigateToAuth)
            }
        }
    }

    /**
     * FAZ 7: Arka Planda GitHub Reposundaki En Son Release Versiyonunu Kontrol Eder.
     * Cihazdaki versiyondan (BuildConfig.VERSION_NAME) yeniyse _updateInfo yayar.
     */
    fun checkForGitHubReleaseUpdate() {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                try {
                    val currentVersion = BuildConfig.VERSION_NAME
                    val githubApiUrl = "https://api.github.com/repos/mbahadir77/IlmNet/releases/latest"
                    val url = URL(githubApiUrl)
                    val connection = (url.openConnection() as HttpURLConnection).apply {
                        requestMethod = "GET"
                        connectTimeout = 3000
                        readTimeout = 3000
                        setRequestProperty("Accept", "application/vnd.github.v3+json")
                        setRequestProperty("User-Agent", "IlmNet-Android")
                    }

                    if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                        val reader = BufferedReader(InputStreamReader(connection.inputStream))
                        val response = reader.readText()
                        reader.close()

                        val json = JSONObject(response)
                        val tagName = json.optString("tag_name", "").removePrefix("v").trim()
                        val htmlUrl = json.optString("html_url", "https://github.com/mbahadir77/IlmNet/releases")
                        val body = json.optString("body", "Yeni özellikler ve performans iyileştirmeleri.")

                        if (isNewerVersion(tagName, currentVersion)) {
                            _updateInfo.value = VersionUpdateInfo(
                                hasUpdate = true,
                                latestVersion = "v$tagName",
                                currentVersion = "v$currentVersion",
                                releaseUrl = htmlUrl,
                                releaseNotes = body
                            )
                        }
                    }
                } catch (_: Exception) {
                    // Ağ yokluğunda veya sınır aşımında sessizce devam eder
                }
            }
        }
    }

    private fun isNewerVersion(remote: String, local: String): Boolean {
        if (remote.isBlank() || local.isBlank()) return false
        val remoteParts = remote.split(".").mapNotNull { it.filter { c -> c.isDigit() }.toIntOrNull() }
        val localParts = local.split(".").mapNotNull { it.filter { c -> c.isDigit() }.toIntOrNull() }
        val maxLen = maxOf(remoteParts.size, localParts.size)
        for (i in 0 until maxLen) {
            val r = remoteParts.getOrElse(i) { 0 }
            val l = localParts.getOrElse(i) { 0 }
            if (r > l) return true
            if (r < l) return false
        }
        return false
    }

    class Factory(private val repository: AuthRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(SplashViewModel::class.java)) {
                return SplashViewModel(repository) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
