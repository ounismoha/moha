package com.nono.iptv.ui.settings

import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.nono.iptv.R
import com.nono.iptv.util.DebugLog

/** Diagnostics screen: shows device info + the in-app log so the user can screenshot errors. */
class DebugFragment : Fragment(R.layout.fragment_debug) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val text: TextView = view.findViewById(R.id.debug_text)
        val refresh: Button = view.findViewById(R.id.btn_refresh)
        val clear: Button = view.findViewById(R.id.btn_clear)

        fun render() {
            val header = buildString {
                append("nono app 1.0.0\n")
                append(Build.MANUFACTURER).append(' ').append(Build.MODEL).append('\n')
                append("Android ").append(Build.VERSION.RELEASE)
                append(" (SDK ").append(Build.VERSION.SDK_INT).append(")\n")
                append("ABIs: ").append(Build.SUPPORTED_ABIS.joinToString()).append("\n\n")
            }
            val log = DebugLog.snapshot().ifBlank { "(aucun journal pour l'instant)" }
            text.text = header + log
        }

        refresh.setOnClickListener { render() }
        clear.setOnClickListener { DebugLog.clear(); render() }
        render()
    }
}
