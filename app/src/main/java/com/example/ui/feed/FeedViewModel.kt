package com.example.ui.feed

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.local.entity.PostEntity
import com.example.data.repository.FeedRepository
import com.example.ui.profile.AcademicBadge
import com.example.ui.profile.AcademicBadgeEngine
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class NavTab {
    FEED,
    ADD_PDF,
    PROFILE
}

enum class ProfileTab {
    SHARED_WORKS,
    BADGES
}

data class SelectedPdfFile(
    val uriString: String,
    val fileName: String,
    val fileSizeFormatted: String
)

sealed class AddPostUiState {
    object Idle : AddPostUiState()
    object Loading : AddPostUiState()
    data class Success(val post: PostEntity) : AddPostUiState()
    data class Error(val message: String) : AddPostUiState()
}

class FeedViewModel(
    private val feedRepository: FeedRepository
) : ViewModel() {

    private val _selectedTab = MutableStateFlow(NavTab.FEED)
    val selectedTab: StateFlow<NavTab> = _selectedTab.asStateFlow()

    private val _selectedCategory = MutableStateFlow("Tümü")
    val selectedCategory: StateFlow<String> = _selectedCategory.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedPdfFile = MutableStateFlow<SelectedPdfFile?>(null)
    val selectedPdfFile: StateFlow<SelectedPdfFile?> = _selectedPdfFile.asStateFlow()

    private val _readingPost = MutableStateFlow<PostEntity?>(null)
    val readingPost: StateFlow<PostEntity?> = _readingPost.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    private val _addPostState = MutableStateFlow<AddPostUiState>(AddPostUiState.Idle)
    val addPostState: StateFlow<AddPostUiState> = _addPostState.asStateFlow()

    private val _followedAuthors = MutableStateFlow<List<String>>(emptyList())
    val followedAuthors: StateFlow<List<String>> = _followedAuthors.asStateFlow()

    val posts: StateFlow<List<PostEntity>> = combine(
        feedRepository.getPostsFlow(),
        _selectedCategory,
        _searchQuery,
        _followedAuthors
    ) { allPosts, category, query, followed ->
        val categoryFiltered = when {
            category.equals("Tümü", ignoreCase = true) -> allPosts
            category.equals("Takip Edilenler", ignoreCase = true) -> {
                allPosts.filter { post -> followed.any { it.equals(post.authorName, ignoreCase = true) } }
            }
            else -> allPosts.filter { it.category.equals(category, ignoreCase = true) }
        }

        if (query.isBlank()) {
            categoryFiltered
        } else {
            val q = query.trim().lowercase()
            categoryFiltered.filter { post ->
                post.title.lowercase().contains(q) ||
                post.authorName.lowercase().contains(q) ||
                post.description.lowercase().contains(q) ||
                post.category.lowercase().contains(q)
            }
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val allPosts: StateFlow<List<PostEntity>> = feedRepository.getPostsFlow().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val badges: StateFlow<List<AcademicBadge>> = feedRepository.getPostsFlow().map { postList ->
        val categoryCounts = postList.groupingBy { it.category }.eachCount()
        AcademicBadgeEngine.calculateBadges(categoryCounts)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = AcademicBadgeEngine.calculateBadges(emptyMap())
    )

    private val _selectedProfileTab = MutableStateFlow(ProfileTab.SHARED_WORKS)
    val selectedProfileTab: StateFlow<ProfileTab> = _selectedProfileTab.asStateFlow()

    init {
        // İlk açılışta GitHub ile senkronize et
        viewModelScope.launch {
            refreshFromGitHub()
        }
    }

    fun selectNavTab(tab: NavTab) {
        _selectedTab.value = tab
    }

    fun selectProfileTab(tab: ProfileTab) {
        _selectedProfileTab.value = tab
    }

    fun selectCategory(category: String) {
        _selectedCategory.value = category
    }

    fun setFollowedAuthors(authors: List<String>) {
        _followedAuthors.value = authors
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setSelectedPdfFile(file: SelectedPdfFile?) {
        _selectedPdfFile.value = file
    }

    fun openPostPdf(post: PostEntity) {
        _readingPost.value = post
    }

    fun closePdfReader() {
        _readingPost.value = null
    }

    fun refreshFromGitHub() {
        viewModelScope.launch {
            _isRefreshing.value = true
            feedRepository.syncWithGitHub()
            _isRefreshing.value = false
        }
    }

    fun toggleLike(postId: String) {
        viewModelScope.launch {
            feedRepository.toggleLike(postId)
        }
    }

    fun createPost(
        title: String,
        category: String,
        description: String,
        authorName: String,
        authorTitle: String,
        userId: String? = null,
        coverImagePath: String? = null
    ) {
        val pickedFile = _selectedPdfFile.value
        if (title.isBlank() || description.isBlank()) {
            _addPostState.value = AddPostUiState.Error("Lütfen makale başlığı ve özet alanlarını doldurunuz.")
            return
        }

        if (pickedFile == null) {
            _addPostState.value = AddPostUiState.Error("Lütfen cihazınızdan geçerli bir akademik PDF seçiniz.")
            return
        }

        viewModelScope.launch {
            _addPostState.value = AddPostUiState.Loading
            // GitHub Releases'a arka planda yükleme taklidi / gerçek Base64 saklama:
            val pdfUrl = pickedFile.uriString
            val result = feedRepository.addNewPost(
                title = title,
                category = category,
                description = description,
                pdfUrl = pdfUrl,
                authorName = authorName,
                authorTitle = authorTitle,
                userId = userId,
                coverImageUrl = coverImagePath,
                pdfSize = pickedFile.fileSizeFormatted
            )
            result.onSuccess { post ->
                _selectedPdfFile.value = null
                _addPostState.value = AddPostUiState.Success(post)
                _selectedTab.value = NavTab.FEED
            }.onFailure { err ->
                _addPostState.value = AddPostUiState.Error(err.localizedMessage ?: "Post oluşturulamadı.")
            }
        }
    }

    fun deletePost(postId: String, onComplete: ((Boolean) -> Unit)? = null) {
        viewModelScope.launch {
            val result = feedRepository.deletePost(postId)
            onComplete?.invoke(result.isSuccess)
        }
    }

    fun resetAddPostState() {
        _addPostState.value = AddPostUiState.Idle
    }

    class Factory(private val feedRepository: FeedRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(FeedViewModel::class.java)) {
                return FeedViewModel(feedRepository) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
