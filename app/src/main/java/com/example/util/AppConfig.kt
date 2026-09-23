package com.example.util

/**
 * İlim Diyârı - Merkezi Uygulama ve GitHub Yapılandırma Sabitleri.
 * GitHub Repo'ya uzaktan doğrudan academic_posts.json dosyası yazma (PUT)
 * ve Issues açma (POST) işlemleri için kimlik doğrulama ayarlarını barındırır.
 */
object AppConfig {
    /**
     * GitHub Personal Access Token (PAT).
     * GitHub Repository üzerindeki academic_posts.json dosyasını güncellemek ve
     * dışarıdan veri yükleyebilmek için buraya geçerli bir token (ghp_... veya github_pat_...) giriniz.
     */
    const val GITHUB_PAT = "BURAYA_TOKEN_GELECEK"

    const val GITHUB_OWNER = "mbahadir77"
    const val GITHUB_REPO = "Pdf_Application_V1"
    const val POSTS_FILE_PATH = "academic_posts.json"

    /**
     * GitHub API istekleri için Authorization başlığını üretir.
     */
    fun getAuthHeader(sessionToken: String? = null): String {
        val token = when {
            GITHUB_PAT.isNotBlank() && GITHUB_PAT != "BURAYA_TOKEN_GELECEK" -> GITHUB_PAT.trim()
            !sessionToken.isNullOrBlank() -> sessionToken.trim()
            else -> GITHUB_PAT.trim()
        }
        return if (token.startsWith("Bearer ", ignoreCase = true) || token.startsWith("token ", ignoreCase = true)) {
            token
        } else {
            "Bearer $token"
        }
    }

    /**
     * Token'ın girilip girilmediğini tespit eder.
     */
    fun isConfigured(sessionToken: String? = null): Boolean {
        if (GITHUB_PAT.isNotBlank() && GITHUB_PAT != "BURAYA_TOKEN_GELECEK") return true
        if (!sessionToken.isNullOrBlank()) return true
        return false
    }
}
