package com.nono.iptv.data.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/** The kind of catalog a category belongs to. */
object CategoryKind {
    const val LIVE = "live"
    const val VOD = "vod"
    const val SERIES = "series"
}

/**
 * A category/group within a playlist (Xtream get_*_categories, or M3U group-title).
 * [remoteId] is the provider's category_id (or the group name for M3U).
 */
@Entity(
    tableName = "categories",
    indices = [Index(value = ["playlistId", "kind"]), Index(value = ["remoteId"])]
)
data class CategoryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val playlistId: Long,
    val remoteId: String,
    val name: String,
    val kind: String,                 // CategoryKind.*
    val sortOrder: Int = 0
)
