package com.nono.iptv.ui.main

import android.os.Bundle
import android.view.View
import android.widget.FrameLayout
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.PlayerView
import com.nono.iptv.NonoApp
import com.nono.iptv.R
import com.nono.iptv.data.IptvRepository
import com.nono.iptv.player.PlayerEngine
import kotlinx.coroutines.launch

/**
 * Multi-Vue: a 2×2 grid of up to four live channels playing at once. Only the focused cell is
 * audible; focusing another cell switches the audio to it. Each cell has its own player, so this
 * is heavier than single playback — fine on capable boxes, but limited to four streams.
 */
@UnstableApi
class MultiViewFragment : Fragment(R.layout.fragment_multiview) {

    private val engines = arrayOfNulls<PlayerEngine>(4)
    private val views = arrayOfNulls<PlayerView>(4)
    private val labels = arrayOfNulls<TextView>(4)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val cellIds = intArrayOf(R.id.mv_cell0, R.id.mv_cell1, R.id.mv_cell2, R.id.mv_cell3)
        val pvIds = intArrayOf(R.id.mv_pv0, R.id.mv_pv1, R.id.mv_pv2, R.id.mv_pv3)
        val labelIds = intArrayOf(R.id.mv_label0, R.id.mv_label1, R.id.mv_label2, R.id.mv_label3)

        for (i in 0 until 4) {
            views[i] = view.findViewById(pvIds[i])
            labels[i] = view.findViewById(labelIds[i])
            labels[i]?.text = "—"
            view.findViewById<FrameLayout>(cellIds[i]).setOnFocusChangeListener { _, hasFocus ->
                if (hasFocus) setActiveAudio(i)
            }
        }

        val repo = NonoApp.repository()
        lifecycleScope.launch {
            val pl = repo.activePlaylist() ?: return@launch
            var channels = repo.channelsIn(pl.id, IptvRepository.CAT_FAVORITES)
            if (channels.isEmpty()) channels = repo.channelsIn(pl.id, IptvRepository.CAT_ALL)
            channels = channels.take(4)

            channels.forEachIndexed { i, ch ->
                val engine = PlayerEngine(requireContext())
                engines[i] = engine
                views[i]?.player = engine.initialize()
                engine.play(ch.streamUrl)
                engine.player?.volume = if (i == 0) 1f else 0f
                labels[i]?.text = "${if (ch.channelNumber > 0) "${ch.channelNumber}  " else ""}${ch.name}"
            }
        }
    }

    private fun setActiveAudio(active: Int) {
        engines.forEachIndexed { i, engine -> engine?.player?.volume = if (i == active) 1f else 0f }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        for (i in 0 until 4) {
            engines[i]?.release()
            engines[i] = null
            views[i] = null
            labels[i] = null
        }
    }
}
