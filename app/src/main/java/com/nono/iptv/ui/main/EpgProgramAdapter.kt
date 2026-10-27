package com.nono.iptv.ui.main

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.nono.iptv.R
import com.nono.iptv.data.entity.ProgramEntity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Program timeline for one channel (EPG guide). */
class EpgProgramAdapter : RecyclerView.Adapter<EpgProgramAdapter.VH>() {

    private val items = mutableListOf<ProgramEntity>()
    private val fmt = SimpleDateFormat("HH:mm", Locale.getDefault())

    fun submit(list: List<ProgramEntity>) {
        items.clear()
        items.addAll(list)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_epg_program, parent, false)
        return VH(v)
    }

    override fun getItemCount() = items.size

    override fun onBindViewHolder(holder: VH, position: Int) {
        val p = items[position]
        holder.time.text = "${fmt.format(Date(p.startMs))} – ${fmt.format(Date(p.stopMs))}"
        holder.title.text = p.title
        val now = System.currentTimeMillis()
        holder.itemView.isSelected = now in p.startMs until p.stopMs
    }

    class VH(v: View) : RecyclerView.ViewHolder(v) {
        val time: TextView = v.findViewById(R.id.epg_time)
        val title: TextView = v.findViewById(R.id.epg_title)
    }
}
