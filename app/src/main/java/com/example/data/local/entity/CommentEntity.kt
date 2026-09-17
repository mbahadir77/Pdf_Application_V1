package com.example.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * İlim Diyârı - Akademik Müzakere Yorum Varlığı (Room Entity).
 * Yorumların çevrimdışı öncelikli saklanması ve hem yerelden hem de
 * GitHub Issues senkronizasyonundan silinebilmesi için kullanılır.
 */
@Entity(tableName = "comments")
data class CommentEntity(
    @PrimaryKey
    val id: Long,
    @ColumnInfo(name = "post_id")
    val postId: String,
    @ColumnInfo(name = "user_id")
    val userId: String?,
    @ColumnInfo(name = "author_name")
    val authorName: String,
    @ColumnInfo(name = "avatar_url")
    val avatarUrl: String?,
    val body: String,
    @ColumnInfo(name = "date_text")
    val dateText: String,
    @ColumnInfo(name = "github_comment_id")
    val githubCommentId: Long? = null,
    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis()
)
