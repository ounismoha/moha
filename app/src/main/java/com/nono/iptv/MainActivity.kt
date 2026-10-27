package com.nono.iptv

import android.os.Bundle
import androidx.fragment.app.FragmentActivity
import androidx.media3.common.util.UnstableApi
import com.nono.iptv.data.ChannelEntity
import com.nono.iptv.databinding.ActivityMainBinding
import com.nono.iptv.player.PlayerEngine
import com.nono.iptv.ui.ChannelBrowseFragment

/**
 * Single-activity host: a full-screen Media3 [androidx.media3.ui.PlayerView] at the back,
 * with the Leanback [ChannelBrowseFragment] overlaid on top. Selecting a channel tunes
 * the shared [PlayerEngine] to its stream.
 *
 * Extends FragmentActivity (not AppCompatActivity): this activity uses the Leanback theme,
 * and AppCompatActivity requires a Theme.AppCompat descendant — pairing it with Theme.Leanback
 * crashes at launch. Leanback fragments expect FragmentActivity + a Leanback theme.
 */
@UnstableApi
class MainActivity : FragmentActivity(), ChannelBrowseFragment.OnChannelSelectedListener {

    private lateinit var binding: ActivityMainBinding
    private lateinit var playerEngine: PlayerEngine

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        playerEngine = PlayerEngine(this)
        binding.playerView.player = playerEngine.initialize()

        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.browse_container, ChannelBrowseFragment())
                .commit()
        }
    }

    override fun onChannelSelected(channel: ChannelEntity) {
        playerEngine.play(channel.streamUrl)
    }

    override fun onStop() {
        super.onStop()
        playerEngine.stop()
    }

    override fun onDestroy() {
        super.onDestroy()
        playerEngine.release()
    }
}
