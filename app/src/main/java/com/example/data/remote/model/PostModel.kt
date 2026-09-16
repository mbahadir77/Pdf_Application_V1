package com.example.data.remote.model

import com.example.data.local.entity.PostEntity

/**
 * İlmNet - Post Domain Modeli.
 */
data class PostModel(
    val id: String,
    val title: String,
    val description: String,
    val authorName: String,
    val authorTitle: String,
    val authorAvatarUrl: String? = null,
    val category: String,
    val pdfUrl: String,
    val pdfSize: String = "2.4 MB",
    val coverImageUrl: String? = null,
    val likeCount: Int = 0,
    val commentCount: Int = 0,
    val isLiked: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

fun PostEntity.toModel(): PostModel = PostModel(
    id = id,
    title = title,
    description = description,
    authorName = authorName,
    authorTitle = authorTitle,
    authorAvatarUrl = authorAvatarUrl,
    category = category,
    pdfUrl = pdfUrl,
    pdfSize = pdfSize,
    coverImageUrl = coverImageUrl,
    likeCount = likeCount,
    commentCount = commentCount,
    isLiked = isLiked,
    createdAt = createdAt
)

fun PostModel.toEntity(githubIssueId: Long? = null): PostEntity = PostEntity(
    id = id,
    title = title,
    description = description,
    authorName = authorName,
    authorTitle = authorTitle,
    authorAvatarUrl = authorAvatarUrl,
    category = category,
    pdfUrl = pdfUrl,
    pdfSize = pdfSize,
    coverImageUrl = coverImageUrl,
    likeCount = likeCount,
    commentCount = commentCount,
    isLiked = isLiked,
    createdAt = createdAt,
    githubIssueId = githubIssueId
)
