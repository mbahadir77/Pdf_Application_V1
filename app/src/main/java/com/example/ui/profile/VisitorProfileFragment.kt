package com.example.ui.profile

import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import coil.load
import com.example.R
import com.example.data.local.AppDatabase
import com.example.data.local.entity.FollowEntity
import com.example.data.local.entity.PostEntity
import com.example.databinding.FragmentVisitorProfileBinding
import com.example.ui.common.IlmToast
import com.example.ui.feed.FeedAdapter
import com.example.ui.notification.NotificationHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * İlmNet - Ziyaretçi Profili ve Sosyal Ağ Takipçi Merkezi (Faz 5).
 */
class VisitorProfileFragment : Fragment() {

    private var _binding: FragmentVisitorProfileBinding? = null
    private val binding get() = _binding!!

    private var authorName: String = ""
    private var avatarUrl: String? = null
    private var authorId: String? = null

    private lateinit var feedAdapter: FeedAdapter
    private val badgeAdapter = AcademicBadgeAdapter { badge ->
        IlmToast.info(requireActivity(), badge.description, title = "${badge.category} • Seviye ${badge.level}: ${badge.rankTitle} ${badge.tierIcon}")
    }

    private var isFollowing = false
    private var currentUserId = "current_user"

    companion object {
        private const val ARG_AUTHOR_NAME = "arg_author_name"
        private const val ARG_AVATAR_URL = "arg_avatar_url"
        private const val ARG_AUTHOR_ID = "arg_author_id"

        fun newInstance(authorName: String, avatarUrl: String?, authorId: String? = null): VisitorProfileFragment {
            val fragment = VisitorProfileFragment()
            val args = Bundle().apply {
                putString(ARG_AUTHOR_NAME, authorName)
                putString(ARG_AVATAR_URL, avatarUrl)
                putString(ARG_AUTHOR_ID, authorId)
            }
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        authorName = arguments?.getString(ARG_AUTHOR_NAME) ?: "İlim Ehli"
        avatarUrl = arguments?.getString(ARG_AVATAR_URL)
        authorId = arguments?.getString(ARG_AUTHOR_ID)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentVisitorProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initUI()
        loadVisitorData()
    }

    private fun initUI() {
        binding.tvVisitorName.text = authorName
        binding.tvVisitorAcademicTitle.text = "Akademik Araştırmacı & Risale Müellifi"
        binding.tvVisitorBio.text = "$authorName, İlmNet akademik havuzunda ilahiyat ve İslami ilimler alanında araştırmalar yapıp PDF risaleleri neşretmektedir."

        if (!avatarUrl.isNullOrBlank()) {
            binding.ivVisitorAvatar.load(avatarUrl) {
                crossfade(true)
                placeholder(R.drawable.ic_academic_logo)
                error(R.drawable.ic_academic_logo)
            }
        } else {
            binding.ivVisitorAvatar.setImageResource(R.drawable.ic_academic_logo)
        }

        binding.btnVisitorBack.setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        // RecyclerViews
        feedAdapter = FeedAdapter(
            onLikeClicked = { post ->
                IlmToast.success(requireActivity(), "${post.title} risalesini beğendiniz! ❤️")
            },
            onCommentClicked = { _ -> },
            onReadPdfClicked = { post ->
                viewLifecycleOwner.lifecycleScope.launch {
                    val db = AppDatabase.getInstance(requireContext())
                    val currentUser = withContext(Dispatchers.IO) { db.userDao().getLastLoggedInUserDirect() }
                    val currentUserName = currentUser?.fullName ?: "Bir araştırmacı"
                    NotificationHelper.showPdfReadNotification(
                        context = requireContext().applicationContext,
                        pdfTitle = post.title,
                        readerName = currentUserName,
                        readerId = currentUser?.id,
                        authorId = post.userId,
                        targetPostId = post.id
                    )
                }
            },
            onAuthorClicked = { _, _, _ -> /* Zaten bu profildeyiz */ }
        )

        binding.rvVisitorPdfs.layoutManager = LinearLayoutManager(requireContext())
        binding.rvVisitorPdfs.adapter = feedAdapter

        binding.rvVisitorBadges.layoutManager = GridLayoutManager(requireContext(), 2)
        binding.rvVisitorBadges.adapter = badgeAdapter

        // Sekmeler
        binding.tabVisitorPdfs.setOnClickListener {
            selectTab(isPdfs = true)
        }
        binding.tabVisitorBadges.setOnClickListener {
            selectTab(isPdfs = false)
        }

        // Takip Butonu
        binding.btnVisitorFollow.setOnClickListener {
            toggleFollow()
        }
    }

    private fun selectTab(isPdfs: Boolean) {
        if (isPdfs) {
            binding.tabVisitorPdfs.setBackgroundResource(R.drawable.bg_tab_active)
            binding.tabVisitorPdfs.setTextColor(ContextCompat.getColor(requireContext(), R.color.gold_vibrant))

            binding.tabVisitorBadges.setBackgroundResource(R.drawable.bg_tab_inactive)
            binding.tabVisitorBadges.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_secondary))

            binding.rvVisitorPdfs.visibility = View.VISIBLE
            binding.rvVisitorBadges.visibility = View.GONE
        } else {
            binding.tabVisitorPdfs.setBackgroundResource(R.drawable.bg_tab_inactive)
            binding.tabVisitorPdfs.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_secondary))

            binding.tabVisitorBadges.setBackgroundResource(R.drawable.bg_tab_active)
            binding.tabVisitorBadges.setTextColor(ContextCompat.getColor(requireContext(), R.color.gold_vibrant))

            binding.rvVisitorPdfs.visibility = View.GONE
            binding.rvVisitorBadges.visibility = View.VISIBLE
        }
    }

    private fun loadVisitorData() {
        lifecycleScope.launch {
            val db = AppDatabase.getInstance(requireContext())

            // Giriş yapmış mevcut kullanıcı kimliğini al
            val currentUser = withContext(Dispatchers.IO) { db.userDao().getLastLoggedInUserDirect() }
            currentUserId = currentUser?.id ?: "current_user"
            val currentUserName = currentUser?.fullName.orEmpty()

            // Kendi profilini inceliyorsa Takip Et butonunu gizle (Self-Follow engelleme)
            val isSelf = (authorId != null && authorId == currentUserId) ||
                    (currentUser != null && authorId == currentUser.id) ||
                    authorName.equals(currentUserName, ignoreCase = true)

            if (isSelf) {
                binding.btnVisitorFollow.visibility = View.GONE
            } else {
                binding.btnVisitorFollow.visibility = View.VISIBLE
                // Takip durumunu dinle
                val following = withContext(Dispatchers.IO) {
                    db.followDao().isFollowingSync(currentUserId, authorName)
                }
                isFollowing = following
                updateFollowButtonUI(isFollowing)
            }

            // Yazarın PDF gönderilerini çek
            val authorPosts = withContext(Dispatchers.IO) {
                db.postDao().getPostsByAuthor(authorName).firstOrNull() ?: emptyList()
            }

            val finalPosts = if (authorPosts.isNotEmpty()) {
                authorPosts
            } else {
                // Eğer post yoksa zengin ilmi içerik simülasyonu
                listOf(
                    PostEntity(
                        id = "author_seed_1",
                        title = "$authorName - Sahih Hadis Usûlünde Metin Tenkidi",
                        description = "Klasik dönem ile çağdaş dönem arasındaki cerh-tadil metodolojilerinin mukayesesi.",
                        authorName = authorName,
                        authorTitle = "Hadis Kürsüsü",
                        authorAvatarUrl = avatarUrl,
                        category = "Hadis",
                        pdfUrl = "https://ilmnet.org/docs/hadis_usulu.pdf",
                        pdfSize = "2.8 MB",
                        likeCount = 18,
                        commentCount = 4
                    ),
                    PostEntity(
                        id = "author_seed_2",
                        title = "$authorName - Güncel İktisadî Meselelerde Fıkhî Çözümler",
                        description = "Katılım bankacılığı, sukuk ve dijital varlıkların fıkıh meclislerindeki hükümleri.",
                        authorName = authorName,
                        authorTitle = "Fıkıh Kürsüsü",
                        authorAvatarUrl = avatarUrl,
                        category = "Fıkıh",
                        pdfUrl = "https://ilmnet.org/docs/fikih_iktisat.pdf",
                        pdfSize = "4.1 MB",
                        likeCount = 31,
                        commentCount = 7
                    )
                )
            }

            feedAdapter.submitList(finalPosts)
            binding.tvVisitorPostsCount.text = finalPosts.size.toString()

            // Rozet hesaplama
            val categoryMap = finalPosts.groupingBy { it.category }.eachCount().toMutableMap()
            // Ziyaretçinin profiline akademik derinlik katmak için ek kategori ağırlıkları
            categoryMap["Tefsir"] = (categoryMap["Tefsir"] ?: 0) + 2
            categoryMap["Hadis"] = (categoryMap["Hadis"] ?: 0) + 5
            categoryMap["Akaid"] = (categoryMap["Akaid"] ?: 0) + 1

            val calculatedBadges = AcademicBadgeEngine.calculateBadges(categoryMap)
            badgeAdapter.submitList(calculatedBadges)

            val unlockedCount = calculatedBadges.count { it.isUnlocked }
            binding.tvVisitorBadgesCount.text = unlockedCount.toString()

            // Takipçi sayısı
            val baseFollowers = 120 + finalPosts.size * 14 + (if (isFollowing) 1 else 0)
            binding.tvVisitorFollowersCount.text = baseFollowers.toString()
        }
    }

    private fun toggleFollow() {
        lifecycleScope.launch {
            val db = AppDatabase.getInstance(requireContext())
            isFollowing = !isFollowing
            updateFollowButtonUI(isFollowing)

            withContext(Dispatchers.IO) {
                if (isFollowing) {
                    db.followDao().follow(
                        FollowEntity(
                            followerId = currentUserId,
                            followedAuthor = authorName
                        )
                    )
                } else {
                    db.followDao().unfollow(currentUserId, authorName)
                }
            }

            if (isFollowing) {
                IlmToast.success(
                    requireActivity(),
                    "$authorName takibe alındı! Yeni risaleleri ve rütbeleri size bildirilecek. ✨"
                )
                // Duolingo tarzı esprili sosyal bildirim
                NotificationHelper.showFollowedUserEarnedBadge(
                    context = requireContext().applicationContext,
                    authorName = authorName,
                    badgeName = "Hadis",
                    tierName = "Gümüş Rozet"
                )
            } else {
                IlmToast.info(requireActivity(), "$authorName takipten çıkarıldı.")
            }
        }
    }

    private fun updateFollowButtonUI(following: Boolean) {
        if (following) {
            binding.btnVisitorFollow.text = "✓ Takip Ediliyor"
            binding.btnVisitorFollow.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#1E293B"))
            binding.btnVisitorFollow.setTextColor(Color.parseColor("#10B981"))
            binding.btnVisitorFollow.strokeColor = ColorStateList.valueOf(Color.parseColor("#10B981"))
            binding.btnVisitorFollow.strokeWidth = 2
        } else {
            binding.btnVisitorFollow.text = "+ Takip Et"
            binding.btnVisitorFollow.backgroundTintList = ColorStateList.valueOf(
                ContextCompat.getColor(requireContext(), R.color.gold_vibrant)
            )
            binding.btnVisitorFollow.setTextColor(
                ContextCompat.getColor(requireContext(), R.color.btn_text_dark)
            )
            binding.btnVisitorFollow.strokeWidth = 0
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
