package com.nono.iptv.data.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * A live TV channel. [streamUrl] is pre-built at import time so playback needs no extra lookup;
 * [epgChannelId] links to [ProgramEntity] rows for now/next program info.
 */
@Entity(
    tableName = "channels",
    indices = [
        Index(value = ["playlistId", "categoryRemoteId"]),
        Index(value = ["streamUrl"], unique = true),
        Index(value = ["isFavorite"])
    ]
)
data class ChannelEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val playlistId: Long,
    val categoryRemoteId: String,
    val streamId: String = "",
    val channelNumber: Int = 0,
    val name: String,
    val logoUrl: String? = null,
    val streamUrl: String,
    val epgChannelId: String? = null,
    val isFavorite: Boolean = false
)
