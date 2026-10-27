package com.nono.iptv.ui.setup

import android.os.Bundle
import androidx.leanback.app.GuidedStepSupportFragment
import androidx.leanback.widget.GuidanceStylist.Guidance
import androidx.leanback.widget.GuidedAction

/** Screen 1 of the wizard — "Type de liste de lecture": Xtream Codes or M3U / URL. */
class PlaylistTypeStepFragment : GuidedStepSupportFragment() {

    override fun onCreateGuidance(savedInstanceState: Bundle?): Guidance = Guidance(
        "Type de liste de lecture",
        "Choisissez comment ajouter vos chaînes à nono app.",
        "nono app",
        null
    )

    override fun onCreateActions(actions: MutableList<GuidedAction>, savedInstanceState: Bundle?) {
        actions.add(
            GuidedAction.Builder(requireContext())
                .id(ACTION_XTREAM)
                .title("Xtream Codes")
                .description("Serveur + nom d'utilisateur + mot de passe")
                .build()
        )
        actions.add(
            GuidedAction.Builder(requireContext())
                .id(ACTION_M3U)
                .title("M3U / URL")
                .description("Lien direct vers une playlist .m3u")
                .build()
        )
    }

    override fun onGuidedActionClicked(action: GuidedAction) {
        when (action.id) {
            ACTION_XTREAM -> add(parentFragmentManager, XtreamStepFragment())
            ACTION_M3U -> add(parentFragmentManager, M3uUrlStepFragment())
        }
    }

    companion object {
        private const val ACTION_XTREAM = 1L
        private const val ACTION_M3U = 2L
    }
}
