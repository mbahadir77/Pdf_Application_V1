package com.example

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.OpenableColumns
import android.util.Log
import android.view.KeyEvent
import android.view.View
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.view.inputmethod.InputMethodManager
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsAnimationCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.core.widget.doAfterTextChanged
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.firstOrNull
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import android.media.AudioManager
import android.view.WindowManager
import com.example.data.category.CategoryManager
import com.example.data.local.AppDatabase
import com.example.data.local.entity.BadgeEntity
import com.example.data.local.entity.PostEntity
import com.example.data.pref.SessionManager
import com.example.data.remote.RetrofitClient
import com.example.data.repository.AuthRepository
import com.example.data.repository.FeedRepository
import com.example.databinding.ActivityMainBinding
import com.example.util.performTokHaptic
import kotlinx.coroutines.delay
import com.example.ui.about.AboutFragment
import com.example.ui.auth.AuthMode
import com.example.ui.auth.AuthUiState
import com.example.ui.auth.AuthViewModel
import com.example.ui.auth.EditProfileUiState
import com.example.ui.feed.AddPostUiState
import com.example.ui.feed.FeedAdapter
import com.example.ui.feed.FeedViewModel
import android.graphics.Bitmap
import com.canhub.cropper.CropImageContract
import com.canhub.cropper.CropImageContractOptions
import com.canhub.cropper.CropImageOptions
import com.canhub.cropper.CropImageView
import com.google.android.material.snackbar.Snackbar
import com.example.ui.feed.NavTab
import com.example.ui.feed.ProfileTab
import com.example.ui.feed.SelectedPdfFile
import com.example.ui.comment.CommentsBottomSheetDialogFragment
import com.example.ui.common.IlmToast
import com.example.ui.notification.MotivationWorker
import com.example.ui.notification.NotificationHelper
import com.example.ui.notification.WisdomNotificationWorker
import com.example.ui.profile.AcademicBadgeAdapter
import com.example.ui.profile.VisitorProfileFragment
import com.example.ui.reader.HighlighterDrawingView
import com.example.ui.reader.PdfDocumentHelper
import com.example.ui.reader.PdfPageAdapter
import com.example.ui.reader.PdfSearchEngine
import com.example.ui.reader.PdfSearchResultAdapter
import com.example.ui.reader.PdfReaderFragment
import com.example.ui.reader.PdfViewerFragment
import com.example.ui.splash.SplashNavigationEvent
import com.example.ui.splash.SplashNavigationState
import com.example.ui.splash.SplashViewModel
import coil.load
import coil.request.CachePolicy
import coil.transform.CircleCropTransformation
import com.example.ui.settings.AboutBottomSheetDialogFragment
import com.example.ui.settings.AppSettingsPreferences
import com.example.ui.settings.AppTheme
import com.example.ui.settings.ThemeDropdownAdapter
import com.example.ui.settings.ThemeManager
import androidx.appcompat.app.AppCompatDelegate
import android.widget.AdapterView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.Locale

