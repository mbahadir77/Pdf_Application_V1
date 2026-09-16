package com.example.ui.reader

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.databinding.ItemPdfSearchResultBinding

/**
 * İlmNet - PDF Belge İçi Arama Sonuçları Adaptörü.
 */
class PdfSearchResultAdapter(
    private val onResultClicked: (PdfSearchResult) -> Unit
) : RecyclerView.Adapter<PdfSearchResultAdapter.ViewHolder>() {

    private val results = mutableListOf<PdfSearchResult>()

    fun submitResults(newResults: List<PdfSearchResult>) {
        results.clear()
        results.addAll(newResults)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemPdfSearchResultBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = results[position]
        holder.bind(item)
    }

    override fun getItemCount(): Int = results.size

    inner class ViewHolder(private val binding: ItemPdfSearchResultBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: PdfSearchResult) {
            binding.tvSearchResultPage.text = "Sayfa ${item.pageNumber}"
            binding.tvSearchResultCount.text = "• ${item.matchCount} eşleşme"
            binding.tvSearchResultSnippet.text = item.snippet

            binding.root.setOnClickListener {
                onResultClicked(item)
            }
        }
    }
}
