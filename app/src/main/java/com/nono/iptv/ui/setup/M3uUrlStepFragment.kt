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

/** Screen 2 (M3U): a playlist name and a direct .m3u URL. */
class M3uUrlStepFragment : GuidedStepSupportFragment() {

    private var loading = false
    private val values = mutableMapOf<Long, String>()

    override fun onCreateGuidance(savedInstanceState: Bundle?): Guidance = Guidance(
        "M3U / URL",
        "Collez le lien direct de votre playlist.",
        "nono app",
        null
    )

    override fun onCreateActions(actions: MutableList<GuidedAction>, savedInstanceState: Bundle?) {
        actions.add(
            GuidedAction.Builder(requireContext())
                .id(ID_NAME).title("").description("Nom de la playlist").editable(true).build()
        )
        actions.add(
            GuidedAction.Builder(requireContext())
                .id(ID_URL).title("").description("URL (http://…/playlist.m3u)")
                .editable(true).editInputType(InputType.TYPE_TEXT_VARIATION_URI).build()
        )
        actions.add(
            GuidedAction.Builder(requireContext()).id(ID_ADD).title("Ajouter").hasNext(true).build()
        )
    }

    override fun onGuidedActionEditedAndProceed(action: GuidedAction): Long {
        capture(action)
        return GuidedAction.ACTION_ID_NEXT
    }

    override fun onGuidedActionEditCanceled(action: GuidedAction) = capture(action)

    private fun capture(action: GuidedAction) {
        values[action.id] = (action.editTitle ?: action.title)?.toString()?.trim().orEmpty()
    }

    override fun onGuidedActionClicked(action: GuidedAction) {
        if (action.id != ID_ADD || loading) return
        listOf(ID_NAME, ID_URL).forEach { id -> findActionById(id)?.let { capture(it) } }

        val name = values[ID_NAME].orEmpty()
        val url = values[ID_URL].orEmpty()
        if (url.isBlank()) {
            Toast.makeText(requireContext(), "Veuillez entrer une URL.", Toast.LENGTH_SHORT).show()
            return
        }
        loading = true
        Toast.makeText(requireContext(), "Chargement…", Toast.LENGTH_SHORT).show()
        lifecycleScope.launch {
            val result = NonoApp.repository().importM3u(name, url)
            loading = false
            result.onSuccess { (activity as? SetupActivity)?.finishToMain() }
                .onFailure { e -> Toast.makeText(requireContext(), e.message ?: "Échec du chargement", Toast.LENGTH_LONG).show() }
        }
    }

    companion object {
        private const val ID_NAME = 20L
        private const val ID_URL = 21L
        private const val ID_ADD = 22L
    }
}
