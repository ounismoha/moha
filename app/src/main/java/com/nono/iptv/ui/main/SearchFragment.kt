package com.nono.iptv.ui.main

import android.os.Bundle
import android.view.View
import android.widget.EditText
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.nono.iptv.NonoApp
import com.nono.iptv.R
import com.nono.iptv.data.IptvRepository
import com.nono.iptv.data.entity.PlaylistEntity
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** Channel search across the active playlist. */
class SearchFragment : Fragment(R.layout.fragment_search) {

    private val repo: IptvRepository get() = NonoApp.repository()
    private var playlist: PlaylistEntity? = null
    private lateinit var adapter: ChannelAdapter
    private var searchJob: Job? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val input: EditText = view.findViewById(R.id.search_input)
        val results: RecyclerView = view.findViewById(R.id.search_results)

        adapter = ChannelAdapter(
            onFocused = { (activity as? MainActivity)?.playStream(it.streamUrl) },
            onClicked = { (activity as? MainActivity)?.enterFullscreen() }
        )
        results.layoutManager = LinearLayoutManager(requireContext())
        results.adapter = adapter

        lifecycleScope.launch { playlist = repo.activePlaylist() }

        input.addTextChangedListener { text ->
            val q = text?.toString()?.trim().orEmpty()
            searchJob?.cancel()
            searchJob = lifecycleScope.launch {
                delay(250) // debounce
                val pl = playlist ?: return@launch
                adapter.submit(if (q.length >= 2) repo.searchChannels(pl.id, q) else emptyList())
            }
        }
    }
}
