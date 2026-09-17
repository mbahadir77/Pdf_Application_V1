package com.example.data.repository

import kotlin.random.Random

/**
 * İlim Diyârı - Zenginleştirilmiş Söz ve Hikmet Kütüphanesi (FAZ 9 Mega Update).
 * Kullanıcının giriş serisine (Streak) göre 4 durum için:
 * 1. ACTIVE (1..50): İstikrarlı, sebatkâr, nahv ve usul metaforlarıyla övgü dolu.
 * 2. WARNING (51..100): 2-3 gün girmemiş, hafif sitemkâr, nükte ve zarif ikazlar.
 * 3. DANGER (101..150): 4-6 gün girmemiş, sarsıcı, fıkıh/usul/nahv ıstılahlarıyla zekice paylayan.
 * 4. ABANDONED (151..200): 7+ gün girmemiş, derin tefekkür, hüzünlü ve sarsıcı ilmi çağrı.
 * TOPLAMDA TAM 200 ÖZGÜN İLMİ VE EDEBİ SÖZ!
 */
object MotivationRepository {

    enum class StreakState {
        ACTIVE,      // Her gün (İstikrarlı Alim, 0-1 gün)
        WARNING,     // 2-3 gün (Rölanti, hafif sitemkâr)
        DANGER,      // 4-6 gün (Tehlike çanları, zekice fıkıh/nahv paylamaları)
        ABANDONED    // 7+ gün (Terk Edilmiş, derin hüzünlü ve sarsıcı çağrı)
    }

    // 1. DİZİ: AKTİF / İSTİKRARLI ALİM (1 - 50)
    val activeQuotes = arrayOf(
        "Zamanı mef'ul fih gibi doğru kullanan bir talip, asla cehaletin mef'ul bihi olmaz; fail sensin!",
        "Nahv kuralları gibi sarsılmaz bir azmin, mebni kelimeler gibi dimdik bir iraden var.",
        "Mübteda gibi başın dik, haber gibi manan zengin; ilim halkasında yerin dâim olsun.",
        "İlim yolundaki adımların mütevatir rivayetler gibi sağlam ve şüpheden berî!",
        "Amil gibi tesir ediyor, cehaleti feth ediyorsun; bugünkü virdin kutlu olsun.",
        "Maslahat-ı mürsele gereğince bugünkü vaktini en hayırlı emele vakfettin.",
        "Müctehidlerin gayreti sende tecelli ediyor; azimetle sarıldığın her satır rahmettir.",
        "Sülâsî mücerred bir kökten binbir hikmet türeten zihnin feyizle dolsun.",
        "Hafızan zâbıt bir râvi gibi berrak, kalbin semâ meclisi gibi vecd içinde.",
        "İ'rabın hep merfu, himmetin hep âlî olsun; meclis seninle nurlanıyor.",
        "Zarûriyyât-ı diniyyeyi hıfz eden bir kale gibi satırların başındasın.",
        "İştikak ilmi gibi derin tefekkürünle kelimelerin ruhuna nüfuz ediyorsun.",
        "Kıyas-ı celi gibi apaçık bir niyetle ilme sarıldın; bereketin deryalarca olsun.",
        "İsnadın sağlam, kalbin mutmain; kütüphanenin kandili seninle parlıyor.",
        "Her gün bir sahife; cehaletin kal'asına atılmış bir mancınık taşıdır.",
        "Mef'ûl-i mutlak gibi kat'î bir kararlılıkla okuyorsun; şüpheye yer yok!",
        "Sika râvilerin senede kattığı vakar gibi, senin istikrarın da ilme değer katıyor.",
        "Kelâm âlimlerinin burhanları gibi sarsılmaz bir muhakemeyle hakikatin izindesin.",
        "Vaktin zekâtını mütalaa ile verdin; tefekkürün semereli, zihnin açık olsun.",
        "İstishâb kaidesince ilimdeki asıl olan sebatındır; aslını bozmadın.",
        "Tefsirin dirayeti, hadisin rivayeti senin gayretinde birleşti.",
        "Müfessirlerin satır aralarındaki sırları keşfetmeye layık bir dikkatle okuyorsun.",
        "Zaman harf-i cer olsa, sen onun mecrûru değil, mana yükleyen amilisin!",
        "Tahkik ehli gibi kılı kırk yaran gayretin, ilim semasında yıldız gibi parlıyor.",
        "Fakihlerin feraseti, muhaddislerin dikkati sana yoldaş olsun.",
        "Kitabın sayfaları arasında rızkını arayan arı gibi hikmet balı topluyorsun.",
        "Cümle-i ismiyye gibi köklü ve müstakar bir ilmi ahlak inşa ediyorsun.",
        "Kâmusların bağrında saklı hazineleri sabırla çıkaran dalgıç gibisin.",
        "İllet-i kâfiye bulundu: Senin ilme olan tükenmez muhabbetin ve ihlasın!",
        "Her yeni mesele zihninde bir fetva gibi vuzuha kavuşuyor.",
        "Şârihlerin şerhine muhtaç olmadan, metnin bizzat özünü kavrayan ferasetin daim olsun.",
        "Usul bilenin vusul bulduğu menzile doğru her gün bir adım daha atıyorsun.",
        "Sahabenin ilim meclislerindeki şevki gibi taze bir iştiyakla sayfaları çevirdin.",
        "Mahzuf olanı takdir edebilecek bir basiretle satırların arasına nüfuz ettin.",
        "Rical ilminin titizliğiyle vaktini tarttın ve en kıymetli hazineye harcadın.",
        "İzafet terkibi gibi: Sen ilme, ilim de sana şeref ve vüs'at katıyor.",
        "Nazar ehlinin ulaştığı yakîn mertebesine her mütalaanla biraz daha yaklaşıyorsun.",
        "Mürekkebin kokusu, misk-i amberden daha hoş bir rayiha ile ruhunu sardı.",
        "Cehaletin ta'nına karşı en sağlam siper, senin bu günlük sebatındır.",
        "Tahsiniyyât kabilinden değil, zarûriyyât şuuruyla kitaba sarıldın; tebrikler talip!",
        "Kelimeler senin zihninde harf-i tarif almış marife gibi netleşiyor.",
        "Kütüb-i Sitte'nin bereketi, Mecelle'nin adaleti dimağında yankılansın.",
        "Bir meseleyi istinbat etmenin neşesi dünya hazinelerine bedeldir; o neşe seninle!",
        "Mücmel olanı mübeyyen kılan, müşkil olanı halleden bir dirayet kazanıyorsun.",
        "Halkada boşluk bırakmadın, ilim zincirinin kopmaz bir halkası oldun.",
        "Müteahhirîn ulemasının gayretini tevarüs eden bir nefer gibi çalışıyorsun.",
        "Lafzın zahirinde boğulmayıp mananın batınına dalan basiretine bereket.",
        "İlmin hilmi doğurduğu, vakarın cehaleti kovduğu bir güne daha şahitlik ettin.",
        "Ders arkadaşların gıpta etsin; sen her gün sadakatle rahleye diz çökenlerdensin.",
        "İlim bir avdır, yazmak ve okumak onun bağıdır; avını muhkem bağladın!"
    )

