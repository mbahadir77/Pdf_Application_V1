package com.example.data.repository

import kotlin.random.Random

/**
 * İlmNet - İlim Aynası Widget & Motivasyon Havuzu (FAZ 8).
 * 4 farklı ilmi kategori ve her birinde durum bazlı söz havuzu barındırır.
 */
object MotivationRepository {

    enum class WidgetState {
        ACTIVE,      // Her gün (İstikrarlı Alim)
        WARNING,     // 2-3 gün (Rölanti)
        ABANDONED    // 7+ gün (Terk Edilmiş)
    }

    // 1. Dizi: İstikrarlı Alim (Aktif / Her Gün)
    val activeQuotes = arrayOf(
        "İlmin nuru parlıyor. Bugün de satırların hakkını verdin!",
        "Kandilin alevi hiç sönmesin; ilim yolundaki adımların sabit olsun.",
        "Mürekkebin kokusu arş-ı âlâyı mest eder. İstikrarın daim olsun!",
        "Her gün bir satır, cehalete vurulmuş bin kalkandır. Tebrikler alim!",
        "Bugün de ilim meclisini yalnız bırakmadın; hikmet kapıları sana açık.",
        "İstikrar, kerametten üstündür. Risalelerin sayfaları seninle aydınlanıyor.",
        "Günde bir sahife dahi olsa okuyan, bin senelik cehaleti yıkar.",
        "İlim bir nehir gibidir; her gün içen asla susuzluk çekmez.",
        "Kalemin ucu bugün de açıldı, tefekkürün meyvesini veriyor.",
        "Alimin uykusu dahi ibadettir; sen ise uyanık ve satırların başındasın!"
    )

    // 2. Dizi: Rölanti (2-3 Gün Giriş Yapmayanlar)
    val warningQuotes = arrayOf(
        "Kandilin yağı azalıyor... Satırların arasına dönme vakti gelmedi mi?",
        "İlim nazlıdır; iki gün terk edersen o seni büsbütün terk eder!",
        "Kitaplığın seni bekliyor. Küçük bir risale molası zihnini diriltir.",
        "Zamanın akışı hızlı, ilmin tahsili sabır ister. Bugün bir sayfa aç!",
        "Hocalarımız derdi ki: 'Ara veren yolda kalır.' Yola devam et!",
        "Tozlanmaya yüz tutmuş sahifeler feryat ediyor. Bir göz atıver!",
        "Fikirlerin soğumasın, tefekkür ateşini yeniden alevlendir.",
        "Dünyanın meşgalesi bitmez; ilmin meşgalesi ise ebediyen sürer.",
        "Bugün kendine bir iyilik yap ve 5 dakikanı ilim meclisine ayır.",
        "Kalem masada öksüz kaldı. Sahifeleri canlandırmak senin elinde."
    )

    // 3. Dizi: Terk Edilmiş (7+ Gün Giriş Yapmayanlar)
    val abandonedQuotes = arrayOf(
        "İlim meclisi tozlandı, kandil söndü. Geri dön ey talip!",
        "Kitaplar boynunu büktü. Hikmet sofrası sensiz öksüz kaldı.",
        "Cehalet karanlığı sessizce yaklaşır; bir kandil yak ve geri dön!",
        "Aylar gibi geçen yedi gün... İlim pınarı seni hâlâ bekliyor.",
        "Bir zamanlar satırları fetheden alim nerede? Kitaplığın seni özledi.",
        "Vakit kılıç gibidir, sen onu kesmezsen o seni keser. Dön ve oku!",
        "Kütüphanenin kapısı açık kaldı; içeri girip bir satır olsun nefes al.",
        "Gönlünü ilmin nurundan mahrum bırakma. Tövbe kapısı gibi sahife de açıktır.",
        "Ne kadar uzaklaşsan da bir 'Bismillah' ile yeniden başlayabilirsin.",
        "Ey talib-i ilim! Asıl gurbet, kitaplardan uzak kalmaktır. İlmNet'e dön."
    )

    // 4. Dizi: Genel Hikmet & Kadim Alim Sözleri
    val generalWisdomQuotes = arrayOf(
        "İlim rütbesi, rütbelerin en yücesidir. (Hz. Ali)",
        "Bilmeyen ve bilmediğini bilen talebedir; ona öğretiniz. (Fârâbî)",
        "İlim amel etmek içindir; amelsiz ilim meyvesiz ağaca benzer. (İmam Gazâlî)",
        "Kelimelerin manasını bilmeyen, fikirlerin derinliğini kavrayamaz. (İbn Sînâ)",
        "Beşikten mezara kadar ilim tahsil ediniz. (Hadis-i Şerif)",
        "Tarih, geçmişin aynasında bugünü ve yarını okuma sanatıdır. (İbn Haldûn)",
        "Hikmet, müminin yitiğidir; nerede bulursa onu almaya en layık odur.",
        "Kalem kılıçtan keskindir, lakin mürekkebi ihlas olmalıdır.",
        "Âlimler peygamberlerin vârisleridir. Mirasına sahip çık.",
        "Her şey paylaşıldıkça azalır; yalnız ilim paylaşıldıkça çoğalır."
    )

    fun getRandomQuote(state: WidgetState): String {
        return when (state) {
            WidgetState.ACTIVE -> activeQuotes[Random.nextInt(activeQuotes.size)]
            WidgetState.WARNING -> warningQuotes[Random.nextInt(warningQuotes.size)]
            WidgetState.ABANDONED -> abandonedQuotes[Random.nextInt(abandonedQuotes.size)]
        }
    }

    fun getRandomGeneralQuote(): String {
        return generalWisdomQuotes[Random.nextInt(generalWisdomQuotes.size)]
    }
}
