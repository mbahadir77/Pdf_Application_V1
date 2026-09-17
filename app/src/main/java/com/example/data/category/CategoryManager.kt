package com.example.data.category

/**
 * İlim Diyârı - Merkezi Kategori ve İlim Dalları Yöneticisi (FAZ 9 Mega Update).
 * Uygulamanın 20 nihai kategorisini tek merkezden yönetir.
 * AddPdf (PDF Ekleme), Keşfet, Filtre Çipleri ve AcademicBadgeEngine
 * SADECE bu merkezi liste üzerinden beslenir.
 */
data class AcademicCategory(
    val id: String,
    val name: String,
    val icon: String,
    val description: String,
    val shortCode: String = id
)

object CategoryManager {

    /**
     * İlim Diyârı nihai, sabit ve resmi 20 kategorisi:
     * 1. Tefsir
     * 2. Hadis
     * 3. Fıkıh
     * 4. Siyer
     * 5. Akaid
     * 6. Kelâm
     * 7. Tefsir Usulü
     * 8. Hadis Usulü
     * 9. Usul-ü Fıkıh
     * 10. Arapça Dil Bilgisi (Sarf-Nahv)
     * 11. Kıraat
     * 12. Tasavvuf
     * 13. Tarih
     * 14. Mantık/Felsefe
     * 15. Roman
     * 16. Makale/Dergi
     * 17. Tez
     * 18. Özet
     * 19. Sözlük
     * 20. Diğer
     */
    val CATEGORIES: List<AcademicCategory> = listOf(
        AcademicCategory(
            id = "tefsir",
            name = "Tefsir",
            icon = "📖",
            description = "Kur'an-ı Kerim İlimleri, Nüzul, Garîb ve Dirayet Tefsirleri"
        ),
        AcademicCategory(
            id = "hadis",
            name = "Hadis",
            icon = "📜",
            description = "Nebevi Rivayetler, Metin ve Senet Tahlilleri, Kütüb-i Sitte"
        ),
        AcademicCategory(
            id = "fikih",
            name = "Fıkıh",
            icon = "⚖️",
            description = "İslam Hukuku, Furû-ı Fıkıh, Muamelat ve Mukayeseli Ahkâm"
        ),
        AcademicCategory(
            id = "siyer",
            name = "Siyer",
            icon = "🌙",
            description = "Asr-ı Saadet, Megâzi, Peygamberler Tarihi ve Sahabe Hayatı"
        ),
        AcademicCategory(
            id = "akaid",
            name = "Akaid",
            icon = "🏛️",
            description = "İman Esasları, İtikad, Tevhid ve Ehl-i Sünnet İnancı"
        ),
        AcademicCategory(
            id = "kelam",
            name = "Kelâm",
            icon = "🛡️",
            description = "Akli Deliller, İtikadi Münazaralar ve Teolojik Felsefe"
        ),
        AcademicCategory(
            id = "tefsir_usulu",
            name = "Tefsir Usulü",
            icon = "📐",
            description = "Kur'an İlimleri Metodolojisi, İ'caz, Vahiy ve Nüzul Esasları"
        ),
        AcademicCategory(
            id = "hadis_usulu",
            name = "Hadis Usulü",
            icon = "🔍",
            description = "Cerh ve Ta'dil, Mustalahu'l-Hadis, İlel ve Rical İlmi"
        ),
        AcademicCategory(
            id = "usul_u_fikih",
            name = "Usul-ü Fıkıh",
            icon = "⚖️",
            description = "İslam Hukuk Metodolojisi, Kıyas, İstinbat ve Makâsıd"
        ),
        AcademicCategory(
            id = "sarf_nahv",
            name = "Arapça Dil Bilgisi (Sarf-Nahv)",
            icon = "✒️",
            description = "Sarf, Nahiv, İ'rab, İştikak, Meânî ve Belâgat Kaideleri"
        ),
        AcademicCategory(
            id = "kiraat",
            name = "Kıraat",
            icon = "🎙️",
            description = "Kıraat-i Aşere ve Takrib, Tecvid, Tertil ve Mahreç İlimleri"
        ),
        AcademicCategory(
            id = "tasavvuf",
            name = "Tasavvuf",
            icon = "🕯️",
            description = "İslam İrfanı, Nefis Tezkiyesi, Ahlak ve Manevi Seyr-i Sülûk"
        ),
        AcademicCategory(
            id = "tarih",
            name = "Tarih",
            icon = "🕌",
            description = "İslam Tarihi, Medeniyetler, Vak'anüvislik ve Tarih Felsefesi"
        ),
        AcademicCategory(
            id = "mantik_felsefe",
            name = "Mantık/Felsefe",
            icon = "🧠",
            description = "Klasik ve Modern Mantık, Burhan, Kıyas ve Hikmet Felsefesi"
        ),
        AcademicCategory(
            id = "roman",
            name = "Roman",
            icon = "📚",
            description = "Edebi Eserler, Klasik ve Modern Romanlar, Hikaye ve Kurgu"
        ),
        AcademicCategory(
            id = "makale_dergi",
            name = "Makale/Dergi",
            icon = "📑",
            description = "Hakemli Akademik Makaleler, Dergiler ve Süreli Yayınlar"
        ),
        AcademicCategory(
            id = "tez",
            name = "Tez",
            icon = "🎓",
            description = "Yüksek Lisans, Doktora, Uzmanlık ve Doçentlik Tezleri"
        ),
        AcademicCategory(
            id = "ozet",
            name = "Özet",
            icon = "📋",
            description = "Kitap Özetleri, Hülasalar, Ders Notları ve Veciz Derlemeler"
        ),
        AcademicCategory(
            id = "sozluk",
            name = "Sözlük",
            icon = "📖",
            description = "Kamuslar, Istılah Lugatları, Ansiklopedik ve Terminoloji Sözlükleri"
        ),
        AcademicCategory(
            id = "diger",
            name = "Diğer",
            icon = "✨",
            description = "Disiplinlerarası İlmi Çalışmalar, Risaleler ve Çeviriler"
        )
    )

    /**
     * Sadece kategori isimlerinden oluşan liste.
     */
    val CATEGORY_NAMES: List<String> = CATEGORIES.map { it.name }

    /**
     * İsimle kategori bulma (büyük/küçük harf duyarsız, kısmi eşleşme destekli).
     */
    fun findCategory(query: String): AcademicCategory? {
        val trimmed = query.trim()
        if (trimmed.isEmpty()) return null
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
