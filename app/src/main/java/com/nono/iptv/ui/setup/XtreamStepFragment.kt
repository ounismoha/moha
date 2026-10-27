package com.nono.iptv.ui.setup

import android.os.Bundle
import android.text.InputType
import android.widget.Toast
import androidx.leanback.app.GuidedStepSupportFragment
import androidx.leanback.widget.GuidanceStylist.Guidance
import androidx.leanback.widget.GuidedAction
import androidx.lifecycle.lifecycleScope
import com.nono.iptv.NonoApp
import kotlinx.coroutines.launch

/** Screen 2 (Xtream): server, username, password, include-live / include-VOD, connect. */
class XtreamStepFragment : GuidedStepSupportFragment() {

    private var connecting = false

    override fun onCreateGuidance(savedInstanceState: Bundle?): Guidance = Guidance(
        "Xtream Codes",
        "Entrez les informations de votre abonnement.",
        "nono app",
        null
    )

    override fun onCreateActions(actions: MutableList<GuidedAction>, savedInstanceState: Bundle?) {
        actions.add(
            GuidedAction.Builder(requireContext())
                .id(ID_SERVER)
                .title("")
                .description("Adresse du serveur (http://host:port)")
                .editable(true)
                .editInputType(InputType.TYPE_TEXT_VARIATION_URI)
                .build()
        )
        actions.add(
            GuidedAction.Builder(requireContext())
                .id(ID_USER)
                .title("")
                .description("Nom d'utilisateur")
                .editable(true)
                .build()
        )
        actions.add(
            GuidedAction.Builder(requireContext())
                .id(ID_PASS)
                .title("")
                .description("Mot de passe")
                .editable(true)
                .editInputType(InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD)
                .build()
        )
        actions.add(
            GuidedAction.Builder(requireContext())
                .id(ID_LIVE)
                .title("Inclure les chaînes de télévision")
                .checkSetId(GuidedAction.CHECKBOX_CHECK_SET_ID)
                .checked(true)
                .build()
        )
        actions.add(
            GuidedAction.Builder(requireContext())
                .id(ID_VOD)
                .title("Inclure la VOD (Films & Séries)")
                .checkSetId(GuidedAction.CHECKBOX_CHECK_SET_ID)
                .checked(true)
                .build()
        )
        actions.add(
            GuidedAction.Builder(requireContext())
                .id(ID_CONNECT)
                .title("Se connecter")
                .hasNext(true)
                .build()
        )
    }

    override fun onGuidedActionClicked(action: GuidedAction) {
        if (action.id != ID_CONNECT || connecting) return

        val server = text(ID_SERVER)
        val user = text(ID_USER)
        val pass = text(ID_PASS)
        val includeLive = findActionById(ID_LIVE)?.isChecked ?: true
        val includeVod = findActionById(ID_VOD)?.isChecked ?: true

        if (server.isBlank() || user.isBlank() || pass.isBlank()) {
            toast("Veuillez remplir le serveur, l'utilisateur et le mot de passe.")
            return
        }

        connecting = true
        toast("Connexion…")
        lifecycleScope.launch {
            val result = NonoApp.repository().importXtream(
                name = "", server = server, username = user, password = pass,
                includeLive = includeLive, includeVod = includeVod
            )
            connecting = false
            result.onSuccess {
                toast("Connecté ✓")
                (activity as? SetupActivity)?.finishToMain()
            }.onFailure { e ->
                toast(e.message ?: "Échec de la connexion")
            }
        }
    }

    private fun text(id: Long): String = findActionById(id)?.title?.toString()?.trim().orEmpty()

    private fun toast(msg: String) {
        Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show()
    }

    companion object {
        private const val ID_SERVER = 10L
        private const val ID_USER = 11L
        private const val ID_PASS = 12L
        private const val ID_LIVE = 13L
        private const val ID_VOD = 14L
        private const val ID_CONNECT = 15L
    }
}
