package com.example.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * İlmNet - Room Database Post Varlığı (Offline-First).
 * Araştırmacıların paylaştığı akademik PDF makale ve risaleleri yerel SQLite/Room tablosunda saklar.
 */
@Entity(tableName = "posts")
data class PostEntity(
    @PrimaryKey
    val id: String,
    @ColumnInfo(name = "user_id")
    val userId: String? = null,
    val title: String,
    val description: String,
    @ColumnInfo(name = "author_name")
    val authorName: String,
    @ColumnInfo(name = "author_title")
    val authorTitle: String,
    @ColumnInfo(name = "author_avatar_url")
    val authorAvatarUrl: String? = null,
    val category: String,
    @ColumnInfo(name = "pdf_url")
    val pdfUrl: String,
    @ColumnInfo(name = "pdf_size")
    val pdfSize: String = "2.4 MB",
    @ColumnInfo(name = "cover_image_url")
    val coverImageUrl: String? = null,
    @ColumnInfo(name = "like_count")
    val likeCount: Int = 0,
    @ColumnInfo(name = "comment_count")
    val commentCount: Int = 0,
    @ColumnInfo(name = "is_liked")
    val isLiked: Boolean = false,
    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "github_issue_id")
    val githubIssueId: Long? = null
)
