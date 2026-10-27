package com.nono.iptv.player

import android.content.Context
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.okhttp.OkHttpDataSource
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

/**
 * Thin wrapper around a Media3 [ExoPlayer] configured for live IPTV streams:
 *  - OkHttp-backed HTTP data source with a configurable User-Agent (and redirect following),
 *  - a [DefaultLoadControl] tuned for low-latency live playback rather than VOD,
 *  - progressive/HLS handled automatically by [DefaultMediaSourceFactory].
 */
@UnstableApi
class PlayerEngine(
    private val context: Context,
    private var userAgent: String = "nono-app/1.0 (Android TV)"
) {

    private val okHttpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .followRedirects(true)
        .followSslRedirects(true)
        .build()

    var player: ExoPlayer? = null
        private set

    fun initialize(): ExoPlayer {
        player?.let { return it }

        val httpFactory = OkHttpDataSource.Factory(okHttpClient)
            .setUserAgent(userAgent)

        // Fall back to file/content/asset sources via DefaultDataSource, HTTP via OkHttp.
        val dataSourceFactory = DataSource.Factory {
            DefaultDataSource.Factory(context, httpFactory).createDataSource()
        }

        // Live-tuned buffering: keep latency low but survive brief network stalls.
        val loadControl = DefaultLoadControl.Builder()
            .setBufferDurationsMs(
                /* minBufferMs = */ 15_000,
                /* maxBufferMs = */ 50_000,
                /* bufferForPlaybackMs = */ 2_500,
                /* bufferForPlaybackAfterRebufferMs = */ 5_000
            )
            .setPrioritizeTimeOverSizeThresholds(true)
            .build()

        return ExoPlayer.Builder(context)
            .setMediaSourceFactory(DefaultMediaSourceFactory(dataSourceFactory))
            .setLoadControl(loadControl)
            .build()
            .also {
                it.playWhenReady = true
                player = it
            }
    }

    /** Switch to a new stream URL, reusing the existing player instance. */
    fun play(streamUrl: String) {
        val p = player ?: initialize()
        p.setMediaItem(MediaItem.fromUri(streamUrl))
        p.prepare()
        p.playWhenReady = true
    }

    fun stop() {
        player?.stop()
    }

    fun release() {
        player?.release()
        player = null
    }
}
