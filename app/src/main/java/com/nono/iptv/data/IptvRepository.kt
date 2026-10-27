package com.nono.iptv.data

import android.content.Context
import com.nono.iptv.data.entity.CategoryEntity
import com.nono.iptv.data.entity.CategoryKind
import com.nono.iptv.data.entity.ChannelEntity
import com.nono.iptv.data.entity.PlaylistEntity
import com.nono.iptv.data.entity.PlaylistType
import com.nono.iptv.data.entity.ProgramEntity
import com.nono.iptv.data.entity.VodEntity
import com.nono.iptv.data.entity.VodKind
import com.nono.iptv.net.XtreamClient
import com.nono.iptv.parser.M3uParser
import com.nono.iptv.util.DebugLog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

/**
 * Single entry point for all playlist data: imports (Xtream / M3U) and the grouped queries the
 * UI needs. Backed by Room; network work runs on [Dispatchers.IO].
 */
class IptvRepository(context: Context) {

    private val db = AppDatabase.get(context)
    private val playlists = db.playlistDao()
    private val categories = db.categoryDao()
    private val channels = db.channelDao()
    private val vod = db.vodDao()
    private val programs = db.programDao()

    private val http = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(45, TimeUnit.SECONDS)
        .build()

    // ---- state ---------------------------------------------------------------

    suspend fun hasAnyPlaylist(): Boolean = playlists.count() > 0

    suspend fun allPlaylists(): List<PlaylistEntity> = playlists.getAll()

    /** The playlist the main UI shows (first configured for now). */
    suspend fun activePlaylist(): PlaylistEntity? = playlists.getAll().firstOrNull()

    suspend fun deletePlaylist(id: Long) {
        channels.clearForPlaylist(id)
        vod.clearForPlaylist(id)
        categories.clearForPlaylist(id)
        playlists.delete(id)
    }

    // ---- queries for the UI --------------------------------------------------

    suspend fun liveCategories(playlistId: Long) = categories.getForKind(playlistId, CategoryKind.LIVE)
    suspend fun vodCategories(playlistId: Long) = categories.getForKind(playlistId, CategoryKind.VOD)
    suspend fun seriesCategories(playlistId: Long) = categories.getForKind(playlistId, CategoryKind.SERIES)

    suspend fun channelsIn(playlistId: Long, categoryRemoteId: String): List<ChannelEntity> =
        when (categoryRemoteId) {
            CAT_ALL -> channels.getAll(playlistId)
            CAT_FAVORITES -> channels.getFavorites(playlistId)
            else -> channels.getByCategory(playlistId, categoryRemoteId)
        }

    suspend fun searchChannels(playlistId: Long, query: String) = channels.search(playlistId, query)

    suspend fun moviesIn(playlistId: Long, categoryRemoteId: String): List<VodEntity> =
        if (categoryRemoteId == CAT_ALL) vod.getAllForKind(playlistId, VodKind.MOVIE)
        else vod.getByCategory(playlistId, VodKind.MOVIE, categoryRemoteId)

    suspend fun seriesIn(playlistId: Long, categoryRemoteId: String): List<VodEntity> =
        if (categoryRemoteId == CAT_ALL) vod.getAllForKind(playlistId, VodKind.SERIES)
        else vod.getByCategory(playlistId, VodKind.SERIES, categoryRemoteId)

    suspend fun toggleFavorite(channel: ChannelEntity) =
        channels.setFavorite(channel.id, !channel.isFavorite)

    // ---- imports -------------------------------------------------------------

