package com.nono.iptv.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * A single IPTV channel parsed from an M3U playlist.
 *
 * [streamUrl] is unique: re-importing the same playlist replaces rather than duplicates channels.
 */
@Entity(
    tableName = "channels",
    indices = [
        Index(value = ["streamUrl"], unique = true),
        Index(value = ["groupName"]),
        Index(value = ["isFavorite"])
    ]
)
data class ChannelEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val tvgId: String? = null,
    val name: String,
    val logoUrl: String? = null,
    val groupName: String,
    val streamUrl: String,
    val isFavorite: Boolean = false,
    val channelNumber: Int = 0
)
