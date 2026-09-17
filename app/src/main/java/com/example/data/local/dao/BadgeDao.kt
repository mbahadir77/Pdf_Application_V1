package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.local.entity.BadgeEntity
import kotlinx.coroutines.flow.Flow

/**
 * İlim Diyârı - Rozet DAO (Veri Erişim Nesnesi).
 */
@Dao
interface BadgeDao {

    @Query("SELECT * FROM badges WHERE user_id = :userId")
    fun getAllBadgesFlow(userId: String): Flow<List<BadgeEntity>>

    @Query("SELECT * FROM badges WHERE user_id = :userId")
    suspend fun getAllBadgesDirect(userId: String): List<BadgeEntity>

    @Query("SELECT * FROM badges WHERE user_id = :userId AND category = :category LIMIT 1")
    suspend fun getBadge(userId: String, category: String): BadgeEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateBadge(badge: BadgeEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateBadges(badges: List<BadgeEntity>)

    @Query("UPDATE badges SET is_notified = 1 WHERE id = :badgeId")
    suspend fun markBadgeAsNotified(badgeId: String)
}