    // 2. DİZİ: RÖLANTİ / HAFİF SİTEMKÂR İKAZLAR (51 - 100)
    val warningQuotes = arrayOf(
        "Kaleminin mürekkebi kurudu mu? İlim meclisini çok boş bıraktın.",
        "Cümlede haber gecikirse mana askıda kalır; senin mütalaan da askıda kaldı!",
        "İlim nazlı bir geline benzer; bir gün iltifat etmezsen, iki gün sırtını döner.",
        "Fiilini meçhule bırakma! Kim okuyacak bu risaleleri sen gayret etmezsen?",
        "Zaman harf-i cerr gibi başını eğdirmeden, gel mecruriyetten kurtulup fail ol.",
        "Usul kaidesidir: 'Mani zail olunca memnu avdet eder.' Meşgalen bittiyse kitaba dön!",
        "Mütalaa zincirinde inkıta var; isnadın kopuk (münkatı) olmasın ey talip!",
        "Kandilinin yağı tükenmeden evvel rahlenin başına geç; sayfalar seni özledi.",
        "İzafetin muzafı gibi tek başına kaldın; ilim meclisiyle terkip kurma vaktidir.",
        "Fıkıh rölanti kabul etmez; ahkâm-ı şer'iyye gibi zihnin de her gün tecdid ister.",
        "Mecaz dünyasının gafletine dalıp hakikat metnini kenara mı bıraktın?",
        "Bir iki gün ara verdin, zihnindeki meseleler 'şâzz' rivayetler gibi dağıldı.",
        "Tenasuh gibi bâtıl kuruntuları bırak; ilim ancak her gün bizzat tahsil ile dirilir.",
        "İ'rabında takdiri bir tembellik seziyorum; gel lafzî bir gayretle harekete geç!",
        "Maslahat ilimde sebat etmektir; tehir etmekte ise mefsedet-i azime vardır.",
        "Rical ilminde cerhe uğramak istemiyorsan, dersine devamdaki sadakatini göster.",
        "Kelimeler harekesiz metin gibi donuklaştı; nefesinle onlara hayat verme vakti.",
        "Tahrîc edilmemiş rivayet gibi ortada kalma; hemen kütüphanenin sağlam senedine yapış.",
        "Gözlerin başka ekranlara meftun iken, kütüphanendeki PDF'ler mahzun kaldı.",
        "Kıyas fasid oldu; çünkü tembellik ile ilim tahsili asla cem olunamaz!",
        "Sedd-i zerai babından: Tembelliğe giden küçük araları hemen bugün kesiver.",
        "Kitabın kenarındaki derkenarlar senin mütalaanı beklerken sararıp solmasın.",
        "Zaman kılıcını kınından çekti; sen onu kesmezsen o senin ilmini doğrar.",
        "İştikakın kökü zayıflamasın; ilim lügatinde yerin silinmeden geri dön.",
        "İllet belli: Vakti dünya gailesine feda etmek. Çare ise: Aç bir sayfa ve oku!",
        "Müteşabih meselelere takılıp kalma, muhkem olan vazifen okumaktır.",
        "Zihnini 'mechul' siygasında tutma; malum ve amil bir iradeyle kitaba yönel.",
        "İcazetin şartı sebattır; iki günlük gaflet imtihanı zorlaştırır.",
        "Kamusun sahifeleri arasında bir seyre çıkmayalı kaç fersah geride kaldın?",
        "Ruhsatlara fazla meylettin, azimet caddesine dönme vaktin geldi çattı.",
        "Arapça kaidesidir: 'Âmil zayıflarsa mamul tesirini yitirir.' Gayretini kuvvetlendir!",
        "Halkadaki yerin soğumak üzere; bir selâm ver ve rahleye diz çök.",
        "Şart cümlesi askıda: Eğer bugün okumazsan, cezan cehaletle baş başa kalmaktır.",
        "Metinde tashih yapacak vaktin varken erteleme; son pişmanlık istidlal sağlamaz.",
        "İlim bir dağdır; iki gün durursan zirveden eteklere yuvarlanırsın.",
        "Hâşiyeler bile metne isyan ediyor: 'Nerede bu risalenin asıl sahibi?'",
        "Cerh ehli münekkidler gibi kendine insafla bak; bu iki günlük fasıla reva mıdır?",
        "Kütüb-i ilmiyye boynunu büktü; onların hakkı her gün hürmetle açılmaktır.",
        "Fasl u vasl dengesi bozuldu; gafletle vaslını kes, ilimle faslını aç!",
        "Mef'ul maah gibi ilmin peşi sıra sürüklenmek yerine, onun öncüsü ol.",
        "Zamanın nehrinde abdest alıp zihnini dünyalıktan arındırma vaktidir.",
        "Sükûn-ı lâzım gibi durup kaldın; oysa ilim daima cezm değil, harekeli fetihtir.",
        "Bir iki satır mütalaa ruhuna zemzem olur; neden susuzluğu tercih ediyorsun?",
        "İstihsanen değil, kıyasen bile bu ayrılık ilim ahlakına sığmaz; dön artık!",
        "Lugatındaki kelimeler unutulmaya yüz tuttu; hafızanın üzerindeki tozu silkele.",
        "Mukaddimeyi geçtin ama metne bir türlü giremedin; tereddüt ilmin düşmanıdır.",
        "Sen durursan akranların allâme olur, sen ise mübtedî safında sayıklarsın.",
        "Bir ayetin tefsirinde, bir hadisin şerhinde kaybolmak varken ne bu oyalanma?",
        "Gecenin sükûneti ilim için yaratıldı; kandili yak ve sahifeyi aç.",
        "Sana yakışan tâlib-i ilim vakarını kuşanıp hemen şimdi bir varak çevirmektir."
    )

