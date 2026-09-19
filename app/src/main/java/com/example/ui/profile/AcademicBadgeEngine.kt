package com.example.ui.profile

import com.example.data.category.CategoryManager

/**
 * İlim Diyârı - 20 Kademeli Dev Rütbe ve Rozet Motoru (FAZ 9 Mega Update).
 * 20 merkezi kategorinin her biri için 1'den 20'ye kadar gerçek ilmi usul hiyerarşisi
 * ve modern samimi/zekice unvanlar barındıran devasa rütbe motoru.
 */
data class AcademicBadge(
    val category: String,
    val description: String,
    val icon: String,
    val count: Int,
    val level: Int, // 0: Kilitli, 1..20: Seviyeler
    val rankTitle: String,
    val tierCategoryName: String, // "Bakır Kademe", "Gümüş Kademe", "Altın Kademe", "Elmas Zirve", "Kilitli"
    val tierIcon: String,
    val tierColorHex: String,
    val targetCount: Int,
    val progressText: String,
    val progressPercent: Int,
    val isUnlocked: Boolean,
    val nextRankTitle: String?,
    val currentTier: Int = when (level) {
        in 16..20 -> 4
        in 11..15 -> 3
        in 6..10 -> 2
        in 1..5 -> 1
        else -> 0
    },
    val tierTitle: String = rankTitle,
    val vectorIconRes: Int = AcademicBadgeEngine.getBadgeVectorRes(category, rankTitle),
    val badgeImageRes: Int = BadgeImageMapper.getStaticBadgeRes(
        when (level) {
            in 16..20 -> 4
            in 11..15 -> 3
            in 6..10 -> 2
            in 1..5 -> 1
            else -> 0
        },
        isUnlocked
    ),
    val tierBadgeVectorRes: Int = AcademicBadgeEngine.getTierBadgeVectorRes(
        when (level) {
            in 16..20 -> 4
            in 11..15 -> 3
            in 6..10 -> 2
            in 1..5 -> 1
            else -> 0
        },
        isUnlocked
    )
)

object AcademicBadgeEngine {

    /**
     * 1'den 20'ye seviye eşikleri (Gerekli paylaşılan/okunan eser sayısı)
     */
    val LEVEL_THRESHOLDS = intArrayOf(
        1,   // Seviye 1 (Mübtedî / Çırak)
        2,   // Seviye 2
        3,   // Seviye 3
        5,   // Seviye 4
        7,   // Seviye 5 (Bakır Zirve)
        10,  // Seviye 6 (Gümüş Başlangıç)
        13,  // Seviye 7
        16,  // Seviye 8
        20,  // Seviye 9
        25,  // Seviye 10 (Gümüş Zirve)
        30,  // Seviye 11 (Altın Başlangıç)
        36,  // Seviye 12
        42,  // Seviye 13
        50,  // Seviye 14
        60,  // Seviye 15 (Altın Zirve)
        72,  // Seviye 16 (Elmas Başlangıç)
        85,  // Seviye 17
        100, // Seviye 18
        120, // Seviye 19
        150  // Seviye 20 (Allâme / Mutlak Zirve)
    )

