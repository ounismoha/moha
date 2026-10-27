package com.nono.iptv.ui.main

import android.app.AlertDialog
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.nono.iptv.R
import com.nono.iptv.data.entity.RecordingEntity
import com.nono.iptv.data.entity.RecordingStatus
import com.nono.iptv.dvr.RecordingManager
import kotlinx.coroutines.launch
import java.io.File

/** DVR library: lists recordings, plays finished ones, stops an active recording, deletes. */
class DvrFragment : Fragment(R.layout.fragment_dvr) {

    private lateinit var adapter: RecordingAdapter
    private lateinit var stopButton: Button
    private lateinit var empty: TextView

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val list: RecyclerView = view.findViewById(R.id.dvr_list)
        stopButton = view.findViewById(R.id.dvr_stop)
        empty = view.findViewById(R.id.dvr_empty)

        adapter = RecordingAdapter(onClick = { playRecording(it) }, onLongClick = { confirmDelete(it) })
        list.layoutManager = LinearLayoutManager(requireContext())
        list.adapter = adapter

        stopButton.setOnClickListener {
            RecordingManager.stop(requireContext())
            Toast.makeText(requireContext(), "Enregistrement arrêté", Toast.LENGTH_SHORT).show()
            view.postDelayed({ refresh() }, 800)
        }

        refresh()
    }

    override fun onResume() {
        super.onResume()
        refresh()
    }

    private fun refresh() {
        lifecycleScope.launch {
            val recordings = RecordingManager.list(requireContext())
            adapter.submit(recordings)
            empty.visibility = if (recordings.isEmpty()) View.VISIBLE else View.GONE
            stopButton.visibility =
                if (recordings.any { it.status == RecordingStatus.RECORDING }) View.VISIBLE else View.GONE
        }
    }

    private fun playRecording(rec: RecordingEntity) {
        if (rec.status == RecordingStatus.RECORDING) {
            Toast.makeText(requireContext(), "Enregistrement en cours…", Toast.LENGTH_SHORT).show()
            return
        }
        val file = File(rec.filePath)
        if (!file.exists() || file.length() == 0L) {
            Toast.makeText(requireContext(), "Fichier introuvable", Toast.LENGTH_SHORT).show()
            return
        }
        (activity as? MainActivity)?.apply {
            playStream(Uri.fromFile(file).toString())
            enterFullscreen()
        }
    }

    private fun confirmDelete(rec: RecordingEntity) {
        AlertDialog.Builder(requireContext())
            .setTitle(rec.title)
            .setMessage("Supprimer cet enregistrement ?")
            .setNegativeButton("Annuler", null)
            .setPositiveButton("Supprimer") { _, _ ->
                lifecycleScope.launch {
                    RecordingManager.delete(requireContext(), rec)
                    refresh()
                }
            }
            .show()
    }
}
