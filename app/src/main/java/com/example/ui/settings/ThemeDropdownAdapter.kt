package com.example.ui.settings

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import com.example.databinding.ItemThemeDropdownBinding

/**
 * İlmNet - 5 Tema İçin Renk Önizlemeli ve Başlıklı Spinner / Dropdown Adaptörü.
 */
class ThemeDropdownAdapter(
    private val context: Context,
    private val themes: List<AppTheme> = AppTheme.values().toList(),
    private var selectedThemeId: String = AppTheme.GLASSMORPHISM.id
) : BaseAdapter() {

    fun setSelectedTheme(themeId: String) {
        selectedThemeId = themeId
        notifyDataSetChanged()
    }

    override fun getCount(): Int = themes.size

    override fun getItem(position: Int): AppTheme = themes[position]

    override fun getItemId(position: Int): Long = position.toLong()

    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
        val binding: ItemThemeDropdownBinding
        val view: View

        if (convertView == null) {
            binding = ItemThemeDropdownBinding.inflate(LayoutInflater.from(context), parent, false)
            view = binding.root
            view.tag = binding
        } else {
            view = convertView
            binding = view.tag as ItemThemeDropdownBinding
        }

        val item = getItem(position)
        binding.tvThemeTitle.text = item.title
        binding.tvThemeSubtitle.text = item.subtitle

        // Renk daresi oluştur
        val colorDrawable = GradientDrawable().apply {
            shape = GradientDrawable.OVAL
            setColor(Color.parseColor(item.accentColorHex))
            setStroke(2, Color.parseColor("#4DFFFFFF"))
        }
        binding.viewThemeColorPreview.background = colorDrawable

        // Seçili işaretini göster
        val isSelected = item.id.equals(selectedThemeId, ignoreCase = true)
        binding.ivThemeCheck.visibility = if (isSelected) View.VISIBLE else View.GONE

        return view
    }

    override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup?): View {
        return getView(position, convertView, parent)
    }
}
