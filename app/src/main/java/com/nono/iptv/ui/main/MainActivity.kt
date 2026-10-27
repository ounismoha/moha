package com.nono.iptv.ui.main

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.FrameLayout
import android.widget.LinearLayout
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.PlayerView
import com.nono.iptv.NonoApp
import com.nono.iptv.R
import com.nono.iptv.data.IptvRepository
import com.nono.iptv.data.entity.VodKind
import com.nono.iptv.player.PlayerEngine
import com.nono.iptv.ui.settings.SettingsDrawerFragment
import com.nono.iptv.ui.setup.SetupActivity
import kotlinx.coroutines.launch

/**
 * Host of the 3-pane interface. Routes to the setup wizard on first launch, otherwise shows the
 * nav rail + a content fragment over a full-screen Media3 player, with a slide-in settings drawer.
 */
@UnstableApi
class MainActivity : FragmentActivity() {

    private val repo: IptvRepository get() = NonoApp.repository()
    private lateinit var playerEngine: PlayerEngine

    private lateinit var chrome: LinearLayout
    private lateinit var settingsContainer: FrameLayout
    private var fullscreen = false

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
        findViewById<PlayerView>(R.id.player_view).player = playerEngine.initialize()

        wireNav()

        // Default section: Live TV.
        select(R.id.nav_tv, LiveFragment())
    }

    private fun wireNav() {
        val map = mapOf(
            R.id.nav_search to { select(R.id.nav_search, SearchFragment()) },
            R.id.nav_tv to { select(R.id.nav_tv, LiveFragment()) },
            R.id.nav_films to { select(R.id.nav_films, CatalogFragment.of(VodKind.MOVIE)) },
            R.id.nav_series to { select(R.id.nav_series, CatalogFragment.of(VodKind.SERIES)) },
            R.id.nav_dvr to { select(R.id.nav_dvr, PlaceholderFragment.of("Enregistrements — bientôt disponible")) },
            R.id.nav_list to { select(R.id.nav_list, PlaceholderFragment.of("Ma liste — bientôt disponible")) }
        )
        map.forEach { (id, action) -> findViewById<View>(id).setOnClickListener { action() } }
        findViewById<View>(R.id.nav_settings).setOnClickListener { openSettings() }
    }

    private val navIds = intArrayOf(
        R.id.nav_search, R.id.nav_tv, R.id.nav_films, R.id.nav_series, R.id.nav_dvr, R.id.nav_list
    )

    private fun select(navId: Int, fragment: Fragment) {
        navIds.forEach { findViewById<View>(it).isSelected = it == navId }
        supportFragmentManager.beginTransaction()
            .replace(R.id.content_frame, fragment)
            .commit()
    }

    // ---- API used by the content fragments -----------------------------------

    fun playStream(url: String) = playerEngine.play(url)

    fun enterFullscreen() {
        fullscreen = true
        chrome.visibility = View.GONE
    }

    private fun exitFullscreen() {
        fullscreen = false
        chrome.visibility = View.VISIBLE
    }

    fun openSettings() {
        settingsContainer.visibility = View.VISIBLE
        supportFragmentManager.beginTransaction()
            .replace(R.id.settings_container, SettingsDrawerFragment())
            .commit()
    }

    private fun closeSettings() {
        settingsContainer.visibility = View.GONE
        supportFragmentManager.findFragmentById(R.id.settings_container)?.let {
            supportFragmentManager.beginTransaction().remove(it).commit()
        }
    }

    // ---- back / PiP ----------------------------------------------------------

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        when {
            settingsContainer.visibility == View.VISIBLE -> closeSettings()
            fullscreen -> exitFullscreen()
            else -> super.onBackPressed()
        }
    }

    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        // Enter Picture-in-Picture when the user leaves while a stream is playing (API 24+).
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
        if (::playerEngine.isInitialized) playerEngine.release()
    }
}
