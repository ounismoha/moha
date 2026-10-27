package com.nono.iptv.ui.main

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.KeyEvent
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import androidx.media3.common.Player
import androidx.media3.common.VideoSize
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.bumptech.glide.Glide
import com.nono.iptv.NonoApp
import com.nono.iptv.R
import com.nono.iptv.data.IptvRepository
import com.nono.iptv.data.entity.ChannelEntity
import com.nono.iptv.data.entity.VodKind
import com.nono.iptv.player.PlayerEngine
import com.nono.iptv.ui.settings.SettingsDrawerFragment
import com.nono.iptv.ui.setup.SetupActivity
import kotlinx.coroutines.launch

/**
 * Host of the 3-pane interface. Routes to the setup wizard on first launch, otherwise shows the
 * nav rail + a content fragment over a Media3 player that runs as a windowed 16:9 preview on the
 * Live screen and expands to full-screen with a TiViMate-style control overlay.
 */
@UnstableApi
class MainActivity : FragmentActivity() {

    private val repo: IptvRepository get() = NonoApp.repository()
    private lateinit var playerEngine: PlayerEngine

    private lateinit var chrome: LinearLayout
    private lateinit var settingsContainer: FrameLayout
    private lateinit var playerView: PlayerView
    private lateinit var playerHost: FrameLayout
    private var windowedParams: FrameLayout.LayoutParams? = null
    private var previewEnabled = false
    private var fullscreen = false

    // ---- full-screen overlay --------------------------------------------------
    private lateinit var playerOverlay: View
    private lateinit var infoNumber: TextView
    private lateinit var infoName: TextView
    private lateinit var infoNow: TextView
    private lateinit var infoLogo: ImageView
    private lateinit var ctrlResLabel: TextView
    private lateinit var ctrlAudioLabel: TextView
    private lateinit var ctrlSubLabel: TextView
    private lateinit var ctrlFormatLabel: TextView
    private lateinit var ctrlSleepLabel: TextView
    private lateinit var ctrlFavLabel: TextView

    private var currentChannel: ChannelEntity? = null
    private var currentFav = false

    private val resizeModes = intArrayOf(
        AspectRatioFrameLayout.RESIZE_MODE_FIT,
        AspectRatioFrameLayout.RESIZE_MODE_FILL,
        AspectRatioFrameLayout.RESIZE_MODE_ZOOM
    )
    private var resizeIdx = 0
    private val sleepMinutes = intArrayOf(0, 15, 30, 60, 90)
    private var sleepIdx = 0

    private val uiHandler = Handler(Looper.getMainLooper())
    private val hideOverlayRunnable = Runnable { hideOverlay() }
    private val sleepRunnable = Runnable {
        if (::playerEngine.isInitialized) playerEngine.stop()
        finish()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        lifecycleScope.launch {
            if (!repo.hasAnyPlaylist()) {
                startActivity(Intent(this@MainActivity, SetupActivity::class.java))
                finish()
                return@launch
            }
            buildUi()
        }
    }

    private fun buildUi() {
        setContentView(R.layout.activity_main)

        chrome = findViewById(R.id.chrome)
        settingsContainer = findViewById(R.id.settings_container)

        playerEngine = PlayerEngine(this)
        playerView = findViewById(R.id.player_view)
        playerHost = findViewById(R.id.player_host)
        windowedParams = FrameLayout.LayoutParams(playerHost.layoutParams as FrameLayout.LayoutParams)
        playerView.player = playerEngine.initialize()
        playerView.useController = false
        playerView.setShowSubtitleButton(true)

        // Keep the resolution label in sync with the active video track.
        playerEngine.player?.addListener(object : Player.Listener {
            override fun onVideoSizeChanged(videoSize: VideoSize) {
                if (::ctrlResLabel.isInitialized) {
                    ctrlResLabel.text =
                        if (videoSize.width > 0) "${videoSize.width} × ${videoSize.height}" else "—"
                }
            }
        })

        wireNav()
        wireOverlay()

        // Default section: Live TV.
        select(R.id.nav_tv, LiveFragment())
    }

    private fun wireNav() {
        val map = mapOf(
            R.id.nav_search to { select(R.id.nav_search, SearchFragment()) },
            R.id.nav_tv to { select(R.id.nav_tv, LiveFragment()) },
            R.id.nav_films to { select(R.id.nav_films, CatalogFragment.of(VodKind.MOVIE)) },
            R.id.nav_series to { select(R.id.nav_series, CatalogFragment.of(VodKind.SERIES)) },
            R.id.nav_guide to { select(R.id.nav_guide, GuideFragment()) },
            R.id.nav_dvr to { select(R.id.nav_dvr, DvrFragment()) },
            R.id.nav_list to { select(R.id.nav_list, PlaceholderFragment.of("Ma liste — bientôt disponible")) },
            R.id.nav_multiview to { select(R.id.nav_multiview, MultiViewFragment()) }
        )
        map.forEach { (id, action) -> findViewById<View>(id).setOnClickListener { action() } }
        findViewById<View>(R.id.nav_settings).setOnClickListener { openSettings() }
    }

