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
    // Values captured the moment each text field's edit is confirmed — reliable on TV keyboards.
    private val values = mutableMapOf<Long, String>()

    override fun onCreateGuidance(savedInstanceState: Bundle?): Guidance = Guidance(
        "Xtream Codes",
        "Entrez les informations de votre abonnement.",
        "nono app",
        null
    )

    override fun onCreateActions(actions: MutableList<GuidedAction>, savedInstanceState: Bundle?) {
        actions.add(
            GuidedAction.Builder(requireContext())
                .id(ID_NAME).title("").description("Nom de la playlist (facultatif)")
                .editable(true).build()
        )
        actions.add(
            GuidedAction.Builder(requireContext())
                .id(ID_SERVER).title("").description("Adresse du serveur (http://host:port)")
                .editable(true).editInputType(InputType.TYPE_TEXT_VARIATION_URI).build()
        )
        actions.add(
            GuidedAction.Builder(requireContext())
                .id(ID_USER).title("").description("Nom d'utilisateur").editable(true).build()
        )
        actions.add(
            GuidedAction.Builder(requireContext())
                .id(ID_PASS).title("").description("Mot de passe")
                .editable(true)
                .editInputType(InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD)
                .build()
        )
        actions.add(
            GuidedAction.Builder(requireContext())
                .id(ID_LIVE).title("Inclure les chaînes de télévision")
                .checkSetId(GuidedAction.CHECKBOX_CHECK_SET_ID).checked(true).build()
        )
        actions.add(
            GuidedAction.Builder(requireContext())
                .id(ID_VOD).title("Inclure la VOD (Films & Séries)")
                .checkSetId(GuidedAction.CHECKBOX_CHECK_SET_ID).checked(true).build()
        )
        actions.add(
            GuidedAction.Builder(requireContext()).id(ID_CONNECT).title("Se connecter").hasNext(true).build()
        )
    }

    /** Called when a text field's edit is confirmed — capture the value immediately. */
    override fun onGuidedActionEditedAndProceed(action: GuidedAction): Long {
        capture(action)
        return GuidedAction.ACTION_ID_NEXT
    }

    override fun onGuidedActionEditCanceled(action: GuidedAction) {
        capture(action)
    }

    private fun capture(action: GuidedAction) {
        val v = (action.editTitle ?: action.title)?.toString()?.trim().orEmpty()
        values[action.id] = v
    }

    override fun onGuidedActionClicked(action: GuidedAction) {
        if (action.id != ID_CONNECT || connecting) return

        // Capture anything still showing in the fields (in case an edit wasn't "proceeded").
        listOf(ID_NAME, ID_SERVER, ID_USER, ID_PASS).forEach { id -> findActionById(id)?.let { capture(it) } }

        val name = values[ID_NAME].orEmpty()
        val server = values[ID_SERVER].orEmpty()
        val user = values[ID_USER].orEmpty()
        val pass = values[ID_PASS].orEmpty()
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
                name = name, server = server, username = user, password = pass,
                includeLive = includeLive, includeVod = includeVod
            )
            connecting = false
            result.onSuccess {
                toast("Connecté ✓")
                (activity as? SetupActivity)?.finishToMain()
            }.onFailure { e -> toast(e.message ?: "Échec de la connexion") }
        }
    }

    private fun toast(msg: String) = Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show()

    companion object {
        private const val ID_NAME = 9L
        private const val ID_SERVER = 10L
        private const val ID_USER = 11L
        private const val ID_PASS = 12L
        private const val ID_LIVE = 13L
        private const val ID_VOD = 14L
        private const val ID_CONNECT = 15L
    }
}
