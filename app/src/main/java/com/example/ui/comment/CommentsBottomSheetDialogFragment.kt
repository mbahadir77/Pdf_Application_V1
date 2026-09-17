package com.example.ui.comment

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.view.inputmethod.InputMethodManager
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.R
import com.example.data.local.AppDatabase
import com.example.data.remote.RetrofitClient
import com.example.data.remote.model.CreateGitHubCommentRequest
import com.example.databinding.DialogCommentsBottomSheetBinding
import com.example.ui.common.IlmToast
import com.example.ui.notification.NotificationHelper
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * İlmNet - GitHub Issues Tabanlı Cam Efektli Yorum & Müzakere Bottom Sheet Dialog (FAZ 8).
 * Shimmer yükleme efekti, dinamik avatar ve silme (Delete) desteği.
 */
class CommentsBottomSheetDialogFragment : BottomSheetDialogFragment() {

    private var _binding: DialogCommentsBottomSheetBinding? = null
    private val binding get() = _binding!!

    private var commentAdapter: CommentAdapter? = null
    private val activeComments = mutableListOf<DisplayComment>()
    private var postId: String = ""
    private var postTitle: String = ""
    private var postAuthor: String = ""
    private var postAuthorId: String? = null
    private var githubIssueNumber: Long = 0L

    var onCommentAddedListener: ((newCount: Int) -> Unit)? = null

    companion object {
        private const val ARG_POST_ID = "arg_post_id"
        private const val ARG_POST_TITLE = "arg_post_title"
        private const val ARG_POST_AUTHOR = "arg_post_author"
        private const val ARG_POST_AUTHOR_ID = "arg_post_author_id"
        private const val ARG_ISSUE_NUMBER = "arg_issue_number"

        fun newInstance(
            postId: String,
            postTitle: String,
            postAuthor: String,
            issueNumber: Long,
            postAuthorId: String? = null
        ): CommentsBottomSheetDialogFragment {
            val fragment = CommentsBottomSheetDialogFragment()
            val args = Bundle().apply {
                putString(ARG_POST_ID, postId)
                putString(ARG_POST_TITLE, postTitle)
                putString(ARG_POST_AUTHOR, postAuthor)
                putString(ARG_POST_AUTHOR_ID, postAuthorId)
                putLong(ARG_ISSUE_NUMBER, issueNumber)
            }
            fragment.arguments = args
            return fragment
        }
    }

