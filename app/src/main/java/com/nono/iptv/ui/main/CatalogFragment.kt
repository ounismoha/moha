package com.nono.iptv.ui.main

import android.app.AlertDialog
import android.os.Bundle
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.nono.iptv.NonoApp
import com.nono.iptv.R
import com.nono.iptv.data.IptvRepository
import com.nono.iptv.data.entity.PlaylistEntity
import com.nono.iptv.data.entity.VodEntity
import com.nono.iptv.data.entity.VodKind
import kotlinx.coroutines.launch

/** Movies or Series pane: categories (col 2) + detail header and poster grid (col 3). */
class CatalogFragment : Fragment(R.layout.fragment_catalog) {

    private val repo: IptvRepository get() = NonoApp.repository()
    private var playlist: PlaylistEntity? = null
    private val kind: String get() = arguments?.getString(ARG_KIND) ?: VodKind.MOVIE

    private lateinit var categoryAdapter: CategoryAdapter
    private lateinit var posterAdapter: PosterAdapter
    private lateinit var detailTitle: TextView
    private lateinit var detailMeta: TextView
    private lateinit var detailPeople: TextView
    private lateinit var detailPlot: TextView

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        detailTitle = view.findViewById(R.id.tv_detail_title)
        detailMeta = view.findViewById(R.id.tv_detail_meta)
        detailPeople = view.findViewById(R.id.tv_detail_people)
        detailPlot = view.findViewById(R.id.tv_detail_plot)

        val categoriesView: RecyclerView = view.findViewById(R.id.catalog_categories)
        val postersView: RecyclerView = view.findViewById(R.id.catalog_posters)

        categoryAdapter = CategoryAdapter { cat -> loadPosters(cat.remoteId) }
        posterAdapter = PosterAdapter(onFocused = { showDetail(it) }, onClicked = { play(it) })

        categoriesView.layoutManager = LinearLayoutManager(requireContext())
        categoriesView.adapter = categoryAdapter
        // Three posters per row keeps the grid tidy and legible on TV (no crowding).
        postersView.layoutManager = GridLayoutManager(requireContext(), 3)
        postersView.adapter = posterAdapter

        loadCategories()
    }

    private fun loadCategories() {
        lifecycleScope.launch {
            val pl = repo.activePlaylist() ?: return@launch
            playlist = pl
            val cats = mutableListOf(CatItem(IptvRepository.CAT_ALL, getString(R.string.cat_all)))
            val source = if (kind == VodKind.SERIES) repo.seriesCategories(pl.id) else repo.vodCategories(pl.id)
            source.forEach { cats.add(CatItem(it.remoteId, it.name)) }
            categoryAdapter.submit(cats)
            loadPosters(IptvRepository.CAT_ALL)
        }
    }

    private fun loadPosters(categoryRemoteId: String) {
        val pl = playlist ?: return
        lifecycleScope.launch {
            val items = if (kind == VodKind.SERIES) repo.seriesIn(pl.id, categoryRemoteId)
            else repo.moviesIn(pl.id, categoryRemoteId)
            posterAdapter.submit(items)
            items.firstOrNull()?.let { showDetail(it) }
        }
    }

    private fun showDetail(item: VodEntity) {
        detailTitle.text = item.name
        detailMeta.text = listOfNotNull(
            item.rating?.let { "★ $it" },
            item.year,
            item.durationText,
            item.genre
        ).joinToString("  •  ")
        detailPeople.text = listOfNotNull(
            item.director?.takeIf { it.isNotBlank() }?.let { "Réalisateur : $it" },
            item.cast?.takeIf { it.isNotBlank() }?.let { "Acteurs : $it" }
        ).joinToString("    ")
        detailPlot.text = item.plot ?: ""
    }

    private fun play(item: VodEntity) {
        val url = item.streamUrl
        if (item.kind == VodKind.MOVIE && !url.isNullOrBlank()) {
            (activity as? MainActivity)?.apply {
                playStream(url)
                enterFullscreen()
            }
            return
        }

        // Series: fetch seasons/episodes, then let the user pick one to play.
        val pl = playlist ?: return
        Toast.makeText(requireContext(), "Chargement des épisodes…", Toast.LENGTH_SHORT).show()
        lifecycleScope.launch {
            val episodes = repo.seriesEpisodes(pl, item)
            if (episodes.isEmpty()) {
                Toast.makeText(requireContext(), "Aucun épisode trouvé", Toast.LENGTH_SHORT).show()
                return@launch
            }
            val titles = episodes.map { it.first }.toTypedArray()
            AlertDialog.Builder(requireContext())
                .setTitle(item.name)
                .setItems(titles) { _, which ->
                    (activity as? MainActivity)?.apply {
                        playStream(episodes[which].second)
                        enterFullscreen()
                    }
                }
                .show()
        }
    }

    companion object {
        private const val ARG_KIND = "kind"
        fun of(kind: String) = CatalogFragment().apply {
            arguments = Bundle().apply { putString(ARG_KIND, kind) }
        }
    }
}
