package com.example.ui.notification

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.data.local.AppDatabase
import com.example.data.local.entity.NotificationEntity
import com.example.databinding.FragmentNotificationHistoryBinding
import com.example.ui.common.IlmToast
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * İlmNet - İlmi Bildirim Geçmişi Merkezi (FAZ 8).
 * Room DB'deki bildirimleri gerçek zamanlı listeler; okunmamış/okunmuş ayrımı ve kaydırarak silme sunar.
 */
class NotificationHistoryFragment : BottomSheetDialogFragment() {

    private var _binding: FragmentNotificationHistoryBinding? = null
    private val binding get() = _binding!!

    private lateinit var adapter: NotificationAdapter
    private val database by lazy { AppDatabase.getInstance(requireContext()) }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentNotificationHistoryBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState) as BottomSheetDialog
        dialog.setOnShowListener {
            val bottomSheet = dialog.findViewById<FrameLayout>(com.google.android.material.R.id.design_bottom_sheet)
            bottomSheet?.let { sheet ->
                val behavior = BottomSheetBehavior.from(sheet)
                behavior.state = BottomSheetBehavior.STATE_EXPANDED
                behavior.isDraggable = true
                behavior.skipCollapsed = true
                val displayMetrics = resources.displayMetrics
                sheet.layoutParams.height = (displayMetrics.heightPixels * 0.90).toInt()
            }
        }
        return dialog
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        setupListeners()
        observeNotifications()
    }

    private fun setupRecyclerView() {
        adapter = NotificationAdapter { notification ->
            markAsRead(notification.id)
        }

        binding.rvNotifications.layoutManager = LinearLayoutManager(requireContext())
        binding.rvNotifications.adapter = adapter

        // FAZ 8: Kaydırarak Silme (Swipe-to-delete)
        val itemTouchHelper = ItemTouchHelper(object : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT) {
            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean = false

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val position = viewHolder.adapterPosition
                if (position != RecyclerView.NO_POSITION && position < adapter.currentList.size) {
                    val itemToDelete = adapter.currentList[position]
                    deleteNotification(itemToDelete)
                }
            }
        })
        itemTouchHelper.attachToRecyclerView(binding.rvNotifications)
    }

    private fun setupListeners() {
        binding.btnNotificationBack.setOnClickListener {
            dismiss()
        }

        binding.btnMarkAllRead.setOnClickListener {
            viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
                database.notificationDao().markAllAsRead()
                withContext(Dispatchers.Main) {
                    activity?.let {
                        IlmToast.info(it, "Tüm bildirimler okundu olarak işaretlendi.")
                    }
                }
            }
        }

        binding.btnClearAllNotifications.setOnClickListener {
            AlertDialog.Builder(requireContext())
                .setTitle("Bildirimleri Temizle 🗑️")
                .setMessage("Tüm bildirim geçmişini silmek istediğinize emin misiniz?")
                .setPositiveButton("Temizle") { _, _ ->
                    viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
                        database.notificationDao().clearAll()
                        withContext(Dispatchers.Main) {
                            activity?.let {
                                IlmToast.info(it, "Bildirim geçmişi temizlendi.")
                            }
                        }
                    }
                }
                .setNegativeButton("Vazgeç", null)
                .show()
        }
    }

    private fun observeNotifications() {
        binding.shimmerNotifications.visibility = View.VISIBLE
        binding.shimmerNotifications.startShimmer()
        binding.rvNotifications.visibility = View.GONE
        binding.layoutEmptyNotifications.visibility = View.GONE

        viewLifecycleOwner.lifecycleScope.launch {
            database.notificationDao().getAllNotifications().collectLatest { list ->
                binding.shimmerNotifications.stopShimmer()
                binding.shimmerNotifications.visibility = View.GONE

                if (list.isEmpty()) {
                    binding.layoutEmptyNotifications.visibility = View.VISIBLE
                    binding.rvNotifications.visibility = View.GONE
                } else {
                    binding.layoutEmptyNotifications.visibility = View.GONE
                    binding.rvNotifications.visibility = View.VISIBLE
                    adapter.submitList(list)
                }
            }
        }
    }

    private fun markAsRead(id: Long) {
        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
            database.notificationDao().markAsRead(id)
        }
    }

    private fun deleteNotification(item: NotificationEntity) {
        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
            database.notificationDao().deleteNotification(item)
            withContext(Dispatchers.Main) {
                activity?.let {
                    IlmToast.info(it, "Bildirim silindi.", title = "Kayıt Silindi 🗑️")
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        fun newInstance(): NotificationHistoryFragment = NotificationHistoryFragment()
    }
}
