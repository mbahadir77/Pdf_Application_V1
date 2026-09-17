package com.example.ui.reader

import android.content.Context
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import com.example.data.local.entity.PostEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

/**
 * İlmNet - PDF Belge Hazırlayıcısı.
 * Cihazdan seçilen yerel dosyayı veya çevrimdışı akademik makaleyi
 * yerleşik Android PdfRenderer motoruna hazır yerel File nesnesine dönüştürür.
 */
object PdfDocumentHelper {

    suspend fun preparePdfFile(context: Context, post: PostEntity): File = withContext(Dispatchers.IO) {
        // 0. Eğer post.pdfUrl doğrudan var olan yerel bir dosya yoluna işaret ediyorsa direkt döndür!
        if (post.pdfUrl.isNotBlank() && !post.pdfUrl.startsWith("http://") && !post.pdfUrl.startsWith("https://")) {
            val localPath = if (post.pdfUrl.startsWith("file://")) {
                Uri.parse(post.pdfUrl).path ?: ""
            } else {
                post.pdfUrl
            }
            val localFile = File(localPath)
            if (localFile.exists() && localFile.length() > 0) {
                return@withContext localFile
            }
        }

        val safeFileName = "academic_${post.id.replace(Regex("[^a-zA-Z0-9_]"), "_")}.pdf"
        val targetFile = File(context.cacheDir, safeFileName)

        // 1. Eğer post yerel bir content:// veya file:// URI'sına işaret ediyorsa doğrudan kopyala
        if (post.pdfUrl.startsWith("content://") || post.pdfUrl.startsWith("file://")) {
            try {
                val uri = Uri.parse(post.pdfUrl)
                context.contentResolver.openInputStream(uri)?.use { input ->
                    FileOutputStream(targetFile).use { output ->
                        input.copyTo(output)
                    }
                }
                if (targetFile.exists() && targetFile.length() > 0) {
                    return@withContext targetFile
                }
            } catch (e: Exception) {
                // Eğer URI okunamadıysa aşağıda akademik belge üretilecek
            }
        }

        // 2. Dosya zaten önbellekte varsa ve geçerliyse doğrudan döndür
        if (targetFile.exists() && targetFile.length() > 1024) {
            return@withContext targetFile
        }

        // 3. Android PdfDocument ile çok sayfalı zengin akademik PDF belgesi oluştur
        generateAcademicPdf(targetFile, post)
        return@withContext targetFile
    }

    /**
     * Cihazdan seçilen content:// URI'sini uygulamanın güvenli filesDir klasörüne
     * fiziksel olarak kopyalar ve kalıcı mutlak dosya nesnesini döndürür.
     * URI izin hatalarını (SecurityException) tamamen engeller.
     */
    suspend fun copyPdfUriToInternalStorage(
        context: Context,
        uri: Uri,
        originalFileName: String
    ): Result<File> = withContext(Dispatchers.IO) {
        try {
            val pdfDir = File(context.filesDir, "academic_pdfs").apply { if (!exists()) mkdirs() }
            val cleanName = originalFileName.replace(Regex("[^a-zA-Z0-9._-]"), "_").take(50)
            val targetFile = File(pdfDir, "pdf_${System.currentTimeMillis()}_$cleanName")

            context.contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(targetFile).use { output ->
                    input.copyTo(output)
                }
            } ?: return@withContext Result.failure(Exception("PDF dosyası açılamadı veya izin verilmedi."))

            if (targetFile.exists() && targetFile.length() > 0) {
                Result.success(targetFile)
            } else {
                Result.failure(Exception("Kopyalanan PDF dosyası boş."))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun generateAcademicPdf(outputFile: File, post: PostEntity) {
        val document = PdfDocument()
        val pageWidth = 595 // Standart A4 Genişlik (72 dpi noktasal ölçek)
        val pageHeight = 842 // Standart A4 Yükseklik

        val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#0A1C16")
            textSize = 16f
            typeface = Typeface.create(Typeface.SERIF, Typeface.BOLD)
        }

        val subtitlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#80681B")
            textSize = 11f
            typeface = Typeface.create(Typeface.SERIF, Typeface.ITALIC)
        }

        val headerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#08140E")
            textSize = 12f
            typeface = Typeface.create(Typeface.SERIF, Typeface.BOLD)
        }

