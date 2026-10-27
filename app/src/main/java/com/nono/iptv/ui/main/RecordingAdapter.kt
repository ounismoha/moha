package com.nono.iptv.ui.main

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.nono.iptv.R
import com.nono.iptv.data.entity.RecordingEntity
import com.nono.iptv.data.entity.RecordingStatus
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Recordings list for the DVR screen. */
class RecordingAdapter(
    private val onClick: (RecordingEntity) -> Unit,
    private val onLongClick: (RecordingEntity) -> Unit
) : RecyclerView.Adapter<RecordingAdapter.VH>() {

    private val items = mutableListOf<RecordingEntity>()
    private val fmt = SimpleDateFormat("d MMM HH:mm", Locale.getDefault())

    fun submit(list: List<RecordingEntity>) {
        items.clear()
        items.addAll(list)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_recording, parent, false)
        return VH(v)
    }

    override fun getItemCount() = items.size

    override fun onBindViewHolder(holder: VH, position: Int) {
        val r = items[position]
        holder.title.text = r.title
        val statusText = when (r.status) {
            RecordingStatus.RECORDING -> "● En cours…"
            RecordingStatus.ERROR -> "Échec"
            else -> "Prêt"
        }
        val mb = r.sizeBytes / (1024.0 * 1024.0)
        holder.subtitle.text = "$statusText  •  %.0f Mo  •  %s".format(mb, fmt.format(Date(r.startedAt)))
        holder.itemView.setOnClickListener { onClick(r) }
        holder.itemView.setOnLongClickListener { onLongClick(r); true }
    }

    class VH(v: View) : RecyclerView.ViewHolder(v) {
        val title: TextView = v.findViewById(R.id.rec_title)
        val subtitle: TextView = v.findViewById(R.id.rec_subtitle)
    }
}
