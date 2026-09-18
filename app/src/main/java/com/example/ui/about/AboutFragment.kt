package com.example.ui.about

import android.content.Context
import android.content.Intent
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AccelerateDecelerateInterpolator
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.lifecycleScope
import com.example.R
import com.example.databinding.FragmentAboutBinding
import com.example.ui.common.IlmToast
import com.example.util.performTokHaptic
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * İlmNet - Oyun Jeneriği (Credits) & Sinematik Gizemler (FAZ 12).
 * - Siyah zemin üzerinde altın sarısı tipografiyle Muhammed Bahadır Yıldırım ve
 *   Yıldırım Technologies kadrosunu listeler.
 * - Logoya 10 kez tıklandığında ekran kararır, epik R.raw.epic_mystery_voice sesi yankılanır
 *   ve 'Gerçeği arayan yolcu... İlim Diyârı'nın kalbine ulaştın.' altın yazısı belirir.
 * - 'Sürüm Günlükleri' butonu ile destansı tarihçe (Faz 1-12) açılır.
 */
class AboutFragment : DialogFragment() {

    private var _binding: FragmentAboutBinding? = null
    private val binding get() = _binding!!

    private var logoClickCount = 0
    private var lastLogoClickTime = 0L
    private var mysteryMediaPlayer: MediaPlayer? = null

    override fun getTheme(): Int = R.style.Theme_IlmNet_FullScreenCredits

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAboutBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Kapat Butonu
        binding.btnCloseCredits.setOnClickListener {
            dismiss()
        }

