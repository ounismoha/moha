package com.nono.iptv.ui.settings

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.nono.iptv.R

/** A row in the settings drawer. */
data class SettingItem(val label: String, val onClick: () -> Unit)

class SettingsAdapter : RecyclerView.Adapter<SettingsAdapter.VH>() {

    private val items = mutableListOf<SettingItem>()

    fun submit(list: List<SettingItem>) {
        items.clear()
        items.addAll(list)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_setting, parent, false)
        return VH(v)
    }

    override fun getItemCount() = items.size

    override fun onBindViewHolder(holder: VH, position: Int) {
        val item = items[position]
        holder.name.text = item.label
        holder.itemView.setOnClickListener { item.onClick() }
    }

    class VH(v: View) : RecyclerView.ViewHolder(v) {
        val name: TextView = v.findViewById(R.id.setting_name)
    }
}