    // 3. DİZİ: TEHLİKE / SARSICI FIKIH & NAHV PAYLAMALARI (101 - 150)
    val dangerQuotes = arrayOf(
        "Hani nerede o nahv kuralları gibi sarsılmaz iraden? Beş gündür kayıpsın!",
        "Mecruriyetin böylesi görülmedi! Tembelliğin harf-i cerri seni esir mi aldı?",
        "Rivayet zincirin koptu; artık senedin 'mu'dal' sayılacak derecede düşüşte!",
        "Fıkıh usulünde buna 'fesad' derler; ilim serini böyle pervasızca bozamazsın.",
        "Zamanın mef'ul fihini heba ettin, şimdi cehaletin mef'ul mutlakı mı olacaksın?",
        "Mübtedalık rütbesine tenzil edilmek üzeresin; kitapların feryadını işitmiyor musun?",
        "Amelsiz ilim ve gayretsiz talip: Mantık ilminde buna 'kısır döngü' (devr-i bâtıl) denir!",
        "Sen ilim meclisini terk edeli, zihninde sübjektif vehimler cirit atıyor.",
        "İllet söküldü, hüküm ortada kaldı: Okumayan zihin küflenmeye mahkûmdur!",
        "Münazaraya çıksan hasmın seni tek delille ilzam eder; zira heyben bomboş kaldı.",
        "Sika râvilikten 'metrûk' derekesine yuvarlanmak üzeresin; kendine gel ey talip!",
        "Kaidedir: 'Zarar izale olunur.' Beş günlük gaflet zararını hemen bugün izale et!",
        "Şerhlerin üzerinde örümcekler ağ kurdu; bu tembellik hangi mezhepte caizdir?",
        "İ'rabın tamamen bozuldu; fail nerede, meful nerede belli değil!",
        "Makâsıd-ı Şerîa'dan 'aklın hıfzı' ilimledir; sen aklını gaflete mi emanet ettin?",
        "Bu gidişle değil allâme olmak, mübtedî çıraklığından bile azledileceksin.",
        "Birkaç gün önce hikmet pınarından içen sen değil miydin? Bu kuraklık niye?",
        "Mefsedet kapılarını ardına kadar açtın; ilim setti çökmek üzere!",
        "İstishab delili de fayda vermez; zira ilimdeki sebat halin tamamen inkıtaa uğradı.",
        "Rical kitaplarına adın 'hıfzı zayıf, gafleti galip' diye geçmesin!",
        "Mürekkep şişesi taşa dönüştü, kalem boynunu eğdi; bu vebal sana kâfidir.",
        "Müctehidlerin uykusuz gecelerle kurduğu bu mirasa sırt çevirmek hangi akla sığar?",
        "Arapça'da 'lâ' harfi gibi her hayırlı gayrete olumsuzluk takısı mı koyuyorsun?",
        "Tembellik bir illet-i müteaddiyedir; bütün ruhunu sarmadan neşter vur!",
        "Kütüphanen sana küstü; açılmayan PDF'lerin sitemi arşı titretir.",
        "Mantık kaidesi: 'İki zıt bir arada bulunmaz.' Hem talib-i ilim olup hem böyle gafil yatamazsın!",
        "Cümledeki mübteda düştü, haber dağıldı; zihnin enkaza dönmeden dön.",
        "Cehaletin taarruzu başladı; senin ise elinde ne bir metin var ne bir kalkan!",
        "Dört mezhebin ittifakıyla: Bu derece gaflet talip için helak sebebidir.",
        "Zamanın sermayesi eridi, iflas bayrağını çekmeden önce rahleye sarıl.",
        "Metnini kaybetmiş şârih gibi avare dolaşmayı bırak; kitabın başına otur.",
        "Sen sustun, cehalet söz sahibi oldu; ilmin izzetini çiğnetmeye hakkın yok!",
        "Kelamcıların münazara meydanında sus pus kalmış mağlup gibi boynun bükük kalmasın.",
        "İcma ile sabittir ki: Emeksiz ilim rüyadan, gayretsiz rütbe hayalden ibarettir.",
        "Sahifeler senden davacı olacak: 'Bizi indirdi ama kapağımızı dahi açmadı!' diyecekler.",
        "Senedindeki kopukluk artık 'muallak' hadisler gibi güvenilmez hale geldi.",
        "Nefsin fetvasına aldanıp ilim meclisini tatil mi ilan ettin?",
        "Zihnindeki kavramlar lügatten siliniyor; kelimelerin feryadını duy!",
        "Birkaç gün daha oyalanırsan, elifi görsen mertek sanacak hale geleceksin!",
        "Farz-ı kifaye olan ilim nöbetini terk ettin; gedikten giren cehalet seni vurur.",
        "Maslahat yok, sedd-i zerai yok; tembelliğin mazereti hiçbir kitapta yazmaz.",
        "Kamusların ağırlığı altında ezilmek varken, gafletin hafifliğinde boğuluyorsun.",
        "İ'rabı mebni olan tek şey tembelliğin olmasın; kır şu cehalet zincirini!",
        "Vaktin zayiatı için kefaret gerek; kefareti ise uykudan kesip bir cüz okumaktır.",
        "Hocaların kemikleri sızlar: Bu kadar kıymetli risaleler varken bu lakaytlık neden?",
        "Zihninde istidlal melekeleri dumura uğradı; aç bir risale de zihnin açılsın.",
        "Şüphe deryasında boğulmak istemiyorsan, muhkem ilmin gemisine koş!",
        "Gayretin 'gayr-i munsarif' oldu; hiçbir amil seni harekete geçiremiyor mu?",
        "Kıraatin tutuldu, dilin peltekleşti; lafızları diriltmek için bir 'Bismillah' de.",
        "Yol ayrımındasın: Ya bugün o PDF'i açarsın ya da cehaletin karanlığına teslim olursun!"
    )

