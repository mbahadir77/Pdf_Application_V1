package com.example.data.repository

import android.util.Base64
import android.util.Log
import com.example.data.local.dao.PostDao
import com.example.data.local.entity.PostEntity
import com.example.data.pref.SessionManager
import com.example.data.remote.GitHubService
import com.example.data.remote.RetrofitClient
import com.example.data.remote.model.CreateGitHubIssueRequest
import com.example.data.remote.model.PostModel
import com.example.data.remote.model.UpdateGistRequest
import com.example.data.remote.model.UpdateRepoContentRequest
import com.example.data.remote.model.toEntity
import com.example.data.remote.model.toModel
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
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.util.UUID
import java.util.concurrent.TimeUnit

/**
 * İlim Diyârı - Global Akış ve Senkronizasyon Deposu (Emir 1).
 * Gerçek ağ çağrıları (OkHttp / Retrofit / HttpURLConnection) ile GitHub Gist ve Repo JSON'ını
 * senkronize eder. Cihazlar arası veri kaybını önler ve Room DB ile Upsert (REPLACE) yapar.
 */
class FeedRepository(
    private val postDao: PostDao,
    private val gitHubService: GitHubService = RetrofitClient.gitHubService,
    private val sessionManager: SessionManager? = null
) {
    companion object {
        private const val TAG = "FeedRepository"
        private const val DEFAULT_OWNER = "mbahadir77"
        private const val DEFAULT_REPO = "Pdf_Application_V1"
        private const val POSTS_JSON_PATH = "academic_posts.json"
        private const val DEFAULT_GIST_ID = "ilim_diyari_global_posts"
    }

    private val postsListType = Types.newParameterizedType(List::class.java, PostModel::class.java)
    private val postsAdapter = RetrofitClient.moshi.adapter<List<PostModel>>(postsListType)

    private val okHttpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .writeTimeout(15, TimeUnit.SECONDS)
            .build()
    }

    /**
     * Room DB Flow üzerinden dinamik akış.
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
     * GitHub Global Senkronizasyonu (GET İsteği & Upsert).
     * Uygulama açıldığında veya giriş yapıldığında çağrılır:
     * 1. GitHub Repo / Raw / Gist üzerinden global JSON dosyasını çeker.
     * 2. GitHub Issues havuzundaki akademik girdileri toplar.
     * 3. Çekilen tüm verileri Room DB'ye OnConflictStrategy.REPLACE (Upsert) ile kaydeder.
     */
    suspend fun syncWithGitHub(
        owner: String = DEFAULT_OWNER,
        repo: String = DEFAULT_REPO
    ): Result<Int> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "GitHub global senkronizasyonu başlatılıyor ($owner/$repo)...")
            val remotePosts = mutableListOf<PostEntity>()

            // 1. GitHub Contents API / Raw GET ile global JSON dosyasını çek
            val jsonPosts = fetchGlobalJsonPosts(owner, repo)
            remotePosts.addAll(jsonPosts)

            // 2. Gist üzerinden veri kontrolü
            val gistId = sessionManager?.getGistId() ?: DEFAULT_GIST_ID
            val gistPosts = fetchGistPosts(gistId)
            for (gp in gistPosts) {
                if (remotePosts.none { it.id == gp.id }) {
                    remotePosts.add(gp)
                }
            }

            // 3. GitHub Issues havuzundan paylaşılan PDF'leri topla
            val issuePosts = fetchGitHubIssuePosts(owner, repo)
            for (ip in issuePosts) {
                if (remotePosts.none { it.id == ip.id || (it.title == ip.title && it.authorName == ip.authorName) }) {
                    remotePosts.add(ip)
                }
            }

            // 4. Room DB'ye Upsert (REPLACE)
            if (remotePosts.isNotEmpty()) {
                postDao.insertPosts(remotePosts)
                Log.d(TAG, "GitHub senkronizasyonu tamamlandı: ${remotePosts.size} eser Room DB'ye aktarıldı.")
                return@withContext Result.success(remotePosts.size)
            }

            Result.success(0)
        } catch (e: Exception) {
            Log.e(TAG, "GitHub senkronizasyonunda hata: ${e.localizedMessage}", e)
            Result.failure(e)
        }
    }

    /**
     * GitHub Repo Content veya Raw URL üzerinden global JSON'ı GERÇEK ağ isteği ile çeker.
     */
    private suspend fun fetchGlobalJsonPosts(owner: String, repo: String): List<PostEntity> {
        val resultList = mutableListOf<PostEntity>()

        // 1. Yöntem: GitHub API Contents Endpoint
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
                    if (resultList.isNotEmpty()) {
                        Log.d(TAG, "GitHub Contents API ile ${resultList.size} eser başarıyla çekildi.")
                        return resultList
                    }
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "GitHub Content API GET hatası: ${e.message}")
        }

        // 2. Yöntem: OkHttp ile Doğrudan Raw CDN URL GET İsteği (Cache-Busting & FORCE_NETWORK)
        val rawUrls = listOf(
            "https://raw.githubusercontent.com/$owner/$repo/main/$POSTS_JSON_PATH",
            "https://raw.githubusercontent.com/$owner/$repo/master/$POSTS_JSON_PATH"
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
                        resultList.addAll(models.map { it.toEntity() })
                        if (resultList.isNotEmpty()) {
                            Log.d(TAG, "Raw GitHub CDN ($noCacheUrl) üzerinden ${resultList.size} eser çekildi.")
                            return resultList
                        }
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Raw URL GET çağrısı başarısız ($rawUrl): ${e.message}")
            }
        }

        // 3. Yöntem: HttpURLConnection ile Saf Ağ İsteği (Fallback & Cache-Busting)
        try {
            val fallbackUrl = "https://raw.githubusercontent.com/$owner/$repo/main/$POSTS_JSON_PATH?t=${System.currentTimeMillis()}"
            val connection = URL(fallbackUrl).openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.useCaches = false
            connection.defaultUseCaches = false
            connection.connectTimeout = 10000
            connection.readTimeout = 10000
            connection.setRequestProperty("User-Agent", "IlimDiyari-Academic/1.0")
            connection.setRequestProperty("Cache-Control", "no-cache, no-store, must-revalidate")
            connection.setRequestProperty("Pragma", "no-cache")
            if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                val reader = BufferedReader(InputStreamReader(connection.inputStream))
                val jsonString = reader.readText()
                reader.close()
                if (jsonString.isNotBlank()) {
                    val models = postsAdapter.fromJson(jsonString).orEmpty()
                    resultList.addAll(models.map { it.toEntity() })
                    return resultList
                }
            }
            connection.disconnect()
        } catch (_: Exception) {}

        return resultList
    }

    /**
     * GitHub Gist üzerinden JSON verilerini çeker.
     */
    private suspend fun fetchGistPosts(gistId: String): List<PostEntity> {
        if (gistId.isBlank() || gistId == DEFAULT_GIST_ID) return emptyList()
        return try {
            val response = gitHubService.getGist(gistId)
            if (response.isSuccessful && response.body() != null) {
                val files = response.body()?.files.orEmpty()
                val targetFile = files[POSTS_JSON_PATH] ?: files.values.firstOrNull()
                val rawContent = targetFile?.content.orEmpty()
                if (rawContent.isNotBlank()) {
                    val models = postsAdapter.fromJson(rawContent).orEmpty()
                    models.map { it.toEntity() }
                } else emptyList()
            } else emptyList()
        } catch (e: Exception) {
            Log.w(TAG, "Gist GET çağrısı başarısız: ${e.message}")
            emptyList()
        }
    }

    /**
     * GitHub Issues sekmesindeki akademik paylaşımları çeker.
     */
    private suspend fun fetchGitHubIssuePosts(owner: String, repo: String): List<PostEntity> {
        return try {
            val response = gitHubService.getRepoIssues(owner, repo)
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
                        authorTitle = "İlim Diyârı Akademik Üyesi",
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
            Log.w(TAG, "GitHub Issues GET çağrısı başarısız: ${e.message}")
            emptyList()
        }
    }

    /**
     * Yeni Akademik PDF Makale Ekle (Emir 1 - Ağ ve Upsert Senkronizasyonu).
     * 1. Sadece yerel veritabanına kaydetmez; önce GitHub'daki güncel listeyi çeker.
     * 2. Yeni kaydı listeye ekleyip de-duplicate eder.
     * 3. POST / PUT ile GitHub'daki JSON'ı KESİNLİKLE günceller.
     * 4. Tüm veriyi Room DB'ye OnConflictStrategy.REPLACE ile yazar.
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
            // 1. GitHub'daki mevcut güncel listeyi çek
            val remotePosts = fetchGlobalJsonPosts(owner, repo).toMutableList()

            // 2. Yerel DB'deki mevcut listeyi de alarak hiçbir verinin kaybolmamasını sağla
            val localPosts = postDao.getAllPostsList()
            val combinedMap = LinkedHashMap<String, PostEntity>()

            for (p in localPosts) {
                combinedMap[p.id] = p
            }
            for (p in remotePosts) {
                combinedMap[p.id] = p
            }
            combinedMap[newPost.id] = newPost

            val fullUpdatedList = combinedMap.values.sortedByDescending { it.createdAt }

            // 3. Güncel listeyi JSON'a dönüştür ve GitHub'a POST/PUT ile gönder
            val updatedJson = postsAdapter.toJson(fullUpdatedList.map { it.toModel() })
            pushUpdatedJsonToGitHub(owner, repo, updatedJson, newPost)

            // 4. Tüm verileri Room DB'ye Upsert (REPLACE) et
            postDao.insertPosts(fullUpdatedList)

            Log.d(TAG, "Yeni PDF başarıyla eklendi, senkronize edildi ve Room DB'ye yazıldı: ${newPost.title}")
            Result.success(newPost)
        } catch (e: Exception) {
            Log.e(TAG, "GitHub senkronizasyonunda istisna: ${e.message}. Güvenlik için yerel DB'ye kaydediliyor.", e)
            postDao.insertPost(newPost)
            Result.success(newPost)
        }
    }

    /**
     * Güncellenen JSON'ı GitHub Repo Content (PUT), Gist (PATCH) ve Issues (POST) ile gönderir.
     */
    private suspend fun pushUpdatedJsonToGitHub(
        owner: String,
        repo: String,
        jsonContent: String,
        newPost: PostEntity
    ) {
        val token = sessionManager?.getGitHubToken()?.let {
            if (it.startsWith("token ") || it.startsWith("Bearer ")) it else "Bearer $it"
        }

        // 1. Repo Content PUT isteği
        try {
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

            val pushResponse = gitHubService.updateRepoContent(token, owner, repo, POSTS_JSON_PATH, updateRequest)
            if (pushResponse.isSuccessful) {
                Log.d(TAG, "GitHub Repo Content PUT 200/201: Global JSON başarıyla güncellendi.")
            } else {
                Log.w(TAG, "GitHub Repo Content PUT yanıt kodu: ${pushResponse.code()}")
            }
        } catch (e: Exception) {
            Log.w(TAG, "GitHub Repo Content PUT hatası: ${e.message}")
        }

        // 2. Gist PATCH isteği (Gist ID yapılandırılmışsa)
        val gistId = sessionManager?.getGistId()
        if (!gistId.isNullOrBlank() && gistId != DEFAULT_GIST_ID) {
            try {
                val gistRequest = UpdateGistRequest(
                    description = "İlim Diyârı Akademik Yayınlar",
                    files = mapOf(
                        POSTS_JSON_PATH to com.example.data.remote.model.GitHubGistFile(
                            content = jsonContent
                        )
                    )
                )
                val gistResponse = gitHubService.updateGist(token, gistId, gistRequest)
                if (gistResponse.isSuccessful) {
                    Log.d(TAG, "GitHub Gist PATCH 200: Gist başarıyla güncellendi.")
                }
            } catch (e: Exception) {
                Log.w(TAG, "Gist PATCH güncelleme hatası: ${e.message}")
            }
        }

        // 3. OkHttp ile Doğrudan GitHub Issues POST Çağrısı
        try {
            val issueJson = """
                {
                    "title": "${newPost.title.replace("\"", "\\\"")}",
                    "body": "${newPost.description.replace("\"", "\\\"")}\n\nYazar: ${newPost.authorName}\nKategori: ${newPost.category}\nPDF: ${newPost.pdfUrl}",
                    "labels": ["${newPost.category}", "İlimDiyarı"]
                }
            """.trimIndent()

            val requestBuilder = Request.Builder()
                .url("https://api.github.com/repos/$owner/$repo/issues")
                .header("User-Agent", "IlimDiyari-Academic/1.0")
                .header("Accept", "application/vnd.github.v3+json")
                .post(issueJson.toRequestBody("application/json; charset=utf-8".toMediaType()))

            if (!token.isNullOrBlank()) {
                requestBuilder.header("Authorization", token)
            }

            val callResponse = okHttpClient.newCall(requestBuilder.build()).execute()
            if (callResponse.isSuccessful) {
                Log.d(TAG, "GitHub Issue POST başarılı: Eser Issues sekmesine kaydedildi.")
            }
        } catch (e: Exception) {
            Log.w(TAG, "GitHub Issue kaydı oluşturulamadı: ${e.message}")
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
