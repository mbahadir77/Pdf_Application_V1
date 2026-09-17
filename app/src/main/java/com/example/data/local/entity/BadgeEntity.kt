package com.example.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * İlim Diyârı - Akademik Rozet Varlığı (Room Entity).
 * is_notified alanı sayesinde kullanıcının daha önce bildirimini aldığı
 * rozetler için mükerrer (spam) bildirim gitmesi engellenir.
 */
@Entity(tableName = "badges")
data class BadgeEntity(
    @PrimaryKey
    val id: String, // "${userId}_${category}"
    @ColumnInfo(name = "user_id")
    val userId: String,
    val category: String,
    val level: Int,
    @ColumnInfo(name = "rank_title")
    val rankTitle: String,
    @ColumnInfo(name = "is_unlocked")
    val isUnlocked: Boolean,
    @ColumnInfo(name = "is_notified")
    val isNotified: Boolean = false,
    @ColumnInfo(name = "updated_at")
    val updatedAt: Long = System.currentTimeMillis()
)
