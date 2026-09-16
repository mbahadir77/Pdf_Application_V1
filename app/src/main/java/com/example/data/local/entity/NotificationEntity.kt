package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * İlmNet - Bildirim Geçmişi Varlığı (FAZ 8).
 * Sistem, motivasyon, müzakere ve rozet bildirimlerini yerel Room DB'de depolar.
 */
@Entity(tableName = "notifications")
data class NotificationEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val message: String,
    val type: String = "INFO", // INFO, MOTIVATION, BADGE, COMMENT, SOCIAL
    val timestamp: Long = System.currentTimeMillis(),
    val isRead: Boolean = false,
    val targetPostId: String? = null
)
