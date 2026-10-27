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
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * TV Guide: a vertical list of channels, each with its own horizontal program timeline.
 * Programs load lazily per row (and are cached), so EPG is fetched only for visible channels.
 */
class GuideFragment : Fragment(R.layout.fragment_guide) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val header: TextView = view.findViewById(R.id.guide_header)
        val rows: RecyclerView = view.findViewById(R.id.guide_rows)
        rows.layoutManager = LinearLayoutManager(requireContext())

        val repo = NonoApp.repository()
        header.text = SimpleDateFormat("EEEE d MMMM — HH:mm", Locale.getDefault()).format(Date())

        lifecycleScope.launch {
            val pl = repo.activePlaylist() ?: return@launch
            val adapter = GuideRowAdapter(viewLifecycleOwner.lifecycleScope) { ch ->
                repo.programsForChannel(pl, ch)
            }
            rows.adapter = adapter
            // Cap the number of rows so we don't fire EPG requests for tens of thousands of channels.
            adapter.submit(repo.channelsIn(pl.id, IptvRepository.CAT_ALL).take(150))
        }
    }
}
