package com.example.ui.settings

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.example.R
import com.example.databinding.DialogAboutBottomSheetBinding
import com.example.ui.common.IlmToast
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

/**
 * İlmNet - Yıldırım Technologies & Açık Kaynak Lisansları "Hakkında" Bottom Sheet Diyaloğu (Faz 6).
 */
class AboutBottomSheetDialogFragment : BottomSheetDialogFragment() {

    private var _binding: DialogAboutBottomSheetBinding? = null
    private val binding get() = _binding!!

    override fun getTheme(): Int = R.style.Theme_IlmNet_BottomSheetDialog

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DialogAboutBottomSheetBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnCloseAbout.setOnClickListener {
            dismiss()
        }

        // Destek ve iletişim e-postası
        binding.btnAboutContactEmail.setOnClickListener {
            val emailIntent = Intent(Intent.ACTION_SENDTO).apply {
                data = Uri.parse("mailto:mhmdbhdryldrm@gmail.com")
                putExtra(Intent.EXTRA_SUBJECT, "İlmNet Akademik Platform Destek ve Geri Bildirim")
            }
            try {
                startActivity(Intent.createChooser(emailIntent, "E-posta Uygulaması Seçin"))
            } catch (e: Exception) {
                IlmToast.error(requireActivity(), "E-posta uygulaması bulunamadı: mhmdbhdryldrm@gmail.com")
            }
        }

        // GitHub deposu
        binding.btnAboutGithubRepo.setOnClickListener {
            val browserIntent = Intent(
                Intent.ACTION_VIEW,
                Uri.parse("https://github.com/ilmnet-academic/ilmnet-android")
            )
            try {
                startActivity(browserIntent)
            } catch (e: Exception) {
                IlmToast.info(requireActivity(), "github.com/ilmnet-academic/ilmnet-android")
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        fun newInstance(): AboutBottomSheetDialogFragment {
            return AboutBottomSheetDialogFragment()
        }
    }
}