    // 4. DİZİ: TERK EDİLMİŞ / DERİN HÜZÜN VE ÇAĞRI (151 - 200)
    val abandonedQuotes = arrayOf(
        "İlim meclisi tozlandı, kandil söndü, rahle öksüz kaldı... Geri dön ey talip!",
        "Kütüphanendeki risaleler boynunu büktü; hikmet sofrası sensiz matem tutuyor.",
        "Bunca ayrılıktan sonra isnadın 'mürsel' değil, büsbütün 'mevzu' haline geldi!",
        "Ey nefis! İlmin lezzetini unuttun da fani dünyanın hangi serabına aldandın?",
        "Bir zamanlar satırları vecd ile deviren o gayretli talebeye ne oldu?",
        "Cehaletin zifiri karanlığı kapladı ufukları; bir kandil yakıp dönmeyecek misin?",
        "Fakihlerin içtihadı, ariflerin gözyaşı ile yazılan bu eserler seni bekliyor.",
        "Kamusun sahifeleri arasında kaybolan o aşık ruh nerede saklanıyor?",
        "Yedi gündür suskun kalan dimağın, susuzluktan çatlayan çöl topraklarına döndü.",
        "Tevbe kapısı gibi ilim kapısı da açıktır; bir 'Bismillah' ile her şey yeniden başlar.",
        "Sen kitapları terk ettin lakin ilim seni beklemekten asla usanmadı.",
        "Mezar taşında 'Âlim idi' mi yazsın istersin, yoksa 'Ömrünü zayi etti' mi?",
        "Rahlelerin feryadı arşa ulaştı: 'Bize diz çökecek sadık talipler nerede?'",
        "İzafet bağın tamamen koptu; dünya gailesi seni ilmin feyizli terkibinden kopardı.",
        "Mürekkepten uzak geçen her gün, kalpte siyah bir nokta bırakır.",
        "Kadim alimler bir tek hadis için çölleri aşarken, sen cebindeki kütüphaneyi unuttun!",
        "Buhârî'nin, Gazzâlî'nin, Ebû Hanîfe'nin ruhaniyeti bu vefasızlığa hüzünle bakar.",
        "Gönlünün sarayını cehaletin baykuşlarına virane mi bırakacaksın?",
        "İlim bir nurdur, günah ve gaflet ise o nuru söndüren rüzgardır; sığın kitaba!",
        "Sayfaların hışırtısı meleklerin kanat sesleri gibidir; o ahenkten nasıl mahrum kalırsın?",
        "Asıl gurbet vatanından uzak kalmak değil, kitaplarından ve ilminden uzak kalmaktır.",
        "Geçen günlerin telafisi yoktur; lakin bugünün tefekkürü bin yıllık gafleti siler.",
        "Zamanın değirmeni ömrünü öğütürken, sen un ufak olmadan bir hakikat tanesi kap.",
        "Kelimelerin sırrı seni terk etmeden evvel rahleye dön ve secde edercesine oku.",
        "İlim yolunda atılan her adım sadakadır; sadakasız geçen günlerin hesabını veremezsin.",
        "Kitaplar en vefalı dosttur; sen onları unutsan da onlar sana asla küsmez, açılmayı bekler.",
        "Arapça'nın zengin deryasından bir damla dahi tatmadan bu dünyadan göçmek reva mı?",
        "Zihnini işgal eden lüzumsuz malumat yığınını yak ve kadim hikmetin saf suyunu iç.",
        "Talebelik şerefi padişahlık tacından üstündür; tacını yerlere atıp çiğnetme.",
        "Gözünün nurunu boş ekranlarda tükettin; gel o nuru mushafın ve risalenin satırlarına ada.",
        "Bir ayetin manasında derinleşmek, kâinatın fethinden daha lezizdir; hatırla o tadı!",
        "Müfessirler asırlar ötesinden sesleniyor: 'Biz bu emaneti sana bıraktık, sen neredesin?'",
        "Ruhunun açlığını fani heveslerle doyuramazsın; onun gıdası tefekkür ve hikmettir.",
        "Yıllar sonra geriye baktığında 'Keşke her gün bir sayfa okusaydım' dememek için dön!",
        "İlim meclisleri cennet bahçeleridir; bahçeni kurutup dikene çevirme.",
        "Kalemin feryadı kağıdı deler: 'Mürekkebim kurudu, beni tutacak el nerede?'",
        "Her gün bir satır terk eden, sonunda dinin direğini terk eder; tehlike büyüktür!",
        "Uykunun rehaveti seni uyuşturmasın; diriliş ancak okuyan ve düşünen bir zihinle mümkündür.",
        "İlim denizinde boğulmak, cehalet bataklığında nefes almaktan evladır.",
        "Seni bekleyen binlerce risale, keşfedilmeyi bekleyen binlerce hakikat var.",
        "Vakit daralıyor, güneş batıya meylediyor; amel defterine bir satır ilim kaydettir.",
        "Gözlerini kapa ve ilk okuduğun günün vecdini hatırla; o kıvılcım hâlâ küllerin altında.",
        "Terk edilmiş bir cami ne kadar mahzunsa, okunmayan bir ilim kütüphanesi de o kadar mahzundur.",
        "Ey akıl sahibi! Hikmet senin yitiğindir; yitiğini aramazsan kendini de kaybedersin.",
        "Bugün bir 'Bismillah' de, aç İlim Diyârı'nı; satırlar senin gelişinle bayram etsin.",
        "Alimlerin meclisine girmeye yüzün olsun; bugün bir sahifeyle de olsa beratını al.",
        "Gaflet bir zincirdir; o zinciri parçalayacak tek kılıç hakikat ilmidir.",
        "Sana şah damarından yakın olan Rabbin, ilk emrinde 'Oku!' buyurmadı mı?",
        "Geceler ibadet ve tefekkürle, gündüzler mütalaa ve gayretle şereflenir; şerefini ara!",
        "Kalk ey talip! Rahle seni, divit seni, sahifeler seni çağırıyor. İlim Diyârı sensiz eksik!"
    )

