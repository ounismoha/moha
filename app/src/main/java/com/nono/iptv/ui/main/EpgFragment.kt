package com.nono.iptv.ui.main

import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.nono.iptv.NonoApp
import com.nono.iptv.R
import com.nono.iptv.data.IptvRepository
import com.nono.iptv.data.entity.ChannelEntity
import com.nono.iptv.data.entity.PlaylistEntity
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Program guide: channels on the left, the focused channel's timeline on the right. */
class EpgFragment : Fragment(R.layout.fragment_epg) {

    private val repo: IptvRepository get() = NonoApp.repository()
    private var playlist: PlaylistEntity? = null
    private lateinit var channelAdapter: ChannelAdapter
    private lateinit var programAdapter: EpgProgramAdapter
    private lateinit var header: TextView
    private val dateFmt = SimpleDateFormat("EEE d MMM", Locale.getDefault())

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        header = view.findViewById(R.id.epg_header)
        val channelsView: RecyclerView = view.findViewById(R.id.epg_channels)
        val programsView: RecyclerView = view.findViewById(R.id.epg_programs)

        channelAdapter = ChannelAdapter(onFocused = { showGuide(it) }, onClicked = { showGuide(it) })
        programAdapter = EpgProgramAdapter()

        channelsView.layoutManager = LinearLayoutManager(requireContext())
        channelsView.adapter = channelAdapter
        programsView.layoutManager = LinearLayoutManager(requireContext())
        programsView.adapter = programAdapter

        lifecycleScope.launch {
            val pl = repo.activePlaylist() ?: return@launch
            playlist = pl
            channelAdapter.submit(repo.channelsIn(pl.id, IptvRepository.CAT_ALL))
        }
    }

    private fun showGuide(ch: ChannelEntity) {
        val pl = playlist ?: return
        header.text = "${ch.name}  •  ${dateFmt.format(Date())}"
        lifecycleScope.launch {
            programAdapter.submit(repo.programsForChannel(pl, ch))
        }
    }
}
