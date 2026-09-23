package com.example.data.repository

import android.content.Context
import android.util.Base64
import android.util.Log
import com.example.data.local.dao.PostDao
import com.example.data.local.entity.PostEntity
import com.example.data.pref.SessionManager
import com.example.data.remote.GitHubService
import com.example.data.remote.RetrofitClient
import com.example.data.remote.model.PostModel
import com.example.data.remote.model.toEntity
import com.example.data.remote.model.toModel
import com.example.util.AppConfig
import com.squareup.moshi.Types
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import okhttp3.CacheControl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.UUID
import java.util.concurrent.TimeUnit

/**
 * İlim Diyârı - Instagram Tarzı Single Source of Truth (SSOT) Akış Deposu.
 * 
 * Mimari İlkeler:
 * 1. GitHub Authorization: Tüm PUT ve POST isteklerinde Authorization: Bearer $GITHUB_PAT header'ı kullanılır.
 * 2. Ağ Başarısı Şartı: Ağda (GitHub API) işlem başarılı olmadan Room DB'ye kayıt yapılmaz.
 * 3. Single Source of Truth: GitHub academic_posts.json -> CacheControl.FORCE_NETWORK ile çekilir -> Room DB'ye yazılır -> UI Room DB Flow'u dinler.
 * 4. Çevrimdışı Destek: İnternet kesilirse Room DB yerel önbellek verileri sunmaya devam eder.
 */
