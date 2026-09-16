package com.example.ui.notification

import android.graphics.Color
import android.text.format.DateUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.data.local.entity.NotificationEntity
import com.example.databinding.ItemNotificationBinding

object NotificationDiffCallback : DiffUtil.ItemCallback<NotificationEntity>() {
    override fun areItemsTheSame(oldItem: NotificationEntity, newItem: NotificationEntity): Boolean =
        oldItem.id == newItem.id

    override fun areContentsTheSame(oldItem: NotificationEntity, newItem: NotificationEntity): Boolean =
        oldItem == newItem
}

/**
 * İlmNet - Bildirim Geçmişi Adaptörü (FAZ 8).
 * Okunmamışlar parlak, okunmuşlar soluk; kaydırarak silme destekli.
 */
class NotificationAdapter(
    private val onItemClicked: (NotificationEntity) -> Unit
) : ListAdapter<NotificationEntity, NotificationAdapter.ViewHolder>(NotificationDiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemNotificationBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(private val binding: ItemNotificationBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: NotificationEntity) {
            binding.tvNotificationTitle.text = item.title
            binding.tvNotificationMessage.text = item.message

            // Göreceli Zaman Formatı (Örn: "5 dk önce", "Dün, 15:30")
            val relativeTime = DateUtils.getRelativeTimeSpanString(
                item.timestamp,
                System.currentTimeMillis(),
                DateUtils.MINUTE_IN_MILLIS,
                DateUtils.FORMAT_ABBREV_RELATIVE
            ).toString()
            binding.tvNotificationTime.text = relativeTime

            // Tür İkonu Belirleme
            binding.tvNotificationIcon.text = when (item.type) {
                "MOTIVATION" -> "📚"
                "BADGE" -> "🏆"
                "COMMENT" -> "💬"
                "LIKE" -> "❤️"
                "READ" -> "👁️"
                else -> "📜"
            }

            // Okunmamış / Okunmuş Görsel Ayrımı (FAZ 8 Şartı)
            if (!item.isRead) {
                // Parlak / Canlı Tema
                binding.cardNotification.setCardBackgroundColor(Color.parseColor("#1F2532"))
                binding.cardNotification.strokeColor = Color.parseColor("#80FFD700")
                binding.cardNotification.strokeWidth = 2
                binding.viewUnreadDot.visibility = View.VISIBLE
                binding.tvNotificationTitle.setTextColor(Color.parseColor("#FFD700"))
                binding.tvNotificationMessage.setTextColor(Color.parseColor("#F0F4F8"))
                binding.tvNotificationTime.setTextColor(Color.parseColor("#B0BEC5"))
            } else {
                // Soluk / Arka Planda Kalan Tema
                binding.cardNotification.setCardBackgroundColor(Color.parseColor("#12141A"))
                binding.cardNotification.strokeColor = Color.parseColor("#15FFFFFF")
                binding.cardNotification.strokeWidth = 1
                binding.viewUnreadDot.visibility = View.GONE
                binding.tvNotificationTitle.setTextColor(Color.parseColor("#90A4AE"))
                binding.tvNotificationMessage.setTextColor(Color.parseColor("#607D8B"))
                binding.tvNotificationTime.setTextColor(Color.parseColor("#455A64"))
            }

            binding.root.setOnClickListener {
                onItemClicked(item)
            }
        }
    }
}
