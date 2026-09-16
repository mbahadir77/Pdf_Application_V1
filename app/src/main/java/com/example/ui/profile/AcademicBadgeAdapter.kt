package com.example.ui.profile

import android.content.res.ColorStateList
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.R
import com.example.databinding.ItemAcademicBadgeBinding

object AcademicBadgeDiffCallback : DiffUtil.ItemCallback<AcademicBadge>() {
    override fun areItemsTheSame(oldItem: AcademicBadge, newItem: AcademicBadge): Boolean =
        oldItem.category == newItem.category

    override fun areContentsTheSame(oldItem: AcademicBadge, newItem: AcademicBadge): Boolean =
        oldItem == newItem
}

/**
 * İlmNet - 20 Akademik Kategori Rozet Matrisi Adaptörü (FAZ 8: ListAdapter + DiffUtil).
 * IndexOutOfBounds çökmelerini ve bellek sızıntılarını önler.
 * 4 Kademeli Askeri Rütbe Hiyerarşisi:
 * - Bakır (1): Mat koyu bakır (#CD7F32) 🥉
 * - Metal/Gümüş (5): Parlak metalik gri (#C0C0C0) 🥈
 * - Altın (15): Göz alıcı ışıltılı altın (#FFD700) 🥇
 * - Elmas (50): Parlayan elmas/kristal mavi (#00E5FF) 💎
 */
class AcademicBadgeAdapter(
    private val onBadgeClick: ((AcademicBadge) -> Unit)? = null
) : ListAdapter<AcademicBadge, AcademicBadgeAdapter.ViewHolder>(AcademicBadgeDiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemAcademicBadgeBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(private val binding: ItemAcademicBadgeBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(badge: AcademicBadge) {
            val context = binding.root.context
            binding.tvBadgeCategory.text = badge.category
            binding.tvBadgeDescription.text = badge.description
            binding.tvBadgeProgressCounter.text = "${badge.progressText} Eser"
            binding.pbBadgeProgress.progress = badge.progressPercent
            binding.tvBadgeTierIcon.text = badge.tierIcon

            binding.root.setOnClickListener {
                onBadgeClick?.invoke(badge)
            }

            if (badge.isUnlocked) {
                binding.cardBadgeRoot.alpha = 1.0f
                val tierColor = Color.parseColor(badge.tierColorHex)

                when (badge.currentTier) {
                    4 -> {
                        // Elmas Rozet (Usta / Zirve) - Mavi-Beyaz Kristal
                        binding.cardBadgeRoot.setBackgroundResource(R.drawable.bg_badge_diamond)
                        binding.tvBadgeCategory.setTextColor(tierColor)
                        binding.tvBadgeTierTitle.setTextColor(tierColor)
                        binding.tvBadgeProgressCounter.setTextColor(tierColor)
                        binding.pbBadgeProgress.progressTintList = ColorStateList.valueOf(tierColor)
                    }
                    3 -> {
                        // Altın Rozet (İleri) - Parlak Işıltılı Altın #FFD700
                        binding.cardBadgeRoot.setBackgroundResource(R.drawable.bg_badge_unlocked)
                        binding.tvBadgeCategory.setTextColor(ContextCompat.getColor(context, R.color.gold_vibrant))
                        binding.tvBadgeTierTitle.setTextColor(ContextCompat.getColor(context, R.color.gold_start))
                        binding.tvBadgeProgressCounter.setTextColor(ContextCompat.getColor(context, R.color.gold_start))
                        binding.pbBadgeProgress.progressTintList = ColorStateList.valueOf(tierColor)
                    }
                    2 -> {
                        // Gümüş / Metal Rozet (Orta) - Parlak Metalik Gri #C0C0C0
                        binding.cardBadgeRoot.setBackgroundResource(R.drawable.bg_badge_unlocked)
                        binding.tvBadgeCategory.setTextColor(Color.parseColor("#E2E8F0"))
                        binding.tvBadgeTierTitle.setTextColor(Color.parseColor("#E2E8F0"))
                        binding.tvBadgeProgressCounter.setTextColor(Color.parseColor("#CBD5E1"))
                        binding.pbBadgeProgress.progressTintList = ColorStateList.valueOf(tierColor)
                    }
                    else -> {
                        // Bakır Rozet (Başlangıç) - Mat Koyu Bakır #CD7F32
                        binding.cardBadgeRoot.setBackgroundResource(R.drawable.bg_badge_bronze)
                        binding.tvBadgeCategory.setTextColor(tierColor)
                        binding.tvBadgeTierTitle.setTextColor(tierColor)
                        binding.tvBadgeProgressCounter.setTextColor(tierColor)
                        binding.pbBadgeProgress.progressTintList = ColorStateList.valueOf(tierColor)
                    }
                }

                binding.tvBadgeTierTitle.text = "${badge.tierTitle} (${badge.tierCategoryName})"
                binding.tvBadgeTierTitle.setBackgroundResource(R.drawable.bg_glass_badge)

                // Yıldızları rütbeye göre güncelle (1..4)
                binding.layoutBadgeStars.visibility = View.VISIBLE
                binding.tvBadgeStar1.text = if (badge.currentTier >= 1) "⭐" else "☆"
                binding.tvBadgeStar2.text = if (badge.currentTier >= 2) "⭐" else "☆"
                binding.tvBadgeStar3.text = if (badge.currentTier >= 3) "⭐" else "☆"
                binding.tvBadgeStar4.text = if (badge.currentTier >= 4) "⭐" else "☆"

                if (badge.nextTierTitle != null) {
                    binding.tvBadgeNextTierHint.text = "Sıradaki: ${badge.nextTierTitle}"
                    binding.tvBadgeNextTierHint.visibility = View.VISIBLE
                } else {
                    binding.tvBadgeNextTierHint.text = "Zirve Rütbe (Elmas)"
                    binding.tvBadgeNextTierHint.visibility = View.VISIBLE
                }
            } else {
                // Kilitli Rozet: Gri / Soluk
                binding.cardBadgeRoot.setBackgroundResource(R.drawable.bg_badge_locked)
                binding.cardBadgeRoot.alpha = 0.5f
                binding.tvBadgeCategory.setTextColor(Color.parseColor("#A0AEC0"))
                binding.tvBadgeTierTitle.text = "🔒 Kilitli"
                binding.tvBadgeTierTitle.setTextColor(Color.parseColor("#718096"))
                binding.tvBadgeTierTitle.background = null
                binding.tvBadgeProgressCounter.setTextColor(Color.parseColor("#718096"))
                binding.pbBadgeProgress.progressTintList = ColorStateList.valueOf(Color.parseColor("#4A5568"))

                binding.layoutBadgeStars.visibility = View.GONE
                binding.tvBadgeNextTierHint.text = "Hedef: Bakır (1 Eser)"
                binding.tvBadgeNextTierHint.visibility = View.VISIBLE
            }
        }
    }
}
