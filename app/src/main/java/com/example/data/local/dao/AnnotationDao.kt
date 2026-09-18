package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.local.entity.AnnotationEntity

/**
 * İlim Diyârı - S-Pen Mürekkep Katmanı Erişim Nesnesi (FAZ 13).
 */
@Dao
interface AnnotationDao {

    @Query("SELECT * FROM pdf_annotations WHERE post_id = :postId AND page_index = :pageIndex LIMIT 1")
    suspend fun getAnnotation(postId: String, pageIndex: Int): AnnotationEntity?

    @Query("SELECT * FROM pdf_annotations WHERE post_id = :postId")
    suspend fun getAnnotationsForPost(postId: String): List<AnnotationEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAnnotation(annotation: AnnotationEntity)

    @Query("DELETE FROM pdf_annotations WHERE post_id = :postId AND page_index = :pageIndex")
    suspend fun deleteAnnotation(postId: String, pageIndex: Int)

    @Query("DELETE FROM pdf_annotations WHERE post_id = :postId")
    suspend fun deleteAllAnnotationsForPost(postId: String)
}
