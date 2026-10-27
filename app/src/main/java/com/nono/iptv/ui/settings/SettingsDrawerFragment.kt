package com.nono.iptv.ui.settings

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.nono.iptv.NonoApp
import com.nono.iptv.R
import com.nono.iptv.ui.setup.SetupActivity
import kotlinx.coroutines.launch

/** Slide-in right drawer with the settings sections and playlist management. */
class SettingsDrawerFragment : Fragment(R.layout.settings_drawer) {

    private lateinit var title: TextView
    private lateinit var adapter: SettingsAdapter

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        title = view.findViewById(R.id.drawer_title)
        val list: RecyclerView = view.findViewById(R.id.drawer_list)
        adapter = SettingsAdapter()
        list.layoutManager = LinearLayoutManager(requireContext())
        list.adapter = adapter
        showSections()
    }

    private fun showSections() {
        title.text = getString(R.string.nav_settings)
        adapter.submit(
            listOf(
                SettingItem(getString(R.string.settings_general)) { soon() },
                SettingItem(getString(R.string.settings_playlists)) { showPlaylists() },
                SettingItem(getString(R.string.settings_epg)) { soon() },
                SettingItem(getString(R.string.settings_appearance)) { soon() },
                SettingItem(getString(R.string.settings_player)) { soon() },
                SettingItem(getString(R.string.settings_remote)) { soon() },
                SettingItem(getString(R.string.settings_parental)) { soon() },
                SettingItem(getString(R.string.settings_advanced)) { soon() },
                SettingItem(getString(R.string.settings_about)) {
                    Toast.makeText(requireContext(), "nono app 1.0", Toast.LENGTH_SHORT).show()
                }
            )
        )
    }

    private fun showPlaylists() {
        title.text = getString(R.string.settings_playlists)
        lifecycleScope.launch {
            val playlists = NonoApp.repository().allPlaylists()
            val rows = playlists.map { pl ->
                SettingItem("${pl.name}  (${pl.type})") {
                    Toast.makeText(requireContext(), pl.name, Toast.LENGTH_SHORT).show()
                }
            }.toMutableList()
            rows.add(SettingItem("＋ ${getString(R.string.add_playlist)}") {
                startActivity(Intent(requireContext(), SetupActivity::class.java))
            })
            adapter.submit(rows)
        }
    }

    private fun soon() {
        Toast.makeText(requireContext(), "Bientôt disponible", Toast.LENGTH_SHORT).show()
    }
}
