package com.example.ui.about

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.example.databinding.DialogReleaseNotesBottomSheetBinding
import com.example.util.performTokHaptic
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

/**
 * İlim Diyârı - Hikayeleştirilmiş Sürüm Notları (Heist-Style Epic Release Notes).
 * Sıkıcı teknik terimler yerine destansı anlatımla Faz 1'den Faz 12'ye kadar olan kronolojiyi sunar.
 */
class ReleaseNotesBottomSheetDialogFragment : BottomSheetDialogFragment() {

    private var _binding: DialogReleaseNotesBottomSheetBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DialogReleaseNotesBottomSheetBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        requireContext().performTokHaptic()

        binding.btnCloseReleaseNotes.setOnClickListener {
            dismiss()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        const val TAG = "ReleaseNotesBottomSheet"
        fun newInstance() = ReleaseNotesBottomSheetDialogFragment()
    }
}
