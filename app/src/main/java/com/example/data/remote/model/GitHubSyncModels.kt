package com.example.data.remote.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * İlim Diyârı - GitHub Global Senkronizasyon Veri Modelleri (Faz 13).
 * Farklı cihazlar ve kullanıcılar arasında ortak JSON veritabanı senkronizasyonu sağlar.
 */
@JsonClass(generateAdapter = true)
data class GitHubGistFile(
    val filename: String? = null,
    val type: String? = null,
    val language: String? = null,
    @Json(name = "raw_url") val rawUrl: String? = null,
    val size: Int? = null,
    val content: String? = null
)

@JsonClass(generateAdapter = true)
data class GitHubGistResponse(
    val id: String,
    val description: String? = null,
    val public: Boolean = true,
    val files: Map<String, GitHubGistFile>? = null,
    @Json(name = "updated_at") val updatedAt: String? = null
)

@JsonClass(generateAdapter = true)
data class UpdateGistRequest(
    val description: String = "İlim Diyârı Akademik Eserler Global Havuzu",
    val files: Map<String, GitHubGistFile>
)

@JsonClass(generateAdapter = true)
data class CreateGistRequest(
    val description: String = "İlim Diyârı Akademik Eserler Global Havuzu",
    val public: Boolean = true,
    val files: Map<String, GitHubGistFile>
)

@JsonClass(generateAdapter = true)
data class GitHubRepoContentResponse(
    val name: String? = null,
    val path: String? = null,
    val sha: String? = null,
    val size: Int? = null,
    val content: String? = null,
    val encoding: String? = null,
    @Json(name = "download_url") val downloadUrl: String? = null
)

@JsonClass(generateAdapter = true)
data class UpdateRepoContentRequest(
    val message: String,
    val content: String, // Base64 encoded JSON
    val sha: String? = null
)
