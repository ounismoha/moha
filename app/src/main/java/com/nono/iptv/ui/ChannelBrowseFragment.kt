package com.nono.iptv.ui

import android.os.Bundle
import android.widget.Toast
import androidx.fragment.app.viewModels
import androidx.leanback.app.BrowseSupportFragment
import androidx.leanback.widget.ArrayObjectAdapter
import androidx.leanback.widget.HeaderItem
import androidx.leanback.widget.ListRow
import androidx.leanback.widget.ListRowPresenter
import androidx.leanback.widget.OnItemViewClickedListener
import com.nono.iptv.R
import com.nono.iptv.data.ChannelEntity

/**
 * Leanback browse screen: one row per playlist group, each row holding the group's
 * channel cards. Selecting a card asks the host activity to tune to that stream.
 */
class ChannelBrowseFragment : BrowseSupportFragment() {

    /** Implemented by the hosting activity to start playback. */
    interface OnChannelSelectedListener {
        fun onChannelSelected(channel: ChannelEntity)
    }

    private val viewModel: ChannelBrowseViewModel by viewModels()
    private val rowsAdapter = ArrayObjectAdapter(ListRowPresenter())

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        title = getString(R.string.browse_title)
        headersState = HEADERS_ENABLED
        isHeadersTransitionOnBackEnabled = true
        adapter = rowsAdapter

        onItemViewClickedListener = OnItemViewClickedListener { _, item, _, _ ->
            if (item is ChannelEntity) {
                (activity as? OnChannelSelectedListener)?.onChannelSelected(item)
            }
        }

        observe()
        viewModel.load()
    }

    private fun observe() {
        viewModel.channelsByGroup.observe(viewLifecycleOwner) { groups ->
            rebuildRows(groups)
        }
        viewModel.status.observe(viewLifecycleOwner) { status ->
            when (status) {
                is ChannelBrowseViewModel.Status.Loading ->
                    Toast.makeText(requireContext(), R.string.loading_playlist, Toast.LENGTH_SHORT).show()
                is ChannelBrowseViewModel.Status.Error ->
                    Toast.makeText(requireContext(), status.message, Toast.LENGTH_LONG).show()
                else -> Unit
            }
        }
    }

    private fun rebuildRows(groups: Map<String, List<ChannelEntity>>) {
        rowsAdapter.clear()
        val cardPresenter = ChannelCardPresenter()
        var headerId = 0L
        groups.forEach { (group, channels) ->
            val listRowAdapter = ArrayObjectAdapter(cardPresenter)
            channels.forEach { listRowAdapter.add(it) }
            val header = HeaderItem(headerId++, "$group  (${channels.size})")
            rowsAdapter.add(ListRow(header, listRowAdapter))
        }
    }
}