/**
 * İlmNet - Single Activity Mimarisi (Faz 1, Faz 2 & Faz 3).
 * ViewBinding, MVVM, Room DB (Offline-First), GitHub Hibrit Altyapısı,
 * Yerel Dosya Seçici (File Picker) ve Yerleşik PDF Okuyucu Motoru (PdfRenderer).
 */
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    private val appDatabase by lazy { AppDatabase.getInstance(applicationContext) }
    private val sessionManager by lazy { SessionManager(applicationContext) }

    private val authRepository by lazy {
        AuthRepository(appDatabase.userDao(), sessionManager)
    }

    private val feedRepository by lazy {
        FeedRepository(appDatabase.postDao(), RetrofitClient.gitHubService, sessionManager, applicationContext)
    }

    private val splashViewModel: SplashViewModel by viewModels {
        SplashViewModel.Factory(authRepository)
    }

    private val authViewModel: AuthViewModel by viewModels {
        AuthViewModel.Factory(authRepository)
    }

    private val feedViewModel: FeedViewModel by viewModels {
        FeedViewModel.Factory(feedRepository)
    }

    private lateinit var feedAdapter: FeedAdapter
    private lateinit var profileSharedWorksAdapter: FeedAdapter
    private lateinit var academicBadgeAdapter: AcademicBadgeAdapter
    private lateinit var searchResultAdapter: PdfSearchResultAdapter
    private var selectedAddPostCategory = "Tefsir"
    private var currentPdfAdapter: PdfPageAdapter? = null
    private var currentActivePdfFile: File? = null
    private var isPdfReaderOpen: Boolean = false
    private var previousBadgeLevels: Map<String, Int>? = null

    // FAZ 7 & 9: Android 13+ Gerçek Sistem Bildirimleri İzni & Hikmet Bildirimi
    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            NotificationHelper.createNotificationChannels(this)
            WisdomNotificationWorker.schedule(this)
        }
    }

    // FAZ 3: Cihazdan PDF Seçici (Yerel Dosya Yöneticisi)
    private val pdfPickerLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { handlePdfSelected(it) }
    }

    private var selectedAvatarPath: String? = null
    private var selectedPdfCoverPath: String? = null
    private var isSplashHandled: Boolean = false

    // FAZ 11: İtikâf Modu (Hardcore Focus Mode) Değişkenleri
    private var isItikafModeActive: Boolean = false
    private var itikafStartPage: Int = 0
    private var itikafMaxPageReached: Int = 0
    private var itikafFailed: Boolean = false

    // FAZ 5 REVİZYONU: WhatsApp Tarzı 1:1 Yuvarlak/Kare Görsel Kırpıcı (CanHub Cropper)
    private val cropImageLauncher = registerForActivityResult(CropImageContract()) { result ->
        if (result.isSuccessful) {
            val croppedUri = result.uriContent
            if (croppedUri != null) {
                handleCroppedAvatar(croppedUri)
            }
        } else {
            val exception = result.error
            if (exception != null) {
                Toast.makeText(this, "Fotoğraf kırpılamadı: ${exception.localizedMessage}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Galeriden fotoğraf seçici
    private val avatarPickerLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { launchCropper(it) }
    }

    private fun launchCropper(sourceUri: Uri) {
        val darkBg = ContextCompat.getColor(this, R.color.splash_bg_start)
        val goldColor = ContextCompat.getColor(this, R.color.gold_vibrant)
        val cropOptions = CropImageOptions(
            cropShape = CropImageView.CropShape.OVAL, // WhatsApp dairesel profil çerçevesi
            fixAspectRatio = true,
            aspectRatioX = 1,
            aspectRatioY = 1,
            guidelines = CropImageView.Guidelines.ON,
            activityTitle = "Profil Fotoğrafını Kırp",
            cropMenuCropButtonTitle = "Tamam",
            toolbarColor = darkBg,
            toolbarTitleColor = goldColor,
            toolbarBackButtonColor = goldColor,
            toolbarTintColor = goldColor,
            activityMenuIconColor = goldColor,
            activityBackgroundColor = darkBg,
            outputCompressFormat = Bitmap.CompressFormat.JPEG,
            outputCompressQuality = 90
        )
        cropImageLauncher.launch(CropImageContractOptions(sourceUri, cropOptions))
    }

    private fun handleCroppedAvatar(uri: Uri) {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val inputStream = contentResolver.openInputStream(uri) ?: return@launch
                val avatarDir = File(filesDir, "avatars").apply { if (!exists()) mkdirs() }
                val targetFile = File(avatarDir, "avatar_${System.currentTimeMillis()}.jpg")
                FileOutputStream(targetFile).use { out ->
                    inputStream.copyTo(out)
                }
                withContext(Dispatchers.Main) {
                    selectedAvatarPath = targetFile.absolutePath
                    displayAvatar(binding.viewEditProfile.ivEditAvatarPreview, selectedAvatarPath)
                    IlmToast.success(this@MainActivity, "Kırpılan profil fotoğrafı seçildi! Kaydetmek için 'Değişiklikleri Kaydet'e dokunun.")
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@MainActivity, "Fotoğraf işlenemedi: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // FAZ 4: Belge içi metin arama motorunu arka planda başlat (UI thread'i kitlemez)
        lifecycleScope.launch(Dispatchers.IO) {
            PdfSearchEngine.init(applicationContext)
        }

        // FAZ 5, 7 & 9: Bildirim Kanalları & 48 Saat İnaktivite Hikmet Bildirimi & Android 13+ İzin
        NotificationHelper.createNotificationChannels(this)
        WisdomNotificationWorker.schedule(this)
        MotivationWorker.schedule(this)
        checkNotificationPermission()
        com.example.data.pref.SessionManager(this).updateLastActiveTime()

        setupWindowInsets()
        setupAuthInteractions()
        setupDashboardInteractions()
        setupEditProfileInteractions()
        setupSettingsInteractions()
        setupFeedInteractions()
        setupAddPdfInteractions()
        setupPdfReaderInteractions()
        setupBottomNav()
        setupBackNavigation()

        // FAZ 6: Dinamik Tema Motoru ve Göz Koruması Başlangıç Durumu
        val appPrefs = AppSettingsPreferences.getInstance(this)
        val initialTheme = ThemeManager.getActiveTheme(this)
        applyAppTheme(initialTheme, showToast = false)
        updateEyeProtectionUI(appPrefs.isEyeProtectionEnabled)

        // FAZ 8: Giriş Serisi (Streak) Kaydı ve İlim Aynası Widget Güncellemesi
        com.example.data.pref.StreakManager.getInstance(this).recordDailyLogin()
        com.example.ui.widget.IlmMirrorWidgetProvider.requestWidgetUpdate(this)

        // FAZ 7: Dışarıdan Paylaşılan veya Açılan PDF Intent'ini Karşıla
        handleIncomingShareIntent(intent)

        observeNavigation()
        observeAuthState()
        observeCurrentUser()
        observeFeedState()
        observeEditProfileState()
        observeNotificationsCount()

        // Global havuz senkronizasyonu - Tüm kullanıcıların ve araştırmacıların eserlerini yükle
        feedViewModel.refreshFromGitHub()

        // Bildirim Deep Link Yönlendirmesini Gerçekleştir
        handleNotificationDeepLink(intent)

        // FAZ 12: Seher Modu (Teheccüd İradesi) - Gece 03:00 - 05:00 Selamı & Rozeti
        checkSeherModeOnLaunch()
    }

    /**
     * BÖLÜM 3.3: Seher Modu (Teheccüd İradesi).
     * Gece 03:00 ile 05:00 arasında uygulama açılırsa:
     * "Gecenin karanlığını ilimle aydınlatanlara selam olsun." mesajı çıkar
     * ve Room DB üzerinden kullanıcıya gizli 'Seher Vakti' rozeti takdim edilir.
     */
    private fun checkSeherModeOnLaunch() {
        val calendar = java.util.Calendar.getInstance()
        val hour = calendar.get(java.util.Calendar.HOUR_OF_DAY)
        if (hour in 3..4) { // 03:00:00 - 04:59:59
            val todayStr = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(java.util.Date())
            val seherPrefs = getSharedPreferences("ilm_seher_prefs", Context.MODE_PRIVATE)
            val lastDate = seherPrefs.getString("key_last_seher_date", null)
            if (lastDate != todayStr) {
                seherPrefs.edit().putString("key_last_seher_date", todayStr).apply()
                lifecycleScope.launch {
                    delay(1200)
                    if (isFinishing || isDestroyed) return@launch
                    IlmToast.success(
                        this@MainActivity,
                        "Gecenin karanlığını ilimle aydınlatanlara selam olsun.",
                        title = "Seher Vakti Mütalaası 🌙"
                    )
                    try {
                        val currentUserId = authViewModel.currentUser.value?.id ?: "local_user"
                        val db = AppDatabase.getInstance(applicationContext)
                        val seherBadge = BadgeEntity(
                            id = "${currentUserId}_seher_vakti",
                            userId = currentUserId,
                            category = "Teheccüd",
                            level = 1,
                            rankTitle = "Seher Vakti İradesi",
                            isUnlocked = true,
                            isNotified = true,
                            updatedAt = System.currentTimeMillis()
                        )
                        db.badgeDao().insertOrUpdateBadge(seherBadge)
                    } catch (_: Exception) {}
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        com.example.data.pref.SessionManager(this).updateLastActiveTime()
        authViewModel.currentUser.value?.let { user ->
            displayAvatar(binding.viewFeed.ivFeedToolbarAvatar, user.avatarUrl)
            displayAvatar(binding.viewDashboard.ivDashAvatar, user.avatarUrl)
        }
        if (itikafFailed) {
            itikafFailed = false
            IlmToast.error(this, "İtikâf Modu bozuldu! Uygulamadan ayrıldığınız için odaklanma zinciri kırıldı.", title = "İtikâf İhlali ⚠️")
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIncomingShareIntent(intent)
        handleNotificationDeepLink(intent)
    }

    private fun checkNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    /**
     * FAZ 7: Android Paylaş Menüsüne Entegrasyon (Incoming Share / View Intent).
     * Kullanıcı dosya yöneticisinde veya Chrome'da bir PDF'i paylaştığında veya açtığında,
     * PDF Uri'si otomatik olarak yakalanır ve doğrudan Add Screen üzerinde hazır hale getirilir.
     */
    private fun handleIncomingShareIntent(incomingIntent: Intent?) {
        if (incomingIntent == null) return
        val action = incomingIntent.action
        val type = incomingIntent.type

        val pdfUri: Uri? = when (action) {
            Intent.ACTION_SEND -> {
                if (type == "application/pdf" || type?.contains("pdf") == true || incomingIntent.hasExtra(Intent.EXTRA_STREAM)) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        incomingIntent.getParcelableExtra(Intent.EXTRA_STREAM, Uri::class.java)
                    } else {
                        @Suppress("DEPRECATION")
                        incomingIntent.getParcelableExtra(Intent.EXTRA_STREAM)
                    }
                } else null
            }
            Intent.ACTION_VIEW -> {
                if (type == "application/pdf" || type?.contains("pdf") == true || incomingIntent.data?.toString()?.endsWith(".pdf", ignoreCase = true) == true) {
                    incomingIntent.data
                } else null
            }
            else -> null
        }

        if (pdfUri != null) {
            // Kullanıcıyı doğrudan AddPdf (Eser Ekle) sekmesine yönlendir
            feedViewModel.selectNavTab(NavTab.ADD_PDF)
            switchMainTab(NavTab.ADD_PDF)

            handlePdfSelected(pdfUri)

            // Başlık alanını dosya adından otomatik olarak temizleyip doldur
            var candidateTitle = ""
            try {
                contentResolver.query(pdfUri, null, null, null, null)?.use { cursor ->
                    val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (cursor.moveToFirst() && nameIndex != -1) {
                        candidateTitle = cursor.getString(nameIndex) ?: ""
                    }
                }
            } catch (_: Exception) {}

            if (candidateTitle.isBlank()) {
                candidateTitle = pdfUri.lastPathSegment?.substringAfterLast('/') ?: ""
            }

            if (candidateTitle.isNotBlank()) {
                val cleanTitle = candidateTitle
                    .substringBeforeLast(".pdf")
                    .replace('_', ' ')
                    .replace('-', ' ')
                    .trim()
                binding.viewAddPdf.etAddTitle.setText(cleanTitle)
            }

            IlmToast.success(
                this,
                "Dışarıdan PDF alındı! Paylaşmak için bilgileri tamamlayın 📄",
                title = "Belge İçe Aktarıldı 📥"
            )
        }
    }

    /**
     * Bildirimlerden gelen Deep Link intentlerini yakalar:
     * - EXTRA_TARGET_POST_ID varsa doğrudan ilgili PDF okuyucusunu açar.
     * - EXTRA_TARGET_AUTHOR varsa ilgili araştırmacının ziyaretçi profilini açar.
     */
    private fun handleNotificationDeepLink(incomingIntent: Intent?) {
        if (incomingIntent == null) return
        val targetPostId = incomingIntent.getStringExtra(NotificationHelper.EXTRA_TARGET_POST_ID)
        val targetAuthor = incomingIntent.getStringExtra(NotificationHelper.EXTRA_TARGET_AUTHOR)

        if (!targetPostId.isNullOrBlank()) {
            lifecycleScope.launch {
                val db = AppDatabase.getInstance(this@MainActivity)
                val post = withContext(Dispatchers.IO) { db.postDao().getPostById(targetPostId) }
                if (post != null) {
                    openInternalPdfReader(post)
                }
            }
        } else if (!targetAuthor.isNullOrBlank()) {
            openVisitorProfile(targetAuthor, null, null)
        }
    }

    private fun setupWindowInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.mainRoot) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            val ime = insets.getInsets(WindowInsetsCompat.Type.ime())
            val bottomInset = maxOf(systemBars.bottom, ime.bottom)
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, bottomInset)

            val isKeyboardVisible = insets.isVisible(WindowInsetsCompat.Type.ime())
            if (isKeyboardVisible) {
                binding.layoutBottomNav.root.visibility = View.GONE
            } else if (binding.viewSplash.root.visibility != View.VISIBLE &&
                binding.viewAuth.root.visibility != View.VISIBLE &&
                binding.viewPdfReader.root.visibility != View.VISIBLE &&
                binding.viewEditProfile.root.visibility != View.VISIBLE &&
                binding.viewSettings.root.visibility != View.VISIBLE &&
                binding.fragmentContainer.visibility != View.VISIBLE
            ) {
                binding.layoutBottomNav.root.visibility = View.VISIBLE
            }

            WindowInsetsCompat.CONSUMED
        }

        ViewCompat.setWindowInsetsAnimationCallback(
            binding.mainRoot,
            object : WindowInsetsAnimationCompat.Callback(DISPATCH_MODE_STOP) {
                override fun onProgress(
                    insets: WindowInsetsCompat,
                    runningAnimations: MutableList<WindowInsetsAnimationCompat>
                ): WindowInsetsCompat {
                    val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
                    val ime = insets.getInsets(WindowInsetsCompat.Type.ime())
                    val bottomInset = maxOf(systemBars.bottom, ime.bottom)
                    binding.mainRoot.setPadding(systemBars.left, systemBars.top, systemBars.right, bottomInset)
                    return insets
                }

                override fun onEnd(animation: WindowInsetsAnimationCompat) {
                    super.onEnd(animation)
                    ViewCompat.requestApplyInsets(binding.mainRoot)
                }
            }
        )
    }

    private fun hideSoftKeyboard() {
        val currentFocusView = currentFocus ?: binding.mainRoot
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
        imm?.hideSoftInputFromWindow(currentFocusView.windowToken, 0)
        currentFocusView.clearFocus()
    }

    private fun setupBackNavigation() {
        supportFragmentManager.addOnBackStackChangedListener {
            if (supportFragmentManager.backStackEntryCount == 0) {
                binding.fragmentContainer.visibility = View.GONE
                if (binding.viewPdfReader.root.visibility != View.VISIBLE &&
                    binding.viewEditProfile.root.visibility != View.VISIBLE &&
                    binding.viewSettings.root.visibility != View.VISIBLE
                ) {
                    binding.layoutBottomNav.root.visibility = View.VISIBLE
                }
            }
        }

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                // PDF Okuma modundayken fiziksel veya sanal geri tuşuna basıldığında
                // uygulamanın kapanması veya çökmesi engellenir; doğrudan ana akışa dönülür.
                if (isPdfReaderOpen || binding.viewPdfReader.root.visibility == View.VISIBLE) {
                    closeInternalPdfReader()
                    return
                }
                if (supportFragmentManager.backStackEntryCount > 0) {
                    supportFragmentManager.popBackStack()
                    return
                }
                if (binding.viewEditProfile.root.visibility == View.VISIBLE) {
                    closeEditProfileScreen()
                    return
                }
                if (binding.viewSettings.root.visibility == View.VISIBLE) {
                    closeSettingsScreen()
                    return
                }
                if (feedViewModel.selectedTab.value != NavTab.FEED) {
                    feedViewModel.selectNavTab(NavTab.FEED)
                    switchMainTab(NavTab.FEED)
                    return
                }
                isEnabled = false
                onBackPressedDispatcher.onBackPressed()
                isEnabled = true
            }
        })
    }

    // ==========================================
    // FAZ 1: Kimlik Doğrulama & Oturum
    // ==========================================
    private fun setupAuthInteractions() {
        val authBinding = binding.viewAuth

        authBinding.btnTabLogin.setOnClickListener {
            authViewModel.setAuthMode(AuthMode.LOGIN)
            updateTabUi(AuthMode.LOGIN)
        }

        authBinding.btnTabRegister.setOnClickListener {
            authViewModel.setAuthMode(AuthMode.REGISTER)
            updateTabUi(AuthMode.REGISTER)
        }

        authBinding.btnAuthSubmit.setOnClickListener {
            val email = authBinding.etEmail.text?.toString().orEmpty()
            val password = authBinding.etPassword.text?.toString().orEmpty()

            when (authViewModel.authMode.value) {
                AuthMode.LOGIN -> {
                    authViewModel.login(email, password)
                }
                AuthMode.REGISTER -> {
                    val fullName = authBinding.etFullName.text?.toString().orEmpty()
                    authViewModel.register(email, fullName, password)
                }
            }
        }
    }

    private fun updateTabUi(mode: AuthMode) {
        val authBinding = binding.viewAuth
        when (mode) {
            AuthMode.LOGIN -> {
                authBinding.btnTabLogin.setBackgroundResource(R.drawable.bg_tab_active)
                authBinding.btnTabLogin.setTextColor(getColor(R.color.gold_start))
                authBinding.btnTabRegister.setBackgroundResource(R.drawable.bg_tab_inactive)
                authBinding.btnTabRegister.setTextColor(getColor(R.color.text_secondary))

                authBinding.tvFormHeader.setText(R.string.title_welcome_back)
                authBinding.containerFullName.visibility = View.GONE
                authBinding.btnAuthSubmit.setText(R.string.btn_login)
                authBinding.tvAuthError.visibility = View.GONE
            }
            AuthMode.REGISTER -> {
                authBinding.btnTabLogin.setBackgroundResource(R.drawable.bg_tab_inactive)
                authBinding.btnTabLogin.setTextColor(getColor(R.color.text_secondary))
                authBinding.btnTabRegister.setBackgroundResource(R.drawable.bg_tab_active)
                authBinding.btnTabRegister.setTextColor(getColor(R.color.gold_start))

                authBinding.tvFormHeader.setText(R.string.title_register)
                authBinding.containerFullName.visibility = View.VISIBLE
                authBinding.btnAuthSubmit.setText(R.string.btn_register)
                authBinding.tvAuthError.visibility = View.GONE
            }
        }
    }

    private fun setupDashboardInteractions() {
        val dashBinding = binding.viewDashboard

        // FAZ 5: Sağ Üst Köşe Ayarlar İkonu ve Profili Düzenle Butonu
        dashBinding.btnDashboardSettings.setOnClickListener {
            openSettingsScreen()
        }
        dashBinding.btnEditProfile.setOnClickListener {
            openEditProfileScreen()
        }
        dashBinding.btnDashLogoutAction.setOnClickListener { performLogout() }

        // FAZ 4: Profil İki Cam Efektli Sekme (Paylaşılan Eserler | Rozetler)
        dashBinding.btnTabSharedWorks.setOnClickListener {
            feedViewModel.selectProfileTab(ProfileTab.SHARED_WORKS)
        }

        dashBinding.btnTabBadges.setOnClickListener {
            feedViewModel.selectProfileTab(ProfileTab.BADGES)
        }

        // Paylaşılan Eserler Listesi (Profile)
        profileSharedWorksAdapter = FeedAdapter(
            onLikeClicked = { post ->
                feedViewModel.toggleLike(post.id)
                if (!post.isLiked) {
                    IlmToast.success(this@MainActivity, "${post.title} beğenildi! ❤️")
                }
            },
            onCommentClicked = { post ->
                openCommentsBottomSheet(post)
            },
            onReadPdfClicked = { post ->
                val appPrefs = AppSettingsPreferences.getInstance(this@MainActivity)
                if (appPrefs.isWifiOnlyDownload && !isWifiConnected(this@MainActivity)) {
                    IlmToast.warning(
                        this@MainActivity,
                        "Veri Tasarrufu Aktif: PDF belgeleri sadece Wi-Fi bağlantısı ile indirilebilir.",
                        title = "Wi-Fi Gerekli 📶"
                    )
                } else {
                    val currentUserName = authViewModel.currentUser.value?.fullName ?: "Bir araştırmacı"
                    val currentUserId = authViewModel.currentUser.value?.id
                    NotificationHelper.showPdfReadNotification(
                        context = this@MainActivity,
                        pdfTitle = post.title,
                        readerName = currentUserName,
                        readerId = currentUserId,
                        authorId = post.userId,
                        targetPostId = post.id
                    )
                    openInternalPdfReader(post)
                }
            },
            onAuthorClicked = { authorName, avatarUrl, authorId ->
                openVisitorProfile(authorName, avatarUrl, authorId)
            },
            onDeletePostClicked = { post ->
                confirmDeletePost(post)
            },
            currentUserId = authViewModel.currentUser.value?.id,
            currentUserName = authViewModel.currentUser.value?.fullName
        )

        // FAZ 12: Ciltli Kitaplık (Book Spine) ve Ahşap Kütüphane Rafı
        val isTablet = resources.configuration.smallestScreenWidthDp >= 600
        val tabletCols = if (resources.configuration.screenWidthDp >= 900) 4 else 3

        var isBookSpineMode = true
        profileSharedWorksAdapter.isBookSpineMode = true

        fun applyProfileLibraryLayout(isSpine: Boolean) {
            isBookSpineMode = isSpine
            profileSharedWorksAdapter.isBookSpineMode = isSpine
            if (isSpine) {
                dashBinding.tvShelfHeaderTitle.text = "📖 Kütüphane Rafı (Ciltli Eserler)"
                dashBinding.rvProfileSharedWorks.setBackgroundResource(R.drawable.bg_wooden_library_shelf)
                dashBinding.btnProfileLayoutToggle.setImageResource(R.drawable.ic_view_list)
                dashBinding.rvProfileSharedWorks.layoutManager = if (isTablet) {
                    GridLayoutManager(this@MainActivity, if (resources.configuration.screenWidthDp >= 900) 8 else 6)
                } else {
                    LinearLayoutManager(this@MainActivity, LinearLayoutManager.HORIZONTAL, false)
                }
            } else {
                dashBinding.tvShelfHeaderTitle.text = "📄 Liste Görünümü (Tüm Risaleler)"
                dashBinding.rvProfileSharedWorks.background = null
                dashBinding.btnProfileLayoutToggle.setImageResource(R.drawable.ic_badge_book)
                dashBinding.rvProfileSharedWorks.layoutManager = if (isTablet) {
                    StaggeredGridLayoutManager(tabletCols, StaggeredGridLayoutManager.VERTICAL)
                } else {
                    LinearLayoutManager(this@MainActivity)
                }
            }
        }

        applyProfileLibraryLayout(true)

        dashBinding.btnProfileLayoutToggle.setOnClickListener {
            performTokHaptic()
            applyProfileLibraryLayout(!isBookSpineMode)
        }

        dashBinding.rvProfileSharedWorks.apply {
            adapter = profileSharedWorksAdapter
            itemAnimator = null
        }

        // 20 Akademik Kategori Rozet Matrisi (Tablet: 3-4 Sütun, Telefon: 2 Sütun + ShelfItemDecoration)
        academicBadgeAdapter = AcademicBadgeAdapter { badge ->
            if (badge.isUnlocked) {
                binding.viewConfetti.startCelebration()
                IlmToast.success(
                    this@MainActivity,
                    badge.description,
                    title = "${badge.category} • ${badge.tierCategoryName} ${badge.tierIcon}"
                )
            } else {
                IlmToast.info(
                    this@MainActivity,
                    badge.description,
                    title = "${badge.category} (Kilitli 🔒)"
                )
            }
        }
        dashBinding.rvAcademicBadges.apply {
            val badgeCols = if (isTablet) tabletCols else 2
            layoutManager = GridLayoutManager(this@MainActivity, badgeCols)
            adapter = academicBadgeAdapter
            itemAnimator = null
            // Rozetlerin havada uçuşmasını engelleyen, 3D ahşap ve altın yaldızlı sergi rafı
            addItemDecoration(com.example.ui.profile.ShelfItemDecoration(this@MainActivity))
        }
    }

    // ==========================================
    // FAZ 5: Profili Düzenle (Edit Profile) & Ayarlar (Settings)
    // ==========================================
    private fun setupEditProfileInteractions() {
        val editBinding = binding.viewEditProfile

        editBinding.btnEditProfileBack.setOnClickListener {
            closeEditProfileScreen()
        }

        editBinding.btnEditPickAvatar.setOnClickListener {
            avatarPickerLauncher.launch("image/*")
        }

        editBinding.containerEditAvatar.setOnClickListener {
            avatarPickerLauncher.launch("image/*")
        }

        editBinding.btnEditProfileSave.setOnClickListener {
            val name = editBinding.etEditName.text?.toString().orEmpty()
            val title = editBinding.etEditTitle.text?.toString().orEmpty()
            val bio = editBinding.etEditBio.text?.toString().orEmpty()
            val avatarPath = selectedAvatarPath ?: authViewModel.currentUser.value?.avatarUrl

            authViewModel.updateProfile(
                fullName = name,
                academicTitle = title,
                bio = bio,
                avatarUrl = avatarPath
            )
        }
    }

    private fun setupSettingsInteractions() {
        val settingsBinding = binding.viewSettings
        val appPrefs = AppSettingsPreferences.getInstance(this)

        settingsBinding.btnSettingsBack.setOnClickListener {
            closeSettingsScreen()
        }

        settingsBinding.btnSettingsEditProfileShortcut.setOnClickListener {
            closeSettingsScreen()
            openEditProfileScreen()
        }

        // 1. Dinamik Tema Seçimi (Dropdown / Spinner)
        val themes = AppTheme.values().toList()
        val currentTheme = ThemeManager.getActiveTheme(this)
        val themeAdapter = ThemeDropdownAdapter(this, themes, currentTheme.id)
        settingsBinding.spSettingsTheme.adapter = themeAdapter

        val initialPosition = themes.indexOfFirst { it.id.equals(currentTheme.id, ignoreCase = true) }.coerceAtLeast(0)
        settingsBinding.spSettingsTheme.setSelection(initialPosition, false)

        settingsBinding.spSettingsTheme.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val selectedTheme = themes[position]
                if (!selectedTheme.id.equals(appPrefs.currentThemeId, ignoreCase = true)) {
                    themeAdapter.setSelectedTheme(selectedTheme.id)
                    applyAppTheme(selectedTheme, showToast = true)
                }
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        // 2. Gece / Gündüz Modu (Switch)
        settingsBinding.switchNightMode.isChecked = appPrefs.nightMode != AppCompatDelegate.MODE_NIGHT_NO
        settingsBinding.switchNightMode.setOnCheckedChangeListener { _, isChecked ->
            val targetMode = if (isChecked) {
                AppCompatDelegate.MODE_NIGHT_YES
            } else {
                AppCompatDelegate.MODE_NIGHT_NO
            }
            appPrefs.nightMode = targetMode
            IlmToast.info(
                this,
                if (isChecked) "Gece Modu (Karanlık Tema) aktif edildi 🌙" else "Gündüz Modu (Aydınlık Tema) aktif edildi ☀️",
                title = "Görünüm Modu"
            )
        }

        // 3. GERÇEK Göz Koruma Modu (Blue Light Filter)
        val isEyeFilterOn = appPrefs.isEyeProtectionEnabled
        settingsBinding.switchEyeProtection.isChecked = isEyeFilterOn
        updateEyeProtectionUI(isEyeFilterOn)

        settingsBinding.switchEyeProtection.setOnCheckedChangeListener { _, isChecked ->
            appPrefs.isEyeProtectionEnabled = isChecked
            updateEyeProtectionUI(isChecked)
            if (isChecked) {
                IlmToast.warning(
                    this,
                    "Mavi ışık filtresi aktif. Sıcak amber tonu (#33FF9900) ile göz yorgunluğu azaltılıyor.",
                    title = "Göz Koruması Devrede 👁️"
                )
            } else {
                IlmToast.info(this, "Göz koruma filtresi devreden çıkarıldı.", title = "Standart Ekran")
            }
        }

        // 4. Sosyal Etkileşim Bildirimleri
        settingsBinding.switchSocialNotifications.isChecked = appPrefs.isSocialNotificationsEnabled
        settingsBinding.switchSocialNotifications.setOnCheckedChangeListener { _, isChecked ->
            appPrefs.isSocialNotificationsEnabled = isChecked
            IlmToast.info(
                this,
                if (isChecked) "Beğeni, yorum ve takipçi bildirimleri açık." else "Sosyal bildirimler sessize alındı."
            )
        }

        // 5. İlmi Motivasyon Bildirimleri (Duolingo Tarzı)
        settingsBinding.switchMotivationNotifications.isChecked = appPrefs.isMotivationNotificationsEnabled
        settingsBinding.switchMotivationNotifications.setOnCheckedChangeListener { _, isChecked ->
            appPrefs.isMotivationNotificationsEnabled = isChecked
            IlmToast.info(
                this,
                if (isChecked) "210+ veciz sözden esprili ilim hatırlatmaları açık 🦉" else "İlmi motivasyon bildirimleri kapatıldı."
            )
        }

        // 6. Veri Tasarrufu: PDF'leri Sadece Wi-Fi İle İndir
        settingsBinding.switchWifiOnly.isChecked = appPrefs.isWifiOnlyDownload
        settingsBinding.switchWifiOnly.setOnCheckedChangeListener { _, isChecked ->
            appPrefs.isWifiOnlyDownload = isChecked
            IlmToast.info(
                this,
                if (isChecked) "PDF indirmeleri sadece Wi-Fi üzerinden yapılacak 📶" else "Mobil veri ile de PDF indirmeye izin verildi."
            )
        }

        // 7. Önbellek Yönetimi
        updateSettingsCacheSizeDisplay()
        settingsBinding.btnSettingsClearCache.setOnClickListener {
            val clearedBytes = appPrefs.clearCache(this)
            val clearedFormatted = appPrefs.formatFileSize(clearedBytes)
            updateSettingsCacheSizeDisplay()
            IlmToast.success(
                this,
                "$clearedFormatted önbellek ve geçici PDF dosyaları temizlendi! Cihazda yer açıldı 🧹",
                title = "Önbellek Temizlendi"
            )
        }

        // 8. FAZ 7: Jenerik & IMF Easter Egg Destekli Hakkında Sayfası (Yıldırım Technologies)
        settingsBinding.cardSettingsAbout.setOnClickListener {
            val aboutDialog = AboutFragment.newInstance()
            aboutDialog.show(supportFragmentManager, "about_credits_fragment")
        }

        settingsBinding.btnSettingsLogout.setOnClickListener {
            performLogout()
        }
    }

    private fun updateSettingsCacheSizeDisplay() {
        val appPrefs = AppSettingsPreferences.getInstance(this)
        val cacheBytes = appPrefs.calculateCacheSizeBytes(this)
        binding.viewSettings.tvSettingsCacheSize.text = "${appPrefs.formatFileSize(cacheBytes)} kullanılan alan"
    }

    private fun updateEyeProtectionUI(isEnabled: Boolean) {
        val settingsBinding = binding.viewSettings
        settingsBinding.tvEyeProtectionStatus.text = if (isEnabled) "Aktif (Amber)" else "Kapalı"
        settingsBinding.tvEyeProtectionStatus.setTextColor(
            ContextCompat.getColor(this, if (isEnabled) R.color.gold_vibrant else R.color.text_secondary)
        )
        binding.viewEyeProtectionOverlay.visibility = if (isEnabled) View.VISIBLE else View.GONE
        binding.viewPdfReader.viewReaderAmberFilter.visibility = if (isEnabled) View.VISIBLE else View.GONE
    }

    private fun applyAppTheme(theme: AppTheme, showToast: Boolean = false) {
        AppSettingsPreferences.getInstance(this).currentThemeId = theme.id

        // Root ve ana ekran görünümlerine yeni tema arka planını uygula
        binding.mainRoot.setBackgroundResource(theme.backgroundDrawableRes)
        binding.viewFeed.root.setBackgroundResource(theme.backgroundDrawableRes)
        binding.viewAddPdf.root.setBackgroundResource(theme.backgroundDrawableRes)
        binding.viewDashboard.root.setBackgroundResource(theme.backgroundDrawableRes)
        binding.viewSettings.settingsRootScroll.setBackgroundResource(theme.backgroundDrawableRes)
        binding.viewPdfReader.root.setBackgroundResource(theme.backgroundDrawableRes)

        if (showToast) {
            IlmToast.success(this, "${theme.title} teması uygulandı ✨", title = "Tema Değiştirildi 🎨")
        }
    }

    private fun isWifiConnected(context: android.content.Context): Boolean {
        val cm = context.getSystemService(android.content.Context.CONNECTIVITY_SERVICE) as? android.net.ConnectivityManager ?: return false
        val activeNetwork = cm.activeNetwork ?: return false
        val capabilities = cm.getNetworkCapabilities(activeNetwork) ?: return false
        return capabilities.hasTransport(android.net.NetworkCapabilities.TRANSPORT_WIFI)
    }

    private fun openEditProfileScreen() {
        val user = authViewModel.currentUser.value
        val editBinding = binding.viewEditProfile
        if (user != null) {
            editBinding.etEditName.setText(user.fullName)
            editBinding.etEditTitle.setText(user.academicTitle.orEmpty())
            editBinding.etEditBio.setText(user.bio.orEmpty())
            selectedAvatarPath = user.avatarUrl
            displayAvatar(editBinding.ivEditAvatarPreview, user.avatarUrl)
        }
        editBinding.tvEditProfileError.visibility = View.GONE

        binding.viewDashboard.root.visibility = View.GONE
        binding.viewSettings.root.visibility = View.GONE
        binding.layoutBottomNav.root.visibility = View.GONE
        editBinding.root.visibility = View.VISIBLE
    }

    private fun closeEditProfileScreen() {
        binding.viewEditProfile.root.visibility = View.GONE
        binding.viewDashboard.root.visibility = View.VISIBLE
        binding.layoutBottomNav.root.visibility = View.VISIBLE
        feedViewModel.selectNavTab(NavTab.PROFILE)
        switchMainTab(NavTab.PROFILE)
    }

    private fun openSettingsScreen() {
        val user = authViewModel.currentUser.value
        if (user != null) {
            binding.viewSettings.tvSettingsUserName.text = user.fullName
            binding.viewSettings.tvSettingsUserEmail.text = user.email
        }
        updateSettingsCacheSizeDisplay()
        val currentTheme = ThemeManager.getActiveTheme(this)
        val themes = AppTheme.values()
        val pos = themes.indexOfFirst { it.id.equals(currentTheme.id, ignoreCase = true) }.coerceAtLeast(0)
        binding.viewSettings.spSettingsTheme.setSelection(pos, false)

        binding.viewDashboard.root.visibility = View.GONE
        binding.viewEditProfile.root.visibility = View.GONE
        binding.layoutBottomNav.root.visibility = View.GONE
        binding.viewSettings.root.visibility = View.VISIBLE
    }

    private fun closeSettingsScreen() {
        binding.viewSettings.root.visibility = View.GONE
        binding.viewDashboard.root.visibility = View.VISIBLE
        binding.layoutBottomNav.root.visibility = View.VISIBLE
        feedViewModel.selectNavTab(NavTab.PROFILE)
        switchMainTab(NavTab.PROFILE)
    }

    private fun displayAvatar(imageView: ImageView, avatarUrl: String?) {
        if (!avatarUrl.isNullOrBlank()) {
            val model: Any = if (avatarUrl.startsWith("http://") || avatarUrl.startsWith("https://") || avatarUrl.startsWith("content://") || avatarUrl.startsWith("file://")) {
                avatarUrl
            } else {
                val f = File(avatarUrl)
                if (f.exists()) f else avatarUrl
            }
            imageView.load(model) {
                crossfade(true)
                memoryCachePolicy(CachePolicy.DISABLED)
                diskCachePolicy(CachePolicy.DISABLED)
                networkCachePolicy(CachePolicy.WRITE_ONLY)
                placeholder(R.drawable.ic_person_outline)
                error(R.drawable.ic_person_outline)
                transformations(CircleCropTransformation())
            }
        } else {
            imageView.load(R.drawable.ic_person_outline) {
                memoryCachePolicy(CachePolicy.DISABLED)
                diskCachePolicy(CachePolicy.DISABLED)
                transformations(CircleCropTransformation())
            }
        }
    }

    private fun performLogout() {
        isSplashHandled = false
        authViewModel.logout()
        Toast.makeText(this, "Oturum güvenle kapatıldı.", Toast.LENGTH_SHORT).show()
        showAuthScreen()
    }

    // ==========================================
    // FAZ 2 & 3: Alt Navigasyon (Floating Bottom Nav)
    // ==========================================
    private fun setupBottomNav() {
        val navBinding = binding.layoutBottomNav

        navBinding.tabNavFeed.setOnClickListener {
            feedViewModel.selectNavTab(NavTab.FEED)
            switchMainTab(NavTab.FEED)
        }

        navBinding.tabNavAddPdf.setOnClickListener {
            feedViewModel.selectNavTab(NavTab.ADD_PDF)
            switchMainTab(NavTab.ADD_PDF)
        }

        navBinding.tabNavProfile.setOnClickListener {
            feedViewModel.selectNavTab(NavTab.PROFILE)
            switchMainTab(NavTab.PROFILE)
        }
    }

    private fun switchMainTab(tab: NavTab) {
        hideSoftKeyboard()
        val navBinding = binding.layoutBottomNav

        binding.viewFeed.root.visibility = if (tab == NavTab.FEED) View.VISIBLE else View.GONE
        binding.viewAddPdf.root.visibility = if (tab == NavTab.ADD_PDF) View.VISIBLE else View.GONE
        binding.viewDashboard.root.visibility = if (tab == NavTab.PROFILE) View.VISIBLE else View.GONE
        binding.viewPdfReader.root.visibility = View.GONE
        binding.viewEditProfile.root.visibility = View.GONE
        binding.viewSettings.root.visibility = View.GONE

        val goldColor = ContextCompat.getColor(this, R.color.gold_vibrant)
        val mutedColor = ContextCompat.getColor(this, R.color.text_tertiary)
        val darkIconColor = ContextCompat.getColor(this, R.color.btn_text_dark)

        // Alt Navigasyon: Seçili sekmede canlı parlak altın (#FFD700)
        when (tab) {
            NavTab.FEED -> {
                navBinding.ivTabFeed.setColorFilter(goldColor)
                navBinding.tvTabFeed.setTextColor(goldColor)
                navBinding.tvTabFeed.paint.isFakeBoldText = true

                navBinding.viewBgAddPdf.setBackgroundResource(R.drawable.bg_btn_gold_pill)
                navBinding.ivTabAddPdf.setColorFilter(darkIconColor)

                navBinding.ivTabProfile.setColorFilter(mutedColor)
                navBinding.tvTabProfile.setTextColor(mutedColor)
                navBinding.tvTabProfile.paint.isFakeBoldText = false
            }
            NavTab.ADD_PDF -> {
                navBinding.ivTabFeed.setColorFilter(mutedColor)
                navBinding.tvTabFeed.setTextColor(mutedColor)
                navBinding.tvTabFeed.paint.isFakeBoldText = false

                // Seçiliyken sapsarı, parlak altın (#FFD700) ve canlı ışıldayan arka plan
                navBinding.viewBgAddPdf.setBackgroundResource(R.drawable.bg_btn_gold_vibrant_pill)
                navBinding.ivTabAddPdf.setColorFilter(darkIconColor)

                navBinding.ivTabProfile.setColorFilter(mutedColor)
                navBinding.tvTabProfile.setTextColor(mutedColor)
                navBinding.tvTabProfile.paint.isFakeBoldText = false
            }
            NavTab.PROFILE -> {
                navBinding.ivTabFeed.setColorFilter(mutedColor)
                navBinding.tvTabFeed.setTextColor(mutedColor)
                navBinding.tvTabFeed.paint.isFakeBoldText = false

                navBinding.viewBgAddPdf.setBackgroundResource(R.drawable.bg_btn_gold_pill)
                navBinding.ivTabAddPdf.setColorFilter(darkIconColor)

                navBinding.ivTabProfile.setColorFilter(goldColor)
                navBinding.tvTabProfile.setTextColor(goldColor)
                navBinding.tvTabProfile.paint.isFakeBoldText = true
            }
        }
    }

    // ==========================================
    // FAZ 2 & 3: Akış (Feed), Arama & Bildirim
    // ==========================================
    private fun setupFeedInteractions() {
        val feedBinding = binding.viewFeed

        feedAdapter = FeedAdapter(
            onLikeClicked = { post ->
                feedViewModel.toggleLike(post.id)
                if (!post.isLiked) {
                    val currentUserName = authViewModel.currentUser.value?.fullName ?: "Bir araştırmacı"
                    NotificationHelper.showLikeNotification(this@MainActivity, post.title, currentUserName)
                    IlmToast.success(this@MainActivity, "${post.title} beğenildi! ❤️")
                }
            },
            onCommentClicked = { post ->
                openCommentsBottomSheet(post)
            },
            onReadPdfClicked = { post ->
                val appPrefs = AppSettingsPreferences.getInstance(this@MainActivity)
                if (appPrefs.isWifiOnlyDownload && !isWifiConnected(this@MainActivity)) {
                    IlmToast.warning(
                        this@MainActivity,
                        "Veri Tasarrufu Aktif: PDF belgeleri sadece Wi-Fi bağlantısı ile indirilebilir.",
                        title = "Wi-Fi Gerekli 📶"
                    )
                } else {
                    val currentUserName = authViewModel.currentUser.value?.fullName ?: "Bir araştırmacı"
                    val currentUserId = authViewModel.currentUser.value?.id
                    NotificationHelper.showPdfReadNotification(
                        context = this@MainActivity,
                        pdfTitle = post.title,
                        readerName = currentUserName,
                        readerId = currentUserId,
                        authorId = post.userId,
                        targetPostId = post.id
                    )
                    openInternalPdfReader(post)
                }
            },
            onAuthorClicked = { authorName, avatarUrl, authorId ->
                openVisitorProfile(authorName, avatarUrl, authorId)
            },
            onDeletePostClicked = { post ->
                confirmDeletePost(post)
            },
            currentUserId = authViewModel.currentUser.value?.id,
            currentUserName = authViewModel.currentUser.value?.fullName
        )

        feedBinding.rvFeedPosts.apply {
            adapter = feedAdapter
            itemAnimator = null
        }

        // FAZ 11 & 12: Pinterest Tarzı Keşfet & Tablet Dinamik Izgara Yönetimi (sw600dp)
        val isTablet = resources.configuration.smallestScreenWidthDp >= 600
        val tabletFeedCols = if (resources.configuration.screenWidthDp >= 900) 4 else 3

        val feedPrefs = getSharedPreferences("ilm_feed_prefs", Context.MODE_PRIVATE)
        // Tablet ekranlarda kenarlarda boşluk kalmaması için varsayılan olarak 3'lü/4'lü ızgara açılır
        var isGridMode = if (isTablet) true else feedPrefs.getBoolean("key_feed_grid_mode", false)

        fun applyFeedLayout(isGrid: Boolean) {
            isGridMode = isGrid
            feedAdapter.isGridMode = isGrid
            if (isGrid) {
                val spanCount = if (isTablet) tabletFeedCols else 2
                feedBinding.rvFeedPosts.layoutManager = StaggeredGridLayoutManager(spanCount, StaggeredGridLayoutManager.VERTICAL)
                feedBinding.btnFeedLayoutToggle.setImageResource(R.drawable.ic_view_list)
            } else {
                if (isTablet) {
                    // Geniş ekranda tek dikey sütun yerine ferah 2 veya 3 sütunlu yapı
                    feedBinding.rvFeedPosts.layoutManager = StaggeredGridLayoutManager(tabletFeedCols, StaggeredGridLayoutManager.VERTICAL)
                    feedBinding.btnFeedLayoutToggle.setImageResource(R.drawable.ic_view_list)
                } else {
                    feedBinding.rvFeedPosts.layoutManager = LinearLayoutManager(this@MainActivity)
                    feedBinding.btnFeedLayoutToggle.setImageResource(R.drawable.ic_view_grid)
                }
            }
            feedPrefs.edit().putBoolean("key_feed_grid_mode", isGrid).apply()
        }

        applyFeedLayout(isGridMode)

        feedBinding.btnFeedLayoutToggle.setOnClickListener {
            performTokHaptic()
            applyFeedLayout(!isGridMode)
        }

        feedBinding.btnEmptyAddFirstPaper.setOnClickListener {
            feedViewModel.selectNavTab(NavTab.ADD_PDF)
            switchMainTab(NavTab.ADD_PDF)
        }

        // Swipe-to-refresh
        feedBinding.swipeRefreshFeed.setColorSchemeResources(R.color.gold_vibrant, R.color.gold_start)
        feedBinding.swipeRefreshFeed.setProgressBackgroundColorSchemeResource(R.color.glass_surface_elevated)
        feedBinding.swipeRefreshFeed.setOnRefreshListener {
            feedViewModel.refreshFromGitHub()
        }

        feedBinding.btnRefreshFeed.setOnClickListener {
            feedViewModel.refreshFromGitHub()
            IlmToast.info(this, "GitHub repository ve Room DB eşitleniyor...", title = "Senkronizasyon 🔄")
        }

        // Cam Efektli Arama Çubuğu (SearchView)
        feedBinding.etFeedSearch.doAfterTextChanged { editable ->
            val query = editable?.toString().orEmpty()
            feedViewModel.setSearchQuery(query)
            feedBinding.btnClearSearch.visibility = if (query.isNotEmpty()) View.VISIBLE else View.GONE
        }

        feedBinding.btnClearSearch.setOnClickListener {
            feedBinding.etFeedSearch.text?.clear()
        }

        // Bildirim Çanı (Altın İkon) - FAZ 8: İlmi Bildirim Merkezi
        feedBinding.btnNotificationBell.setOnClickListener {
            feedBinding.viewNotificationBadge.visibility = View.GONE
            com.example.ui.notification.NotificationHistoryFragment.newInstance()
                .show(supportFragmentManager, "NotificationHistoryFragment")
        }

        // Kategori Filtreleri
        setupCategoryChips()
    }

    private fun observeNotificationsCount() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                try {
                    AppDatabase.getInstance(this@MainActivity)
                        .notificationDao()
                        .getUnreadCount()
                        .collectLatest { unreadCount ->
                            binding.viewFeed.viewNotificationBadge.visibility =
                                if (unreadCount > 0) View.VISIBLE else View.GONE
                        }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    private fun setupCategoryChips() {
        val feedBinding = binding.viewFeed
        val chips = listOf(
            feedBinding.chipCatAll to "Tüm Eserler",
            feedBinding.chipCatTefsir to "Tefsir",
            feedBinding.chipCatHadis to "Hadis",
            feedBinding.chipCatAkaidKelam to "Akaid & Kelam",
            feedBinding.chipCatFikih to "Fıkıh",
            feedBinding.chipCatSiyerTarih to "Siyer & Tarih",
            feedBinding.chipCatTasavvuf to "Tasavvuf",
            feedBinding.chipCatSarfNahiv to "Sarf & Nahiv",
            feedBinding.chipCatMantikFelsefe to "Mantık & Felsefe"
        )

        chips.forEach { (chipView, categoryName) ->
            chipView.setOnClickListener {
                feedViewModel.selectCategory(categoryName)
                updateCategoryChipsUi(chipView, chips.map { it.first })
            }
        }
    }

    private fun updateCategoryChipsUi(activeChip: TextView, allChips: List<TextView>) {
        allChips.forEach { chip ->
            if (chip == activeChip) {
                chip.setBackgroundResource(R.drawable.bg_chip_category_active)
                chip.setTextColor(ContextCompat.getColor(this, R.color.gold_vibrant))
            } else {
                chip.setBackgroundResource(R.drawable.bg_chip_category_inactive)
                chip.setTextColor(ContextCompat.getColor(this, R.color.text_secondary))
            }
        }
    }

    // ==========================================
    // FAZ 3 & 7: Cihazdan PDF Seçme, Kategori Senkronizasyonu (20 Kategori)
    // ==========================================
    private fun setupAddPdfInteractions() {
        val addBinding = binding.viewAddPdf
        addBinding.layoutAddCategoryChips.removeAllViews()

        val chipViews = mutableListOf<TextView>()
        val density = resources.displayMetrics.density
        val padH = (14 * density).toInt()
        val padV = (7 * density).toInt()
        val marginEnd = (8 * density).toInt()

        if (selectedAddPostCategory.isEmpty() && CategoryManager.CATEGORIES.isNotEmpty()) {
            selectedAddPostCategory = CategoryManager.CATEGORIES.first().name
        }

        // FAZ 7: Dropdown (Spinner) ve Hızlı Çipler Senkronizasyonu (Merkezi CategoryManager)
        val categoryDisplayNames = CategoryManager.CATEGORIES.map { "${it.icon}  ${it.name}" }
        val spinnerAdapter = android.widget.ArrayAdapter(
            this,
            android.R.layout.simple_spinner_dropdown_item,
            categoryDisplayNames
        )
        addBinding.spinnerAddCategory.adapter = spinnerAdapter

        val initialIndex = CategoryManager.CATEGORIES.indexOfFirst { it.name.equals(selectedAddPostCategory, ignoreCase = true) }
        if (initialIndex >= 0) {
            addBinding.spinnerAddCategory.setSelection(initialIndex)
        }

        var isUpdatingFromSpinner = false
        var isUpdatingFromChips = false

        addBinding.spinnerAddCategory.onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: android.widget.AdapterView<*>?, view: View?, position: Int, id: Long) {
                if (isUpdatingFromChips) return
                if (position in CategoryManager.CATEGORIES.indices) {
                    val cat = CategoryManager.CATEGORIES[position]
                    selectedAddPostCategory = cat.name
                    isUpdatingFromSpinner = true
                    chipViews.forEachIndexed { i, otherView ->
                        val active = i == position
                        otherView.setBackgroundResource(
                            if (active) R.drawable.bg_chip_category_active
                            else R.drawable.bg_chip_category_inactive
                        )
                        otherView.setTextColor(
                            ContextCompat.getColor(
                                this@MainActivity,
                                if (active) R.color.gold_vibrant else R.color.text_secondary
                            )
                        )
                        otherView.paint.isFakeBoldText = active
                    }
                    isUpdatingFromSpinner = false
                }
            }

            override fun onNothingSelected(parent: android.widget.AdapterView<*>?) {}
        }

        CategoryManager.CATEGORIES.forEachIndexed { index, cat ->
            val textView = TextView(this).apply {
                text = "${cat.icon} ${cat.name}"
                textSize = 12f
                setPadding(padH, padV, padH, padV)
                isClickable = true
                isFocusable = true

                val lp = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    if (index < CategoryManager.CATEGORIES.size - 1) {
                        this.marginEnd = marginEnd
                    }
                }
                layoutParams = lp

                val isSelected = cat.name.equals(selectedAddPostCategory, ignoreCase = true)
                if (isSelected) {
                    setBackgroundResource(R.drawable.bg_chip_category_active)
                    setTextColor(ContextCompat.getColor(this@MainActivity, R.color.gold_vibrant))
                    paint.isFakeBoldText = true
                } else {
                    setBackgroundResource(R.drawable.bg_chip_category_inactive)
                    setTextColor(ContextCompat.getColor(this@MainActivity, R.color.text_secondary))
                    paint.isFakeBoldText = false
                }

                setOnClickListener {
                    selectedAddPostCategory = cat.name
                    if (!isUpdatingFromSpinner) {
                        isUpdatingFromChips = true
                        addBinding.spinnerAddCategory.setSelection(index)
                        isUpdatingFromChips = false
                    }
                    chipViews.forEach { otherView ->
                        val active = otherView == this
                        otherView.setBackgroundResource(
                            if (active) R.drawable.bg_chip_category_active
                            else R.drawable.bg_chip_category_inactive
                        )
                        otherView.setTextColor(
                            ContextCompat.getColor(
                                this@MainActivity,
                                if (active) R.color.gold_vibrant else R.color.text_secondary
                            )
                        )
                        otherView.paint.isFakeBoldText = active
                    }
                }
            }
            chipViews.add(textView)
            addBinding.layoutAddCategoryChips.addView(textView)
        }

        // Cihazdan Yerel Dosya Seçiciyi Başlat
        addBinding.btnPickPdfFile.setOnClickListener {
            pdfPickerLauncher.launch("application/pdf")
        }

        addBinding.btnRemoveSelectedPdf.setOnClickListener {
            feedViewModel.setSelectedPdfFile(null)
        }

        addBinding.btnSubmitPost.setOnClickListener {
            val title = addBinding.etAddTitle.text?.toString().orEmpty()
            val description = addBinding.etAddDescription.text?.toString().orEmpty()
            val currentUser = authViewModel.currentUser.value

            val authorName = currentUser?.fullName ?: "Araştırmacı"
            val authorTitle = if (!currentUser?.githubUsername.isNullOrBlank()) {
                "@${currentUser?.githubUsername} • Akademik Araştırmacı"
            } else {
                "İlim Diyârı Araştırmacısı"
            }

            feedViewModel.createPost(
                title = title,
                category = selectedAddPostCategory,
                description = description,
                authorName = authorName,
                authorTitle = authorTitle,
                userId = currentUser?.id,
                coverImagePath = selectedPdfCoverPath
            )
        }
    }

    private fun confirmDeletePost(post: PostEntity) {
        AlertDialog.Builder(this)
            .setTitle("Risaleyi Sil 🗑️")
            .setMessage("'${post.title}' başlıklı akademik eserinizi silmek istediğinizden emin misiniz? Bu işlem geri alınamaz.")
            .setPositiveButton("Sil") { _, _ ->
                feedViewModel.deletePost(post.id) { success ->
                    if (success) {
                        IlmToast.info(this, "'${post.title}' başarıyla silindi.")
                    } else {
                        IlmToast.error(this, "Risale silinirken bir hata oluştu.")
                    }
                }
            }
            .setNegativeButton("Vazgeç", null)
            .show()
    }

    private fun handlePdfSelected(uri: Uri) {
        lifecycleScope.launch {
            var fileName = "secilen_belge.pdf"
            var fileSizeFormatted = "PDF Belgesi"

            try {
                contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                    val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                    if (cursor.moveToFirst()) {
                        if (nameIndex != -1) {
                            fileName = cursor.getString(nameIndex) ?: fileName
                        }
                        if (sizeIndex != -1) {
                            val sizeBytes = cursor.getLong(sizeIndex)
                            fileSizeFormatted = formatFileSize(sizeBytes)
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }

            // GÜVENLİ FİZİKSEL KOPYALAMA:
            // content:// URI'sini uygulamanın filesDir/academic_pdfs/ dizinine kopyala
            val copyResult = PdfDocumentHelper.copyPdfUriToInternalStorage(this@MainActivity, uri, fileName)
            if (copyResult.isSuccess) {
                val copiedFile = copyResult.getOrThrow()
                val permanentPath = copiedFile.absolutePath
                val actualSize = formatFileSize(copiedFile.length())

                // 1. Sayfa Kapak Önizlemesini Kopyalanan Kalıcı Dosyadan Üret
                try {
                    selectedPdfCoverPath = PdfDocumentHelper.renderPdfFirstPageThumbnail(this@MainActivity, copiedFile)
                } catch (_: Exception) {}

                feedViewModel.setSelectedPdfFile(
                    SelectedPdfFile(
                        uriString = permanentPath, // Kalıcı fiziksel dosya yolu (SecurityException engellendi)
                        fileName = fileName,
                        fileSizeFormatted = actualSize
                    )
                )
                IlmToast.success(this@MainActivity, "PDF güvenle hazırlandı: $fileName", title = "Belge Hazır 📄")
            } else {
                Toast.makeText(
                    this@MainActivity,
                    "PDF kopyalanamadı: ${copyResult.exceptionOrNull()?.localizedMessage}",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    private fun formatFileSize(bytes: Long): String {
        if (bytes <= 0) return "PDF Belgesi"
        val kb = bytes / 1024.0
        val mb = kb / 1024.0
        return if (mb >= 1.0) {
            String.format(Locale.getDefault(), "%.1f MB", mb)
        } else {
            String.format(Locale.getDefault(), "%.0f KB", kb)
        }
    }

    // ==========================================
    // FAZ 4: Gelişmiş İnteraktif Okuyucu & Arama Motoru
    // ==========================================
    private fun setupPdfReaderInteractions() {
        val readerBinding = binding.viewPdfReader

        readerBinding.btnReaderBack.setOnClickListener {
            closeInternalPdfReader()
        }

        readerBinding.btnReaderModeEye.setOnClickListener {
            setReaderToolMode(isPenMode = false)
        }

        readerBinding.btnReaderModePen.setOnClickListener {
            setReaderToolMode(isPenMode = true)
        }

        readerBinding.btnReaderClearDraw.setOnClickListener {
            currentPdfAdapter?.clearAllDrawings()
            IlmToast.info(this, "Fosforlu vurgular temizlendi.")
        }

        // FAZ 11: İtikâf Modu (Hardcore Focus Mode) Butonları
        readerBinding.btnReaderModeItikaf.setOnClickListener {
            toggleItikafMode()
        }

        readerBinding.btnItikafExit.setOnClickListener {
            exitItikafMode(userExitedManually = true)
        }

        // Çok Renkli Kalem Seçimi (Sarı, Mavi, Yeşil, Kırmızı)
        readerBinding.btnColorYellow.setOnClickListener {
            selectHighlighterColor(HighlighterDrawingView.COLOR_YELLOW, "Sarı")
        }
        readerBinding.btnColorBlue.setOnClickListener {
            selectHighlighterColor(HighlighterDrawingView.COLOR_BLUE, "Mavi")
        }
        readerBinding.btnColorGreen.setOnClickListener {
            selectHighlighterColor(HighlighterDrawingView.COLOR_GREEN, "Yeşil")
        }
        readerBinding.btnColorRed.setOnClickListener {
            selectHighlighterColor(HighlighterDrawingView.COLOR_RED, "Kırmızı")
        }

        // Belge İçi Metin Arama Motoru (PdfSearchEngine)
        searchResultAdapter = PdfSearchResultAdapter { result ->
            val pageIndex = (result.pageNumber - 1).coerceAtLeast(0)
            readerBinding.rvPdfPages.smoothScrollToPosition(pageIndex)
            Toast.makeText(this, "Sayfa ${result.pageNumber} konumuna gidildi.", Toast.LENGTH_SHORT).show()
        }
        readerBinding.rvSearchResults.apply {
            layoutManager = LinearLayoutManager(this@MainActivity)
            adapter = searchResultAdapter
            itemAnimator = null
        }

        readerBinding.btnReaderSearch.setOnClickListener {
            val isPanelOpen = readerBinding.layoutReaderSearchPanel.visibility == View.VISIBLE
            if (isPanelOpen) {
                readerBinding.layoutReaderSearchPanel.visibility = View.GONE
            } else {
                readerBinding.layoutReaderSearchPanel.visibility = View.VISIBLE
                readerBinding.etReaderSearch.requestFocus()
            }
        }

        readerBinding.btnReaderCloseSearch.setOnClickListener {
            readerBinding.layoutReaderSearchPanel.visibility = View.GONE
            readerBinding.tvSearchSummary.visibility = View.GONE
            searchResultAdapter.submitResults(emptyList())
        }

        readerBinding.btnReaderSearchExecute.setOnClickListener {
            executePdfTextSearch()
        }

        readerBinding.etReaderSearch.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == android.view.inputmethod.EditorInfo.IME_ACTION_SEARCH) {
                executePdfTextSearch()
                true
            } else {
                false
            }
        }
    }

    private fun selectHighlighterColor(color: Int, colorName: String) {
        currentPdfAdapter?.activeColor = color
        val readerBinding = binding.viewPdfReader
        readerBinding.tvActiveColorName.text = colorName

        val selFrame = R.drawable.bg_color_dot_selected_frame
        val unselFrame = R.drawable.bg_color_dot_unselected_frame

        readerBinding.btnColorYellow.setBackgroundResource(
            if (color == HighlighterDrawingView.COLOR_YELLOW) selFrame else unselFrame
        )
        readerBinding.btnColorBlue.setBackgroundResource(
            if (color == HighlighterDrawingView.COLOR_BLUE) selFrame else unselFrame
        )
        readerBinding.btnColorGreen.setBackgroundResource(
            if (color == HighlighterDrawingView.COLOR_GREEN) selFrame else unselFrame
        )
        readerBinding.btnColorRed.setBackgroundResource(
            if (color == HighlighterDrawingView.COLOR_RED) selFrame else unselFrame
        )
    }

    private fun executePdfTextSearch() {
        val readerBinding = binding.viewPdfReader
        val query = readerBinding.etReaderSearch.text?.toString().orEmpty().trim()
        if (query.length < 2) {
            Toast.makeText(this, "Arama için en az 2 harf giriniz.", Toast.LENGTH_SHORT).show()
            return
        }
        val file = currentActivePdfFile
        if (file == null || !file.exists()) {
            Toast.makeText(this, "Arama yapılacak PDF belgesi henüz hazır değil.", Toast.LENGTH_SHORT).show()
            return
        }

        readerBinding.pbSearchLoading.visibility = View.VISIBLE
        readerBinding.tvSearchSummary.visibility = View.GONE

        lifecycleScope.launch {
            val results = PdfSearchEngine.searchInPdf(file, query)
            readerBinding.pbSearchLoading.visibility = View.GONE
            readerBinding.tvSearchSummary.visibility = View.VISIBLE

            if (results.isEmpty()) {
                readerBinding.tvSearchSummary.text = "Belgede \"$query\" kelimesine rastlanmadı."
            } else {
                val totalCount = results.sumOf { it.matchCount }
                readerBinding.tvSearchSummary.text = "${results.size} sayfada toplam $totalCount eşleşme bulundu:"
            }
            searchResultAdapter.submitResults(results)
        }
    }

    private fun openInternalPdfReader(post: PostEntity) {
        isPdfReaderOpen = true
        // Ana ekranları ve alt menüyü gizle, tam ekran PDF okuyucuyu göster
        binding.viewFeed.root.visibility = View.GONE
        binding.viewAddPdf.root.visibility = View.GONE
        binding.viewDashboard.root.visibility = View.GONE
        binding.layoutBottomNav.root.visibility = View.GONE

        val readerBinding = binding.viewPdfReader
        readerBinding.root.visibility = View.VISIBLE
        val enterAnim = AnimationUtils.loadAnimation(this, R.anim.pdf_reader_enter)
        readerBinding.root.startAnimation(enterAnim)

        readerBinding.tvReaderDocTitle.text = post.title
        readerBinding.tvReaderDocAuthor.text = "${post.authorName} • ${post.category}"
        readerBinding.layoutReaderLoading.visibility = View.VISIBLE
        readerBinding.rvPdfPages.visibility = View.GONE

        // Arama panelini sıfırla
        readerBinding.layoutReaderSearchPanel.visibility = View.GONE
        readerBinding.tvSearchSummary.visibility = View.GONE
        searchResultAdapter.submitResults(emptyList())

        setReaderToolMode(isPenMode = false)

        val isEyeProtectionOn = AppSettingsPreferences.getInstance(this).isEyeProtectionEnabled
        readerBinding.viewReaderAmberFilter.visibility = if (isEyeProtectionOn) View.VISIBLE else View.GONE

        lifecycleScope.launch {
            try {
                val pdfFile = PdfDocumentHelper.preparePdfFile(this@MainActivity, post)
                currentActivePdfFile = pdfFile
                currentPdfAdapter?.close()
                val adapter = PdfPageAdapter(
                    pdfFile = pdfFile,
                    postId = post.id,
                    onAnnotationSaved = { pageIndex, filePath ->
                        lifecycleScope.launch(Dispatchers.IO) {
                            val db = AppDatabase.getInstance(this@MainActivity)
                            db.postDao().updateAnnotationImagePath(post.id, filePath)
                            db.annotationDao().insertAnnotation(
                                com.example.data.local.entity.AnnotationEntity(
                                    postId = post.id,
                                    pageIndex = pageIndex,
                                    imagePath = filePath
                                )
                            )
                        }
                    }
                )
                currentPdfAdapter = adapter

                readerBinding.rvPdfPages.apply {
                    layoutManager = LinearLayoutManager(this@MainActivity)
                    this.adapter = adapter
                }

                // FAZ 11: Smart Resume (Kaldığın Yerden Devam Et)
                if (post.lastReadPage > 0) {
                    readerBinding.rvPdfPages.scrollToPosition(post.lastReadPage)
                    IlmToast.info(
                        this@MainActivity,
                        "Kaldığınız yerden devam ediliyor: Sayfa ${post.lastReadPage + 1}",
                        title = "Smart Resume 📖"
                    )
                }

                readerBinding.rvPdfPages.clearOnScrollListeners()
                readerBinding.rvPdfPages.addOnScrollListener(object : RecyclerView.OnScrollListener() {
                    override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                        super.onScrollStateChanged(recyclerView, newState)
                        if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                            val lm = recyclerView.layoutManager as? LinearLayoutManager
                            val firstVisible = lm?.findFirstVisibleItemPosition() ?: 0
                            if (firstVisible >= 0) {
                                lifecycleScope.launch(Dispatchers.IO) {
                                    AppDatabase.getInstance(this@MainActivity).postDao().updateLastReadPage(post.id, firstVisible)
                                }
                                if (isItikafModeActive) {
                                    if (firstVisible > itikafMaxPageReached) {
                                        itikafMaxPageReached = firstVisible
                                        val readSoFar = (itikafMaxPageReached - itikafStartPage).coerceAtLeast(0)
                                        readerBinding.tvItikafStatus.text = "İtikâf Modu Devrede • Okunan: $readSoFar sayfa (Hedef: 10)"
                                    }
                                }
                            }
                        }
                    }
                })

                // Varsayılan fosforlu renk: Sarı (#FFD700)
                selectHighlighterColor(HighlighterDrawingView.COLOR_YELLOW, "Sarı")

                readerBinding.layoutReaderLoading.visibility = View.GONE
                readerBinding.rvPdfPages.visibility = View.VISIBLE
                val docAnim = AnimationUtils.loadAnimation(this@MainActivity, R.anim.doc_switch_in)
                readerBinding.rvPdfPages.startAnimation(docAnim)
            } catch (e: Exception) {
                readerBinding.layoutReaderLoading.visibility = View.GONE
                IlmToast.error(this@MainActivity, "PDF açılamadı: ${e.localizedMessage}")
            }
        }
    }

    private fun setReaderToolMode(isPenMode: Boolean) {
        val readerBinding = binding.viewPdfReader
        currentPdfAdapter?.isDrawingMode = isPenMode
        if (isPenMode) {
            readerBinding.btnReaderModePen.setBackgroundResource(R.drawable.bg_tab_active)
            readerBinding.btnReaderModeEye.setBackgroundResource(R.drawable.bg_tab_inactive)
            readerBinding.layoutColorPalette.visibility = View.VISIBLE
            readerBinding.tvReaderModeHint.text = "✍️ Kalem Modu: Dokunarak fosforlu vurgulayın, iki parmakla yakınlaştırın."
        } else {
            readerBinding.btnReaderModeEye.setBackgroundResource(R.drawable.bg_tab_active)
            readerBinding.btnReaderModePen.setBackgroundResource(R.drawable.bg_tab_inactive)
            readerBinding.layoutColorPalette.visibility = View.GONE
            readerBinding.tvReaderModeHint.text = "👀 Okuma Modu: Sayfaları dikey kaydırın, iki parmakla yakınlaştırın."
        }
    }

    fun closeInternalPdfReaderPublic() {
        closeInternalPdfReader()
    }

    private fun closeInternalPdfReader() {
        if (!isPdfReaderOpen && binding.viewPdfReader.root.visibility != View.VISIBLE) return
        isPdfReaderOpen = false

        if (isItikafModeActive) {
            exitItikafMode(userExitedManually = true)
        }
        val readerBinding = binding.viewPdfReader
        readerBinding.root.clearAnimation()

        // 1. Önce RecyclerView adapter'ını ayır (View-Bitmap ilişiğini güvenle kes)
        readerBinding.rvPdfPages.adapter = null

        // 2. Okuyucu arayüzünü anında gizle
        readerBinding.root.visibility = View.GONE
        readerBinding.layoutReaderSearchPanel.visibility = View.GONE
        readerBinding.etReaderSearch.text = null
        readerBinding.tvSearchSummary.visibility = View.GONE
        searchResultAdapter.submitResults(emptyList())

        // 3. Adapter ve PdfRenderer kaynaklarını güvenle serbest bırak
        try {
            currentPdfAdapter?.close()
        } catch (e: Exception) {
            Log.e("MainActivity", "PDF adapter kapatma hatası: ${e.message}")
        }
        currentPdfAdapter = null
        currentActivePdfFile = null

        // 4. Ana akış ekranını geri getir
        showMainAppView()
    }

    // ==========================================
    // FAZ 5: Ziyaretçi Profili ve Yorum Bottom Sheet
    // ==========================================
    private fun openVisitorProfile(authorName: String, avatarUrl: String?, authorId: String? = null) {
        binding.fragmentContainer.visibility = View.VISIBLE
        binding.layoutBottomNav.root.visibility = View.GONE
        supportFragmentManager.beginTransaction()
            .setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out, android.R.anim.fade_in, android.R.anim.fade_out)
            .replace(
                R.id.fragment_container,
                VisitorProfileFragment.newInstance(authorName, avatarUrl, authorId)
            )
            .addToBackStack("visitor_profile")
            .commit()
    }

    private fun openCommentsBottomSheet(post: PostEntity) {
        val dialog = CommentsBottomSheetDialogFragment.newInstance(
            postId = post.id,
            postTitle = post.title,
            postAuthor = post.authorName,
            issueNumber = post.githubIssueId ?: 0L,
            postAuthorId = post.userId
        )
        dialog.onCommentAddedListener = {
            feedViewModel.refreshFromGitHub()
        }
        dialog.show(supportFragmentManager, "comments_dialog")
    }

    // ==========================================
    // State ve Navigation Akış Gözlemcileri
    // ==========================================
    private fun observeNavigation() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                splashViewModel.navigationState.collect { state ->
                    when (state) {
                        SplashNavigationState.NavigateToDashboard -> {
                            if (!isSplashHandled) {
                                isSplashHandled = true
                                showMainAppView()
                            }
                        }
                        SplashNavigationState.NavigateToAuth -> {
                            if (!isSplashHandled) {
                                isSplashHandled = true
                                showAuthScreen()
                            }
                        }
                        SplashNavigationState.Idle -> { /* Splash bekletiliyor */ }
                    }
                }
            }
        }

        // FAZ 7: Versiyon Kontrolü (GitHub Release Güncelleme Bildirimi)
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                splashViewModel.updateInfo.collect { info ->
                    if (info != null && info.hasUpdate) {
                        showAppUpdateDialog(info)
                    }
                }
            }
        }
    }

    private fun showAppUpdateDialog(info: com.example.ui.splash.VersionUpdateInfo) {
        com.google.android.material.dialog.MaterialAlertDialogBuilder(this)
            .setTitle("✨ Yeni İlim Diyârı Sürümü Mevcut! (${info.latestVersion})")
            .setMessage("Cihazınızdaki sürüm: ${info.currentVersion}\nEn son sürüm: ${info.latestVersion}\n\n${info.releaseNotes}\n\nİlim Diyârı'nın en güncel ilmi veri tabanına, akademik rozet yeniliklerine ve performans güncellemelerine erişmek için şimdi güncelleyin.")
            .setPositiveButton("Şimdi Güncelle") { _, _ ->
                try {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(info.releaseUrl))
                    startActivity(intent)
                } catch (e: Exception) {
                    IlmToast.info(this, info.releaseUrl, title = "İndirme Adresi")
                }
            }
            .setNegativeButton("Daha Sonra", null)
            .show()
    }

    private fun observeAuthState() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                authViewModel.uiState.collect { state ->
                    val authBinding = binding.viewAuth
                    when (state) {
                        is AuthUiState.Idle -> {
                            authBinding.pbAuthLoading.visibility = View.GONE
                            authBinding.btnAuthSubmit.isEnabled = true
                            authBinding.tvAuthError.visibility = View.GONE
                        }
                        is AuthUiState.Loading -> {
                            authBinding.pbAuthLoading.visibility = View.VISIBLE
                            authBinding.btnAuthSubmit.isEnabled = false
                            authBinding.tvAuthError.visibility = View.GONE
                        }
                        is AuthUiState.Success -> {
                            authBinding.pbAuthLoading.visibility = View.GONE
                            authBinding.btnAuthSubmit.isEnabled = true
                            authBinding.tvAuthError.visibility = View.GONE
                            Toast.makeText(this@MainActivity, "Hoş geldiniz, ${state.user.fullName}", Toast.LENGTH_SHORT).show()
                            showMainAppView()
                        }
                        is AuthUiState.Error -> {
                            authBinding.pbAuthLoading.visibility = View.GONE
                            authBinding.btnAuthSubmit.isEnabled = true
                            authBinding.tvAuthError.text = state.message
                            authBinding.tvAuthError.visibility = View.VISIBLE
                        }
                    }
                }
            }
        }
    }

    private fun observeCurrentUser() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                authViewModel.currentUser.collect { user ->
                    if (user != null) {
                        feedAdapter.currentUserId = user.id
                        feedAdapter.currentUserName = user.fullName
                        feedAdapter.notifyDataSetChanged()
                        profileSharedWorksAdapter.currentUserId = user.id
                        profileSharedWorksAdapter.currentUserName = user.fullName
                        profileSharedWorksAdapter.notifyDataSetChanged()

                        // Kullanıcı değiştiğinde veya giriş yaptığında akışı güncelle
                        feedViewModel.refreshFromGitHub()

                        val dashBinding = binding.viewDashboard
                        dashBinding.tvDashUserName.text = user.fullName
                        dashBinding.tvDashUserEmail.text = user.email
                        dashBinding.tvDashUserTitle.text = user.academicTitle ?: "Araştırmacı • İlim Diyârı Portali"
                        if (!user.bio.isNullOrBlank()) {
                            dashBinding.tvDashUserBio.text = user.bio
                            dashBinding.tvDashUserBio.visibility = View.VISIBLE
                        } else {
                            dashBinding.tvDashUserBio.visibility = View.GONE
                        }
                        displayAvatar(dashBinding.ivDashAvatar, user.avatarUrl)
                        displayAvatar(binding.viewFeed.ivFeedToolbarAvatar, user.avatarUrl)

                        binding.viewFeed.ivFeedToolbarAvatar.setOnClickListener {
                            feedViewModel.selectNavTab(NavTab.PROFILE)
                            switchMainTab(NavTab.PROFILE)
                        }

                        val settingsBinding = binding.viewSettings
                        settingsBinding.tvSettingsUserName.text = user.fullName
                        settingsBinding.tvSettingsUserEmail.text = user.email
                    }
                }
            }
        }
    }

    private fun observeEditProfileState() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                authViewModel.editProfileState.collect { state ->
                    val editBinding = binding.viewEditProfile
                    when (state) {
                        is EditProfileUiState.Idle -> {
                            editBinding.pbEditProfileLoading.visibility = View.GONE
                            editBinding.btnEditProfileSave.isEnabled = true
                            editBinding.tvEditProfileError.visibility = View.GONE
                        }
                        is EditProfileUiState.Loading -> {
                            editBinding.pbEditProfileLoading.visibility = View.VISIBLE
                            editBinding.btnEditProfileSave.isEnabled = false
                            editBinding.tvEditProfileError.visibility = View.GONE
                        }
                        is EditProfileUiState.Success -> {
                            editBinding.pbEditProfileLoading.visibility = View.GONE
                            editBinding.btnEditProfileSave.isEnabled = true
                            editBinding.tvEditProfileError.visibility = View.GONE

                            // Altın Vurgulu Bildirim (Snackbar)
                            val snackbar = Snackbar.make(binding.root, "Profil başarıyla güncellendi ✨", Snackbar.LENGTH_LONG)
                            snackbar.setBackgroundTint(ContextCompat.getColor(this@MainActivity, R.color.splash_bg_start))
                            snackbar.setTextColor(ContextCompat.getColor(this@MainActivity, R.color.text_primary))
                            snackbar.setActionTextColor(ContextCompat.getColor(this@MainActivity, R.color.gold_vibrant))
                            snackbar.setAction("Tamam") { snackbar.dismiss() }
                            snackbar.show()

                            // Profil ekranındaki ve Dashboard'daki avatarı direkt güncelle
                            displayAvatar(binding.viewDashboard.ivDashAvatar, state.user.avatarUrl)
                            binding.viewDashboard.tvDashUserName.text = state.user.fullName

                            authViewModel.resetEditProfileState()
                            closeEditProfileScreen()
                        }
                        is EditProfileUiState.Error -> {
                            editBinding.pbEditProfileLoading.visibility = View.GONE
                            editBinding.btnEditProfileSave.isEnabled = true
                            editBinding.tvEditProfileError.text = state.message
                            editBinding.tvEditProfileError.visibility = View.VISIBLE
                        }
                    }
                }
            }
        }
    }

    private fun observeFeedState() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    feedViewModel.posts.collect { postList ->
                        binding.viewFeed.shimmerFeed.root.stopShimmer()
                        binding.viewFeed.shimmerFeed.root.visibility = View.GONE
                        feedAdapter.submitList(postList)
                        binding.viewFeed.rvFeedPosts.visibility = if (postList.isEmpty()) View.GONE else View.VISIBLE
                        binding.viewFeed.layoutEmptyFeed.visibility = if (postList.isEmpty()) View.VISIBLE else View.GONE
                    }
                }

                launch {
                    feedViewModel.isRefreshing.collect { isRefreshing ->
                        binding.viewFeed.swipeRefreshFeed.isRefreshing = isRefreshing
                        if (isRefreshing && feedAdapter.itemCount == 0) {
                            binding.viewFeed.shimmerFeed.root.visibility = View.VISIBLE
                            binding.viewFeed.shimmerFeed.root.startShimmer()
                            binding.viewFeed.rvFeedPosts.visibility = View.GONE
                            binding.viewFeed.layoutEmptyFeed.visibility = View.GONE
                        }
                    }
                }

                // Seçilen PDF Dosyasını Add Screen üzerinde göster
                launch {
                    feedViewModel.selectedPdfFile.collect { file ->
                        val addBinding = binding.viewAddPdf
                        if (file != null) {
                            addBinding.cardSelectedPdfInfo.visibility = View.VISIBLE
                            addBinding.tvSelectedPdfName.text = file.fileName
                            addBinding.tvSelectedPdfSize.text = "Seçildi • ${file.fileSizeFormatted}"
                        } else {
                            addBinding.cardSelectedPdfInfo.visibility = View.GONE
                        }
                    }
                }

                launch {
                    feedViewModel.addPostState.collect { state ->
                        val addBinding = binding.viewAddPdf
                        when (state) {
                            is AddPostUiState.Idle -> {
                                addBinding.tvAddError.visibility = View.GONE
                                addBinding.btnSubmitPost.isEnabled = true
                            }
                            is AddPostUiState.Loading -> {
                                addBinding.tvAddError.visibility = View.GONE
                                addBinding.btnSubmitPost.isEnabled = false
                            }
                            is AddPostUiState.Success -> {
                                addBinding.btnSubmitPost.isEnabled = true
                                addBinding.tvAddError.visibility = View.GONE
                                addBinding.etAddTitle.text = null
                                addBinding.etAddDescription.text = null
                                selectedPdfCoverPath = null
                                IlmToast.success(this@MainActivity, "Akademik PDF yayına alındı! Tebrikler ✨", title = "Yeni Risale Neşredildi 📜")
                                binding.viewConfetti.startCelebration()
                                feedViewModel.resetAddPostState()
                                switchMainTab(NavTab.FEED)
                            }
                            is AddPostUiState.Error -> {
                                addBinding.btnSubmitPost.isEnabled = true
                                addBinding.tvAddError.text = state.message
                                addBinding.tvAddError.visibility = View.VISIBLE
                            }
                        }
                    }
                }

                // FAZ 4: Profil Sekmeleri (Paylaşılan Eserler vs Rozetler)
                launch {
                    feedViewModel.selectedProfileTab.collect { profileTab ->
                        val dashBinding = binding.viewDashboard
                        when (profileTab) {
                            ProfileTab.SHARED_WORKS -> {
                                dashBinding.btnTabSharedWorks.setBackgroundResource(R.drawable.bg_tab_active)
                                dashBinding.tvTabSharedWorksLabel.setTextColor(ContextCompat.getColor(this@MainActivity, R.color.gold_start))
                                dashBinding.btnTabBadges.setBackgroundResource(R.drawable.bg_tab_inactive)
                                dashBinding.tvTabBadgesLabel.setTextColor(ContextCompat.getColor(this@MainActivity, R.color.text_secondary))
                                dashBinding.layoutTabSharedWorks.visibility = View.VISIBLE
                                dashBinding.layoutTabBadges.visibility = View.GONE
                            }
                            ProfileTab.BADGES -> {
                                dashBinding.btnTabSharedWorks.setBackgroundResource(R.drawable.bg_tab_inactive)
                                dashBinding.tvTabSharedWorksLabel.setTextColor(ContextCompat.getColor(this@MainActivity, R.color.text_secondary))
                                dashBinding.btnTabBadges.setBackgroundResource(R.drawable.bg_tab_active)
                                dashBinding.tvTabBadgesLabel.setTextColor(ContextCompat.getColor(this@MainActivity, R.color.gold_start))
                                dashBinding.layoutTabSharedWorks.visibility = View.GONE
                                dashBinding.layoutTabBadges.visibility = View.VISIBLE
                            }
                        }
                    }
                }

                // FAZ 4: Profil Paylaşılan Eserler Listesi (Kullanıcının Kendi Eserleri)
                launch {
                    feedViewModel.allPosts.collect { allPosts ->
                        val currentUserId = authViewModel.currentUser.value?.id
                        val currentUserName = authViewModel.currentUser.value?.fullName
                        val myWorks = if (currentUserId != null) {
                            allPosts.filter { it.userId == currentUserId || (currentUserName != null && it.authorName.equals(currentUserName, ignoreCase = true)) }
                        } else {
                            allPosts
                        }
                        profileSharedWorksAdapter.submitList(myWorks)
                        val dashBinding = binding.viewDashboard
                        dashBinding.tvDashStatsWorksCount.text = "${myWorks.size}"
                        dashBinding.tvProfileNoWorks.visibility = if (myWorks.isEmpty()) View.VISIBLE else View.GONE
                    }
                }

                // FAZ 4 & 9: 20 Kategori Akademik Rozet Matrisi ve Dış Sistem Bildirimleri
                launch {
                    feedViewModel.badges.collect { badgeList ->
                        academicBadgeAdapter.submitList(badgeList)
                        val dashBinding = binding.viewDashboard
                        val unlockedCount = badgeList.count { it.isUnlocked }
                        dashBinding.tvDashStatsBadgesCount.text = "$unlockedCount/20"

                        val maxLevel = badgeList.maxOfOrNull { it.level } ?: 0
                        dashBinding.tvDashStatsLevel.text = when {
                            maxLevel >= 20 -> "💎 Seviye 20 • Allâme (Zirve)"
                            maxLevel >= 16 -> "💎 Elmas (Seviye $maxLevel)"
                            maxLevel >= 11 -> "🥇 Altın (Seviye $maxLevel)"
                            maxLevel >= 6 -> "🥈 Gümüş (Seviye $maxLevel)"
                            maxLevel >= 1 -> "🥉 Bakır (Seviye $maxLevel)"
                            else -> "🔒 Mübtedî"
                        }

                        // Rozet seviye artışı / yeni rozet kazanımı durumunda OS Push Bildirimi gönder (SharedPreferences Çözümü)
                        // Room DB gecikmeleri veya her girişte bildirim spamını önlemek için SessionManager kullanılır:
                        // Yalnızca mevcut seviye (level) > last_notified_badge_level ise tetiklenir ve yeni seviye kaydedilir.
                        val sessionMgr = SessionManager(this@MainActivity)
                        for (badge in badgeList) {
                            val lastNotifiedLevel = sessionMgr.getLastNotifiedBadgeLevel(badge.category)
                            if (badge.isUnlocked && badge.level > lastNotifiedLevel) {
                                NotificationHelper.showBadgeUnlockedNotification(
                                    context = this@MainActivity,
                                    categoryName = badge.category,
                                    rankTitle = badge.rankTitle,
                                    level = badge.level,
                                    icon = badge.icon
                                )
                                sessionMgr.saveLastNotifiedBadgeLevel(badge.category, badge.level)
                                if (badge.level > sessionMgr.getLastNotifiedBadgeLevel()) {
                                    sessionMgr.saveLastNotifiedBadgeLevel(badge.level)
                                }
                            }
                        }
                        previousBadgeLevels = badgeList.associate { it.category to it.level }
                    }
                }
            }
        }
    }

    private fun showMainAppView() {
        hideSoftKeyboard()
        binding.viewSplash.root.visibility = View.GONE
        binding.viewAuth.root.visibility = View.GONE
        binding.viewPdfReader.root.visibility = View.GONE
        binding.viewEditProfile.root.visibility = View.GONE
        binding.viewSettings.root.visibility = View.GONE
        binding.layoutBottomNav.root.visibility = View.VISIBLE
        switchMainTab(feedViewModel.selectedTab.value)
    }

    private fun showAuthScreen() {
        hideSoftKeyboard()
        binding.viewSplash.root.visibility = View.GONE
        binding.viewFeed.root.visibility = View.GONE
        binding.viewAddPdf.root.visibility = View.GONE
        binding.viewDashboard.root.visibility = View.GONE
        binding.viewPdfReader.root.visibility = View.GONE
        binding.viewEditProfile.root.visibility = View.GONE
        binding.viewSettings.root.visibility = View.GONE
        binding.layoutBottomNav.root.visibility = View.GONE
        binding.viewAuth.root.visibility = View.VISIBLE
        updateTabUi(authViewModel.authMode.value)
    }

    // ==========================================
    // FAZ 11: İtikâf Modu (Hardcore Focus Mode) Yönetimi
    // ==========================================
    private fun toggleItikafMode() {
        if (!isItikafModeActive) {
            startItikafMode()
        } else {
            exitItikafMode(userExitedManually = true)
        }
    }

    private fun startItikafMode() {
        isItikafModeActive = true
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        val insetsController = WindowCompat.getInsetsController(window, window.decorView)
        insetsController.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        insetsController.hide(WindowInsetsCompat.Type.systemBars())

        val audio = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        val currentVol = audio.getStreamVolume(AudioManager.STREAM_MUSIC)
        if (currentVol > 0) {
            IlmToast.info(this, "Odaklanma devrede: Ortamı sessize almanız tavsiye edilir 🎧", title = "İtikâf Uyarısı")
        }

        val readerBinding = binding.viewPdfReader
        readerBinding.layoutItikafBanner.visibility = View.VISIBLE
        readerBinding.btnReaderModeItikaf.setBackgroundResource(R.drawable.bg_tab_active)
        val lm = readerBinding.rvPdfPages.layoutManager as? LinearLayoutManager
        itikafStartPage = lm?.findFirstVisibleItemPosition() ?: 0
        itikafMaxPageReached = itikafStartPage
        itikafFailed = false
        readerBinding.tvItikafStatus.text = "İtikâf Modu Devrede • Derin Mütalaa (Ayrılmayın!)"
        IlmToast.info(this, "İtikâf Modu Aktif! Ekran açık tutuluyor, ayrılmadan mütalaa ediniz.", title = "İtikâf Modu 🕊️")
    }

    private fun exitItikafMode(userExitedManually: Boolean) {
        window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        WindowCompat.getInsetsController(window, window.decorView).show(WindowInsetsCompat.Type.systemBars())

        val readerBinding = binding.viewPdfReader
        readerBinding.layoutItikafBanner.visibility = View.GONE
        readerBinding.btnReaderModeItikaf.setBackgroundResource(R.drawable.bg_tab_inactive)

        if (userExitedManually && !itikafFailed) {
            val pagesRead = (itikafMaxPageReached - itikafStartPage).coerceAtLeast(0)
            if (pagesRead >= 10) {
                lifecycleScope.launch(Dispatchers.IO) {
                    val user = authRepository.getCurrentUserFlow().firstOrNull()
                    if (user != null) {
                        val db = AppDatabase.getInstance(this@MainActivity)
                        val itikafBadge = com.example.data.local.entity.BadgeEntity(
                            id = "${user.id}_itikaf",
                            userId = user.id,
                            category = "İtikâf",
                            level = 1,
                            rankTitle = "İtikâf İradesi",
                            isUnlocked = true,
                            isNotified = true,
                            updatedAt = System.currentTimeMillis()
                        )
                        db.badgeDao().insertOrUpdateBadge(itikafBadge)
                        withContext(Dispatchers.Main) {
                            IlmToast.success(this@MainActivity, "✨ Tebrikler! 10+ sayfa kesintisiz mütalaa ile 'İtikâf İradesi' rozetini kazandınız!", title = "Özel Rozet 🎖️")
                        }
                    }
                }
            } else {
                IlmToast.info(this, "İtikâf mütalaası tamamlandı. Okunan sayfa: $pagesRead", title = "Mütalaa Notu")
            }
        }
        isItikafModeActive = false
    }

    override fun onPause() {
        super.onPause()
        if (isItikafModeActive) {
            itikafFailed = true
            exitItikafMode(userExitedManually = false)
        }
    }

    override fun onBackPressed() {
        if (isPdfReaderOpen || binding.viewPdfReader.root.visibility == View.VISIBLE) {
            closeInternalPdfReader()
            return
        }
        super.onBackPressed()
    }

    override fun onKeyDown(keyCode: Int, event: android.view.KeyEvent?): Boolean {
        if (keyCode == android.view.KeyEvent.KEYCODE_BACK && (isPdfReaderOpen || binding.viewPdfReader.root.visibility == View.VISIBLE)) {
            closeInternalPdfReader()
            return true
        }
        return super.onKeyDown(keyCode, event)
    }
}

/**
 * Destekleyici Composable (GreetingScreenshotTest için korunmuştur).
 */
@androidx.compose.runtime.Composable
fun Greeting(name: String, modifier: androidx.compose.ui.Modifier = androidx.compose.ui.Modifier) {
    androidx.compose.material3.Text(text = "Hello $name!", modifier = modifier)
}
