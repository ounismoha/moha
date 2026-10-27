package com.nono.iptv.ui.setup

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import androidx.fragment.app.Fragment
import com.nono.iptv.R
import com.nono.iptv.ui.settings.DebugActivity

/** Setup step 1: choose the playlist type (plain buttons — reliable focus/click on TV). */
class PlaylistTypeFragment : Fragment(R.layout.fragment_playlist_type) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        view.findViewById<Button>(R.id.btn_qr).setOnClickListener {
            (activity as? SetupActivity)?.showFragment(QrSetupFragment())
        }
        view.findViewById<Button>(R.id.btn_xtream).setOnClickListener {
            (activity as? SetupActivity)?.showFragment(XtreamFormFragment())
        }
        view.findViewById<Button>(R.id.btn_m3u).setOnClickListener {
            (activity as? SetupActivity)?.showFragment(M3uFormFragment())
        }
        view.findViewById<Button>(R.id.btn_debug).setOnClickListener {
            startActivity(Intent(requireContext(), DebugActivity::class.java))
        }
        view.findViewById<Button>(R.id.btn_xtream).requestFocus()
    }
}