    /** Authenticate and import an Xtream account. Returns the new playlist id. */
    suspend fun importXtream(
        name: String,
        server: String,
        username: String,
        password: String,
        includeLive: Boolean,
        includeVod: Boolean
    ): Result<Long> = withContext(Dispatchers.IO) {
        try {
            val client = XtreamClient(server, username, password)
            DebugLog.d("Import", "xtream auth → ${client.server} user=$username")
            if (!client.authenticate()) {
                DebugLog.e("Import", "xtream auth failed for ${client.server}")
                return@withContext Result.failure(IllegalStateException("Authentication failed — check server, username and password."))
            }

            val playlistId = playlists.insert(
                PlaylistEntity(
                    name = name.ifBlank { client.server.substringAfter("://") },
                    type = PlaylistType.XTREAM,
                    serverUrl = client.server,
                    username = username,
                    password = password,
                    includeLive = includeLive,
                    includeVod = includeVod
                )
            )

            val catRows = ArrayList<CategoryEntity>()

            if (includeLive) {
                client.liveCategories().forEachIndexed { i, c ->
                    catRows.add(CategoryEntity(playlistId = playlistId, remoteId = c.categoryId, name = c.name, kind = CategoryKind.LIVE, sortOrder = i))
                }
                val chRows = client.liveStreams().map { s ->
                    ChannelEntity(
                        playlistId = playlistId,
                        categoryRemoteId = s.categoryId,
                        streamId = s.streamId,
                        channelNumber = s.num,
                        name = s.name,
                        logoUrl = s.logo,
                        streamUrl = client.liveUrl(s.streamId),
                        epgChannelId = s.epgChannelId
                    )
                }
                channels.insertAllChunked(chRows)
            }

            if (includeVod) {
                client.vodCategories().forEachIndexed { i, c ->
                    catRows.add(CategoryEntity(playlistId = playlistId, remoteId = c.categoryId, name = c.name, kind = CategoryKind.VOD, sortOrder = i))
                }
                val movieRows = client.vodStreams().map { m ->
                    VodEntity(
                        playlistId = playlistId,
                        categoryRemoteId = m.categoryId,
                        kind = VodKind.MOVIE,
                        remoteId = m.remoteId,
                        name = m.name,
                        posterUrl = m.poster,
                        rating = m.rating,
                        year = m.year,
                        genre = m.genre,
                        plot = m.plot,
                        cast = m.cast,
                        director = m.director,
                        durationText = m.duration,
                        containerExt = m.containerExt,
                        streamUrl = client.movieUrl(m.remoteId, m.containerExt)
                    )
                }
                vod.insertAllChunked(movieRows)

                client.seriesCategories().forEachIndexed { i, c ->
                    catRows.add(CategoryEntity(playlistId = playlistId, remoteId = c.categoryId, name = c.name, kind = CategoryKind.SERIES, sortOrder = i))
                }
                val seriesRows = client.series().map { s ->
                    VodEntity(
                        playlistId = playlistId,
                        categoryRemoteId = s.categoryId,
                        kind = VodKind.SERIES,
                        remoteId = s.remoteId,
                        name = s.name,
                        posterUrl = s.poster,
                        rating = s.rating,
                        year = s.year,
                        genre = s.genre,
                        plot = s.plot,
                        cast = s.cast,
                        director = s.director,
                        durationText = s.duration,
                        streamUrl = null
                    )
                }
                vod.insertAllChunked(seriesRows)
            }

            categories.insertAll(catRows)
            DebugLog.d("Import", "xtream ok: categories=${catRows.size}")
            Result.success(playlistId)
        } catch (t: Throwable) {
            DebugLog.e("Import", "xtream import failed", t)
            Result.failure(t)
        }
    }

    /** Download and import an M3U playlist. Returns the new playlist id. */
    suspend fun importM3u(name: String, url: String): Result<Long> = withContext(Dispatchers.IO) {
        try {
            val req = Request.Builder().url(url)
                .header("User-Agent", "nono-app/1.0 (Android TV)").build()
            val parsed = http.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) throw IllegalStateException("Playlist request failed: HTTP ${resp.code}")
                val body = resp.body ?: throw IllegalStateException("Empty playlist response")
                body.byteStream().use { M3uParser.parse(it) }
            }

            val playlistId = playlists.insert(
                PlaylistEntity(name = name.ifBlank { "M3U playlist" }, type = PlaylistType.M3U, m3uUrl = url)
            )

            val groups = parsed.map { it.group }.distinct()
            categories.insertAll(groups.mapIndexed { i, g ->
                CategoryEntity(playlistId = playlistId, remoteId = g, name = g, kind = CategoryKind.LIVE, sortOrder = i)
            })

            val chRows = parsed.mapIndexed { i, p ->
                ChannelEntity(
                    playlistId = playlistId,
                    categoryRemoteId = p.group,
                    channelNumber = i + 1,
                    name = p.name,
                    logoUrl = p.logoUrl,
                    streamUrl = p.streamUrl,
                    epgChannelId = p.tvgId
                )
            }
            channels.insertAllChunked(chRows)
            DebugLog.d("Import", "m3u ok: channels=${chRows.size}")
            Result.success(playlistId)
        } catch (t: Throwable) {
            DebugLog.e("Import", "m3u import failed", t)
            Result.failure(t)
        }
    }

    // ---- EPG -----------------------------------------------------------------

    private fun epgIdOf(channel: ChannelEntity): String =
        channel.epgChannelId?.takeIf { it.isNotBlank() } ?: channel.streamId

    /** Program now airing on a channel, from whatever EPG is cached. */
    suspend fun nowProgram(channel: ChannelEntity): ProgramEntity? {
        val id = epgIdOf(channel)
        if (id.isBlank()) return null
        return programs.getNow(id, System.currentTimeMillis())
    }

    /**
     * Programs for a channel (a few hours back through upcoming). For Xtream playlists this
     * fetches and caches the short EPG on first use.
     */
    suspend fun programsForChannel(playlist: PlaylistEntity, channel: ChannelEntity): List<ProgramEntity> =
        withContext(Dispatchers.IO) {
            val id = epgIdOf(channel)
            if (id.isBlank()) return@withContext emptyList()
            val from = System.currentTimeMillis() - 2 * 3600_000L
            var rows = programs.getUpcoming(id, from, 40)
            if (rows.isEmpty() && playlist.type == PlaylistType.XTREAM && channel.streamId.isNotBlank()) {
                try {
                    val client = XtreamClient(playlist.serverUrl, playlist.username, playlist.password)
                    val mapped = client.shortEpg(channel.streamId).map {
                        ProgramEntity(epgChannelId = id, title = it.title, description = it.description, startMs = it.startMs, stopMs = it.stopMs)
                    }
                    if (mapped.isNotEmpty()) {
                        programs.insertAll(mapped)
                        rows = programs.getUpcoming(id, from, 40)
                    }
                } catch (_: Throwable) { /* EPG is best-effort */ }
            }
            rows
        }

    companion object {
        const val CAT_FAVORITES = "__favorites__"
        const val CAT_ALL = "__all__"
    }
}
