package com.example.ui.feed

import android.content.Context
import android.content.Intent
import android.text.format.DateUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.PopupMenu
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.example.R
import com.example.data.local.entity.PostEntity
import com.example.databinding.ItemPostBinding
import com.example.databinding.ItemPostGridBinding
import com.example.databinding.ItemBookSpineBinding
import com.example.util.performTokHaptic
import java.io.File

/**
 * İlmNet - Akademik Akış Adaptörü (FAZ 12: List + Staggered Grid + Ciltli Kitap Sırtı).
 * - Dinamik liste, Pinterest ızgarası ve kütüphane rafındaki 'Kitap Sırtı' formatı.
 * - Bordo, lacivert ve zümrüt deri sırt dokuları ile altın yaldızlı şirazeler.
 */
class FeedAdapter(
    private val onLikeClicked: (PostEntity) -> Unit,
    private val onCommentClicked: (PostEntity) -> Unit,
    private val onReadPdfClicked: (PostEntity) -> Unit,
    private val onAuthorClicked: ((authorName: String, avatarUrl: String?, authorId: String?) -> Unit)? = null,
    private val onDeletePostClicked: ((PostEntity) -> Unit)? = null,
    var currentUserId: String? = null,
    var currentUserName: String? = null
) : ListAdapter<PostEntity, RecyclerView.ViewHolder>(PostDiffCallback) {

    var isGridMode: Boolean = false
        set(value) {
            if (field != value) {
                field = value
                notifyDataSetChanged()
            }
        }

    var isBookSpineMode: Boolean = false
        set(value) {
            if (field != value) {
                field = value
                notifyDataSetChanged()
            }
        }

    companion object {
        const val VIEW_TYPE_LIST = 0
        const val VIEW_TYPE_GRID = 1
        const val VIEW_TYPE_BOOK_SPINE = 2

        private fun getRelativeTimeString(timeMillis: Long): String {
            val now = System.currentTimeMillis()
            val diff = now - timeMillis
            return when {
                diff < DateUtils.MINUTE_IN_MILLIS -> "Az önce"
                diff < DateUtils.HOUR_IN_MILLIS -> "${diff / DateUtils.MINUTE_IN_MILLIS} dk önce"
                diff < DateUtils.DAY_IN_MILLIS -> "${diff / DateUtils.HOUR_IN_MILLIS} sa önce"
                else -> "${diff / DateUtils.DAY_IN_MILLIS} gün önce"
            }
        }

        private fun sharePost(context: Context, post: PostEntity) {
            val shareText = "İlmNet'te harika bir ilmi eser buldum: ${post.title} - ${post.authorName} (${post.category}). İncelemek için İlmNet uygulamasını kullanabilirsiniz."
            val sendIntent = Intent().apply {
                action = Intent.ACTION_SEND
                putExtra(Intent.EXTRA_TEXT, shareText)
                putExtra(Intent.EXTRA_SUBJECT, post.title)
                type = "text/plain"
            }
            val shareIntent = Intent.createChooser(sendIntent, "Eseri Paylaş")
            context.startActivity(shareIntent)
        }
    }

    override fun getItemViewType(position: Int): Int {
        return when {
            isBookSpineMode -> VIEW_TYPE_BOOK_SPINE
            isGridMode -> VIEW_TYPE_GRID
            else -> VIEW_TYPE_LIST
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            VIEW_TYPE_BOOK_SPINE -> {
                val binding = ItemBookSpineBinding.inflate(inflater, parent, false)
                PostBookSpineViewHolder(binding)
            }
            VIEW_TYPE_GRID -> {
                val binding = ItemPostGridBinding.inflate(inflater, parent, false)
                PostGridViewHolder(binding)
            }
            else -> {
                val binding = ItemPostBinding.inflate(inflater, parent, false)
                PostListViewHolder(binding)
            }
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val item = getItem(position)
        when (holder) {
            is PostBookSpineViewHolder -> holder.bind(item, position)
            is PostListViewHolder -> holder.bind(item)
            is PostGridViewHolder -> holder.bind(item)
        }
    }

    inner class PostBookSpineViewHolder(
        private val binding: ItemBookSpineBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(post: PostEntity, position: Int) {
            binding.tvBookSpineTitle.text = post.title
            binding.tvBookSpineCategory.text = post.category.uppercase()

            // Bordo, Lacivert, Zümrüt dönüşümlü asil deri cilt sırtı
            val spineBackgroundRes = when (position % 3) {
                0 -> R.drawable.bg_book_spine_burgundy
                1 -> R.drawable.bg_book_spine_navy
                else -> R.drawable.bg_book_spine_emerald
            }
            binding.containerBookSpine.setBackgroundResource(spineBackgroundRes)

            binding.containerBookSpine.setOnClickListener {
                binding.root.context.performTokHaptic()
                onReadPdfClicked(post)
            }
        }
    }

    inner class PostListViewHolder(
        private val binding: ItemPostBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(post: PostEntity) {
            val context = binding.root.context

            binding.tvAuthorName.text = post.authorName
            binding.tvAuthorTitle.text = post.authorTitle
            binding.tvPostCategory.text = post.category

            if (!post.authorAvatarUrl.isNullOrBlank()) {
                binding.ivAuthorAvatar.load(post.authorAvatarUrl) {
                    crossfade(true)
                    placeholder(R.drawable.ic_academic_logo)
                    error(R.drawable.ic_academic_logo)
                }
            } else {
                binding.ivAuthorAvatar.setImageResource(R.drawable.ic_academic_logo)
            }

            binding.tvPostTitle.text = post.title
            binding.tvPostDescription.text = post.description
            binding.tvPdfSize.text = "• ${post.pdfSize}"

            if (!post.coverImageUrl.isNullOrBlank()) {
                val file = File(post.coverImageUrl)
                val modelToLoad: Any = if (file.exists()) file else post.coverImageUrl
                binding.ivPdfCover.load(modelToLoad) {
                    crossfade(true)
                    placeholder(R.drawable.bg_academic_parchment_cover)
                    error(R.drawable.bg_academic_parchment_cover)
                }
            } else {
                binding.ivPdfCover.setImageResource(R.drawable.bg_academic_parchment_cover)
            }

            binding.containerCoverPreview.setOnClickListener {
                onReadPdfClicked(post)
            }

            binding.tvPostDate.text = getRelativeTimeString(post.createdAt)

            val likeCountText = if (post.likeCount > 0) post.likeCount.toString() else "0"
            binding.tvLikeCount.text = likeCountText

            if (post.isLiked) {
                binding.ivLikeIcon.setImageResource(R.drawable.ic_heart_filled)
                binding.ivLikeIcon.setColorFilter(ContextCompat.getColor(context, R.color.heart_red))
                binding.tvLikeCount.setTextColor(ContextCompat.getColor(context, R.color.heart_red))
            } else {
                binding.ivLikeIcon.setImageResource(R.drawable.ic_heart_outline)
                binding.ivLikeIcon.setColorFilter(ContextCompat.getColor(context, R.color.text_secondary))
                binding.tvLikeCount.setTextColor(ContextCompat.getColor(context, R.color.text_secondary))
            }

            val commentCountText = if (post.commentCount > 0) post.commentCount.toString() else "0"
            binding.tvCommentCount.text = commentCountText

            // Üç Nokta Seçenekler Menüsü
            val isAuthor = (currentUserId != null && currentUserId == post.userId) ||
                    (currentUserName != null && currentUserName.equals(post.authorName, ignoreCase = true))

            binding.btnPostOptions.setOnClickListener { view ->
                val popup = PopupMenu(context, view)
                if (isAuthor) {
                    popup.menu.add(0, 1, 0, "🗑️ Bu Risaleyi Sil")
                }
                popup.menu.add(0, 2, 1, "🔗 Eseri Paylaş")

                popup.setOnMenuItemClickListener { menuItem ->
                    when (menuItem.itemId) {
                        1 -> {
                            onDeletePostClicked?.invoke(post)
                            true
                        }
                        2 -> {
                            sharePost(context, post)
                            true
                        }
                        else -> false
                    }
                }
                popup.show()
            }

            val authorClickListener = {
                onAuthorClicked?.invoke(post.authorName, post.authorAvatarUrl, post.userId)
            }
            binding.ivAuthorAvatar.setOnClickListener { authorClickListener() }
            binding.tvAuthorName.setOnClickListener { authorClickListener() }
            binding.tvAuthorTitle.setOnClickListener { authorClickListener() }

            binding.btnPostLike.setOnClickListener { view ->
                view.performTokHaptic()
                onLikeClicked(post)
            }

            binding.btnPostComment.setOnClickListener {
                onCommentClicked(post)
            }

            binding.btnPostShare.setOnClickListener {
                sharePost(context, post)
            }

            binding.btnReadPdf.setOnClickListener {
                onReadPdfClicked(post)
            }
        }
    }

    inner class PostGridViewHolder(
        private val binding: ItemPostGridBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(post: PostEntity) {
            val context = binding.root.context

            binding.tvAuthorName.text = post.authorName
            binding.tvPostCategory.text = post.category

            if (!post.authorAvatarUrl.isNullOrBlank()) {
                binding.ivAuthorAvatar.load(post.authorAvatarUrl) {
                    crossfade(true)
                    placeholder(R.drawable.ic_academic_logo)
                    error(R.drawable.ic_academic_logo)
                }
            } else {
                binding.ivAuthorAvatar.setImageResource(R.drawable.ic_academic_logo)
            }

            binding.tvPostTitle.text = post.title
            binding.tvPostDescription.text = post.description
            binding.tvPdfSize.text = post.pdfSize

            if (!post.coverImageUrl.isNullOrBlank()) {
                val file = File(post.coverImageUrl)
                val modelToLoad: Any = if (file.exists()) file else post.coverImageUrl
                binding.ivPdfCover.load(modelToLoad) {
                    crossfade(true)
                    placeholder(R.drawable.bg_academic_parchment_cover)
                    error(R.drawable.bg_academic_parchment_cover)
                }
            } else {
                binding.ivPdfCover.setImageResource(R.drawable.bg_academic_parchment_cover)
            }

            binding.containerCoverPreview.setOnClickListener {
                onReadPdfClicked(post)
            }

            val likeCountText = if (post.likeCount > 0) post.likeCount.toString() else "0"
            binding.tvLikeCount.text = likeCountText

            if (post.isLiked) {
                binding.ivLikeIcon.setImageResource(R.drawable.ic_heart_filled)
                binding.ivLikeIcon.setColorFilter(ContextCompat.getColor(context, R.color.heart_red))
                binding.tvLikeCount.setTextColor(ContextCompat.getColor(context, R.color.heart_red))
            } else {
                binding.ivLikeIcon.setImageResource(R.drawable.ic_heart_outline)
                binding.ivLikeIcon.setColorFilter(ContextCompat.getColor(context, R.color.text_secondary))
                binding.tvLikeCount.setTextColor(ContextCompat.getColor(context, R.color.text_secondary))
            }

            val commentCountText = if (post.commentCount > 0) post.commentCount.toString() else "0"
            binding.tvCommentCount.text = commentCountText

            val isAuthor = (currentUserId != null && currentUserId == post.userId) ||
                    (currentUserName != null && currentUserName.equals(post.authorName, ignoreCase = true))

            binding.btnPostOptions.setOnClickListener { view ->
                val popup = PopupMenu(context, view)
                if (isAuthor) {
                    popup.menu.add(0, 1, 0, "🗑️ Bu Risaleyi Sil")
                }
                popup.menu.add(0, 2, 1, "🔗 Eseri Paylaş")

                popup.setOnMenuItemClickListener { menuItem ->
                    when (menuItem.itemId) {
                        1 -> {
                            onDeletePostClicked?.invoke(post)
                            true
                        }
                        2 -> {
                            sharePost(context, post)
                            true
                        }
                        else -> false
                    }
                }
                popup.show()
            }

            val authorClickListener = {
                onAuthorClicked?.invoke(post.authorName, post.authorAvatarUrl, post.userId)
            }
            binding.ivAuthorAvatar.setOnClickListener { authorClickListener() }
            binding.tvAuthorName.setOnClickListener { authorClickListener() }

            binding.btnPostLike.setOnClickListener { view ->
                view.performTokHaptic()
                onLikeClicked(post)
            }

            binding.btnPostComment.setOnClickListener {
                onCommentClicked(post)
            }

            binding.btnReadPdf.setOnClickListener {
                onReadPdfClicked(post)
            }
        }
    }

    object PostDiffCallback : DiffUtil.ItemCallback<PostEntity>() {
        override fun areItemsTheSame(oldItem: PostEntity, newItem: PostEntity): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: PostEntity, newItem: PostEntity): Boolean {
            return oldItem == newItem
        }
    }
}
