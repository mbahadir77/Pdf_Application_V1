package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.local.entity.CommentEntity
import kotlinx.coroutines.flow.Flow

/**
 * İlim Diyârı - Yorum DAO (Veri Erişim Nesnesi).
 */
@Dao
interface CommentDao {

    @Query("SELECT * FROM comments WHERE post_id = :postId ORDER BY created_at ASC")
    fun getCommentsForPost(postId: String): Flow<List<CommentEntity>>

    @Query("SELECT * FROM comments WHERE post_id = :postId ORDER BY created_at ASC")
    suspend fun getCommentsForPostDirect(postId: String): List<CommentEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertComment(comment: CommentEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertComments(comments: List<CommentEntity>)

    @Query("DELETE FROM comments WHERE id = :commentId")
    suspend fun deleteComment(commentId: Long)

    @Query("DELETE FROM comments WHERE post_id = :postId")
    suspend fun deleteCommentsForPost(postId: String)
}
