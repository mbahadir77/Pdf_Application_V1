package com.example.data.category

/**
 * İlmNet - Merkezi Kategori ve İlim Dalları Yöneticisi (Faz 7).
 * AddPdf (Eser Ekleme), Profile (Rozet ve Rütbe Motoru), Keşfet ve İstatistik
 * ekranlarındaki tam 20 kategoriyi tek bir kaynaktan yönetir.
 */
data class AcademicCategory(
    val id: String,
    val name: String,
    val icon: String,
    val description: String,
    val tier1Title: String = "Talip",
    val tier2Title: String,
    val tier3Title: String,
    val tier4Title: String = "Allâme"
)

object CategoryManager {

    /**
     * İlmNet platformunun resmi ve tam 20 kategorisi.
     */
    val CATEGORIES: List<AcademicCategory> = listOf(
        AcademicCategory(
            id = "akaid",
            name = "Akaid",
            icon = "🏛️",
            description = "İman Esasları, İtikad ve Tevhid İncelemeleri",
            tier1Title = "Talip",
            tier2Title = "Muakkid",
            tier3Title = "Mütefekkir",
            tier4Title = "Allâme"
        ),
        AcademicCategory(
            id = "tefsir",
            name = "Tefsir",
            icon = "📖",
            description = "Kur'an-ı Kerim İlimleri, Nüzul ve Dirayet Tefsirleri",
            tier1Title = "Talip",
            tier2Title = "Müfessir",
            tier3Title = "Hâfız-ı Kelâm",
            tier4Title = "Allâme"
        ),
        AcademicCategory(
            id = "hadis",
            name = "Hadis",
            icon = "📜",
            description = "Nebevi Rivayetler, Metin ve Senet Tahlilleri",
            tier1Title = "Talip",
            tier2Title = "Muhaddis",
            tier3Title = "Hâfız",
            tier4Title = "Allâme"
        ),
        AcademicCategory(
            id = "fikih",
            name = "Fıkıh",
            icon = "⚖️",
            description = "İslam Hukuku, Furû-ı Fıkıh ve Mukayeseli Ahkam",
            tier1Title = "Talip",
            tier2Title = "Fakih",
            tier3Title = "Müçtehid",
            tier4Title = "Allâme"
        ),
        AcademicCategory(
            id = "siyer",
            name = "Siyer",
            icon = "🌙",
            description = "Asr-ı Saadet, Peygamberler Tarihi ve Sahabe Hayatı",
            tier1Title = "Talip",
            tier2Title = "Siyerci",
            tier3Title = "Tarihçi",
            tier4Title = "Allâme"
        ),
        AcademicCategory(
            id = "mantik",
            name = "Mantık",
            icon = "🧠",
            description = "Klasik ve Modern Mantık, Kıyas ve Delil Teorisi",
            tier1Title = "Talip",
            tier2Title = "Mantıkî",
            tier3Title = "Burhan",
            tier4Title = "Allâme"
        ),
        AcademicCategory(
            id = "belagat",
            name = "Belagat",
            icon = "🪶",
            description = "Meani, Beyan ve Bedi' İlimleri, Edebi Fesahat",
            tier1Title = "Talip",
            tier2Title = "Edip",
            tier3Title = "Belig",
            tier4Title = "Allâme"
        ),
        AcademicCategory(
            id = "tasavvuf",
            name = "Tasavvuf",
            icon = "🕯️",
            description = "İslam İrfanı, Nefis Tezkiyesi ve Manevi Seyir",
            tier1Title = "Talip",
            tier2Title = "Salik",
            tier3Title = "Arif",
            tier4Title = "Allâme"
        ),
        AcademicCategory(
            id = "islam_tarihi",
            name = "İslam Tarihi",
            icon = "🕌",
            description = "Hilafet Dönemleri, Medeniyet ve Kurumlar Tarihi",
            tier1Title = "Talip",
            tier2Title = "Tarihçi",
            tier3Title = "Vakkas",
            tier4Title = "Allâme"
        ),
        AcademicCategory(
            id = "usul_u_fikih",
            name = "Usul-ü Fıkıh",
            icon = "📐",
            description = "İslam Hukuk Metodolojisi, Kıyas ve İstinbat Esasları",
            tier1Title = "Talip",
            tier2Title = "Usûlcü",
            tier3Title = "Muhakkik",
            tier4Title = "Allâme"
        ),
        AcademicCategory(
            id = "usul_u_hadis",
            name = "Usul-ü Hadis",
            icon = "🔍",
            description = "Cerh ve Ta'dil, Hadis Istılahları ve Rical İlmi",
            tier1Title = "Talip",
            tier2Title = "Ricalî",
            tier3Title = "Müdekkik",
            tier4Title = "Allâme"
        ),
        AcademicCategory(
            id = "felsefe",
            name = "Felsefe",
            icon = "🌌",
            description = "İslam Felsefesi, Hikmet, Meşşailik ve İşrakilik",
            tier1Title = "Talip",
            tier2Title = "Feylesof",
            tier3Title = "Hâkim",
            tier4Title = "Allâme"
        ),
        AcademicCategory(
            id = "dinler_tarihi",
            name = "Dinler Tarihi",
            icon = "🌐",
            description = "Mukayeseli Dinler, Kadim İnanışlar ve Teolojik Doktrinler",
            tier1Title = "Talip",
            tier2Title = "Mukayeseci",
            tier3Title = "Din Bilimci",
            tier4Title = "Allâme"
        ),
        AcademicCategory(
            id = "arap_dili",
            name = "Arap Dili ve Edebiyatı",
            icon = "✒️",
            description = "Nahiv, Sarf, Lugat, Filoloji ve Şiir Divanları",
            tier1Title = "Talip",
            tier2Title = "Lugavî",
            tier3Title = "Üstat",
            tier4Title = "Allâme"
        ),
        AcademicCategory(
            id = "islami_sanatlar",
            name = "İslami Sanatlar",
            icon = "🎨",
            description = "Hüsn-i Hat, Tezhip, Ebru, Minyatür ve Mimari",
            tier1Title = "Talip",
            tier2Title = "Hattat/Sanatkâr",
            tier3Title = "Üstat Sanatkâr",
            tier4Title = "Allâme"
        ),
        AcademicCategory(
            id = "osmanlica_metinler",
            name = "Osmanlıca Metinler",
            icon = "📜",
            description = "Osmanlı Türkçesi Risaleler, Matbu ve El Yazmaları",
            tier1Title = "Talip",
            tier2Title = "Müstensih",
            tier3Title = "Paleograf",
            tier4Title = "Allâme"
        ),
        AcademicCategory(
            id = "makaleler",
            name = "Makaleler",
            icon = "📑",
            description = "Hakemli Akademik Makaleler ve Araştırma Yazıları",
            tier1Title = "Talip",
            tier2Title = "Yazar",
            tier3Title = "Müellif",
            tier4Title = "Allâme"
        ),
        AcademicCategory(
            id = "dergiler",
            name = "Dergiler",
            icon = "📰",
            description = "İlmi, Fikri ve Akademik Süreli Yayınlar",
            tier1Title = "Talip",
            tier2Title = "Muharrir",
            tier3Title = "Başyazar",
            tier4Title = "Allâme"
        ),
        AcademicCategory(
            id = "tez",
            name = "Tez",
            icon = "🎓",
            description = "Yüksek Lisans, Doktora ve Doçentlik Tezleri",
            tier1Title = "Talip",
            tier2Title = "Tez Yazarı",
            tier3Title = "Doktor/Doçent",
            tier4Title = "Allâme"
        ),
        AcademicCategory(
            id = "diger",
            name = "Diğer",
            icon = "📚",
            description = "Disiplinlerarası İlmi Çalışmalar ve Çeviriler",
            tier1Title = "Talip",
            tier2Title = "Mütercim",
            tier3Title = "Müstakil Araştırmacı",
            tier4Title = "Allâme"
        )
    )

    /**
     * Sadece kategori isimlerinden oluşan liste.
     */
    val CATEGORY_NAMES: List<String> = CATEGORIES.map { it.name }

    /**
     * İsimle kategori bulma (büyük/küçük harf duyarsız).
     */
    fun findCategory(query: String): AcademicCategory? {
        val trimmed = query.trim()
        return CATEGORIES.firstOrNull {
            it.name.equals(trimmed, ignoreCase = true) ||
                    it.id.equals(trimmed, ignoreCase = true)
        } ?: CATEGORIES.firstOrNull {
            it.name.contains(trimmed, ignoreCase = true) || trimmed.contains(it.name, ignoreCase = true)
        }
    }

    /**
     * Kategoriye ait simgeyi getirir.
     */
    fun getIconForCategory(categoryName: String): String {
        return findCategory(categoryName)?.icon ?: "📚"
    }
}