class FeedRepository(
    private val postDao: PostDao,
    private val gitHubService: GitHubService = RetrofitClient.gitHubService,
    private val sessionManager: SessionManager? = null,
    private val context: Context? = null
) {
    companion object {
        private const val TAG = "FeedRepository"
    }

    private val postsListType = Types.newParameterizedType(List::class.java, PostModel::class.java)
    private val postsAdapter = RetrofitClient.moshi.adapter<List<PostModel>>(postsListType)

    private val okHttpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(20, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .writeTimeout(20, TimeUnit.SECONDS)
            .build()
    }

    /**
     * Room DB Flow üzerinden dinamik ana akış.
     * UI doğrudan bu akışı dinler; GitHub'dan yeni veri geldiğinde Room DB güncellenir ve UI anında yenilenir.
     */
    fun getPostsFlow(): Flow<List<PostEntity>> {
        return postDao.getAllPostsWithAuthor().map { list ->
            list.map { it.toSynchronizedPost() }
        }
    }

    /**
     * Kategoriye göre filtrelenmiş Room DB akışı.
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
     * INSTAGRAM TARZI SINGLE SOURCE OF TRUTH (SSOT) SENKRONİZASYON:
     * Uygulama açıldığında veya yenilendiğinde:
     * 1. CacheControl.FORCE_NETWORK ile GitHub'daki güncel academic_posts.json dosyasını çeker.
     * 2. İnternet varsa ve istek başarılıysa: Gelen tüm kullanıcıların paylaştığı PDF'leri Room DB'ye yazar.
     * 3. İnternet yoksa: Hata loglanır ve Room DB'deki çevrimdışı veriler ekranda gösterilmeye devam eder.
     */
    suspend fun syncWithGitHub(
        owner: String = AppConfig.GITHUB_OWNER,
        repo: String = AppConfig.GITHUB_REPO
    ): Result<Int> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "GitHub SSOT Senkronizasyonu başlatılıyor ($owner/$repo)...")
            val authHeader = AppConfig.getAuthHeader(sessionManager?.getGitHubToken())

            // GitHub'dan FORCE_NETWORK ile en güncel veriyi çek
            val remotePosts = fetchRemotePostsFromGitHub(owner, repo, authHeader)

            if (remotePosts != null) {
                // Ağdan veriler başarıyla alındı -> Room DB'ye yaz (SSOT)
                if (remotePosts.isNotEmpty()) {
                    postDao.insertPosts(remotePosts)
                }
                Log.d(TAG, "GitHub senkronizasyonu tamamlandı: ${remotePosts.size} eser Room DB'ye aktarıldı.")
                Result.success(remotePosts.size)
            } else {
                // Ağ isteği başarısız oldu (internet yok) -> Çevrimdışı Room DB verilerini koru
                val cachedCount = postDao.getPostCount()
                Log.w(TAG, "GitHub ağına ulaşılamadı. Çevrimdışı $cachedCount eser Room DB'den sunuluyor.")
                Result.success(cachedCount)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Senkronizasyon sırasında hata: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * GitHub Contents API ve Raw CDN üzerinden güncel academic_posts.json dosyasını çeker.
     * CacheControl.FORCE_NETWORK kullanarak önbelleği tamamen atlar.
     */
    private fun fetchRemotePostsFromGitHub(
        owner: String,
        repo: String,
        authHeader: String
    ): List<PostEntity>? {
        val (posts, _) = fetchRemotePostsAndSha(owner, repo, authHeader)
        return posts
    }

    /**
     * GitHub Contents API üzerinden mevcut JSON içeriğini ve dosyanın 'sha' değerini okur.
     */
    private fun fetchRemotePostsAndSha(
        owner: String,
        repo: String,
        authHeader: String
    ): Pair<List<PostEntity>?, String?> {
        // 1. Öncelik: GitHub Contents API
        try {
            val requestBuilder = Request.Builder()
                .url("https://api.github.com/repos/$owner/$repo/contents/${AppConfig.POSTS_FILE_PATH}")
                .cacheControl(CacheControl.FORCE_NETWORK)
                .header("Accept", "application/vnd.github+json")
                .header("X-GitHub-Api-Version", "2022-11-28")
                .header("User-Agent", "IlimDiyari-Academic/1.0")
                .header("Cache-Control", "no-cache, no-store, must-revalidate")
                .header("Pragma", "no-cache")

            if (AppConfig.isConfigured(sessionManager?.getGitHubToken())) {
                requestBuilder.header("Authorization", authHeader)
            }

            val response = okHttpClient.newCall(requestBuilder.build()).execute()
            if (response.isSuccessful) {
                val bodyString = response.body?.string().orEmpty()
                if (bodyString.isNotBlank()) {
                    val jsonObject = JSONObject(bodyString)
                    val sha = jsonObject.optString("sha").takeIf { it.isNotBlank() }
                    val contentBase64 = jsonObject.optString("content").replace("\n", "").replace("\r", "")
                    if (contentBase64.isNotBlank()) {
                        val decodedJson = String(Base64.decode(contentBase64, Base64.DEFAULT), Charsets.UTF_8)
                        val models = postsAdapter.fromJson(decodedJson).orEmpty()
                        Log.d(TAG, "GitHub API üzerinden ${models.size} eser ve SHA=$sha çekildi.")
                        return Pair(models.map { it.toEntity() }, sha)
                    }
                }
            } else if (response.code == 404) {
                // Dosya henüz oluşturulmamış, yeni dosya için sha null döner
                Log.d(TAG, "GitHub'da ${AppConfig.POSTS_FILE_PATH} henüz mevcut değil (404), yeni oluşturulacak.")
                return Pair(emptyList(), null)
            } else {
                Log.w(TAG, "GitHub Contents GET yanıt kodu: ${response.code}")
            }
        } catch (e: Exception) {
            Log.w(TAG, "GitHub Contents API GET başarısız: ${e.message}")
        }

        // 2. Yedek: Raw GitHub CDN üzerinden FORCE_NETWORK ile okuma
        val rawUrls = listOf(
            "https://raw.githubusercontent.com/$owner/$repo/main/${AppConfig.POSTS_FILE_PATH}",
            "https://raw.githubusercontent.com/$owner/$repo/master/${AppConfig.POSTS_FILE_PATH}"
        )
        for (rawUrl in rawUrls) {
            try {
                val noCacheUrl = "$rawUrl?t=${System.currentTimeMillis()}"
                val request = Request.Builder()
                    .url(noCacheUrl)
                    .cacheControl(CacheControl.FORCE_NETWORK)
                    .header("User-Agent", "IlimDiyari-Academic/1.0")
                    .header("Accept", "application/json")
                    .header("Cache-Control", "no-cache, no-store, must-revalidate")
                    .header("Pragma", "no-cache")
                    .get()
                    .build()

                val response = okHttpClient.newCall(request).execute()
                if (response.isSuccessful) {
                    val bodyString = response.body?.string().orEmpty()
                    if (bodyString.isNotBlank()) {
                        val models = postsAdapter.fromJson(bodyString).orEmpty()
                        Log.d(TAG, "Raw CDN üzerinden ${models.size} eser çekildi.")
                        return Pair(models.map { it.toEntity() }, null)
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Raw URL GET başarısız ($rawUrl): ${e.message}")
            }
        }

        return Pair(null, null)
    }

    /**
     * YENİ AKADEMİK PDF ESER EKLEME:
     * Kural: Ağda (GitHub API) işlem başarılı olmadan KESİNLİKLE Room DB'ye kayıt yapılmaz!
     * İstek 'Authorization: Bearer $GITHUB_PAT' başlığıyla GitHub'a PUT edilir.
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
        owner: String = AppConfig.GITHUB_OWNER,
        repo: String = AppConfig.GITHUB_REPO
    ): Result<PostEntity> = withContext(Dispatchers.IO) {
        val authHeader = AppConfig.getAuthHeader(sessionManager?.getGitHubToken())

        // 1. PAT Kontrolü: Token olmadan GitHub'a dosya yazılamaz
        if (!AppConfig.isConfigured(sessionManager?.getGitHubToken())) {
            val tokenError = "GitHub Personal Access Token (PAT) bulunamadı! academic_posts.json dosyasına yazabilmek için AppConfig.kt içerisindeki GITHUB_PAT değişkenine geçerli bir GitHub Token giriniz."
            Log.e(TAG, tokenError)
            return@withContext Result.failure(IllegalStateException(tokenError))
        }

        val newPost = PostEntity(
            id = UUID.randomUUID().toString(),
            userId = userId,
            title = title.trim(),
            description = description.trim(),
            authorName = authorName.ifBlank { "İlim Yolcusu" },
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

        // 2. GitHub'daki mevcut dosyayı ve geçerli commit SHA'sını çek
        val (existingPosts, currentSha) = fetchRemotePostsAndSha(owner, repo, authHeader)
        val currentList = existingPosts ?: postDao.getAllPostsList()

        // 3. Yeni eseri listenin başına ekle
        val updatedModels = listOf(newPost.toModel()) + currentList.filter { it.id != newPost.id }.map { it.toModel() }
        val jsonString = postsAdapter.toJson(updatedModels)
        val base64Content = Base64.encodeToString(jsonString.toByteArray(Charsets.UTF_8), Base64.NO_WRAP)

        // 4. GitHub API PUT isteği at (Authorization: Bearer $GITHUB_PAT ile)
        val putResult = putPostsJsonToGitHub(owner, repo, base64Content, currentSha, newPost.title, authHeader)

        if (putResult.isSuccess) {
            // AĞDA İŞLEM BAŞARILI! ŞİMDİ ROOM DB'YE YAZ
            postDao.insertPost(newPost)
            postDao.insertPosts(updatedModels.map { it.toEntity() })
            Log.d(TAG, "Eser GitHub'a başarıyla yazıldı (HTTP 200/201) ve Room DB senkronize edildi: ${newPost.title}")

            // İsteğe bağlı: Issues sekmesine de kayıt aç
            createIssueNotification(owner, repo, newPost, authHeader)

            Result.success(newPost)
        } else {
            // Ağda işlem başarısız olduysa KESİNLİKLE Room DB'ye kaydetme!
            val error = putResult.exceptionOrNull() ?: Exception("GitHub API yazma hatası.")
            Log.e(TAG, "Ağ işlemi başarısız olduğu için eser Room DB'ye kaydedilmedi: ${error.message}")
            Result.failure(error)
        }
    }

    /**
     * GitHub Contents API'ye PUT isteği atarak academic_posts.json dosyasını günceller.
     * Header KESİNLİKLE 'Authorization: Bearer $GITHUB_PAT' içerir.
     */
    private fun putPostsJsonToGitHub(
        owner: String,
        repo: String,
        base64Content: String,
        sha: String?,
        postTitle: String,
        authHeader: String
    ): Result<Unit> {
        return try {
            val payloadObj = JSONObject().apply {
                put("message", "İlim Diyârı: Yeni Eser Eklendi - $postTitle")
                put("content", base64Content)
                if (!sha.isNullOrBlank()) {
                    put("sha", sha)
                }
            }

            val request = Request.Builder()
                .url("https://api.github.com/repos/$owner/$repo/contents/${AppConfig.POSTS_FILE_PATH}")
                .header("Authorization", authHeader)
                .header("Accept", "application/vnd.github+json")
                .header("X-GitHub-Api-Version", "2022-11-28")
                .header("User-Agent", "IlimDiyari-Academic/1.0")
                .put(payloadObj.toString().toRequestBody("application/json; charset=utf-8".toMediaType()))
                .build()

            val response = okHttpClient.newCall(request).execute()
            if (response.isSuccessful) {
                Log.d(TAG, "GitHub Contents PUT başarılı (HTTP ${response.code})")
                Result.success(Unit)
            } else {
                val errorBody = response.body?.string().orEmpty()
                val errorMsg = when (response.code) {
                    401 -> "GitHub Yetkilendirme Hatası (401 Unauthorized): GITHUB_PAT token'ı geçersiz veya yetkisiz. Lütfen AppConfig.kt içerisindeki token'ı kontrol ediniz."
                    403 -> "GitHub Yetki Yetersiz (403 Forbidden): Token'ın 'repo' veya 'contents:write' yazma izni bulunmuyor."
                    404 -> "GitHub Repo Bulunamadı (404 Not Found): '$owner/$repo' deposuna erişilemedi."
                    409 -> "GitHub Çakışma Hatası (409 Conflict): Dosya SHA değeri uyuşmadı, lütfen tekrar deneyiniz."
                    else -> "GitHub PUT başarısız (HTTP ${response.code}): $errorBody"
                }
                Log.e(TAG, errorMsg)
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            Log.e(TAG, "GitHub PUT isteği sırasında ağ istisnası: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * GitHub Issues sekmesine bildirim kaydı açar.
     */
    private fun createIssueNotification(owner: String, repo: String, post: PostEntity, authHeader: String) {
        try {
            val issueJson = JSONObject().apply {
                put("title", post.title)
                put("body", "${post.description}\n\n**Yazar:** ${post.authorName}\n**Kategori:** ${post.category}\n**PDF:** ${post.pdfUrl}")
                put("labels", org.json.JSONArray().apply {
                    put(post.category)
                    put("İlimDiyarı")
                })
            }.toString()

            val request = Request.Builder()
                .url("https://api.github.com/repos/$owner/$repo/issues")
                .header("Authorization", authHeader)
                .header("Accept", "application/vnd.github+json")
                .header("X-GitHub-Api-Version", "2022-11-28")
                .header("User-Agent", "IlimDiyari-Academic/1.0")
                .post(issueJson.toRequestBody("application/json; charset=utf-8".toMediaType()))
                .build()

            val res = okHttpClient.newCall(request).execute()
            if (res.isSuccessful) {
                Log.d(TAG, "GitHub Issue kaydı başarıyla oluşturuldu.")
            }
        } catch (e: Exception) {
            Log.w(TAG, "GitHub Issue oluşturulamadı: ${e.message}")
        }
    }

    /**
     * Post Beğeni Durumu (Room DB anında güncellenir).
     */
    suspend fun toggleLike(postId: String) = withContext(Dispatchers.IO) {
        val post = postDao.getPostById(postId) ?: return@withContext
        val newIsLiked = !post.isLiked
        val newCount = if (newIsLiked) post.likeCount + 1 else (post.likeCount - 1).coerceAtLeast(0)
        postDao.updateLikeStatus(postId, newIsLiked, newCount)
    }

    /**
     * Post Silme (Ağda işlem başarılı olursa Room DB'den silinir).
     */
    suspend fun deletePost(
        postId: String,
        owner: String = AppConfig.GITHUB_OWNER,
        repo: String = AppConfig.GITHUB_REPO
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val authHeader = AppConfig.getAuthHeader(sessionManager?.getGitHubToken())
            if (!AppConfig.isConfigured(sessionManager?.getGitHubToken())) {
                postDao.deletePostById(postId)
                return@withContext Result.success(Unit)
            }

            val (existingPosts, currentSha) = fetchRemotePostsAndSha(owner, repo, authHeader)
            if (existingPosts != null) {
                val updatedModels = existingPosts.filter { it.id != postId }.map { it.toModel() }
                val jsonString = postsAdapter.toJson(updatedModels)
                val base64Content = Base64.encodeToString(jsonString.toByteArray(Charsets.UTF_8), Base64.NO_WRAP)

                val putResult = putPostsJsonToGitHub(owner, repo, base64Content, currentSha, "Eser Silindi ($postId)", authHeader)
                if (putResult.isSuccess) {
                    postDao.deletePostById(postId)
                    Result.success(Unit)
                } else {
                    val err = putResult.exceptionOrNull() ?: Exception("Eser GitHub'dan silinemedi.")
                    Result.failure(err)
                }
            } else {
                postDao.deletePostById(postId)
                Result.success(Unit)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
