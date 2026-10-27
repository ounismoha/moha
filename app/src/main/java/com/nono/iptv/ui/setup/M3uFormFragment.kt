package com.nono.iptv.ui.setup

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.nono.iptv.NonoApp
import com.nono.iptv.R
import kotlinx.coroutines.launch

/** M3U playlist import using real EditText fields. */
class M3uFormFragment : Fragment(R.layout.fragment_m3u_form) {

    private var loading = false

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val name: EditText = view.findViewById(R.id.et_m3u_name)
        val url: EditText = view.findViewById(R.id.et_m3u_url)
        val add: Button = view.findViewById(R.id.btn_add)
        val status: TextView = view.findViewById(R.id.tv_m3u_status)

        add.setOnClickListener {
            if (loading) return@setOnClickListener
            val u = url.text.toString().trim()
            val n = name.text.toString().trim()
            if (u.isBlank()) {
                status.text = "Veuillez entrer une URL."
                return@setOnClickListener
            }
            loading = true
            status.text = "Chargement…"
            lifecycleScope.launch {
                val result = NonoApp.repository().importM3u(n, u)
                loading = false
                result.onSuccess {
                    (activity as? SetupActivity)?.finishToMain()
                }.onFailure {
                    status.text = it.message ?: "Échec du chargement"
                }
            }
        }
    }
}
