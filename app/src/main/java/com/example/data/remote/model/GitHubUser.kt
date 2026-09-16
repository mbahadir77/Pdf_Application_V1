package com.example.data.remote.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class GitHubUser(
    val login: String,
    val id: Long,
    @Json(name = "avatar_url")
    val avatarUrl: String? = null,
    val name: String? = null,
    val bio: String? = null,
    @Json(name = "public_repos")
    val publicRepos: Int? = null
)
