package com.nono.iptv.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/** Recording status values. */
object RecordingStatus {
    const val RECORDING = "recording"
    const val DONE = "done"
    const val ERROR = "error"
}

/** A DVR recording: a captured live stream saved to a local file. */
@Entity(tableName = "recordings")
data class RecordingEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val channelName: String,
    val filePath: String,
    val startedAt: Long = System.currentTimeMillis(),
    val sizeBytes: Long = 0,
    val status: String = RecordingStatus.RECORDING
)
