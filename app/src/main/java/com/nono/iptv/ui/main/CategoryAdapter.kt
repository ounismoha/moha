package com.nono.iptv.ui.main

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.nono.iptv.R

/** One category row ("Favoris", "All", or a provider category). */
data class CatItem(val remoteId: String, val label: String)

class CategoryAdapter(
    private val onSelected: (CatItem) -> Unit
) : RecyclerView.Adapter<CategoryAdapter.VH>() {

    private val items = mutableListOf<CatItem>()
    private var selectedPos = 0

    fun submit(list: List<CatItem>) {
        items.clear()
        items.addAll(list)
        selectedPos = 0
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_category, parent, false)
        return VH(v)
    }

    override fun getItemCount() = items.size

    override fun onBindViewHolder(holder: VH, position: Int) {
        val item = items[position]
        holder.name.text = item.label
        holder.itemView.isSelected = position == selectedPos
        // Selecting on focus mirrors TiViMate: the middle pane drives the right pane.
        holder.itemView.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                selectedPos = holder.bindingAdapterPosition
                onSelected(item)
            }
        }
        holder.itemView.setOnClickListener { onSelected(item) }
    }

    class VH(v: View) : RecyclerView.ViewHolder(v) {
        val name: TextView = v.findViewById(R.id.category_name)
    }
}
