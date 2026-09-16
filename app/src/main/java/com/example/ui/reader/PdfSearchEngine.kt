package com.example.ui.reader

import android.content.Context
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.text.PDFTextStripper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.Locale

/**
 * İlmNet - PDF Belge İçi Metin Arama Motoru (Faz 4).
 * PdfBox-Android kütüphanesi kullanarak yerel PDF dokümanlarını sayfa sayfa tarar.
 * Aranan kelimelerin geçtiği sayfaları, eşleşme sayısını ve çevreleyen metin özetini (snippet) döner.
 */
data class PdfSearchResult(
    val pageNumber: Int, // 1-tabanlı sayfa numarası
    val matchCount: Int,
    val snippet: String
)

object PdfSearchEngine {

    private var isInitialized = false

    fun init(context: Context) {
        if (!isInitialized) {
            try {
                PDFBoxResourceLoader.init(context.applicationContext)
                isInitialized = true
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    suspend fun searchInPdf(file: File, query: String): List<PdfSearchResult> = withContext(Dispatchers.IO) {
        val results = mutableListOf<PdfSearchResult>()
        val trimmedQuery = query.trim()
        if (trimmedQuery.length < 2 || !file.exists()) return@withContext results

        val turkishLocale = Locale("tr", "TR")
        val lowerQuery = trimmedQuery.lowercase(turkishLocale)

        var document: PDDocument? = null
        try {
            document = PDDocument.load(file)
            val totalPages = document.numberOfPages
            val stripper = PDFTextStripper()

            for (pageIndex in 1..totalPages) {
                stripper.startPage = pageIndex
                stripper.endPage = pageIndex
                val pageText = try {
                    stripper.getText(document)
                } catch (e: Exception) {
                    ""
                }

                val lowerText = pageText.lowercase(turkishLocale)
                if (lowerText.contains(lowerQuery)) {
                    var count = 0
                    var startIndex = 0
                    var firstMatchIndex = -1
                    while (true) {
                        val idx = lowerText.indexOf(lowerQuery, startIndex)
                        if (idx == -1) break
                        if (firstMatchIndex == -1) firstMatchIndex = idx
                        count++
                        startIndex = idx + lowerQuery.length
                    }

                    val snippetStart = (firstMatchIndex - 32).coerceAtLeast(0)
                    val snippetEnd = (firstMatchIndex + lowerQuery.length + 42).coerceAtMost(pageText.length)
                    var snippet = pageText.substring(snippetStart, snippetEnd).replace("\r", " ").replace("\n", " ").trim()
                    if (snippetStart > 0) snippet = "...$snippet"
                    if (snippetEnd < pageText.length) snippet = "$snippet..."

                    results.add(
                        PdfSearchResult(
                            pageNumber = pageIndex,
                            matchCount = count,
                            snippet = snippet
                        )
                    )
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            try {
                document?.close()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        results
    }
}
