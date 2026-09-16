package com.example.ui.profile

import com.example.data.category.CategoryManager

/**
 * İlmNet - Akademik Rozet Modeli (Faz 5 & Faz 7).
 * CategoryManager içindeki tam 20 ilim dalını esas alan, 4 kademeli askeri rütbe hissiyatlı elmas rozet sistemi:
 * 1 Eser = Bakır (Başlangıç): Mat, koyu bakır renkli (#CD7F32) 🥉
 * 5 Eser = Metal / Gümüş (Orta): Parlak metalik gri (#C0C0C0) 🥈
 * 15 Eser = Altın (İleri): Göz alıcı ışıltılı altın (#FFD700) 🥇
 * 50 Eser = Elmas (Usta / Zirve): Parlayan mavi-beyaz kristal (#00E5FF) 💎
 */
data class AcademicBadge(
    val category: String,
    val description: String,
    val count: Int,
    val currentTier: Int, // 0: Kilitli, 1: Bakır, 2: Gümüş, 3: Altın, 4: Elmas
    val tierTitle: String,
    val tierCategoryName: String, // "Bakır", "Gümüş", "Altın", "Elmas", "Kilitli"
    val tierIcon: String,
    val tierColorHex: String,
    val targetCount: Int,
    val progressText: String,
    val progressPercent: Int,
    val isUnlocked: Boolean,
    val nextTierTitle: String?
)

data class BadgeDefinition(
    val category: String,
    val baseDescription: String,
    val tier1Title: String = "Talip",
    val tier2Title: String,
    val tier3Title: String,
    val tier4Title: String = "Allâme"
)

object AcademicBadgeEngine {

    val CATEGORIES: List<BadgeDefinition> = CategoryManager.CATEGORIES.map { cat ->
        BadgeDefinition(
            category = cat.name,
            baseDescription = cat.description,
            tier1Title = cat.tier1Title,
            tier2Title = cat.tier2Title,
            tier3Title = cat.tier3Title,
            tier4Title = cat.tier4Title
        )
    }

    fun calculateBadges(categoryCountMap: Map<String, Int>): List<AcademicBadge> {
        return CATEGORIES.map { def ->
            val count = categoryCountMap.entries
                .filter { it.key.contains(def.category, ignoreCase = true) || def.category.contains(it.key, ignoreCase = true) }
                .sumOf { it.value }

            val tier: Int
            val title: String
            val tierName: String
            val tierIcon: String
            val tierColor: String
            val target: Int
            val progressText: String
            val progressPercent: Int
            val nextTitle: String?
            val dynamicDesc: String

            when {
                count >= 50 -> {
                    tier = 4
                    title = def.tier4Title
                    tierName = "Elmas Rozet"
                    tierIcon = "💎"
                    tierColor = "#00E5FF"
                    target = 50
                    progressText = "$count/50"
                    progressPercent = 100
                    nextTitle = null
                    dynamicDesc = "${def.tier4Title}: 50 ${def.category} eseri paylaştın. Zirvedesin!"
                }
                count >= 15 -> {
                    tier = 3
                    title = def.tier3Title
                    tierName = "Altın Rozet"
                    tierIcon = "🥇"
                    tierColor = "#FFD700"
                    target = 50
                    progressText = "$count/50"
                    progressPercent = ((count * 100) / 50).coerceIn(0, 100)
                    nextTitle = def.tier4Title
                    dynamicDesc = "${def.tier3Title}: 15 ${def.category} eseri paylaştın."
                }
                count >= 5 -> {
                    tier = 2
                    title = def.tier2Title
                    tierName = "Gümüş Rozet"
                    tierIcon = "🥈"
                    tierColor = "#C0C0C0"
                    target = 15
                    progressText = "$count/15"
                    progressPercent = ((count * 100) / 15).coerceIn(0, 100)
                    nextTitle = def.tier3Title
                    dynamicDesc = "${def.tier2Title}: 5 ${def.category} eseri paylaştın."
                }
                count >= 1 -> {
                    tier = 1
                    title = def.tier1Title
                    tierName = "Bakır Rozet"
                    tierIcon = "🥉"
                    tierColor = "#CD7F32"
                    target = 5
                    progressText = "$count/5"
                    progressPercent = ((count * 100) / 5).coerceIn(0, 100)
                    nextTitle = def.tier2Title
                    dynamicDesc = "${def.tier1Title}: 1 ${def.category} eseri paylaştın."
                }
                else -> {
                    tier = 0
                    title = "Kilitli"
                    tierName = "Kilitli"
                    tierIcon = "🔒"
                    tierColor = "#718096"
                    target = 1
                    progressText = "0/1"
                    progressPercent = 0
                    nextTitle = def.tier1Title
                    dynamicDesc = "İlk ${def.category} eserini paylaş ve Bakır rozeti aç!"
                }
            }

            AcademicBadge(
                category = def.category,
                description = dynamicDesc,
                count = count,
                currentTier = tier,
                tierTitle = title,
                tierCategoryName = tierName,
                tierIcon = tierIcon,
                tierColorHex = tierColor,
                targetCount = target,
                progressText = progressText,
                progressPercent = progressPercent,
                isUnlocked = count >= 1,
                nextTierTitle = nextTitle
            )
        }
    }
}
