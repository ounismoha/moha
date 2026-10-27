package com.nono.iptv.ui.main

import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.nono.iptv.R

/** Simple "coming soon" pane for sections not yet built out (Recordings, My List). */
class PlaceholderFragment : Fragment(R.layout.fragment_placeholder) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        view.findViewById<TextView>(R.id.tv_placeholder).text =
            arguments?.getString(ARG_TEXT) ?: getString(R.string.no_content)
    }

    companion object {
        private const val ARG_TEXT = "text"
        fun of(text: String) = PlaceholderFragment().apply {
            arguments = Bundle().apply { putString(ARG_TEXT, text) }
        }
    }
}
