package com.nono.iptv.data.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/** VOD item kind. Movies are directly playable; series expand into seasons/episodes. */
object VodKind {
    const val MOVIE = "movie"
    const val SERIES = "series"
}

/**
 * A movie or a series entry. For a movie, [streamUrl] is playable directly; for a series,
 * [remoteId] is the series_id used to fetch seasons/episodes on demand.
 */
@Entity(
    tableName = "vod",
    indices = [Index(value = ["playlistId", "kind", "categoryRemoteId"])]
)
data class VodEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val playlistId: Long,
    val categoryRemoteId: String,
    val kind: String,                 // VodKind.*
    val remoteId: String,             // stream_id (movie) or series_id (series)
    val name: String,
    val posterUrl: String? = null,
    val rating: String? = null,
    val year: String? = null,
    val durationText: String? = null,
    val genre: String? = null,
    val director: String? = null,
    val cast: String? = null,
    val plot: String? = null,
    val containerExt: String = "mp4",
    val streamUrl: String? = null     // built for movies; null for series
)
