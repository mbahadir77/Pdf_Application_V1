package com.example.ui.comment

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.PopupMenu
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.example.R
import com.example.databinding.ItemCommentBinding

data class DisplayComment(
    val id: Long,
    val userId: String? = null,
    val authorName: String,
    val avatarUrl: String?,
    val body: String,
    val dateText: String
)

object CommentDiffCallback : DiffUtil.ItemCallback<DisplayComment>() {
    override fun areItemsTheSame(oldItem: DisplayComment, newItem: DisplayComment): Boolean =
        oldItem.id == newItem.id

    override fun areContentsTheSame(oldItem: DisplayComment, newItem: DisplayComment): Boolean =
        oldItem == newItem
}

/**
 * İlmNet - Akademik Müzakere & Yorum Adaptörü (FAZ 8: ListAdapter + DiffUtil).
 * IndexOutOfBounds ve bellek sızıntılarını önler; yorum silme (CRUD) opsiyonu sunar.
 */
class CommentAdapter(
    private val currentUserName: String? = null,
    private val currentUserId: String? = null,
    private val onDeleteCommentClicked: ((DisplayComment) -> Unit)? = null
) : ListAdapter<DisplayComment, CommentAdapter.ViewHolder>(CommentDiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemCommentBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(private val binding: ItemCommentBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: DisplayComment) {
            val context = binding.root.context
            binding.tvCommentAuthor.text = item.authorName
            binding.tvCommentBody.text = item.body
            binding.tvCommentDate.text = item.dateText

            // Dinamik Avatar
            if (!item.avatarUrl.isNullOrBlank()) {
                binding.ivCommentAvatar.load(item.avatarUrl) {
                    crossfade(true)
                    placeholder(R.drawable.ic_academic_logo)
                    error(R.drawable.ic_academic_logo)
                }
            } else {
                binding.ivCommentAvatar.setImageResource(R.drawable.ic_academic_logo)
            }

            // Yorum Silme Opsiyonu (Yorum sahibine veya yerel kullanıcıya özel)
            val isMyComment = (currentUserId != null && item.userId == currentUserId) ||
                    (currentUserName != null && item.authorName.equals(currentUserName, ignoreCase = true))

            if (isMyComment) {
                binding.btnCommentOptions.visibility = View.VISIBLE
                binding.btnCommentOptions.setOnClickListener { view ->
                    val popup = PopupMenu(context, view)
                    popup.menu.add("Yorumu Sil")
                    popup.setOnMenuItemClickListener { menuItem ->
                        if (menuItem.title == "Yorumu Sil") {
                            onDeleteCommentClicked?.invoke(item)
                            true
                        } else false
                    }
                    popup.show()
                }
            } else {
                binding.btnCommentOptions.visibility = View.GONE
                binding.btnCommentOptions.setOnClickListener(null)
            }
        }
    }
}
