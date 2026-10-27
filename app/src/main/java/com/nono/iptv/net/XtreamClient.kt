package com.nono.iptv.net

import com.nono.iptv.util.DebugLog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * Minimal client for the Xtream Codes "player_api.php" interface used by most IPTV panels.
 * It authenticates and reads the catalog endpoints; responses are parsed with org.json so no
 * extra JSON dependency is needed.
 *
 * Endpoints used:
 *  - player_api.php?username=&password=                     (auth / user_info)
 *  - &action=get_live_categories | get_live_streams
 *  - &action=get_vod_categories  | get_vod_streams
 *  - &action=get_series_categories | get_series
 *
 * Stream URL conventions:
 *  - live:   {server}/live/{user}/{pass}/{stream_id}.{ext}
 *  - movie:  {server}/movie/{user}/{pass}/{stream_id}.{ext}
 *  - series: {server}/series/{user}/{pass}/{episode_id}.{ext}
 */
class XtreamClient(
    rawServer: String,
    private val username: String,
    private val password: String,
    userAgent: String = "nono-app/1.0 (Android TV)"
) {

    /** server normalized to scheme://host[:port] with no trailing slash. */
    val server: String = normalizeServer(rawServer)

    private val http = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(45, TimeUnit.SECONDS)
        .addInterceptor { chain ->
            chain.proceed(chain.request().newBuilder().header("User-Agent", userAgent).build())
        }
        .build()

    data class RemoteCategory(val categoryId: String, val name: String)

    data class RemoteChannel(
        val streamId: String,
        val num: Int,
        val name: String,
        val logo: String?,
        val categoryId: String,
        val epgChannelId: String?,
        val containerExt: String
    )

    data class RemoteVod(
        val remoteId: String,          // stream_id (movie) or series_id (series)
        val name: String,
        val poster: String?,
        val rating: String?,
        val year: String?,
        val categoryId: String,
        val containerExt: String,
        val genre: String?,
        val plot: String?,
        val cast: String?,
        val director: String?,
        val duration: String?
    )

    // ---- auth ----------------------------------------------------------------

    /** Returns true when the panel authenticates the credentials (user_info.auth == 1). */
    suspend fun authenticate(): Boolean = withContext(Dispatchers.IO) {
        val url = apiUrl(null)
        DebugLog.d("Xtream", "GET $url")
        val body = try {
            getBody(url)
        } catch (t: Throwable) {
            DebugLog.e("Xtream", "auth request failed: ${t.message}", t)
            return@withContext false
        }
        val trimmed = body.trim()
        if (!trimmed.startsWith("{")) {
            DebugLog.e("Xtream", "auth: non-JSON response (len=${body.length}): ${trimmed.take(200)}")
            return@withContext false
        }
        val root = JSONObject(trimmed)
        val userInfo = root.optJSONObject("user_info")
        val auth = userInfo?.optInt("auth", 0) ?: 0
        val status = userInfo?.optString("status", "").orEmpty()
        val exp = userInfo?.optString("exp_date", "").orEmpty()
        DebugLog.d("Xtream", "auth=$auth status='$status' exp=$exp")
        // Accept any account the panel authenticates (auth=1), like most IPTV players do;
        // the status is logged for diagnostics but not used to reject.
        userInfo != null && auth == 1
    }

    // ---- live ----------------------------------------------------------------

    suspend fun liveCategories(): List<RemoteCategory> =
        withContext(Dispatchers.IO) { parseCategories(getArray(apiUrl("get_live_categories"))) }

    suspend fun liveStreams(): List<RemoteChannel> = withContext(Dispatchers.IO) {
        val arr = getArray(apiUrl("get_live_streams"))
        (0 until arr.length()).map { i ->
            val o = arr.getJSONObject(i)
            RemoteChannel(
                streamId = o.optString("stream_id"),
                num = o.optInt("num", 0),
                name = o.optString("name"),
                logo = o.optString("stream_icon").ifBlank { null },
                categoryId = o.optString("category_id"),
                epgChannelId = o.optString("epg_channel_id").ifBlank { null },
                containerExt = "ts"
            )
        }
    }

    // ---- vod -----------------------------------------------------------------

    suspend fun vodCategories(): List<RemoteCategory> =
        withContext(Dispatchers.IO) { parseCategories(getArray(apiUrl("get_vod_categories"))) }

    suspend fun vodStreams(): List<RemoteVod> = withContext(Dispatchers.IO) {
        val arr = getArray(apiUrl("get_vod_streams"))
        (0 until arr.length()).map { i ->
            val o = arr.getJSONObject(i)
            RemoteVod(
                remoteId = o.optString("stream_id"),
                name = o.optString("name"),
                poster = o.optString("stream_icon").ifBlank { o.optString("cover").ifBlank { null } },
                rating = o.optString("rating").ifBlank { null },
                year = o.optString("year").ifBlank { null },
                categoryId = o.optString("category_id"),
                containerExt = o.optString("container_extension").ifBlank { "mp4" },
                genre = null, plot = null, cast = null, director = null, duration = null
            )
        }
    }

    // ---- series --------------------------------------------------------------

    suspend fun seriesCategories(): List<RemoteCategory> =
        withContext(Dispatchers.IO) { parseCategories(getArray(apiUrl("get_series_categories"))) }

    suspend fun series(): List<RemoteVod> = withContext(Dispatchers.IO) {
        val arr = getArray(apiUrl("get_series"))
        (0 until arr.length()).map { i ->
            val o = arr.getJSONObject(i)
            RemoteVod(
                remoteId = o.optString("series_id"),
                name = o.optString("name"),
                poster = o.optString("cover").ifBlank { null },
                rating = o.optString("rating").ifBlank { null },
                year = o.optString("releaseDate").take(4).ifBlank { null },
                categoryId = o.optString("category_id"),
                containerExt = "mp4",
                genre = o.optString("genre").ifBlank { null },
                plot = o.optString("plot").ifBlank { null },
                cast = o.optString("cast").ifBlank { null },
                director = o.optString("director").ifBlank { null },
                duration = o.optString("episode_run_time").ifBlank { null }
            )
        }
    }

    // ---- epg (short / now-next) ----------------------------------------------

    data class RemoteProgram(val title: String, val startMs: Long, val stopMs: Long, val description: String?)

    /** Short EPG for one stream via get_simple_data_table (titles/descriptions are base64). */
    suspend fun shortEpg(streamId: String, limit: Int = 24): List<RemoteProgram> = withContext(Dispatchers.IO) {
        val url = "${apiUrl("get_simple_data_table")}&stream_id=$streamId&limit=$limit"
        val arr = getJson(url).optJSONArray("epg_listings") ?: JSONArray()
        (0 until arr.length()).mapNotNull { i ->
            val o = arr.getJSONObject(i)
            val start = o.optString("start_timestamp").toLongOrNull() ?: return@mapNotNull null
            val stop = o.optString("stop_timestamp").toLongOrNull() ?: return@mapNotNull null
            RemoteProgram(
                title = decodeB64(o.optString("title")),
                startMs = start * 1000L,
                stopMs = stop * 1000L,
                description = decodeB64(o.optString("description")).ifBlank { null }
            )
        }
    }

    private fun decodeB64(s: String): String = try {
        if (s.isBlank()) "" else String(android.util.Base64.decode(s, android.util.Base64.DEFAULT), Charsets.UTF_8)
    } catch (e: Exception) { s }

    // ---- series episodes -----------------------------------------------------

    data class RemoteEpisode(val season: Int, val episode: Int, val title: String, val id: String, val ext: String)

    /** Seasons/episodes for a series via get_series_info. */
    suspend fun seriesInfo(seriesId: String): List<RemoteEpisode> = withContext(Dispatchers.IO) {
        val root = getJson("${apiUrl("get_series_info")}&series_id=$seriesId")
        val episodesObj = root.optJSONObject("episodes") ?: return@withContext emptyList()
        val out = ArrayList<RemoteEpisode>()
        episodesObj.keys().forEach { seasonKey ->
            val arr = episodesObj.optJSONArray(seasonKey) ?: return@forEach
            val seasonNum = seasonKey.toIntOrNull() ?: 0
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                val epNum = o.optInt("episode_num", i + 1)
                val title = o.optString("title").ifBlank { "Épisode $epNum" }
                out.add(
                    RemoteEpisode(
                        season = seasonNum,
                        episode = epNum,
                        title = title,
                        id = o.optString("id"),
                        ext = o.optString("container_extension").ifBlank { "mp4" }
                    )
                )
            }
        }
        out.sortWith(compareBy({ it.season }, { it.episode }))
        out
    }

    // ---- url builders --------------------------------------------------------

    fun liveUrl(streamId: String, ext: String = "ts"): String =
        "$server/live/$username/$password/$streamId.$ext"

    fun movieUrl(streamId: String, ext: String): String =
        "$server/movie/$username/$password/$streamId.$ext"

    fun seriesEpisodeUrl(episodeId: String, ext: String): String =
        "$server/series/$username/$password/$episodeId.$ext"

    // ---- helpers -------------------------------------------------------------

    private fun apiUrl(action: String?): String {
        val base = "$server/player_api.php?username=$username&password=$password"
        return if (action == null) base else "$base&action=$action"
    }

    private fun getBody(url: String): String {
        val req = Request.Builder().url(url).build()
        http.newCall(req).execute().use { resp ->
            if (!resp.isSuccessful) throw IOException("HTTP ${resp.code} for $url")
            return resp.body?.string() ?: throw IOException("Empty body for $url")
        }
    }

    private fun getJson(url: String): JSONObject {
        val body = getBody(url).trim()
        return if (body.startsWith("{")) JSONObject(body) else JSONObject()
    }

    private fun getArray(url: String): JSONArray {
        val body = getBody(url).trim()
        return if (body.startsWith("[")) JSONArray(body) else JSONArray()
    }

    private fun parseCategories(arr: JSONArray): List<RemoteCategory> =
        (0 until arr.length()).map { i ->
            val o = arr.getJSONObject(i)
            RemoteCategory(o.optString("category_id"), o.optString("category_name"))
        }

    companion object {
        /** Accepts "host", "host:port", "http://host:port", "https://host/" and normalizes. */
        fun normalizeServer(raw: String): String {
            var s = raw.trim().removeSuffix("/")
            if (!s.startsWith("http://") && !s.startsWith("https://")) s = "http://$s"
            return s
        }
    }
}
