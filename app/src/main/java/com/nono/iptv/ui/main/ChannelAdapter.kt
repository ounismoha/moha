package com.nono.iptv.ui.main

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.nono.iptv.R
import com.nono.iptv.data.entity.ChannelEntity
import com.nono.iptv.data.entity.ProgramEntity

/**
 * Live channel list. Focusing a row previews it; OK opens full-screen. The current-program
 * text + progress bar are filled from a per-channel EPG cache the fragment updates.
 */
class ChannelAdapter(
    private val onFocused: (ChannelEntity) -> Unit,
    private val onClicked: (ChannelEntity) -> Unit
) : RecyclerView.Adapter<ChannelAdapter.VH>() {

    private val items = mutableListOf<ChannelEntity>()
    private val epg = mutableMapOf<Long, ProgramEntity?>()

    fun submit(list: List<ChannelEntity>) {
        items.clear()
        items.addAll(list)
        epg.clear()
        notifyDataSetChanged()
    }

    fun setProgram(channelId: Long, program: ProgramEntity?) {
        epg[channelId] = program
        val idx = items.indexOfFirst { it.id == channelId }
        if (idx >= 0) notifyItemChanged(idx)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_channel, parent, false)
        return VH(v)
    }

    override fun getItemCount() = items.size

    override fun onBindViewHolder(holder: VH, position: Int) {
        val ch = items[position]
        holder.number.text = if (ch.channelNumber > 0) ch.channelNumber.toString() else (position + 1).toString()
        holder.name.text = ch.name
        holder.fav.visibility = if (ch.isFavorite) View.VISIBLE else View.GONE

        if (!ch.logoUrl.isNullOrBlank()) {
            Glide.with(holder.logo).load(ch.logoUrl).placeholder(R.drawable.default_logo)
                .error(R.drawable.default_logo).into(holder.logo)
        } else {
            holder.logo.setImageResource(R.drawable.default_logo)
        }

        val program = epg[ch.id]
        if (program != null) {
            holder.program.text = program.title
            val span = (program.stopMs - program.startMs).coerceAtLeast(1)
            val done = (System.currentTimeMillis() - program.startMs).coerceIn(0, span)
            holder.progress.progress = (done * 100 / span).toInt()
            holder.progress.visibility = View.VISIBLE
        } else {
            holder.program.text = holder.itemView.context.getString(R.string.no_program_info)
            holder.progress.visibility = View.INVISIBLE
        }

        holder.itemView.setOnFocusChangeListener { _, hasFocus -> if (hasFocus) onFocused(ch) }
        holder.itemView.setOnClickListener { onClicked(ch) }
    }

    class VH(v: View) : RecyclerView.ViewHolder(v) {
        val number: TextView = v.findViewById(R.id.channel_number)
        val logo: ImageView = v.findViewById(R.id.channel_logo)
        val name: TextView = v.findViewById(R.id.channel_name)
        val program: TextView = v.findViewById(R.id.channel_program)
        val progress: ProgressBar = v.findViewById(R.id.channel_progress)
        val fav: TextView = v.findViewById(R.id.channel_fav)
    }
}
