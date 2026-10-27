package com.nono.iptv.data

import android.content.Context
import com.nono.iptv.parser.M3uParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.InputStream
import java.util.concurrent.TimeUnit

/**
 * Loads playlists (from a URL or a local stream) into the Room database and
 * exposes grouped channel queries for the UI.
 */
class PlaylistRepository(context: Context) {

    private val dao = AppDatabase.get(context).channelDao()

    private val http = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    suspend fun channelCount(): Int = dao.count()

    /** Returns channels grouped by [ChannelEntity.groupName], preserving group order. */
    suspend fun channelsByGroup(): Map<String, List<ChannelEntity>> = withContext(Dispatchers.IO) {
        dao.getAll().groupBy { it.groupName }
    }

    suspend fun toggleFavorite(channel: ChannelEntity) = withContext(Dispatchers.IO) {
        dao.setFavorite(channel.id, !channel.isFavorite)
    }

    /**
     * Download an M3U playlist over HTTP(S), parse it line-by-line, and replace the
     * channel table. [userAgent] is forwarded because many IPTV providers gate on it.
     */
    suspend fun importFromUrl(
        url: String,
        userAgent: String = DEFAULT_USER_AGENT
    ): Int = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url(url)
            .header("User-Agent", userAgent)
            .build()

        http.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw java.io.IOException("Playlist request failed: HTTP ${response.code}")
            }
            val body = response.body ?: throw java.io.IOException("Empty playlist response")
            body.byteStream().use { stream -> importFromStream(stream) }
        }
    }

    /** Parse an already-open stream (local file or network) and replace the table. */
    suspend fun importFromStream(stream: InputStream): Int = withContext(Dispatchers.IO) {
        val channels = M3uParser.parse(stream)
        dao.replaceAll(channels)
        channels.size
    }

    companion object {
        const val DEFAULT_USER_AGENT = "nono-app/1.0 (Android TV)"

        // A public, legal aggregator playlist, handy for first-run verification.
        const val DEFAULT_PLAYLIST_URL = "https://iptv-org.github.io/iptv/index.m3u"
    }
}
