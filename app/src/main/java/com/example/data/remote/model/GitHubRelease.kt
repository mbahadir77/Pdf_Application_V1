package com.example.data.remote.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class GitHubRelease(
    val id: Long,
    @Json(name = "tag_name")
    val tagName: String,
    val name: String? = null,
    val body: String? = null,
    @Json(name = "created_at")
    val createdAt: String? = null
)
