package com.nono.iptv.data.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * An EPG program entry (XMLTV / Xtream short EPG). [startMs]/[stopMs] are epoch millis so the
 * UI can compute now/next and a progress bar for the current show.
 */
@Entity(
    tableName = "programs",
    indices = [Index(value = ["epgChannelId", "startMs"])]
)
data class ProgramEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val epgChannelId: String,
    val title: String,
    val description: String? = null,
    val startMs: Long,
    val stopMs: Long
)
