package com.nono.iptv.ui.main

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.nono.iptv.R
import com.nono.iptv.NonoApp
import com.nono.iptv.data.IptvRepository
import com.nono.iptv.data.entity.ChannelEntity
import com.nono.iptv.data.entity.PlaylistEntity
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Live TV pane: categories (col 2) + channel list with mini-preview and EPG (col 3). */
class LiveFragment : Fragment(R.layout.fragment_live) {

    private val repo: IptvRepository get() = NonoApp.repository()
    private var playlist: PlaylistEntity? = null

    private lateinit var categoryAdapter: CategoryAdapter
    private lateinit var channelAdapter: ChannelAdapter
    private lateinit var programTitle: TextView
    private lateinit var programTime: TextView
    private val timeFmt = SimpleDateFormat("HH:mm", Locale.getDefault())

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        programTitle = view.findViewById(R.id.tv_program_title)
        programTime = view.findViewById(R.id.tv_program_time)

        val categoriesView: RecyclerView = view.findViewById(R.id.live_categories)
        val channelsView: RecyclerView = view.findViewById(R.id.live_channels)

        categoryAdapter = CategoryAdapter { cat -> loadChannels(cat.remoteId) }
        channelAdapter = ChannelAdapter(
            onFocused = { ch -> onChannelFocused(ch) },
            onClicked = { (activity as? MainActivity)?.enterFullscreen() }
        )

        categoriesView.layoutManager = LinearLayoutManager(requireContext())
        categoriesView.adapter = categoryAdapter
        channelsView.layoutManager = LinearLayoutManager(requireContext())
        channelsView.adapter = channelAdapter

        loadCategories()
    }

    private fun loadCategories() {
        lifecycleScope.launch {
            val pl = repo.activePlaylist() ?: return@launch
            playlist = pl
            val cats = mutableListOf(
                CatItem(IptvRepository.CAT_FAVORITES, getString(R.string.cat_favorites)),
                CatItem(IptvRepository.CAT_ALL, getString(R.string.cat_all_channels))
            )
            repo.liveCategories(pl.id).forEach { cats.add(CatItem(it.remoteId, it.name)) }
            categoryAdapter.submit(cats)
            // Show the first real/virtual category immediately.
            loadChannels(cats.firstOrNull()?.remoteId ?: IptvRepository.CAT_ALL)
        }
    }

    private fun loadChannels(categoryRemoteId: String) {
        val pl = playlist ?: return
        lifecycleScope.launch {
            val channels = repo.channelsIn(pl.id, categoryRemoteId)
            channelAdapter.submit(channels)
        }
    }

    private fun onChannelFocused(ch: ChannelEntity) {
        (activity as? MainActivity)?.playStream(ch.streamUrl)
        programTitle.text = ch.name
        programTime.text = ""
        val pl = playlist ?: return
        lifecycleScope.launch {
            val now = repo.nowProgram(ch) ?: repo.programsForChannel(pl, ch).let { repo.nowProgram(ch) }
            if (now != null) {
                programTitle.text = now.title
                programTime.text = "${timeFmt.format(Date(now.startMs))} – ${timeFmt.format(Date(now.stopMs))}"
                channelAdapter.setProgram(ch.id, now)
            }
        }
    }
}