    /**
     * 20 Kategorinin her biri için tam 20 seviyelik rütbe unvanları haritası.
     */
    private val RANK_TREES: Map<String, List<String>> = mapOf(
        "Tefsir" to listOf(
            "Tâlib-i Kelâm", "Müsteid", "Kâri-i Âyât", "Muallim-i Kelâm", "Müstemî-i Vahy",
            "Mütâli-i Mushaf", "Müfessir Çırağı", "Nâkil-i Tefsîr", "Şârih-i Âyât", "Müfessir-i Sânî",
            "Kâtib-i Tefsîr", "Muhakkik-i Âyât", "Sâhib-i Dirâyet", "Müfessir", "Hâfız-ı Kelâmullah",
            "Üstâzü'l-Müfessirîn", "Fahrü'l-Müfessirîn", "Sultânü'l-Müfessirîn", "Huccetü'l-Kelâm", "Allâme-i Cihan (Zemahşerî-i Sânî)"
        ),
        "Hadis" to listOf(
            "Tâlibü'l-Hadîs", "Müstemî-i Âsâr", "Nâkilü'l-Hadîs", "Râvi-i Sadûk", "Tâlib-i Rivâyet",
            "Zâbıt-ı Rivâyet", "Hâfız-ı Sünnet", "Sika-i Sebte", "Müsnidü'l-Vakt", "Râviyü'l-Asr",
            "Nâkidü's-Sened", "Muhakkikü'l-Âsâr", "Muhaddis", "Müdekkik-i Sünnet", "Hâfızü'l-Hadîs",
            "Hüccetü'l-Hadîs", "Hâkimü'l-Hadîs", "Şeyhü'l-Muhaddisîn", "Fahrü's-Sünne", "Emîrü'l-Mü'minîn fi'l-Hadîs (Buhârî-i Sânî)"
        ),
        "Fıkıh" to listOf(
            "Müteallim-i Ahkâm", "Tâlib-i Fıkıh", "Müştehî", "Mütâlaacı-i Şer'", "Maslahatgüzâr",
            "Ehl-i Fetvâ Tâlibi", "Mültezim-i Ahkâm", "Nâzırü'l-Ahkâm", "Fakîh-i Nevres", "Fakîh",
            "Ehl-i Terkîk", "Ashâb-ı Tahrîc", "Ashâb-ı Temyîz", "Ashâb-ı Tercîh", "Müftî-i Enâm",
            "Müctehid fi'l-Mes'ele", "Müctehid fi'l-Mezheb", "Müctehid-i Mutlak", "Şeyhü'l-İslâm", "Allâme-i Fıkıh (Ebû Hanîfe-i Asr)"
        ),
        "Siyer" to listOf(
            "Tâlib-i Siyer", "Siyer Okuru", "Nâkil-i Megâzî", "Âşık-ı Nebî", "Râvi-i Siyer",
            "Ashâb-ı Vakar", "Hâdim-i Siyer", "Müverrih-i Saâdet", "Vak'anüvis-i Nebevî", "Siyer Araştırmacısı",
            "Muhakkik-i Megâzî", "Şârih-i Şemâil", "Şemâil-i Şerîf Üstadı", "Mütehassıs-ı Siyer", "Hâfız-ı Asr-ı Saâdet",
            "Fahrü's-Siyer", "Allâme-i Siyer", "Şeyhü's-Siyer", "Muallimü'l-Megâzî", "Kutbü's-Siyer (İbn Hişâm-ı Asr)"
        ),
        "Akaid" to listOf(
            "Tâlib-i Tevhid", "Müstemî-i Akîde", "Ehl-i Tasdîk", "Muakkid-i Nevres", "Ehl-i Delîl",
            "Ehl-i Basîret", "Tahkîk Yolcusu", "Müdâfi-i Akîde", "Hâdim-i Ehl-i Sünnet", "Muakkid",
            "Ehl-i Tahkîk", "Burhânü't-Tevhîd", "Muhakkik-i Akâid", "Nâsırü't-Tevhîd", "Üstâzü'l-İtikâd",
            "Kal'atü't-Tevhîd", "Fahrü'l-Akâid", "Huccetü't-Tevhîd", "Şeyhü'l-Akîde", "Kutbü'l-İtikâd (Tahâvî-i Asr)"
        ),
        "Kelâm" to listOf(
            "Tâlib-i Kelâm", "Nazar Meraklısı", "Ehl-i Münâzara", "İstidlâl Yolcusu", "Mütekellim Çırağı",
            "Burhancı", "Ehl-i Reddiye", "Kelâmî", "Sâhibü'n-Nazar", "Mütekellim",
            "Cehbîz-i Kelâm", "Şârih-i Mevâkıf", "Müsbitü'l-Akâid", "Muhakkik-i Kelâm", "Şemsü'l-Kelâm",
            "Seyfü'l-Kelâm", "Fahrü'l-Mütekellimîn", "Huccetü'l-İslâm", "Üstâzü'l-Kelâm", "Sultânü'l-Mütekellimîn (Gazzâlî-i Sânî)"
        ),
        "Tefsir Usulü" to listOf(
            "Tâlib-i Usûl-i Tefsîr", "Mübdî-i Tenzîl", "Nüzûl Çırağı", "Garîbü'l-Kur'an Okuru", "Nâsih-Mensûh Tâlibi",
            "İ'câz Meraklısı", "Muharrir-i Vahy", "Ehl-i Tertîl Usûlü", "Kâşif-i Esbâb-ı Nüzûl", "Usûl-i Tefsîrî",
            "Muhakkik-i Nüzûl", "Müdekkik-i Kırâat", "Şârih-i İtkân", "Üstâd-ı Tefsîr Usûlü", "Sâhib-i Burhân fi'l-Ulûm",
            "Fahrü'l-Usûl", "Huccetü't-Tenzîl", "Allâme-i Tefsîr Usûlü", "Seyfü'l-Beyân", "Allâme-i Asr (Süyûtî-i Sânî)"
        ),
        "Hadis Usulü" to listOf(
            "Tâlib-i Mustalah", "Mübtedî-i Usûl", "Senet Çırağı", "Metin Okuru", "Cerh Meraklısı",
            "Ta'dîl Yolcusu", "Sâhib-i Tahrîc", "Istılahçı", "Ricâl Heveslisi", "Mustalahî",
            "Cehbîz-i Ricâl", "Nâkid-i İlel", "Şârih-i Nuhbe", "Sâhibü't-Ta'dîl", "Müdekkik-i Cerh ve Ta'dîl",
            "Şeyhü'l-Mustalah", "Huccetü'l-İlel", "Fahrü'r-Ricâl", "Allâme-i Usûl-i Hadîs", "Kutbü'l-Hadîs Usûlü (İbn Hacer-i Asr)"
        ),
        "Usul-ü Fıkıh" to listOf(
            "Tâlib-i Kıyâs", "Mübdî-i Usûl", "Delîl Talebesi", "İstinbât Çırağı", "Lafızlar Kâşifi",
            "Âmm-Hâss Okuru", "Ehl-i İcmâ", "Makâsıd Yolcusu", "Muharrirü'l-Kavâid", "Usûlî",
            "Sâhib-i Menât", "Mütenakkıd-ı Edille", "Müctehid-i Usûl", "Şârih-i Menâr", "Üstâzü'l-Kıyâs",
            "Kutbü'l-İstinbât", "Fahrü'l-Usûliyyîn", "Şeyhü'l-Usûl", "Huccetü'l-Kıyâs", "Sultânü'l-Usûl (Şâtıbî-i Asr)"
        ),
        "Arapça Dil Bilgisi (Sarf-Nahv)" to listOf(
            "Tâlib-i İ'rab", "Kelime Avcısı", "Mübtedî-i Emsile", "Binâ Çırağı", "Maksûd Yolcusu",
            "Avâmil Müteallimi", "İzhâr Okuru", "Kâfiye Sevdalısı", "Fâil-Mef'ûl Sarrafı", "Nahvî",
            "Sarf Ustası", "Şârih-i Katrunnedâ", "Cezb-i İ'rab", "Müdekkik-i Elfiyye", "Cehbîz-i Nahiv",
            "İbnü'l-Hâcib-i Sânî", "Fahrü'n-Nuhât", "Şeyhü'l-Lisân", "Huccetü'l-İ'rab", "Sultânü'n-Nuhât (Sîbeveyh-i Asr)"
        ),
        "Kıraat" to listOf(
            "Tâlib-i Tecvîd", "Mübtedî-i Tertîl", "Mahreç Talebesi", "Kâri Heveslisi", "Ehl-i Medd",
            "Vakf-İbtidâ Çırağı", "Hafs Râvisi", "Şu'be Mütâlaacısı", "Cüz Hâfızı", "Kârî-i Kur'an",
            "Aşere Yolcusu", "Takrîb Tâlibi", "Tayyibe Şârihi", "Hâfız-ı Mutkın", "Ehl-i Seb'a",
            "Kurrâ-i Aşere", "Şeyhü'l-Kurrâ", "Fahrü'l-Kurrâ", "Allâme-i Kırâat", "Reîsü'l-Kurrâ (İbnü'l-Cezerî-i Asr)"
        ),
        "Tasavvuf" to listOf(
            "Tâlib-i Hakîkat", "Muhibb", "Mübtedî Sâlik", "Ehl-i Zikir", "Mücâhede Yolcusu",
            "Sâlik", "Ehl-i Halvet", "Ehl-i Riyâzet", "Ehl-i Vefâ", "Ehl-i İrfân",
            "Ehl-i Fenâ", "Sâhib-i Ahlâk", "Ârif-i Billâh", "Vâsıl-ı İrfân", "Pîr-i Tarîkat",
            "Kutbü'l-Ârifîn", "Şeyhü'l-Ekber-i Asr", "Gavsü'l-Vakt", "Fahrü't-Tasavvuf", "Sultânü'l-Ârifîn (Cüneyd-i Bağdâdî-i Asr)"
        ),
        "Tarih" to listOf(
            "Tarih Meraklısı", "Kronoloji Yolcusu", "Nâkil-i Ahbâr", "Arşiv Çırağı", "Vak'anüvis Çırağı",
            "Belge Avcısı", "Sahîfe Gezgini", "Sâhib-i Salnâme", "Vak'anüvis", "Müverrih",
            "Arşiv Muhafızı", "Muhakkik-i Tarih", "Şârih-i Mukaddime", "Medeniyet Tarihçisi", "Cehbîz-i Tevârih",
            "Üstâd-ı Müverrihîn", "Fahrü'l-Müverrihîn", "Şeyhü't-Tarih", "Huccetü'l-Ahbâr", "Kutbü't-Tevârih (İbn Haldûn-ı Asr)"
        ),
        "Mantık/Felsefe" to listOf(
            "Tâlib-i Hikmet", "Meraklı Zihin", "Soru Avcısı", "Kıyâs Çırağı", "Tefekkür Yolcusu",
            "Burhân Talebesi", "Tenkidî Akıl", "Şüphe Avcısı", "Mantıkî", "Feylesof",
            "Sâhib-i Tahlîl", "Cehbîz-i Felsefe", "Muhakkik-i Mantık", "Hikmet-i Meşşâiyye Üstadı", "Sâhib-i İşrâk",
            "Muallim-i Sânî", "Fahrü'l-Hukemâ", "Şeyhü'r-Reîs", "Huccetü'l-Felsefe", "Sultânü'l-Hukemâ (Fârâbî-i Asr)"
        ),
        "Roman" to listOf(
            "Satır Çırağı", "Sayfa Karıştıran", "Meraklı Okur", "Kahve Yanı Okuru", "Satır Arası Yolcusu",
            "Bölüm Avcısı", "Kitap Kurdu", "Kurgu Dedektifi", "Uykusuz Okur", "Sayfa Fatihi",
            "Edebî Seyyah", "Cilt Avcısı", "Karakter Sarrafı", "Roman Müptelası", "Kurgu Mimarı",
            "Kitaplık Hükümdarı", "Kelime Büyücüsü", "Üstat Romancı", "Edebiyat Şövalyesi", "Kitaplar Sultanı (Dostoyevski-i Asr)"
        ),
        "Makale/Dergi" to listOf(
            "Taslak Okuru", "Dipnot Meraklısı", "Giriş Yazarı", "Kaynakça Avcısı", "Sayı Takipçisi",
            "Makale Kurdu", "Cilt Müdavimi", "Hakem Adayı", "Süreli Yayın Âşığı", "Muharrir",
            "Tahlilci Kalem", "Hakem Kurulu Üyesi", "Başmuharrir", "Yayın Yönetmeni", "Akademik Kalemşor",
            "İndeks Fatihi", "Mümtaz Müellif", "Edebî Muharrir-i A'zam", "Başyazar-ı Dihkân", "Matbuat Sultanı"
        ),
        "Tez" to listOf(
            "Tez Heveslisi", "Konu Arayan", "Literatür Tarayıcısı", "Danışman Mağduru", "Hipotez Avcısı",
            "Gececi Araştırmacı", "Uykusuz Tezci", "Dipnot Fatihi", "Metodoloji Ustası", "Tez Canlı",
            "Taslak Avcısı", "Savunma Gazisi", "Düzeltme Kahramanı", "Jüri Fatihi", "Doktoralı Savaşçı",
            "Cübbe Sahibi", "Akademik Pîr", "Tez Fabrikası", "Ordinaryüs Zihin", "Kürsü Sahibi Üstat"
        ),
        "Özet" to listOf(
            "Altını Çizen", "Fosforlu Kalem", "Ana Fikir Avcısı", "Kenar Notçusu", "Sayfa Süzgeci",
            "Hülasacı", "Veciz Kalem", "Kısa ve Öz", "Muhtasar Meraklısı", "Özetbaz",
            "Zübde-i Kelâm", "Muhtasar Ustası", "Hikmet Damlası", "Cümle Mühendisi", "İ'câz Üstadı",
            "Lübbü'l-Lüb", "Fihrist-i Cihan", "Özetin Şâhı", "Az Sözle Çok Mana", "Özetin Özeti"
        ),
        "Sözlük" to listOf(
            "Kelime Meraklısı", "Madde Okuru", "Kök Arayıcısı", "Lugat Çırağı", "İştikak Yolcusu",
            "Kelime Avcısı", "Sayfa Karıştırıcısı", "Lügatperver", "Kâmus Dostu", "Kelime Sarrafı",
            "Fasîh Kalem", "Lugavî Zihin", "Kâmus Bekçisi", "Istılah Avcısı", "Lisân Üstadı",
            "Ansiklopedik Zihin", "Sâhib-i Kâmus", "Ayaklı Kütüphane", "Ayaklı Lügat", "Kâmus-ı A'zam (Cevherî-i Asr)"
        ),
        "Diğer" to listOf(
            "Meraklı Tâlip", "İlim Yolcusu", "Çırak Araştırmacı", "Satır Kâşifi", "Not Defteri",
            "Bilgi Avcısı", "Serbest Mütefekkir", "Kitapsever", "Mütecessis Kalem", "Hezarfen Çırağı",
            "Disiplinlerüstü Zihin", "Çok Yönlü Okur", "Ansiklopedist", "Hür Araştırmacı", "İrfân Hamalı",
            "İrfân Köprüsü", "Mütebahhir Zihin", "Allâme-i Fenûn", "Hezarfen", "Câmiu'l-Fünûn"
        )
    )