        // GitHub Butonu (mbahadir77)
        binding.btnCreditsGithub.setOnClickListener {
            val githubUrl = "https://github.com/mbahadir77"
            try {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(githubUrl))
                startActivity(intent)
            } catch (e: Exception) {
                IlmToast.info(requireActivity(), githubUrl, title = "GitHub Adresi")
            }
        }

        // FAZ 12: Hikayeleştirilmiş Sürüm Notları (Heist-Style Release Notes)
        binding.btnReleaseNotes.setOnClickListener {
            requireContext().performTokHaptic()
            ReleaseNotesBottomSheetDialogFragment.newInstance()
                .show(parentFragmentManager, ReleaseNotesBottomSheetDialogFragment.TAG)
        }

        // FAZ 12: Sinematik Gizem (10 Tıklama Easter Egg)
        setupEasterEggListener()

        // Sinematik gizem overlay dokunma dinleyicisi (Kapatma)
        binding.layoutMysteryCinematic.setOnClickListener {
            stopMysteryAudio()
            binding.layoutMysteryCinematic.animate()
                .alpha(0f)
                .setDuration(500)
                .withEndAction {
                    binding.layoutMysteryCinematic.visibility = View.GONE
                }
                .start()
            logoClickCount = 0
        }

        // IMF Terminal ekranını kapatma
        binding.layoutEasterEggTerminal.setOnClickListener {
            binding.layoutEasterEggTerminal.visibility = View.GONE
            logoClickCount = 0
        }

        binding.btnTerminalExit.setOnClickListener {
            binding.layoutEasterEggTerminal.visibility = View.GONE
            logoClickCount = 0
        }

        binding.btnTerminalConfirm.setOnClickListener {
            binding.layoutEasterEggTerminal.visibility = View.GONE
            logoClickCount = 0
            IlmToast.success(requireActivity(), "Protokol onaylandı. Sadaka-i cariye operasyonu devrede.", title = "Görev Aktif 🛡️")
        }
    }

    private fun setupEasterEggListener() {
        binding.ivCreditsLogo.setOnClickListener {
            val currentTime = System.currentTimeMillis()
            if (currentTime - lastLogoClickTime > 2500) {
                logoClickCount = 0
            }
            lastLogoClickTime = currentTime
            logoClickCount++

            // Haptik geri bildirim (kısa titreşim)
            triggerHapticClick()

            if (logoClickCount in 6..9) {
                val remaining = 10 - logoClickCount
                IlmToast.info(
                    requireActivity(),
                    "Mabedin kapısı aralanıyor... ($remaining)",
                    title = "Kalbe Doğru 📜"
                )
            } else if (logoClickCount >= 10) {
                logoClickCount = 0
                triggerCinematicMysteryEasterEgg()
            }
        }
    }

    /**
     * BÖLÜM 3.1: Sinematik Gizem (10 Tıklama Easter Egg).
     * Ekranda her şey kararır (fade out), arkadan epik ses (R.raw.epic_mystery_voice) çalmaya başlar,
     * ekranda altın rengiyle yavaşça şu yazı belirir:
     * 'Gerçeği arayan yolcu... İlim Diyârı'nın kalbine ulaştın.'
     */
    private fun triggerCinematicMysteryEasterEgg() {
        triggerSuccessVibration()

        // 1. Ekranı tamamen karart ve sinematik katmanı görünür yap
        binding.layoutMysteryCinematic.alpha = 0f
        binding.layoutMysteryCinematic.visibility = View.VISIBLE
        binding.layoutCinematicContent.alpha = 0f

        binding.layoutMysteryCinematic.animate()
            .alpha(1f)
            .setDuration(1200)
            .setInterpolator(AccelerateDecelerateInterpolator())
            .start()

        // 2. Altın yazıyı ve mührü yavaşça parlatarak ortaya çıkar
        binding.layoutCinematicContent.animate()
            .alpha(1f)
            .setDuration(2500)
            .setStartDelay(600)
            .setInterpolator(AccelerateDecelerateInterpolator())
            .start()

        // 3. Epik ses dosyasını (R.raw.epic_mystery_voice) oynat
        playMysteryAudio()
    }

    private fun playMysteryAudio() {
        try {
            stopMysteryAudio()
            val resId = resources.getIdentifier("epic_mystery_voice", "raw", requireContext().packageName)
            val finalResId = if (resId != 0) resId else R.raw.epic_mystery_voice
            mysteryMediaPlayer = MediaPlayer.create(requireContext(), finalResId)
            mysteryMediaPlayer?.isLooping = false
            mysteryMediaPlayer?.start()
        } catch (_: Exception) {
            // Ses donanımı veya dosya gecikmesi durumunda çökmeyi önle
        }
    }

    private fun stopMysteryAudio() {
        try {
            mysteryMediaPlayer?.stop()
            mysteryMediaPlayer?.release()
            mysteryMediaPlayer = null
        } catch (_: Exception) {}
    }

    private fun triggerSecretProtocolEasterEgg() {
        // Güçlü titreşim darbesi
        triggerSuccessVibration()

        binding.layoutEasterEggTerminal.visibility = View.VISIBLE

        val terminalText = buildString {
            append(">>> SECURE TERMINAL INITIATED...\n")
            append(">>> AUTHENTICATION: LEVEL 10 CLEARANCE\n")
            append(">>> ENCRYPTION: 4096-BIT RSA SHA-256 // VERIFIED\n")
            append("═══════════════════════════════════════════════════\n\n")
            append("Gizli protokole erişildi.\n\n")
            append("Hoş geldin, Yönetici Bahadır.\n\n")
            append("Sunucular çevrimiçi, akademisyenler aktif.\n")
            append("İlmNet operasyonu başarıyla tamamlandı.\n\n")
            append("Kendini imha etmeyecek, sonsuza dek yaşayacak.\n\n")
            append("═══════════════════════════════════════════════════\n")
            append("OPERASYON KODU : ILMNET-ULTIMATE-MAX-PRO-2026\n")
            append("KADRO          : Yıldırım Technologies (M. Bahadır Yıldırım)\n")
            append("DURUM          : SISTEM TAM GÜÇLE DEVREDE [OK]\n")
        }

        // Terminal daktilo efekti
        viewLifecycleOwner.lifecycleScope.launch {
            binding.tvTerminalOutput.text = ""
            val sb = java.lang.StringBuilder()
            for (char in terminalText) {
                sb.append(char)
                binding.tvTerminalOutput.text = sb.toString()
                if (char == '\n') {
                    delay(25)
                } else {
                    delay(6)
                }
            }

            // Metin daktilo gibi aktıktan SONRA ekranda kırmızı bir sayaç başlar:
            // "Bu mesaj 10 saniye içinde kendini imha edecektir: 10, 9, 8..."
            binding.tvTerminalCountdown.visibility = View.VISIBLE
            for (secondsRemaining in 10 downTo 0) {
                if (!isAdded || _binding == null) break
                binding.tvTerminalCountdown.text =
                    "⚠️ Bu mesaj $secondsRemaining saniye içinde kendini imha edecektir: $secondsRemaining"
                triggerHapticClick()
                if (secondsRemaining == 0) {
                    delay(400)
                    if (isAdded) {
                        dismissAllowingStateLoss()
                        IlmToast.info(requireActivity(), "İlmi protokol tamamlandı. Akış meclisine dönüldü.", title = "Görev Aktif")
                    }
                    break
                }
                delay(1000)
            }
        }
    }

    private fun triggerHapticClick() {
        try {
            val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val manager = requireContext().getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                manager?.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                requireContext().getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator?.vibrate(VibrationEffect.createOneShot(35, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(35)
            }
        } catch (_: Exception) {}
    }

    private fun triggerSuccessVibration() {
        try {
            val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val manager = requireContext().getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                manager?.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                requireContext().getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            }
            val timings = longArrayOf(0, 100, 80, 200, 100, 300)
            val amplitudes = intArrayOf(0, 150, 0, 200, 0, 255)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator?.vibrate(VibrationEffect.createWaveform(timings, amplitudes, -1))
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(timings, -1)
            }
        } catch (_: Exception) {}
    }

    override fun onDestroyView() {
        super.onDestroyView()
        stopMysteryAudio()
        _binding = null
    }

    companion object {
        fun newInstance(): AboutFragment = AboutFragment()
    }
}