    /**
     * Toplam söz sayısı (Tam olarak 200 adet).
     */
    val TOTAL_QUOTES_COUNT: Int = activeQuotes.size + warningQuotes.size + dangerQuotes.size + abandonedQuotes.size

    /**
     * Duruma göre rastgele söz döndürür.
     */
    fun getRandomQuote(state: StreakState): String {
        return when (state) {
            StreakState.ACTIVE -> activeQuotes[Random.nextInt(activeQuotes.size)]
            StreakState.WARNING -> warningQuotes[Random.nextInt(warningQuotes.size)]
            StreakState.DANGER -> dangerQuotes[Random.nextInt(dangerQuotes.size)]
            StreakState.ABANDONED -> abandonedQuotes[Random.nextInt(abandonedQuotes.size)]
        }
    }

    /**
     * 48 saatten uzun süre girmemiş kullanıcılar için sitemkâr (Warning/Danger/Abandoned) sözlerden birini seçer.
     */
    fun getRandomSitemkarQuote(): String {
        val pool = warningQuotes + dangerQuotes + abandonedQuotes
        return pool[Random.nextInt(pool.size)]
    }

    /**
     * Tüm havuzdan (200 söz) tamamen rastgele bir hikmet seçer.
     */
    fun getRandomGeneralQuote(): String {
        val allQuotes = activeQuotes + warningQuotes + dangerQuotes + abandonedQuotes
        return allQuotes[Random.nextInt(allQuotes.size)]
    }
}