    /**
     * Kullanıcının kategori bazlı paylaştığı/okuduğu eser sayısına göre
     * tam 20 kategorilik 20 seviyeli rozet listesini hesaplar.
     */
    fun calculateBadges(categoryCountMap: Map<String, Int>): List<AcademicBadge> {
        return CategoryManager.CATEGORIES.map { category ->
            val count = categoryCountMap.entries
                .filter { it.key.contains(category.name, ignoreCase = true) || category.name.contains(it.key, ignoreCase = true) }
                .sumOf { it.value }

            val rankList = RANK_TREES[category.name] ?: RANK_TREES["Diğer"]!!

            // Seviye belirleme (0..20)
            var level = 0
            for (i in LEVEL_THRESHOLDS.indices.reversed()) {
                if (count >= LEVEL_THRESHOLDS[i]) {
                    level = i + 1
                    break
                }
            }

            val isUnlocked = level > 0
            val rankTitle: String
            val tierCategoryName: String
            val tierIcon: String
            val tierColor: String
            val targetCount: Int
            val progressText: String
            val progressPercent: Int
            val nextRankTitle: String?

            if (level == 20) {
                // Zirve (20. Seviye)
                rankTitle = rankList[19]
                tierCategoryName = "Elmas Zirve (Seviye 20)"
                tierIcon = "Elmas"
                tierColor = "#00E5FF"
                targetCount = LEVEL_THRESHOLDS[19]
                progressText = "$count/$targetCount"
                progressPercent = 100
                nextRankTitle = null
            } else if (level > 0) {
                val currentRankIndex = level - 1
                val nextRankIndex = level
                rankTitle = rankList[currentRankIndex]
                val nextThreshold = LEVEL_THRESHOLDS[nextRankIndex]
                targetCount = nextThreshold
                progressText = "$count/$nextThreshold"
                progressPercent = ((count * 100) / nextThreshold).coerceIn(0, 100)
                nextRankTitle = rankList[nextRankIndex]

                when (level) {
                    in 16..19 -> {
                        tierCategoryName = "Elmas Kademe (Seviye $level)"
                        tierIcon = "Elmas"
                        tierColor = "#00E5FF"
                    }
                    in 11..15 -> {
                        tierCategoryName = "Altın Kademe (Seviye $level)"
                        tierIcon = "Altın"
                        tierColor = "#FFD700"
                    }
                    in 6..10 -> {
                        tierCategoryName = "Gümüş Kademe (Seviye $level)"
                        tierIcon = "Gümüş"
                        tierColor = "#C0C0C0"
                    }
                    else -> {
                        tierCategoryName = "Bakır Kademe (Seviye $level)"
                        tierIcon = "Bakır"
                        tierColor = "#CD7F32"
                    }
                }
            } else {
                // Kilitli (0 Eser)
                rankTitle = "Kilitli (Mübtedî)"
                tierCategoryName = "Kilitli"
                tierIcon = "Kilitli"
                tierColor = "#64748B"
                targetCount = LEVEL_THRESHOLDS[0]
                progressText = "$count/$targetCount"
                progressPercent = 0
                nextRankTitle = rankList[0]
            }

            val dynamicDesc = if (isUnlocked) {
                "$rankTitle: $count ${category.name} eseri mütalaa ettin/paylaştın."
            } else {
                "Bu kategoride henüz eser paylaşılmadı. İlk eseri paylaşarak '${rankList[0]}' unvanını kazan!"
            }

            AcademicBadge(
                category = category.name,
                description = dynamicDesc,
                icon = category.icon,
                count = count,
                level = level,
                rankTitle = rankTitle,
                tierCategoryName = tierCategoryName,
                tierIcon = tierIcon,
                tierColorHex = tierColor,
                targetCount = targetCount,
                progressText = progressText,
                progressPercent = progressPercent,
                isUnlocked = isUnlocked,
                nextRankTitle = nextRankTitle
            )
        }
    }

