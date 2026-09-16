package com.example.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * İlmNet Akademik PDF Sosyal Ağı - Çevrimdışı (Offline-First) Kullanıcı Varlığı.
 */
@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey
    val id: String,
    val email: String,
    @ColumnInfo(name = "full_name")
    val fullName: String,
    @ColumnInfo(name = "github_username")
    val githubUsername: String? = null,
    @ColumnInfo(name = "academic_title")
    val academicTitle: String? = null,
    @ColumnInfo(name = "bio")
    val bio: String? = null,
    @ColumnInfo(name = "avatar_url")
    val avatarUrl: String? = null,
    val token: String? = null,
    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "last_login_at")
    val lastLoginAt: Long = System.currentTimeMillis()
)
