package com.example.data.repository

import com.example.data.local.dao.PostDao
import com.example.data.local.entity.PostEntity
import com.example.data.remote.GitHubService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.util.UUID

/**
 * İlmNet Akış Deposu (Offline-First Hibrit & Dinamik Avatar Senkronizasyonu - FAZ 8).
 * - İlk olarak yerel Room Database'deki dinamik UserEntity ilişkili (PostWithAuthor) verileri sunar.
 * - Kullanıcı profil fotoğrafını veya unvanını değiştirdiğinde, Room Flow ile anında tüm eski gönderilere yansır.
 * - Sahte / Dummy post üretimi kaldırılmıştır; tamamen gerçek kullanıcı ve GitHub verileri kullanılır.
 */
class FeedRepository(
    private val postDao: PostDao,
    private val gitHubService: GitHubService
) {

    /**
     * Çevrimdışı öncelikli akış (Room DB Flow - Dinamik Avatar & Yazar Senkronizasyonlu).
     */
    fun getPostsFlow(): Flow<List<PostEntity>> {
        return postDao.getAllPostsWithAuthor().map { list ->
            list.map { it.toSynchronizedPost() }
        }
    }

    /**
     * Kategoriye göre filtrelenmiş akış (Dinamik Avatar Senkronizasyonlu).
     */
    fun getPostsByCategoryFlow(category: String): Flow<List<PostEntity>> {
        return if (category == "Tümü") {
            postDao.getAllPostsWithAuthor().map { list ->
                list.map { it.toSynchronizedPost() }
            }
        } else {
            postDao.getPostsByCategoryWithAuthor(category).map { list ->
                list.map { it.toSynchronizedPost() }
            }
        }
    }

    /**
     * Arka Planda GitHub API ile Senkronizasyon (Ağ Mantığı).
     * GitHub repository Issues listesinden akademik post havuzunu çeker.
     * Tüm kullanıcıların paylaştığı PDF'ler (farklı cihazlar/katılımcılar dahil)
     * eksiksiz olarak Room DB'ye aktarılır ve ana akışta tarihe göre en yeni en üstte listelenir.
     */
    suspend fun syncWithGitHub(
        owner: String = "mbahadir77",
        repo: String = "Pdf_Application_V1"
    ): Result<Int> = withContext(Dispatchers.IO) {
        try {
            var response = gitHubService.getRepoIssues(owner, repo)
            if (!response.isSuccessful && owner != "ilmnet") {
                // Fallback repo denemesi
                response = gitHubService.getRepoIssues("ilmnet", "academic-papers")
            }
            if (response.isSuccessful) {
                val issues = response.body().orEmpty()
                if (issues.isNotEmpty()) {
                    val entities = issues.map { issue ->
                        val category = issue.labels?.firstOrNull()?.name ?: "Genel"
                        val issueUserId = issue.user?.id?.toString() ?: "gh_${issue.user?.login ?: "user"}"
                        PostEntity(
                            id = "gh_issue_${issue.number}",
                            userId = issueUserId,
                            title = issue.title,
                            description = issue.body ?: "Açıklama belirtilmemiş.",
                            authorName = issue.user?.login ?: "Akademik Araştırmacı",
                            authorTitle = "İlim Diyârı Araştırmacısı",
                            authorAvatarUrl = issue.user?.avatarUrl,
                            category = category,
                            pdfUrl = "https://github.com/$owner/$repo/releases/download/v1.0/paper_${issue.number}.pdf",
                            pdfSize = "2.5 MB",
                            coverImageUrl = null,
                            likeCount = 0,
                            commentCount = issue.comments,
                            isLiked = false,
                            createdAt = System.currentTimeMillis(),
                            githubIssueId = issue.id
                        )
                    }
                    postDao.insertPosts(entities)
                    return@withContext Result.success(entities.size)
                }
            }
            Result.success(0)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Post Beğeni Durumunu Değiştir (Room DB anında güncellenir).
     */
    suspend fun toggleLike(postId: String) = withContext(Dispatchers.IO) {
        val post = postDao.getPostById(postId) ?: return@withContext
        val newIsLiked = !post.isLiked
        val newCount = if (newIsLiked) post.likeCount + 1 else (post.likeCount - 1).coerceAtLeast(0)
        postDao.updateLikeStatus(postId, newIsLiked, newCount)
    }

    /**
     * Post Silme (Room DB ve Akıştan anında kaldırılır - FAZ 8).
     */
    suspend fun deletePost(postId: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            postDao.deletePostById(postId)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Yeni Akademik PDF Makale Ekle (Yerel Room DB'ye anında kaydeder).
     * Gerçek kapak görüntüsü (PdfRenderer) ve userId bağlantısıyla saklanır.
     */
    suspend fun addNewPost(
        title: String,
        category: String,
        description: String,
        pdfUrl: String,
        authorName: String,
        authorTitle: String,
        userId: String? = null,
        coverImageUrl: String? = null,
        pdfSize: String = "2.4 MB"
    ): Result<PostEntity> = withContext(Dispatchers.IO) {
        val newPost = PostEntity(
            id = "post_${UUID.randomUUID()}",
            userId = userId,
            title = title.trim(),
            description = description.trim(),
            authorName = authorName.ifBlank { "Araştırmacı" },
            authorTitle = authorTitle.ifBlank { "İlim Diyârı Akademik Üyesi" },
            authorAvatarUrl = null,
            category = category,
            pdfUrl = pdfUrl.trim(),
            pdfSize = pdfSize,
            coverImageUrl = coverImageUrl,
            likeCount = 1,
            commentCount = 0,
            isLiked = true,
            createdAt = System.currentTimeMillis()
        )
        postDao.insertPost(newPost)
        Result.success(newPost)
    }
}
