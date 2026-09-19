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
 * İlim Diyârı - Gerçek Vektörel Rozet ve Rütbe Adaptörü (Faz 13).
 * Emojilerden arındırılmış, yüksek çözünürlüklü vektör madalyonlar (Bronz, Gümüş, Altın, Elmas).
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

            // Gerçek Vektörel Madalyon İkonu (Bölüm 3.2)
            binding.ivBadgeTierIcon.setImageResource(badge.tierBadgeVectorRes)

            binding.root.setOnClickListener {
                onBadgeClick?.invoke(badge)
            }

            if (badge.isUnlocked) {
                binding.cardBadgeRoot.alpha = 1.0f
                val tierColor = try {
                    Color.parseColor(badge.tierColorHex)
                } catch (e: Exception) {
                    Color.parseColor("#FFD700")
                }

                // Seviye Etiketi (Örn: LVL 14)
                binding.tvBadgeLevelTag.text = "LVL ${badge.level}"
                binding.tvBadgeLevelTag.visibility = View.VISIBLE
                binding.tvBadgeLevelTag.setTextColor(tierColor)

                // Rütbe Başlığı
                binding.tvBadgeTierTitle.text = badge.rankTitle
                binding.tvBadgeTierTitle.setTextColor(tierColor)

                // Arka plan: Elmas / Altın / Gümüş / Bakır
                when (badge.currentTier) {
                    4 -> {
                        // Elmas Zirve (Seviye 16-20)
                        binding.cardBadgeRoot.setBackgroundResource(R.drawable.bg_badge_diamond)
                        binding.tvBadgeCategory.setTextColor(tierColor)
                        binding.tvBadgeProgressCounter.setTextColor(tierColor)
                        binding.pbBadgeProgress.progressTintList = ColorStateList.valueOf(tierColor)
                    }
                    3 -> {
                        // Altın Kademe (Seviye 11-15)
                        binding.cardBadgeRoot.setBackgroundResource(R.drawable.bg_badge_unlocked)
                        binding.tvBadgeCategory.setTextColor(ContextCompat.getColor(context, R.color.gold_vibrant))
                        binding.tvBadgeProgressCounter.setTextColor(ContextCompat.getColor(context, R.color.gold_start))
                        binding.pbBadgeProgress.progressTintList = ColorStateList.valueOf(tierColor)
                    }
                    2 -> {
                        // Gümüş Kademe (Seviye 6-10)
                        binding.cardBadgeRoot.setBackgroundResource(R.drawable.bg_badge_unlocked)
                        binding.tvBadgeCategory.setTextColor(Color.parseColor("#E2E8F0"))
                        binding.tvBadgeProgressCounter.setTextColor(Color.parseColor("#CBD5E1"))
                        binding.pbBadgeProgress.progressTintList = ColorStateList.valueOf(tierColor)
                    }
                    else -> {
                        // Bakır Kademe (Seviye 1-5)
                        binding.cardBadgeRoot.setBackgroundResource(R.drawable.bg_badge_bronze)
                        binding.tvBadgeCategory.setTextColor(tierColor)
                        binding.tvBadgeProgressCounter.setTextColor(tierColor)
                        binding.pbBadgeProgress.progressTintList = ColorStateList.valueOf(tierColor)
                    }
                }

                // Vektörel Yıldızlar (Emoji Değil, Vektör İkon)
                binding.layoutBadgeStars.visibility = View.VISIBLE
                binding.ivBadgeStar1.setImageResource(if (badge.level >= 5) R.drawable.ic_star_filled else R.drawable.ic_star_border)
                binding.ivBadgeStar2.setImageResource(if (badge.level >= 10) R.drawable.ic_star_filled else R.drawable.ic_star_border)
                binding.ivBadgeStar3.setImageResource(if (badge.level >= 15) R.drawable.ic_star_filled else R.drawable.ic_star_border)
                binding.ivBadgeStar4.setImageResource(if (badge.level >= 20) R.drawable.ic_star_filled else R.drawable.ic_star_border)

                if (badge.nextRankTitle != null) {
                    binding.tvBadgeNextTierHint.text = "Sıradaki: ${badge.nextRankTitle}"
                    binding.tvBadgeNextTierHint.visibility = View.VISIBLE
                } else {
                    binding.tvBadgeNextTierHint.text = "Zirve Rütbe"
                    binding.tvBadgeNextTierHint.visibility = View.VISIBLE
                }
            } else {
                // Kilitli Rozet: Gri / Soluk
                binding.cardBadgeRoot.setBackgroundResource(R.drawable.bg_badge_locked)
                binding.cardBadgeRoot.alpha = 0.55f
                binding.tvBadgeCategory.setTextColor(Color.parseColor("#A0AEC0"))
                binding.tvBadgeLevelTag.text = "KİLİTLİ"
                binding.tvBadgeLevelTag.setTextColor(Color.parseColor("#718096"))
                binding.tvBadgeTierTitle.text = "Mübtedî (Kilitli)"
                binding.tvBadgeTierTitle.setTextColor(Color.parseColor("#718096"))
                binding.tvBadgeProgressCounter.setTextColor(Color.parseColor("#718096"))
                binding.pbBadgeProgress.progressTintList = ColorStateList.valueOf(Color.parseColor("#4A5568"))

                binding.layoutBadgeStars.visibility = View.GONE
                binding.tvBadgeNextTierHint.text = "Hedef: 1 Eser (${badge.nextRankTitle ?: "Aç"})"
                binding.tvBadgeNextTierHint.visibility = View.VISIBLE
            }
        }
    }
}
