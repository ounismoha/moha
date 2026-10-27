package com.nono.iptv.ui.setup

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.nono.iptv.NonoApp
import com.nono.iptv.R
import kotlinx.coroutines.launch

/**
 * Xtream login using real EditText fields — `editText.text` always reflects what the user typed,
 * so there is none of the GuidedStep "typed text not synced until focus change" problem.
 */
class XtreamFormFragment : Fragment(R.layout.fragment_xtream_form) {

    private var connecting = false

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val name: EditText = view.findViewById(R.id.et_name)
        val server: EditText = view.findViewById(R.id.et_server)
        val user: EditText = view.findViewById(R.id.et_user)
        val pass: EditText = view.findViewById(R.id.et_pass)
        val live: CheckBox = view.findViewById(R.id.cb_live)
        val vod: CheckBox = view.findViewById(R.id.cb_vod)
        val connect: Button = view.findViewById(R.id.btn_connect)
        val status: TextView = view.findViewById(R.id.tv_status)

        connect.setOnClickListener {
            if (connecting) return@setOnClickListener
            val s = server.text.toString().trim()
            val u = user.text.toString().trim()
            val p = pass.text.toString().trim()
            val n = name.text.toString().trim()

            if (s.isBlank() || u.isBlank() || p.isBlank()) {
                status.text = "Veuillez remplir le serveur, l'utilisateur et le mot de passe."
                return@setOnClickListener
            }

            connecting = true
            status.text = "Connexion…"
            lifecycleScope.launch {
                val result = NonoApp.repository().importXtream(
                    name = n, server = s, username = u, password = p,
                    includeLive = live.isChecked, includeVod = vod.isChecked
                )
                connecting = false
                result.onSuccess {
                    (activity as? SetupActivity)?.finishToMain()
                }.onFailure {
                    status.text = it.message ?: "Échec de la connexion"
                }
            }
        }
    }
}
