package com.example.ui.reader

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.R
import com.example.data.local.entity.PostEntity
import com.example.databinding.ViewPdfReaderBinding
import kotlinx.coroutines.launch

/**
 * İlim Diyârı - Yerleşik PDF Okuyucu Fragment'ı (Faz 3 & 13).
 * android.graphics.pdf.PdfRenderer kütüphanesini kullanarak PDF belgesini
 * harici tarayıcıya (Chrome vb.) ihtiyaç duymadan uygulama içinde tam ekran render eder.
 *
 * Geri (Back) tuşuna basıldığında uygulamanın tamamen kapanmasını (finish) engeller;
 * OnBackPressedCallback ile güvenli bir şekilde Ana Akış'a (Feed) dönüş yapar.
 */
typealias PdfViewerFragment = PdfReaderFragment

class PdfReaderFragment : Fragment() {

    private var _binding: ViewPdfReaderBinding? = null
    private val binding get() = _binding!!

    private var currentPost: PostEntity? = null
    private var pdfAdapter: PdfPageAdapter? = null
    private var onBackClickListener: (() -> Unit)? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = ViewPdfReaderBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupBackNavigation()
        setupListeners()
        currentPost?.let { loadPdfDocument(it) }
    }

    private fun setupBackNavigation() {
        requireActivity().onBackPressedDispatcher.addCallback(
            viewLifecycleOwner,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    handleBackPress()
                }
            }
        )
    }

    private fun handleBackPress() {
        if (onBackClickListener != null) {
            onBackClickListener?.invoke()
        } else if (parentFragmentManager.backStackEntryCount > 0) {
            parentFragmentManager.popBackStack()
        } else {
            (activity as? com.example.MainActivity)?.closeInternalPdfReaderPublic()
        }
    }

    fun setPost(post: PostEntity) {
        this.currentPost = post
        if (_binding != null) {
            loadPdfDocument(post)
        }
    }

    fun setOnBackClickListener(listener: () -> Unit) {
        this.onBackClickListener = listener
    }

    private fun setupListeners() {
        binding.btnReaderBack.setOnClickListener {
            handleBackPress()
        }

        binding.btnReaderModeEye.setOnClickListener {
            setToolMode(isPenMode = false)
        }

        binding.btnReaderModePen.setOnClickListener {
            setToolMode(isPenMode = true)
        }

        binding.btnReaderClearDraw.setOnClickListener {
            pdfAdapter?.clearAllDrawings()
            Toast.makeText(requireContext(), "Sayfa vurguları temizlendi.", Toast.LENGTH_SHORT).show()
        }
    }

    fun loadPdfDocument(post: PostEntity) {
        binding.tvReaderDocTitle.text = post.title
        binding.tvReaderDocAuthor.text = "${post.authorName} • ${post.category}"
        binding.layoutReaderLoading.visibility = View.VISIBLE
        binding.rvPdfPages.visibility = View.GONE

        setToolMode(isPenMode = false)

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val pdfFile = PdfDocumentHelper.preparePdfFile(requireContext(), post)
                pdfAdapter?.close()
                val adapter = PdfPageAdapter(pdfFile)
                pdfAdapter = adapter

                binding.rvPdfPages.apply {
                    layoutManager = LinearLayoutManager(requireContext())
                    this.adapter = adapter
                }

                binding.layoutReaderLoading.visibility = View.GONE
                binding.rvPdfPages.visibility = View.VISIBLE
            } catch (e: Exception) {
                binding.layoutReaderLoading.visibility = View.GONE
                Toast.makeText(
                    requireContext(),
                    "PDF belgesi işlenemedi: ${e.localizedMessage}",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    private fun setToolMode(isPenMode: Boolean) {
        pdfAdapter?.isDrawingMode = isPenMode
        if (isPenMode) {
            binding.btnReaderModePen.setBackgroundResource(R.drawable.bg_tab_active)
            binding.btnReaderModeEye.setBackgroundResource(R.drawable.bg_tab_inactive)
            binding.tvReaderModeHint.text = "✍️ Kalem Modu: Sayfa üzerine dokunarak fosforlu sarı vurgulama yapın."
        } else {
            binding.btnReaderModeEye.setBackgroundResource(R.drawable.bg_tab_active)
            binding.btnReaderModePen.setBackgroundResource(R.drawable.bg_tab_inactive)
            binding.tvReaderModeHint.text = "👀 Okuma Modu: Sayfaları dikey kaydırarak inceleyin."
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        pdfAdapter?.close()
        pdfAdapter = null
        _binding = null
    }

    companion object {
        fun newInstance(post: PostEntity): PdfReaderFragment {
            return PdfReaderFragment().apply {
                setPost(post)
            }
        }
    }
}
