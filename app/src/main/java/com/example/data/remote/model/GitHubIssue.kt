package com.example.data.remote.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class GitHubIssueUser(
    val login: String,
    val id: Long,
    @Json(name = "avatar_url")
    val avatarUrl: String? = null
)

@JsonClass(generateAdapter = true)
data class GitHubLabel(
    val id: Long,
    val name: String,
    val color: String? = null
)

@JsonClass(generateAdapter = true)
data class GitHubIssue(
    val id: Long,
    val number: Long,
    val title: String,
    val body: String? = null,
    val user: GitHubIssueUser? = null,
    val state: String? = null,
    val comments: Int = 0,
    @Json(name = "created_at")
    val createdAt: String? = null,
    val labels: List<GitHubLabel>? = null
)

@JsonClass(generateAdapter = true)
data class CreateGitHubIssueRequest(
    val title: String,
    val body: String,
    val labels: List<String>? = null
)

@JsonClass(generateAdapter = true)
data class GitHubIssueComment(
    val id: Long,
    val body: String,
    val user: GitHubIssueUser? = null,
    @Json(name = "created_at")
    val createdAt: String? = null
)

@JsonClass(generateAdapter = true)
data class CreateGitHubCommentRequest(
    val body: String
)
