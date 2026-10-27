package com.nono.iptv.player

import android.content.Context
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.TrackSelectionOverride
import androidx.media3.common.util.UnstableApi
import com.nono.iptv.util.DebugLog
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.okhttp.OkHttpDataSource
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

/**
 * Media3 [ExoPlayer] wrapper tuned for IPTV, including 4K / HDR / Dolby:
 *  - OkHttp HTTP data source with a configurable User-Agent (and redirect following),
 *  - hardware decoders drive 4K, HDR and Dolby Vision (HEVC/VP9/AV1 profiles) on the TV,
 *    with software decoder fallback for robustness,
 *  - audio channel count is NOT constrained to device capabilities, so multichannel
 *    Dolby Digital / Dolby Digital Plus (AC-3 / E-AC-3 / Atmos) can pass through to a
 *    receiver or soundbar instead of being downmixed to stereo,
 *  - no video resolution cap, so the best (up to 4K) track is selected,
 *  - live-tuned buffering.
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

    private var trackSelector: DefaultTrackSelector? = null
    private var subsOn = false
    private var audioIdx = 0

    fun initialize(): ExoPlayer {
        player?.let { return it }

        val httpFactory = OkHttpDataSource.Factory(okHttpClient).setUserAgent(userAgent)
        val dataSourceFactory = DataSource.Factory {
            DefaultDataSource.Factory(context, httpFactory).createDataSource()
        }

        // Prefer hardware decoders (needed for 4K/HDR/Dolby Vision); fall back to software if needed.
        val renderersFactory = DefaultRenderersFactory(context)
            .setEnableDecoderFallback(true)
            .setExtensionRendererMode(DefaultRenderersFactory.EXTENSION_RENDERER_MODE_OFF)

        // Allow best video (up to 4K) and let Dolby multichannel pass through un-downmixed.
        val selector = DefaultTrackSelector(context).apply {
            setParameters(
                buildUponParameters()
                    .setConstrainAudioChannelCountToDeviceCapabilities(false)
                    .clearVideoSizeConstraints()
                    .build()
            )
        }
        trackSelector = selector

        val loadControl = DefaultLoadControl.Builder()
            .setBufferDurationsMs(15_000, 50_000, 2_500, 5_000)
            .setPrioritizeTimeOverSizeThresholds(true)
            .build()

        return ExoPlayer.Builder(context, renderersFactory)
            .setMediaSourceFactory(DefaultMediaSourceFactory(dataSourceFactory))
            .setTrackSelector(selector)
            .setLoadControl(loadControl)
            .build()
            .also {
                it.playWhenReady = true
                it.addListener(object : Player.Listener {
                    override fun onPlayerError(error: PlaybackException) {
                        DebugLog.e("Player", "error ${error.errorCodeName}: ${error.message}", error)
                    }
                })
                player = it
            }
    }

    /** Switch to a new stream URL, reusing the existing player instance. */
    fun play(streamUrl: String) {
        DebugLog.d("Player", "play $streamUrl")
        val p = player ?: initialize()
        p.setMediaItem(MediaItem.fromUri(streamUrl))
        p.prepare()
        p.playWhenReady = true
    }

    /** Toggle subtitle (text) tracks on/off. Returns the new state. */
    fun toggleSubtitles(): Boolean {
        val sel = trackSelector ?: return false
        subsOn = !subsOn
        sel.setParameters(
            sel.buildUponParameters()
                .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, !subsOn)
                .setSelectUndeterminedTextLanguage(subsOn)
                .build()
        )
        return subsOn
    }

    /** Cycle to the next available audio track. Returns a short label for it. */
    fun cycleAudioTrack(): String {
        val p = player ?: return "Audio"
        val groups = p.currentTracks.groups.filter { it.type == C.TRACK_TYPE_AUDIO }
        if (groups.isEmpty()) return "Audio"
        audioIdx = (audioIdx + 1) % groups.size
        val group = groups[audioIdx].mediaTrackGroup
        p.trackSelectionParameters = p.trackSelectionParameters.buildUpon()
            .setOverrideForType(TrackSelectionOverride(group, 0))
            .build()
        val f = group.getFormat(0)
        return f.label ?: f.language?.uppercase() ?: "Audio ${audioIdx + 1}"
    }

    fun stop() { player?.stop() }

    fun release() {
        player?.release()
        player = null
        trackSelector = null
    }
}