    override fun getTheme(): Int = R.style.Theme_IlmNet_BottomSheetDialog

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState) as BottomSheetDialog
        dialog.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
        return dialog
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        postId = arguments?.getString(ARG_POST_ID) ?: ""
        postTitle = arguments?.getString(ARG_POST_TITLE) ?: ""
        postAuthor = arguments?.getString(ARG_POST_AUTHOR) ?: ""
        postAuthorId = arguments?.getString(ARG_POST_AUTHOR_ID)
        githubIssueNumber = arguments?.getLong(ARG_ISSUE_NUMBER) ?: 0L
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DialogCommentsBottomSheetBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.tvCommentPostTitle.text = "Risale: $postTitle"
        binding.rvComments.layoutManager = LinearLayoutManager(requireContext())

        lifecycleScope.launch {
            val db = AppDatabase.getInstance(requireContext())
            val currentUser = withContext(Dispatchers.IO) { db.userDao().getLastLoggedInUserDirect() }
            val adapter = CommentAdapter(
                currentUserName = currentUser?.fullName,
                currentUserId = currentUser?.id,
                postAuthorName = postAuthor,
                postAuthorId = postAuthorId,
                onDeleteCommentClicked = { comment ->
                    deleteComment(comment)
                }
            )
            commentAdapter = adapter
            binding.rvComments.adapter = adapter
            loadComments()
        }

        binding.btnCloseComments.setOnClickListener {
            dismiss()
        }

        binding.btnSendComment.setOnClickListener {
            submitComment()
        }
    }

    private fun deleteComment(comment: DisplayComment) {
        activeComments.removeAll { it.id == comment.id }
        commentAdapter?.submitList(activeComments.toList())

        if (activeComments.isEmpty()) {
            binding.layoutCommentsEmpty.visibility = View.VISIBLE
        }

        lifecycleScope.launch {
            withContext(Dispatchers.IO) {
                val db = AppDatabase.getInstance(requireContext())
                // 1. Yerel Room DB'den sil
                db.commentDao().deleteComment(comment.id)

                // 2. GitHub Backend'den sil
                try {
                    RetrofitClient.gitHubService.deleteIssueComment(
                        token = "token_bearer",
                        owner = "mbahadir77",
                        repo = "Pdf_Application_V1",
                        commentId = comment.id
                    )
                } catch (_: Exception) {
                    try {
                        RetrofitClient.gitHubService.deleteIssueComment(
                            token = "token_bearer",
                            owner = "ilmnet-academic",
                            repo = "ilmnet-feed",
                            commentId = comment.id
                        )
                    } catch (_: Exception) {}
                }

                // 3. Post yorum sayısını güncelle
                val post = db.postDao().getPostById(postId)
                if (post != null) {
                    val updatedCount = (post.commentCount - 1).coerceAtLeast(0)
                    db.postDao().updateCommentsCount(postId, updatedCount)
                    withContext(Dispatchers.Main) {
                        onCommentAddedListener?.invoke(updatedCount)
                    }
                }
            }
        }
        IlmToast.info(requireActivity(), "Müzakere yorumu başarıyla silindi.")
    }

    private fun loadComments() {
        binding.shimmerComments.visibility = View.VISIBLE
        binding.shimmerComments.startShimmer()
        binding.layoutCommentsEmpty.visibility = View.GONE

        lifecycleScope.launch {
            activeComments.clear()
            val db = AppDatabase.getInstance(requireContext())

            // 1. Yerel Room DB'den bu PDF'e ait yorumları çek
            val localEntities = withContext(Dispatchers.IO) {
                db.commentDao().getCommentsForPostDirect(postId)
            }
            localEntities.forEach { entity ->
                activeComments.add(
                    DisplayComment(
                        id = entity.id,
                        userId = entity.userId,
                        authorName = entity.authorName,
                        avatarUrl = entity.avatarUrl,
                        body = entity.body,
                        dateText = entity.dateText
                    )
                )
            }

            // 2. GitHub Issue'dan çek
            if (githubIssueNumber > 0) {
                try {
                    val response = withContext(Dispatchers.IO) {
                        try {
                            RetrofitClient.gitHubService.getIssueComments(
                                owner = "mbahadir77",
                                repo = "Pdf_Application_V1",
                                issueNumber = githubIssueNumber
                            )
                        } catch (_: Exception) {
                            RetrofitClient.gitHubService.getIssueComments(
                                owner = "ilmnet-academic",
                                repo = "ilmnet-feed",
                                issueNumber = githubIssueNumber
                            )
                        }
                    }
                    if (response.isSuccessful) {
                        val body = response.body()
                        if (!body.isNullOrEmpty()) {
                            body.forEach { comment ->
                                if (activeComments.none { it.id == comment.id }) {
                                    activeComments.add(
                                        DisplayComment(
                                            id = comment.id,
                                            authorName = comment.user?.login ?: "Araştırmacı",
                                            avatarUrl = comment.user?.avatarUrl,
                                            body = comment.body,
                                            dateText = comment.createdAt?.take(10) ?: "Yeni"
                                        )
                                    )
                                }
                            }
                        }
                    }
                } catch (_: Exception) {}
            }

            binding.shimmerComments.stopShimmer()
            binding.shimmerComments.visibility = View.GONE
            if (activeComments.isEmpty()) {
                binding.layoutCommentsEmpty.visibility = View.VISIBLE
            } else {
                binding.layoutCommentsEmpty.visibility = View.GONE
                commentAdapter?.submitList(activeComments.toList())
            }
        }
    }

    private fun submitComment() {
        val commentText = binding.etCommentInput.text.toString().trim()
        if (commentText.isBlank()) {
            IlmToast.error(requireActivity(), "Lütfen bir tahlil veya yorum metni yazın.")
            return
        }

        lifecycleScope.launch {
            binding.btnSendComment.isEnabled = false
            val db = AppDatabase.getInstance(requireContext())
            val currentUser = withContext(Dispatchers.IO) { db.userDao().getLastLoggedInUserDirect() }
            val currentUserName = currentUser?.fullName ?: "İlmNet Araştırmacısı"
            val avatarUrl = currentUser?.avatarUrl

            val commentId = System.currentTimeMillis()
            val dateStr = SimpleDateFormat("dd MMM yyyy • HH:mm", Locale("tr")).format(Date())

            // 1. Yerel Room DB'ye kaydet
            withContext(Dispatchers.IO) {
                db.commentDao().insertComment(
                    com.example.data.local.entity.CommentEntity(
                        id = commentId,
                        postId = postId,
                        userId = currentUser?.id,
                        authorName = currentUserName,
                        avatarUrl = avatarUrl,
                        body = commentText,
                        dateText = dateStr,
                        createdAt = commentId
                    )
                )
            }

            // 2. GitHub Issue'ya gönder
            if (githubIssueNumber > 0) {
                try {
                    withContext(Dispatchers.IO) {
                        try {
                            RetrofitClient.gitHubService.createIssueComment(
                                token = "token_bearer",
                                owner = "mbahadir77",
                                repo = "Pdf_Application_V1",
                                issueNumber = githubIssueNumber,
                                request = CreateGitHubCommentRequest(body = commentText)
                            )
                        } catch (_: Exception) {
                            RetrofitClient.gitHubService.createIssueComment(
                                token = "token_bearer",
                                owner = "ilmnet-academic",
                                repo = "ilmnet-feed",
                                issueNumber = githubIssueNumber,
                                request = CreateGitHubCommentRequest(body = commentText)
                            )
                        }
                    }
                } catch (_: Exception) {}
            }

            val newComment = DisplayComment(
                id = commentId,
                userId = currentUser?.id,
                authorName = currentUserName,
                avatarUrl = avatarUrl,
                body = commentText,
                dateText = dateStr
            )

            activeComments.add(newComment)
            binding.layoutCommentsEmpty.visibility = View.GONE
            commentAdapter?.submitList(activeComments.toList())
            binding.rvComments.smoothScrollToPosition((commentAdapter?.itemCount ?: 1) - 1)
            binding.etCommentInput.text?.clear()
            val imm = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
            imm?.hideSoftInputFromWindow(binding.etCommentInput.windowToken, 0)
            binding.etCommentInput.clearFocus()
            binding.btnSendComment.isEnabled = true

            // Yerel veritabanında post'un yorum sayısını artır
            withContext(Dispatchers.IO) {
                val post = db.postDao().getPostById(postId)
                if (post != null) {
                    val updatedCount = post.commentCount + 1
                    db.postDao().updateCommentsCount(postId, updatedCount)
                    withContext(Dispatchers.Main) {
                        onCommentAddedListener?.invoke(updatedCount)
                    }
                }
            }

            NotificationHelper.showCommentNotification(
                context = requireContext().applicationContext,
                pdfTitle = postTitle,
                commenterName = currentUserName,
                commentText = commentText,
                targetPostId = postId
            )

            IlmToast.success(requireActivity(), "Akademik tahliliniz müzakere meclisine eklendi! 🖋️")
        }
    }

    private fun getAcademicSeedComments(title: String, author: String): List<DisplayComment> {
        return emptyList() // Sahte akademik yorumlar kaldırıldı
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
