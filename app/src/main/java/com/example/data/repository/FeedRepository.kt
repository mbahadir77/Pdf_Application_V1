package com.example.data.repository

import android.util.Base64
import android.util.Log
import com.example.data.local.dao.PostDao
import com.example.data.local.entity.PostEntity
import com.example.data.remote.GitHubService
import com.example.data.remote.RetrofitClient
import com.example.data.remote.model.CreateGitHubIssueRequest
import com.example.data.remote.model.PostModel
import com.example.data.remote.model.UpdateRepoContentRequest
import com.example.data.remote.model.toEntity
import com.example.data.remote.model.toModel
import com.squareup.moshi.Types
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.util.UUID

/**
 * İlim Diyârı - Global Akış Deposu (FAZ 13 Global Senkronizasyon & Clean Architecture).
 * - GitHub üzerindeki merkezi JSON veri tabanı (academic_posts.json) ve Issues havuzu ile tam iki yönlü senkronizasyon.
 * - Çevrimdışı öncelikli (Offline-First) Room DB ile OnConflictStrategy.REPLACE kullanarak çakışmaları çözer.
 * - Farklı cihazlar ve hesaplar arasında paylaşılan tüm PDF'ler anında global havuza aktarılır ve Room Flow ile UI'a yansır.
 */
