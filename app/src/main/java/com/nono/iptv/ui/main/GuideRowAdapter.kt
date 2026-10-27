package com.nono.iptv.ui.main

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.nono.iptv.R
import com.nono.iptv.data.entity.ChannelEntity
import com.nono.iptv.data.entity.ProgramEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * One guide row per channel: a fixed left cell (number + name) and a horizontal program strip.
 * Programs are fetched lazily per visible row (via [fetch]) and cached so scrolling back is instant.
 */
class GuideRowAdapter(
    private val scope: CoroutineScope,
    private val fetch: suspend (ChannelEntity) -> List<ProgramEntity>
) : RecyclerView.Adapter<GuideRowAdapter.VH>() {

    private val channels = mutableListOf<ChannelEntity>()
    private val cache = HashMap<Long, List<ProgramEntity>>()

    fun submit(list: List<ChannelEntity>) {
        channels.clear()
        channels.addAll(list)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_guide_row, parent, false)
        val vh = VH(v)
        vh.programs.layoutManager =
            LinearLayoutManager(parent.context, LinearLayoutManager.HORIZONTAL, false)
        vh.programs.adapter = vh.programAdapter
        vh.programs.isFocusable = false
        return vh
    }

    override fun getItemCount() = channels.size

    override fun onBindViewHolder(holder: VH, position: Int) {
        val ch = channels[position]
        holder.boundId = ch.id
        holder.number.text = if (ch.channelNumber > 0) ch.channelNumber.toString() else (position + 1).toString()
        holder.name.text = ch.name

        val cached = cache[ch.id]
        if (cached != null) {
            holder.programAdapter.submit(cached)
        } else {
            holder.programAdapter.submit(emptyList())
            scope.launch {
                val progs = fetch(ch)
                cache[ch.id] = progs
                if (holder.boundId == ch.id) holder.programAdapter.submit(progs)
            }
        }
    }

    class VH(v: View) : RecyclerView.ViewHolder(v) {
        val number: TextView = v.findViewById(R.id.guide_channel_number)
        val name: TextView = v.findViewById(R.id.guide_channel_name)
        val programs: RecyclerView = v.findViewById(R.id.guide_row_programs)
        val programAdapter = GuideProgramAdapter()
        var boundId: Long = -1
    }
}