    private fun wireOverlay() {
        playerOverlay = findViewById(R.id.player_overlay)
        infoNumber = findViewById(R.id.info_number)
        infoName = findViewById(R.id.info_name)
        infoNow = findViewById(R.id.info_now)
        infoLogo = findViewById(R.id.info_logo)
        ctrlResLabel = findViewById(R.id.ctrl_res_label)
        ctrlAudioLabel = findViewById(R.id.ctrl_audio_label)
        ctrlSubLabel = findViewById(R.id.ctrl_sub_label)
        ctrlFormatLabel = findViewById(R.id.ctrl_format_label)
        ctrlSleepLabel = findViewById(R.id.ctrl_sleep_label)
        ctrlFavLabel = findViewById(R.id.ctrl_fav_label)

        findViewById<View>(R.id.ctrl_format).setOnClickListener { cycleAspect(); keepOverlay() }
        findViewById<View>(R.id.ctrl_sub).setOnClickListener { toggleSubtitles(); keepOverlay() }
        findViewById<View>(R.id.ctrl_audio).setOnClickListener { cycleAudio(); keepOverlay() }
        findViewById<View>(R.id.ctrl_sleep).setOnClickListener { cycleSleep(); keepOverlay() }
        findViewById<View>(R.id.ctrl_fav).setOnClickListener { toggleFav(); keepOverlay() }
        findViewById<View>(R.id.ctrl_sync).setOnClickListener {
            toast("Réglage audio — bientôt"); keepOverlay()
        }
        findViewById<View>(R.id.ctrl_options).setOnClickListener {
            toast("Options de chaîne — bientôt"); keepOverlay()
        }
        findViewById<View>(R.id.ctrl_settings).setOnClickListener { openSettings() }
    }

    private val navIds = intArrayOf(
        R.id.nav_search, R.id.nav_tv, R.id.nav_films, R.id.nav_series, R.id.nav_guide,
        R.id.nav_dvr, R.id.nav_list, R.id.nav_multiview
    )

    private fun select(navId: Int, fragment: Fragment) {
        navIds.forEach { findViewById<View>(it).isSelected = it == navId }
        previewEnabled = fragment is LiveFragment
        if (::playerHost.isInitialized && !fullscreen) {
            applyWindowedPreview()
            if (!previewEnabled && ::playerEngine.isInitialized) playerEngine.stop()
        }
        supportFragmentManager.beginTransaction()
            .replace(R.id.content_frame, fragment)
            .commit()
    }

    /** Restore the top-right 16:9 window and show it only when a Live preview is wanted. */
    private fun applyWindowedPreview() {
        windowedParams?.let { playerHost.layoutParams = FrameLayout.LayoutParams(it) }
        playerHost.visibility = if (previewEnabled) View.VISIBLE else View.GONE
    }

    // ---- API used by the content fragments -----------------------------------

    fun playStream(url: String) {
        if (::playerHost.isInitialized && playerHost.visibility != View.VISIBLE) {
            playerHost.visibility = View.VISIBLE
        }
        playerEngine.play(url)
    }

    /** Live screen reports the focused channel so the overlay info bar + favorite stay in sync. */
    fun setLiveContext(channel: ChannelEntity, programTitle: String?, programTime: String?) {
        currentChannel = channel
        currentFav = channel.isFavorite
        if (!::infoName.isInitialized) return
        infoNumber.text = if (channel.channelNumber > 0) channel.channelNumber.toString() else ""
        infoName.text = channel.name
        infoNow.text = listOfNotNull(programTime, programTitle)
            .filter { it.isNotBlank() }.joinToString("   ")
        ctrlFavLabel.text = if (currentFav) "Retirer" else "Favori"
        if (!channel.logoUrl.isNullOrBlank()) {
            Glide.with(infoLogo).load(channel.logoUrl).placeholder(R.drawable.default_logo)
                .error(R.drawable.default_logo).into(infoLogo)
        } else {
            infoLogo.setImageResource(R.drawable.default_logo)
        }
    }

