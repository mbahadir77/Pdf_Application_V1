package com.example.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity

/**
 * İlmNet - Sosyal Ağ Takipçi İlişkisi Varlığı (Social Graph).
 * Kullanıcıların akademik araştırmacıları, hocaları ve yazarları takip etmesini sağlar.
 */
@Entity(tableName = "follows", primaryKeys = ["follower_id", "followed_author"])
data class FollowEntity(
    @ColumnInfo(name = "follower_id")
    val followerId: String,
    @ColumnInfo(name = "followed_author")
    val followedAuthor: String,
    @ColumnInfo(name = "followed_at")
    val followedAt: Long = System.currentTimeMillis()
)