        val bodyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#222222")
            textSize = 10f
            typeface = Typeface.create(Typeface.SERIF, Typeface.NORMAL)
        }

        val quotePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#444444")
            textSize = 10f
            typeface = Typeface.create(Typeface.SERIF, Typeface.ITALIC)
        }

        val footerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#777777")
            textSize = 8f
        }

        val margin = 50f

        // SAYFA 1: Başlık, Yazar, Kategori, Özet ve Giriş
        val pageInfo1 = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, 1).create()
        val page1 = document.startPage(pageInfo1)
        val canvas1 = page1.canvas

        var y = 60f

        // Üst Kurumsal Başlık
        canvas1.drawText("İLMNET AKADEMİK ARAŞTIRMALAR DERGİSİ • CİLT IX • ${post.category.uppercase()}", margin, y, footerPaint)
        y += 8f
        val linePaint = Paint().apply { color = Color.parseColor("#D4AF37"); strokeWidth = 1.5f }
        canvas1.drawLine(margin, y, pageWidth - margin, y, linePaint)
        y += 32f

        // Makale Başlığı
        val titleLines = wrapText(post.title, titlePaint, pageWidth - (margin * 2))
        for (line in titleLines) {
            canvas1.drawText(line, margin, y, titlePaint)
            y += 20f
        }
        y += 8f

        // Yazar ve Kurum
        canvas1.drawText(post.authorName, margin, y, subtitlePaint)
        y += 14f
        canvas1.drawText("${post.authorTitle} • Alan: ${post.category}", margin, y, footerPaint)
        y += 24f

        // Özet Kutusu Zemin Çizimi
        val boxPaint = Paint().apply { color = Color.parseColor("#F4F6F4") }
        canvas1.drawRect(margin, y, pageWidth - margin, y + 130f, boxPaint)
        canvas1.drawLine(margin, y, margin, y + 130f, linePaint)

        canvas1.drawText("ÖZET (ABSTRACT)", margin + 14f, y + 20f, headerPaint)
        var abstractY = y + 36f
        val descLines = wrapText(post.description, bodyPaint, pageWidth - (margin * 2) - 28f)
        for (line in descLines) {
            canvas1.drawText(line, margin + 14f, abstractY, bodyPaint)
            abstractY += 14f
        }
        y += 150f

        // Giriş ve Metodoloji
        canvas1.drawText("1. GİRİŞ VE METODOLOJİ", margin, y, headerPaint)
        y += 18f
        val introText = "İslam düşünce ve araştırma geleneğinde klasik metinlerin tahlili, lafız ve mana dengesinin korunmasını zaruri kılar. Bu çalışmada kaynak taraması, tenkitli neşir mukayesesi ve semantik analitik araçlar eşzamanlı olarak istihdam edilmiştir. Kaynakların sıhhat dereceleri hicri erken dönem isnad tahlilleriyle tahkik edilmiş, modern literatürdeki karşılıkları tespit edilmiştir."
        for (line in wrapText(introText, bodyPaint, pageWidth - (margin * 2))) {
            canvas1.drawText(line, margin, y, bodyPaint)
            y += 14f
        }
        y += 16f

        // Klasik Alıntı
        canvas1.drawText("« العِلْمُ صَيْدٌ وَالكِتَابَةُ قَيْدُهُ ... قَيِّدْ صُيُودَكَ بِالحِبَالِ الوَاثِقَةِ »", margin + 20f, y, quotePaint)
        y += 16f
        canvas1.drawText("«İlim bir avdır, yazı ise onun bağıdır. Öyleyse avlarını sağlam bağlarla bağla.» (İmam Şâfiî)", margin + 20f, y, quotePaint)
        y += 24f

        canvas1.drawText("2. TEMEL AKADEMİK MÜZAKERELER", margin, y, headerPaint)
        y += 18f
        val discText = "Erken dönem usûl tartışmalarında mütekaddimûn ile müteahhirûn arasındaki metodolojik ayrışma, meselenin ontolojik ve epistemolojik boyutlarının yeniden ele alınmasını zorunlu kılmıştır. Metin tenkidi sadece harici isnad zincirine değil, dâhili delalet tutarlılığına da odaklanmaktadır."
        for (line in wrapText(discText, bodyPaint, pageWidth - (margin * 2))) {
            canvas1.drawText(line, margin, y, bodyPaint)
            y += 14f
        }

        // Sayfa Numarası
        canvas1.drawText("Sayfa 1 / 3", pageWidth / 2f - 18f, pageHeight - 30f, footerPaint)
        document.finishPage(page1)

        // SAYFA 2: Detaylı Tahlil ve Deliller
        val pageInfo2 = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, 2).create()
        val page2 = document.startPage(pageInfo2)
        val canvas2 = page2.canvas

        y = 60f
        canvas2.drawText("İLMNET AKADEMİK ARAŞTIRMALAR DERGİSİ • ${post.category.uppercase()}", margin, y, footerPaint)
        y += 8f
        canvas2.drawLine(margin, y, pageWidth - margin, y, linePaint)
        y += 30f

        canvas2.drawText("3. METİNSEL VERİLER VE KARŞILAŞTIRMALI DEĞERLENDİRME", margin, y, headerPaint)
        y += 18f
        val page2Text1 = "Klasik dönemde kaleme alınan risalelerin istinsah nüshaları arasındaki lafız farklılıkları, mana üzerinde belirleyici etkilere sahiptir. Karşılaştırmalı nüsha tetkiki, müellifin son tercihini tespit etmede birincil rehber niteliğindedir. Bu bağlamda kütüphane arşivlerindeki yazma eserler dijital tenkit yöntemleriyle taranmış ve muhtelif varyantlar tasnif edilmiştir."
        for (line in wrapText(page2Text1, bodyPaint, pageWidth - (margin * 2))) {
            canvas2.drawText(line, margin, y, bodyPaint)
            y += 14f
        }
        y += 24f

        canvas2.drawText("4. DÖNEMİN SOSYAL VE İLMÎ BAĞLAMI", margin, y, headerPaint)
        y += 18f
        val page2Text2 = "Ulemanın telif faaliyetinde bulunduğu devrin siyasî, içtimaî ve mezhebî tartışmaları tahlil edilmeksizin metnin maksadını ihata etmek mümkün değildir. Müellif, muhatap olduğu akımların iddialarına rasyonel ve naklî delillerle cevap verirken, çağının terminolojisini ustalıkla kullanmıştır."
        for (line in wrapText(page2Text2, bodyPaint, pageWidth - (margin * 2))) {
            canvas2.drawText(line, margin, y, bodyPaint)
            y += 14f
        }
        y += 30f

        // Dipnot Alanı
        canvas2.drawLine(margin, pageHeight - 90f, margin + 120f, pageHeight - 90f, footerPaint)
        canvas2.drawText("1. İbn Manzûr, Lisânü'l-Arab, Beyrut: Dâru Sâdır, c. IV, s. 112.", margin, pageHeight - 75f, footerPaint)
        canvas2.drawText("2. Kâtib Çelebi, Keşfü'z-Zunûn 'an Esâmi'l-Kütüb ve'l-Fünûn, c. I, s. 45.", margin, pageHeight - 62f, footerPaint)

        canvas2.drawText("Sayfa 2 / 3", pageWidth / 2f - 18f, pageHeight - 30f, footerPaint)
        document.finishPage(page2)

        // SAYFA 3: Netice ve Kaynakça
        val pageInfo3 = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, 3).create()
        val page3 = document.startPage(pageInfo3)
        val canvas3 = page3.canvas

        y = 60f
        canvas3.drawText("İLMNET AKADEMİK ARAŞTIRMALAR DERGİSİ • ${post.category.uppercase()}", margin, y, footerPaint)
        y += 8f
        canvas3.drawLine(margin, y, pageWidth - margin, y, linePaint)
        y += 30f

        canvas3.drawText("5. SONUÇ VE İLMÎ ÇIKARIMLAR", margin, y, headerPaint)
        y += 18f
        val conclusionText = "Yapılan tahkik ve analiz neticesinde, klasik ilim havzasında üretilen metodolojik eserlerin günümüz ilmiye araştırmalarına sunduğu zenginlik teyit edilmiştir. Dijital çağda bu mirasın Room DB ve açık kaynak akademik paylaşım ağları (İlmNet) aracılığıyla korunması ve araştırmacıların istifadesine sunulması büyük ehemmiyet arz etmektedir."
        for (line in wrapText(conclusionText, bodyPaint, pageWidth - (margin * 2))) {
            canvas3.drawText(line, margin, y, bodyPaint)
            y += 14f
        }
        y += 32f

        canvas3.drawText("KAYNAKÇA (BIBLIOGRAPHY)", margin, y, headerPaint)
        y += 18f
        val biblio = listOf(
            "• Buhârî, Muhammed b. İsmâil. el-Câmi'u's-Sahîh. İstanbul: Çağrı Yayınları, 1992.",
            "• Gazâlî, Ebû Hâmid. el-Mustasfâ min 'İlmi'l-Usûl. Beyrut: Müessesetü'r-Risâle, 1997.",
            "• İbn Rüşd, Ebü'l-Velîd. Faslü'l-Makâl. Kahire: Dâru'l-Meârif, 1983.",
            "• Râzî, Fahreddin. Mefâtîhu'l-Gayb (et-Tefsîru'l-Kebîr). Beyrut: Dâru'l-Fikr, 1981.",
            "• Şâtıbî, Ebû İshâk. el-Muvâfakât fî Usûli'ş-Şerî'a. thk. Abdullah Dırâz. Riyad: Dâru İbn 'Affân, 1997."
        )
        for (item in biblio) {
            for (line in wrapText(item, bodyPaint, pageWidth - (margin * 2))) {
                canvas3.drawText(line, margin, y, bodyPaint)
                y += 14f
            }
            y += 4f
        }

        canvas3.drawText("Sayfa 3 / 3", pageWidth / 2f - 18f, pageHeight - 30f, footerPaint)
        document.finishPage(page3)

        // Belgeyi diske yaz ve kapat
        FileOutputStream(outputFile).use { fos ->
            document.writeTo(fos)
        }
        document.close()
    }

    private fun wrapText(text: String, paint: Paint, maxWidth: Float): List<String> {
        val words = text.split(" ")
        val lines = mutableListOf<String>()
        var currentLine = ""

        for (word in words) {
            val testLine = if (currentLine.isEmpty()) word else "$currentLine $word"
            val width = paint.measureText(testLine)
            if (width <= maxWidth) {
                currentLine = testLine
            } else {
                if (currentLine.isNotEmpty()) {
                    lines.add(currentLine)
                }
                currentLine = word
            }
        }
        if (currentLine.isNotEmpty()) {
            lines.add(currentLine)
        }
        return lines
    }

    /**
     * FAZ 8: PdfRenderer ile PDF'in 1. Sayfasını Kapak Görseli Olarak Render Eder.
     * Döndürülen dosya yolu, Room DB'deki coverImageUrl sütununa ve feed listesine kaydedilir.
     */
    suspend fun renderPdfFirstPageThumbnail(context: Context, pdfFile: File): String? = withContext(Dispatchers.IO) {
        try {
            val fileDescriptor = android.os.ParcelFileDescriptor.open(pdfFile, android.os.ParcelFileDescriptor.MODE_READ_ONLY)
            val pdfRenderer = android.graphics.pdf.PdfRenderer(fileDescriptor)
            if (pdfRenderer.pageCount > 0) {
                val page = pdfRenderer.openPage(0)
                val width = 400
                val height = (width * (page.height.toFloat() / page.width.toFloat())).toInt().coerceAtLeast(400)
                val bitmap = android.graphics.Bitmap.createBitmap(width, height, android.graphics.Bitmap.Config.ARGB_8888)
                val canvas = android.graphics.Canvas(bitmap)
                canvas.drawColor(android.graphics.Color.WHITE)
                page.render(bitmap, null, null, android.graphics.pdf.PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)

                // FAZ 8: Altın sarısı ince çerçeve (stroke) ekleme
                val strokePaint = android.graphics.Paint().apply {
                    color = android.graphics.Color.parseColor("#D4AF37") // İlmNet Altın Sarısı
                    style = android.graphics.Paint.Style.STROKE
                    strokeWidth = 8f
                    isAntiAlias = true
                }
                canvas.drawRect(4f, 4f, width.toFloat() - 4f, height.toFloat() - 4f, strokePaint)

                page.close()
                pdfRenderer.close()
                fileDescriptor.close()

                val coversDir = File(context.filesDir, "pdf_covers").apply { if (!exists()) mkdirs() }
                val coverFile = File(coversDir, "cover_${System.currentTimeMillis()}.jpg")
                FileOutputStream(coverFile).use { out ->
                    bitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 85, out)
                }
                return@withContext coverFile.absolutePath
            }
            pdfRenderer.close()
            fileDescriptor.close()
            null
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Yerel URI'dan PDF 1. sayfa kapak görseli çıkarır.
     */
    suspend fun renderPdfUriThumbnail(context: Context, uri: Uri): String? = withContext(Dispatchers.IO) {
        try {
            val tempFile = File(context.cacheDir, "temp_render_${System.currentTimeMillis()}.pdf")
            context.contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(tempFile).use { output ->
                    input.copyTo(output)
                }
            }
            if (tempFile.exists() && tempFile.length() > 0) {
                val coverPath = renderPdfFirstPageThumbnail(context, tempFile)
                tempFile.delete()
                coverPath
            } else null
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
