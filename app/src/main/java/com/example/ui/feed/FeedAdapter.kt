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
import com.example.util.performTokHaptic
import java.io.File

/**
 * İlmNet - Akademik Akış Adaptörü (FAZ 8: ListAdapter + DiffUtil + CRUD + Haptic).
 * - IndexOutOfBounds ve bellek sızıntısı çökmelerini kesinlikle önler.
 * - Kullanıcının kendi risaleleri için 'Üç Nokta' Sil seçeneği sunar.
 * - PdfRenderer ile üretilen 1. sayfa kapaklarını (#80D4AF37 altın çerçeveli) yükler.
 * - Dinamik avatar senkronizasyonunu destekler.
 */
class FeedAdapter(
    private val onLikeClicked: (PostEntity) -> Unit,
    private val onCommentClicked: (PostEntity) -> Unit,
    private val onReadPdfClicked: (PostEntity) -> Unit,
    private val onAuthorClicked: ((authorName: String, avatarUrl: String?) -> Unit)? = null,
    private val onDeletePostClicked: ((PostEntity) -> Unit)? = null,
    var currentUserId: String? = null,
    var currentUserName: String? = null
) : ListAdapter<PostEntity, FeedAdapter.PostViewHolder>(PostDiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PostViewHolder {
        val binding = ItemPostBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return PostViewHolder(binding)
    }

    override fun onBindViewHolder(holder: PostViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class PostViewHolder(
        private val binding: ItemPostBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(post: PostEntity) {
            val context = binding.root.context

            // Yazar ve Kategori Bilgileri (Dinamik UserEntity senkronizasyonlu)
            binding.tvAuthorName.text = post.authorName
            binding.tvAuthorTitle.text = post.authorTitle
            binding.tvPostCategory.text = post.category

            // Dinamik Yazar Profil Fotoğrafı
            if (!post.authorAvatarUrl.isNullOrBlank()) {
                binding.ivAuthorAvatar.load(post.authorAvatarUrl) {
                    crossfade(true)
                    placeholder(R.drawable.ic_academic_logo)
                    error(R.drawable.ic_academic_logo)
                }
            } else {
                binding.ivAuthorAvatar.setImageResource(R.drawable.ic_academic_logo)
            }

            // Makale Başlığı ve Açıklaması
            binding.tvPostTitle.text = post.title
            binding.tvPostDescription.text = post.description
            binding.tvPdfSize.text = "• ${post.pdfSize}"

            // PDF Gerçek Kapak Önizlemesi (PdfRenderer 1. Sayfa Thumbnail)
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

            // Kapak tıklaması da okuyucuyu açar
            binding.containerCoverPreview.setOnClickListener {
                onReadPdfClicked(post)
            }

            // Paylaşım Tarihi (Bağıl Zaman)
            binding.tvPostDate.text = getRelativeTimeString(context, post.createdAt)

            // Beğeni Sayısı ve İkon Durumu
            binding.tvLikeCount.text = post.likeCount.toString()
            if (post.isLiked) {
                binding.ivLikeIcon.setImageResource(R.drawable.ic_heart_filled)
                binding.tvLikeCount.setTextColor(ContextCompat.getColor(context, R.color.gold_start))
            } else {
                binding.ivLikeIcon.setImageResource(R.drawable.ic_heart_outline)
                binding.tvLikeCount.setTextColor(ContextCompat.getColor(context, R.color.text_secondary))
            }

            // Yorum Sayısı
            binding.tvCommentCount.text = post.commentCount.toString()

            // Üç Nokta / Seçenekler Menüsü (Silme ve Paylaşma - FAZ 8)
            val isMyPost = (currentUserId != null && post.userId == currentUserId) ||
                    (currentUserName != null && post.authorName.equals(currentUserName, ignoreCase = true))

            binding.btnPostOptions.setOnClickListener { view ->
                val popup = PopupMenu(context, view)
                if (isMyPost) {
                    popup.menu.add(0, 1, 0, "Risaleyi Sil 🗑️")
                }
                popup.menu.add(0, 2, 1, "Eseri Dışa Paylaş 📤")
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

            // Tıklama Olayları
            val authorClickListener = {
                onAuthorClicked?.invoke(post.authorName, post.authorAvatarUrl)
            }
            binding.ivAuthorAvatar.setOnClickListener { authorClickListener() }
            binding.tvAuthorName.setOnClickListener { authorClickListener() }
            binding.tvAuthorTitle.setOnClickListener { authorClickListener() }

            // Tok Haptik Titreşimli Beğen Butonu
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

        private fun getRelativeTimeString(context: Context, timeMillis: Long): String {
            val now = System.currentTimeMillis()
            val diff = now - timeMillis
            return when {
                diff < DateUtils.MINUTE_IN_MILLIS -> "Az önce"
                diff < DateUtils.HOUR_IN_MILLIS -> "${diff / DateUtils.MINUTE_IN_MILLIS} dk önce"
                diff < DateUtils.DAY_IN_MILLIS -> "${diff / DateUtils.HOUR_IN_MILLIS} sa önce"
                else -> "${diff / DateUtils.DAY_IN_MILLIS} gün önce"
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