    /**
     * Kategori adına göre rütbe ağacını döndürür (1'den 20'ye).
     */
    fun getRankTreeForCategory(categoryName: String): List<String> {
        return RANK_TREES[categoryName] ?: RANK_TREES["Diğer"]!!
    }

    /**
     * Rozet ve rütbe unvanına göre vektör ikon kaynağını belirler (Faz 11).
     */
    fun getBadgeVectorRes(category: String, rankTitle: String): Int {
        val lower = rankTitle.lowercase()
        val catLower = category.lowercase()
        return when {
            lower.contains("itikaf") || lower.contains("irade") -> com.example.R.drawable.ic_badge_itikaf
            lower.contains("hâfız") || lower.contains("hafız") || lower.contains("allâme") ||
                lower.contains("sultan") || lower.contains("zirve") || lower.contains("kupa") ||
                lower.contains("üstad") || lower.contains("allame") -> com.example.R.drawable.ic_badge_trophy
            lower.contains("kurt") || lower.contains("okur") || lower.contains("fatih") ||
                catLower.contains("roman") || catLower.contains("dergi") || lower.contains("araştırmacı") -> com.example.R.drawable.ic_badge_glasses
            catLower.contains("fıkıh") || catLower.contains("usul") || lower.contains("ahkâm") ||
                lower.contains("kıyâs") || lower.contains("fetvâ") -> com.example.R.drawable.ic_badge_scroll
            catLower.contains("arapça") || catLower.contains("nahv") || lower.contains("kalem") ||
                lower.contains("muharrir") || lower.contains("kâtib") -> com.example.R.drawable.ic_badge_pen
            catLower.contains("akaid") || catLower.contains("kelâm") || lower.contains("tevhid") ||
                lower.contains("burhân") -> com.example.R.drawable.ic_badge_star
            else -> com.example.R.drawable.ic_badge_book
        }
    }

    /**
     * Kademeye göre gerçek vektörel rozet madalyonunu döndürür (Bölüm 3.2).
     */
    fun getTierBadgeVectorRes(currentTier: Int, isUnlocked: Boolean): Int {
        if (!isUnlocked) return com.example.R.drawable.ic_badge_locked_vector
        return when (currentTier) {
            4 -> com.example.R.drawable.ic_badge_diamond
            3 -> com.example.R.drawable.ic_badge_gold
            2 -> com.example.R.drawable.ic_badge_silver
            else -> com.example.R.drawable.ic_badge_bronze
        }
    }
}
