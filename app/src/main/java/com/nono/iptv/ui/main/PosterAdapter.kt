package com.nono.iptv.ui.main

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.nono.iptv.R
import com.nono.iptv.data.entity.VodEntity

/** Movie/series poster grid. Focus updates the detail header; OK plays (movie) or opens (series). */
class PosterAdapter(
    private val onFocused: (VodEntity) -> Unit,
    private val onClicked: (VodEntity) -> Unit
) : RecyclerView.Adapter<PosterAdapter.VH>() {

    private val items = mutableListOf<VodEntity>()

    fun submit(list: List<VodEntity>) {
        items.clear()
        items.addAll(list)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_poster, parent, false)
        return VH(v)
    }

    override fun getItemCount() = items.size

    override fun onBindViewHolder(holder: VH, position: Int) {
        val item = items[position]
        holder.title.text = item.name
        if (!item.rating.isNullOrBlank()) {
            holder.rating.text = item.rating
            holder.rating.visibility = View.VISIBLE
        } else {
            holder.rating.visibility = View.GONE
        }
        if (!item.posterUrl.isNullOrBlank()) {
            Glide.with(holder.image).load(item.posterUrl).placeholder(R.drawable.poster_placeholder)
                .error(R.drawable.poster_placeholder).centerCrop().into(holder.image)
        } else {
            holder.image.setImageResource(R.drawable.poster_placeholder)
        }
        holder.itemView.setOnFocusChangeListener { _, hasFocus -> if (hasFocus) onFocused(item) }
        holder.itemView.setOnClickListener { onClicked(item) }
    }

    class VH(v: View) : RecyclerView.ViewHolder(v) {
        val image: ImageView = v.findViewById(R.id.poster_image)
        val title: TextView = v.findViewById(R.id.poster_title)
        val rating: TextView = v.findViewById(R.id.poster_rating)
    }
}
