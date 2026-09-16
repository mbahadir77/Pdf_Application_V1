package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.local.entity.FollowEntity
import kotlinx.coroutines.flow.Flow

/**
 * İlmNet - Takipçi ve Takip Edilenler DAO (Social Graph Data Access).
 */
@Dao
interface FollowDao {
    @Query("SELECT EXISTS(SELECT 1 FROM follows WHERE follower_id = :followerId AND followed_author = :authorName)")
    fun isFollowing(followerId: String, authorName: String): Flow<Boolean>

    @Query("SELECT EXISTS(SELECT 1 FROM follows WHERE follower_id = :followerId AND followed_author = :authorName)")
    suspend fun isFollowingSync(followerId: String, authorName: String): Boolean

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun follow(follow: FollowEntity)

    @Query("DELETE FROM follows WHERE follower_id = :followerId AND followed_author = :authorName")
    suspend fun unfollow(followerId: String, authorName: String)

    @Query("SELECT COUNT(*) FROM follows WHERE followed_author = :authorName")
    fun getFollowerCount(authorName: String): Flow<Int>

    @Query("SELECT COUNT(*) FROM follows WHERE follower_id = :followerId")
    fun getFollowingCount(followerId: String): Flow<Int>

    @Query("SELECT followed_author FROM follows WHERE follower_id = :followerId")
    fun getFollowedAuthors(followerId: String): Flow<List<String>>
}
