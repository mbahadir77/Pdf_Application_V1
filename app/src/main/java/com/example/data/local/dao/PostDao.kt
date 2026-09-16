package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.example.data.local.entity.PostEntity
import com.example.data.local.relation.PostWithAuthor
import kotlinx.coroutines.flow.Flow

/**
 * İlmNet - Room Database Post Erişim Nesnesi (DAO).
 */
@Dao
interface PostDao {
    @Transaction
    @Query("SELECT * FROM posts ORDER BY created_at DESC")
    fun getAllPostsWithAuthor(): Flow<List<PostWithAuthor>>

    @Transaction
    @Query("SELECT * FROM posts WHERE category = :category ORDER BY created_at DESC")
    fun getPostsByCategoryWithAuthor(category: String): Flow<List<PostWithAuthor>>

    @Transaction
    @Query("SELECT * FROM posts WHERE author_name = :authorName ORDER BY created_at DESC")
    fun getPostsByAuthorWithAuthor(authorName: String): Flow<List<PostWithAuthor>>

    @Query("SELECT * FROM posts ORDER BY created_at DESC")
    fun getAllPosts(): Flow<List<PostEntity>>

    @Query("SELECT * FROM posts WHERE category = :category ORDER BY created_at DESC")
    fun getPostsByCategory(category: String): Flow<List<PostEntity>>

    @Query("SELECT * FROM posts WHERE author_name = :authorName ORDER BY created_at DESC")
    fun getPostsByAuthor(authorName: String): Flow<List<PostEntity>>

    @Query("SELECT * FROM posts WHERE id = :id LIMIT 1")
    suspend fun getPostById(id: String): PostEntity?

    @Query("SELECT COUNT(*) FROM posts")
    suspend fun getPostCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPosts(posts: List<PostEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPost(post: PostEntity)

    @Update
    suspend fun updatePost(post: PostEntity)

    @Query("UPDATE posts SET is_liked = :isLiked, like_count = :newLikeCount WHERE id = :postId")
    suspend fun updateLikeStatus(postId: String, isLiked: Boolean, newLikeCount: Int)

    @Query("UPDATE posts SET comment_count = :count WHERE id = :postId")
    suspend fun updateCommentsCount(postId: String, count: Int)

    @Query("DELETE FROM posts WHERE id = :postId")
    suspend fun deletePostById(postId: String)

    @Query("DELETE FROM posts")
    suspend fun clearAllPosts()
}
