package com.example.ui.reader

import android.os.Bundle
import android.util.Log
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
 * İlim Diyârı - Yerleşik PDF Okuyucu Fragment'ı.
 * android.graphics.pdf.PdfRenderer kullanarak PDF belgesini uygulama içinde tam ekran render eder.
 *
 * Geri (Back) tuşuna basıldığında donanım ve yazılım geri isteklerini OnBackPressedCallback ile
 * yakalayarak uygulamanın kapanmasını önler ve güvenli şekilde önceki ekrana döner.
 */
typealias PdfViewerFragment = PdfReaderFragment

class PdfReaderFragment : Fragment() {

    private var _binding: ViewPdfReaderBinding? = null
    private val binding get() = _binding!!

    private var currentPost: PostEntity? = null
    private var pdfAdapter: PdfPageAdapter? = null
    private var onBackClickListener: (() -> Unit)? = null
    private var backPressedCallback: OnBackPressedCallback? = null

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
        backPressedCallback?.remove()
        val callback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                handleBackPress()
            }
        }
        backPressedCallback = callback
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, callback)
    }

    private fun handleBackPress() {
        if (onBackClickListener != null) {
            onBackClickListener?.invoke()
            return
        }

        val popped = try {
            findNavController().popBackStack()
        } catch (e: Exception) {
            Log.d("PdfReaderFragment", "NavController popBackStack fallback: ${e.message}")
            false
        }

        if (!popped) {
            if (parentFragmentManager.backStackEntryCount > 0) {
                parentFragmentManager.popBackStack()
            } else {
                (activity as? com.example.MainActivity)?.closeInternalPdfReaderPublic()
            }
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
        // Toolbar Geri Butonu
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
        val b = _binding ?: return
        b.tvReaderDocTitle.text = post.title
        b.tvReaderDocAuthor.text = "${post.authorName} • ${post.category}"
        b.layoutReaderLoading.visibility = View.VISIBLE
        b.rvPdfPages.visibility = View.GONE

        setToolMode(isPenMode = false)

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val pdfFile = PdfDocumentHelper.preparePdfFile(requireContext(), post)
                pdfAdapter?.close()
                val adapter = PdfPageAdapter(
                    pdfFile = pdfFile,
                    postId = post.id
                )
                pdfAdapter = adapter

                _binding?.let { currentBinding ->
                    currentBinding.rvPdfPages.apply {
                        layoutManager = LinearLayoutManager(requireContext())
                        this.adapter = adapter
                    }
                    currentBinding.layoutReaderLoading.visibility = View.GONE
                    currentBinding.rvPdfPages.visibility = View.VISIBLE
                }
            } catch (e: Exception) {
                _binding?.let { currentBinding ->
                    currentBinding.layoutReaderLoading.visibility = View.GONE
                    Toast.makeText(
                        requireContext(),
                        "PDF belgesi işlenemedi: ${e.localizedMessage}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    private fun setToolMode(isPenMode: Boolean) {
        pdfAdapter?.isDrawingMode = isPenMode
        _binding?.let { b ->
            if (isPenMode) {
                b.btnReaderModePen.setBackgroundResource(R.drawable.bg_tab_active)
                b.btnReaderModeEye.setBackgroundResource(R.drawable.bg_tab_inactive)
                b.tvReaderModeHint.text = "✍️ Kalem Modu: Sayfa üzerine dokunarak fosforlu sarı vurgulama yapın."
            } else {
                b.btnReaderModeEye.setBackgroundResource(R.drawable.bg_tab_active)
                b.btnReaderModePen.setBackgroundResource(R.drawable.bg_tab_inactive)
                b.tvReaderModeHint.text = "👀 Okuma Modu: Sayfaları dikey kaydırarak inceleyin."
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        backPressedCallback?.remove()
        backPressedCallback = null
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

/**
 * Güvenli Navigation popBackStack yardımcısı.
 * NavHost / NavController mevcutsa doğrudan tetikler, aksi halde FragmentManager / Activity ile popBackStack yapar.
 */
private fun Fragment.findNavController(): SafeNavController = SafeNavController(this)

private class SafeNavController(private val fragment: Fragment) {
    fun popBackStack(): Boolean {
        return try {
            val navHostClass = Class.forName("androidx.navigation.fragment.NavHostFragment")
            val method = navHostClass.getMethod("findNavController", Fragment::class.java)
            val controller = method.invoke(null, fragment)
            val popMethod = controller.javaClass.getMethod("popBackStack")
            popMethod.invoke(controller) as? Boolean ?: false
        } catch (_: Throwable) {
            if (fragment.parentFragmentManager.backStackEntryCount > 0) {
                fragment.parentFragmentManager.popBackStack()
                true
            } else if (fragment.activity?.supportFragmentManager?.backStackEntryCount ?: 0 > 0) {
                fragment.activity?.supportFragmentManager?.popBackStack()
                true
            } else {
                (fragment.activity as? com.example.MainActivity)?.closeInternalPdfReaderPublic()
                true
            }
        }
    }
}
