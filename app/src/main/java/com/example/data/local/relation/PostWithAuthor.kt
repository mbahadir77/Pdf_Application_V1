package com.example.data.local.relation

import androidx.room.Embedded
import androidx.room.Relation
import com.example.data.local.entity.PostEntity
import com.example.data.local.entity.UserEntity

/**
 * Dinamik Avatar & Yazar Senkronizasyonu İlişkisi (Faz 8).
 * Kullanıcı profil fotoğrafını veya unvanını değiştirdiğinde, Room DB
 * üzerinden bu ilişki otomatik tetiklenir ve eski gönderiler anında güncellenir.
 */
data class PostWithAuthor(
    @Embedded
    val post: PostEntity,

    @Relation(
        parentColumn = "user_id",
        entityColumn = "id"
    )
    val authorUser: UserEntity? = null
) {
    fun toSynchronizedPost(): PostEntity {
        val dynamicAvatar = authorUser?.avatarUrl ?: post.authorAvatarUrl
        val dynamicName = authorUser?.fullName ?: post.authorName
        val dynamicTitle = authorUser?.academicTitle ?: post.authorTitle
        return post.copy(
            authorName = dynamicName,
            authorTitle = dynamicTitle,
            authorAvatarUrl = dynamicAvatar
        )
    }
}
