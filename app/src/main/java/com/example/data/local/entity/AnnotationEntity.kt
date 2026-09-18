package com.example.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity

/**
 * İlim Diyârı - S-Pen Mürekkep & Not Katmanı Varlığı (FAZ 13).
 * Kullanıcının her bir PDF sayfası üzerine aldığı el yazısı ve şerhleri
 * yerel diskteki PNG dosyası yoluyla eşleştirir.
 */
@Entity(
    tableName = "pdf_annotations",
    primaryKeys = ["post_id", "page_index"]
)
data class AnnotationEntity(
    @ColumnInfo(name = "post_id")
    val postId: String,
    @ColumnInfo(name = "page_index")
    val pageIndex: Int,
    @ColumnInfo(name = "image_path")
    val imagePath: String,
    @ColumnInfo(name = "updated_at")
    val updatedAt: Long = System.currentTimeMillis()
)