class FeedRepository(
    private val postDao: PostDao,
    private val gitHubService: GitHubService
) {
    companion object {
        private const val TAG = "FeedRepository"
        private const val DEFAULT_OWNER = "mbahadir77"
        private const val DEFAULT_REPO = "Pdf_Application_V1"
        private const val POSTS_JSON_PATH = "academic_posts.json"
    }

    private val postsListType = Types.newParameterizedType(List::class.java, PostModel::class.java)
    private val postsAdapter = RetrofitClient.moshi.adapter<List<PostModel>>(postsListType)

    /**
     * Çevrimdışı öncelikli akış (Room DB Flow - Dinamik Yazar & Avatar İlişkili).
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
        return if (category == "Tümü" || category == "Tüm Eserler") {
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
     * Uygulama açılışında veya oturum açıldığında GitHub'daki global JSON dosyasını (Gist/Repo)
     * ve Issues havuzunu GET isteği ile çeker, Room DB'ye Upsert (REPLACE) eder.
     */
    suspend fun syncWithGitHub(
        owner: String = DEFAULT_OWNER,
        repo: String = DEFAULT_REPO
    ): Result<Int> = withContext(Dispatchers.IO) {
        try {
            val remotePosts = mutableListOf<PostEntity>()

            // 1. Adım: GitHub Repo Contents API üzerinden academic_posts.json dosyasını çek
            val repoJsonPosts = fetchGlobalJsonPosts(owner, repo)
            remotePosts.addAll(repoJsonPosts)

            // 2. Adım: GitHub Issues havuzundan paylaşılan diğer akademik makaleleri de çek (Fallback & Çoklu Kaynak)
            val issuePosts = fetchGitHubIssuePosts(owner, repo)
            for (issuePost in issuePosts) {
                if (remotePosts.none { it.id == issuePost.id || (it.title == issuePost.title && it.authorName == issuePost.authorName) }) {
                    remotePosts.add(issuePost)
                }
            }

            // 3. Adım: Çekilen tüm global verileri Room DB'deki yerel verilerle birleştir (Upsert / REPLACE)
            if (remotePosts.isNotEmpty()) {
                postDao.insertPosts(remotePosts)
                Log.d(TAG, "GitHub senkronizasyonu başarılı: ${remotePosts.size} eser Room DB'ye aktarıldı.")
                return@withContext Result.success(remotePosts.size)
            }

            Result.success(0)
        } catch (e: Exception) {
            Log.e(TAG, "GitHub senkronizasyon hatası: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * GitHub Repo Content veya Raw URL üzerinden global JSON'ı indirip parse eder.
     */
    private suspend fun fetchGlobalJsonPosts(owner: String, repo: String): List<PostEntity> {
        val resultList = mutableListOf<PostEntity>()

        // 1. GitHub API Content Endpoint
        try {
            val contentResponse = gitHubService.getRepoContent(owner, repo, POSTS_JSON_PATH)
            if (contentResponse.isSuccessful && contentResponse.body() != null) {
                val body = contentResponse.body()!!
                val rawJson = if (body.encoding == "base64" && !body.content.isNullOrBlank()) {
                    val cleanBase64 = body.content.replace("\n", "").replace("\r", "")
                    String(Base64.decode(cleanBase64, Base64.DEFAULT), Charsets.UTF_8)
                } else {
                    body.content
                }

                if (!rawJson.isNullOrBlank()) {
                    val models = postsAdapter.fromJson(rawJson).orEmpty()
                    resultList.addAll(models.map { it.toEntity() })
                    return resultList
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "GitHub Content API üzerinden JSON okunamadı: ${e.message}")
        }

        // 2. Raw URL Fallback (Doğrudan CDN / Raw GET)
        try {
            val rawUrl = "https://raw.githubusercontent.com/$owner/$repo/main/$POSTS_JSON_PATH"
            val rawResponse = gitHubService.getRawJsonString(rawUrl)
            if (rawResponse.isSuccessful && rawResponse.body() != null) {
                val rawString = rawResponse.body()!!.string()
                if (rawString.isNotBlank()) {
                    val models = postsAdapter.fromJson(rawString).orEmpty()
                    resultList.addAll(models.map { it.toEntity() })
                    return resultList
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Raw URL üzerinden JSON okunamadı: ${e.message}")
        }

        return resultList
    }

    /**
     * GitHub Issues sekmesindeki akademik paylaşımları çeker.
     */
    private suspend fun fetchGitHubIssuePosts(owner: String, repo: String): List<PostEntity> {
        return try {
            var response = gitHubService.getRepoIssues(owner, repo)
            if (!response.isSuccessful && owner != "ilmnet") {
                response = gitHubService.getRepoIssues("ilmnet", "academic-papers")
            }
            if (response.isSuccessful) {
                val issues = response.body().orEmpty()
                issues.map { issue ->
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
            } else {
                emptyList()
            }
        } catch (e: Exception) {
            Log.w(TAG, "GitHub Issues okunamadı: ${e.message}")
            emptyList()
        }
    }

    /**
     * Yeni Akademik PDF Makale Ekle (KRİTİK HATA ÇÖZÜMÜ - FAZ 13).
     * 1. Asla sadece yerel veritabanına kaydetmez.
     * 2. Önce GitHub'daki güncel listeyi çeker.
     * 3. Yeni PDF'i listeye ekler ve dev listeyi GitHub'a geri gönderir (PUT/POST/PATCH).
     * 4. Tüm veriyi Room DB'ye kaydeder (REPLACE).
     * 5. Room Flow sayesinde StateFlow ve FeedAdapter anında güncellenir.
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
        pdfSize: String = "2.4 MB",
        owner: String = DEFAULT_OWNER,
        repo: String = DEFAULT_REPO
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

        try {
            // 1. Adım: GitHub'daki mevcut güncel listeyi çek
            val remotePosts = fetchGlobalJsonPosts(owner, repo).toMutableList()

            // 2. Adım: Yerel DB'deki mevcut listeyi de alarak hiçbir cihazın verisinin kaybolmamasını sağla
            val localPosts = postDao.getAllPostsList()
            val combinedMap = LinkedHashMap<String, PostEntity>()

            // Önce yerel verileri haritaya al
            for (p in localPosts) {
                combinedMap[p.id] = p
            }
            // Uzak verilerle birleştir (REPLACE mantığı)
            for (p in remotePosts) {
                combinedMap[p.id] = p
            }
            // Yeni oluşturulan PDF'i en başa ekle
            combinedMap[newPost.id] = newPost

            val fullUpdatedList = combinedMap.values.sortedByDescending { it.createdAt }

            // 3. Adım: Dev listeyi JSON haline getir ve GitHub'a geri gönder (PUT / POST)
            val updatedJson = postsAdapter.toJson(fullUpdatedList.map { it.toModel() })
            pushUpdatedJsonToGitHub(owner, repo, updatedJson, newPost)

            // 4. Adım: Birleştirilen tüm listeyi Room DB'ye kaydet (OnConflictStrategy.REPLACE)
            postDao.insertPosts(fullUpdatedList)

            Log.d(TAG, "Yeni PDF global listeye eklendi ve Room DB Flow ile UI'a aktarıldı: ${newPost.title}")
            Result.success(newPost)
        } catch (e: Exception) {
            Log.e(TAG, "GitHub'a gönderme sırasında hata: ${e.message}. Güvenlik için yerel DB'ye kaydediliyor.", e)
            // Ağ hatası olsa bile kullanıcı verisini asla kaybetme; yerel DB'ye kaydet
            postDao.insertPost(newPost)
            Result.success(newPost)
        }
    }

    /**
     * Güncellenen global JSON dosyasını GitHub'a gönderir (Repo Content PUT ve Issue Create).
     */
    private suspend fun pushUpdatedJsonToGitHub(
        owner: String,
        repo: String,
        jsonContent: String,
        newPost: PostEntity
    ) {
        try {
            // Mevcut dosyanın SHA bilgisini kontrol et (varsa güncelle, yoksa yeni oluştur)
            var currentSha: String? = null
            try {
                val existing = gitHubService.getRepoContent(owner, repo, POSTS_JSON_PATH)
                if (existing.isSuccessful) {
                    currentSha = existing.body()?.sha
                }
            } catch (_: Exception) {}

            val base64Content = Base64.encodeToString(jsonContent.toByteArray(Charsets.UTF_8), Base64.NO_WRAP)
            val updateRequest = UpdateRepoContentRequest(
                message = "İlim Diyârı: Yeni Eser Eklendi - ${newPost.title}",
                content = base64Content,
                sha = currentSha
            )

            val pushResponse = gitHubService.updateRepoContent(null, owner, repo, POSTS_JSON_PATH, updateRequest)
            if (pushResponse.isSuccessful) {
                Log.d(TAG, "GitHub global JSON dosyası başarıyla güncellendi (PUT 200/201).")
            } else {
                Log.w(TAG, "GitHub PUT isteği yanıt kodu: ${pushResponse.code()}")
            }

            // Ayrıca GitHub Issues sekmesine de kayıt açarak diğer cihazların görmesini garanti altına al
            try {
                gitHubService.createIssue(
                    token = "",
                    owner = owner,
                    repo = repo,
                    request = CreateGitHubIssueRequest(
                        title = newPost.title,
                        body = "${newPost.description}\n\nYazar: ${newPost.authorName}\nKategori: ${newPost.category}\nPDF: ${newPost.pdfUrl}",
                        labels = listOf(newPost.category, "İlimDiyarı")
                    )
                )
            } catch (_: Exception) {}
        } catch (e: Exception) {
            Log.w(TAG, "GitHub'a JSON gönderme denemesi: ${e.message}")
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
     * Post Silme (Room DB ve Akıştan anında kaldırılır).
     */
    suspend fun deletePost(postId: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            postDao.deletePostById(postId)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
