package com.nono.iptv.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/** Playlist types nono app can load. */
object PlaylistType {
    const val XTREAM = "xtream"
    const val M3U = "m3u"
}

/**
 * A configured source. For Xtream it holds server/user/pass; for M3U it holds the URL.
 * Stream URLs for Xtream content are built from [serverUrl] + [username]/[password] + stream id.
 */
@Entity(tableName = "playlists")
data class PlaylistEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val type: String,                 // PlaylistType.*
    val serverUrl: String = "",       // normalized scheme://host:port  (Xtream)
    val username: String = "",
    val password: String = "",
    val m3uUrl: String = "",          // M3U playlists
    val includeLive: Boolean = true,
    val includeVod: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
)
