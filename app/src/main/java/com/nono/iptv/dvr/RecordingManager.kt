package com.nono.iptv.dvr

import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import com.nono.iptv.data.AppDatabase
import com.nono.iptv.data.entity.ChannelEntity
import com.nono.iptv.data.entity.RecordingEntity
import com.nono.iptv.data.entity.RecordingStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Starts/stops recordings and exposes the recordings list for the DVR screen. */
object RecordingManager {

    private fun dao(context: Context) = AppDatabase.get(context).recordingDao()

    suspend fun list(context: Context): List<RecordingEntity> =
        withContext(Dispatchers.IO) { dao(context).getAll() }

    suspend fun activeRecording(context: Context): RecordingEntity? =
        withContext(Dispatchers.IO) { dao(context).active() }

    /** Create a recording row + file and start the foreground capture service. */
    suspend fun start(context: Context, channel: ChannelEntity): RecordingEntity =
        withContext(Dispatchers.IO) {
            val dir = File(context.getExternalFilesDir(null), "recordings").apply { mkdirs() }
            val stamp = SimpleDateFormat("yyyyMMdd_HHmm", Locale.US).format(Date())
            val title = "${channel.name} — $stamp"
            val id = dao(context).insert(
                RecordingEntity(title = title, channelName = channel.name, filePath = "", status = RecordingStatus.RECORDING)
            )
            val file = File(dir, "rec_$id.ts")
            dao(context).updatePath(id, file.absolutePath)

            val intent = Intent(context, RecordingService::class.java).apply {
                action = RecordingService.ACTION_START
                putExtra(RecordingService.EXTRA_URL, channel.streamUrl)
                putExtra(RecordingService.EXTRA_PATH, file.absolutePath)
                putExtra(RecordingService.EXTRA_TITLE, title)
                putExtra(RecordingService.EXTRA_ID, id)
            }
            ContextCompat.startForegroundService(context, intent)
            dao(context).get(id) ?: RecordingEntity(id = id, title = title, channelName = channel.name, filePath = file.absolutePath)
        }

    fun stop(context: Context) {
        val intent = Intent(context, RecordingService::class.java).apply {
            action = RecordingService.ACTION_STOP
        }
        ContextCompat.startForegroundService(context, intent)
    }

    suspend fun delete(context: Context, recording: RecordingEntity) = withContext(Dispatchers.IO) {
        runCatching { File(recording.filePath).delete() }
        dao(context).delete(recording.id)
    }
}