    fun enterFullscreen() {
        fullscreen = true
        playerHost.layoutParams = FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT
        )
        playerHost.visibility = View.VISIBLE
        playerHost.bringToFront()
        playerOverlay.bringToFront()
        chrome.visibility = View.GONE
        playerView.useController = false
        showOverlay()
    }

    private fun exitFullscreen() {
        fullscreen = false
        hideOverlay()
        applyWindowedPreview()
        chrome.visibility = View.VISIBLE
        findViewById<View>(R.id.content_frame).requestFocus()
    }

    // ---- overlay show / hide --------------------------------------------------

    private fun overlayVisible() =
        ::playerOverlay.isInitialized && playerOverlay.visibility == View.VISIBLE

    private fun showOverlay() {
        if (!fullscreen) return
        playerOverlay.visibility = View.VISIBLE
        playerOverlay.bringToFront()
        findViewById<View>(R.id.ctrl_format).requestFocus()
        keepOverlay()
    }

    private fun hideOverlay() {
        uiHandler.removeCallbacks(hideOverlayRunnable)
        if (::playerOverlay.isInitialized) playerOverlay.visibility = View.GONE
    }

    /** (Re)start the auto-hide countdown. */
    private fun keepOverlay() {
        uiHandler.removeCallbacks(hideOverlayRunnable)
        uiHandler.postDelayed(hideOverlayRunnable, 5000)
    }

    // ---- control actions ------------------------------------------------------

    private fun cycleAspect() {
        resizeIdx = (resizeIdx + 1) % resizeModes.size
        playerView.resizeMode = resizeModes[resizeIdx]
        ctrlFormatLabel.text = when (resizeModes[resizeIdx]) {
            AspectRatioFrameLayout.RESIZE_MODE_FILL -> "Étirer"
            AspectRatioFrameLayout.RESIZE_MODE_ZOOM -> "Zoom"
            else -> "Normal"
        }
    }

    private fun toggleSubtitles() {
        val on = playerEngine.toggleSubtitles()
        ctrlSubLabel.text = if (on) "Activés" else "Sous-titres"
    }

    private fun cycleAudio() {
        ctrlAudioLabel.text = playerEngine.cycleAudioTrack()
    }

    private fun cycleSleep() {
        sleepIdx = (sleepIdx + 1) % sleepMinutes.size
        uiHandler.removeCallbacks(sleepRunnable)
        val m = sleepMinutes[sleepIdx]
        ctrlSleepLabel.text = if (m == 0) "Veille" else "$m min"
        if (m > 0) uiHandler.postDelayed(sleepRunnable, m * 60_000L)
    }

    private fun toggleFav() {
        val ch = currentChannel ?: return
        lifecycleScope.launch {
            repo.toggleFavorite(ch)
            currentFav = !currentFav
            ctrlFavLabel.text = if (currentFav) "Retirer" else "Favori"
        }
    }

    private fun toast(msg: String) = Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()

    // ---- settings / debug -----------------------------------------------------

    fun openSettings() {
        settingsContainer.visibility = View.VISIBLE
        supportFragmentManager.beginTransaction()
            .replace(R.id.settings_container, SettingsDrawerFragment())
            .commit()
    }

    fun openDebug() {
        if (settingsContainer.visibility == View.VISIBLE) closeSettings()
        navIds.forEach { findViewById<View>(it).isSelected = false }
        previewEnabled = false
        if (::playerHost.isInitialized && !fullscreen) applyWindowedPreview()
        supportFragmentManager.beginTransaction()
            .replace(R.id.content_frame, com.nono.iptv.ui.settings.DebugFragment())
            .commit()
    }

    private fun closeSettings() {
        settingsContainer.visibility = View.GONE
        supportFragmentManager.findFragmentById(R.id.settings_container)?.let {
            supportFragmentManager.beginTransaction().remove(it).commit()
        }
    }

    // ---- keys / back / PiP ----------------------------------------------------

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (fullscreen && event.action == KeyEvent.ACTION_DOWN) {
            val code = event.keyCode
            if (!overlayVisible()) {
                when (code) {
                    KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.KEYCODE_ENTER,
                    KeyEvent.KEYCODE_DPAD_UP, KeyEvent.KEYCODE_DPAD_DOWN,
                    KeyEvent.KEYCODE_MENU -> {
                        showOverlay()
                        return true
                    }
                }
            } else if (code != KeyEvent.KEYCODE_BACK) {
                keepOverlay()
            }
        }
        return super.dispatchKeyEvent(event)
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        when {
            settingsContainer.visibility == View.VISIBLE -> closeSettings()
            overlayVisible() -> hideOverlay()
            fullscreen -> exitFullscreen()
            else -> super.onBackPressed()
        }
    }

    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N &&
            ::playerEngine.isInitialized && playerEngine.player?.isPlaying == true &&
            packageManager.hasSystemFeature(android.content.pm.PackageManager.FEATURE_PICTURE_IN_PICTURE)
        ) {
            @Suppress("DEPRECATION")
            runCatching { enterPictureInPictureMode() }
        }
    }

    override fun onStop() {
        super.onStop()
        val inPip = Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && isInPictureInPictureMode
        if (::playerEngine.isInitialized && !inPip) playerEngine.stop()
    }

    override fun onDestroy() {
        super.onDestroy()
        uiHandler.removeCallbacksAndMessages(null)
        if (::playerEngine.isInitialized) playerEngine.release()
    }
}
