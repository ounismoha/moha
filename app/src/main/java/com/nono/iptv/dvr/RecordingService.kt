package com.nono.iptv.dvr

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import com.nono.iptv.R
import com.nono.iptv.data.AppDatabase
import com.nono.iptv.data.entity.RecordingStatus
import com.nono.iptv.util.DebugLog
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.util.concurrent.TimeUnit

/**
 * Foreground service that records a live stream to a local file by streaming its bytes to disk.
 * Works for progressive / MPEG-TS streams (the common Xtream live format); the resulting .ts file
 * is directly playable. Stopping cancels the copy and keeps what was captured so far.
 */
class RecordingService : Service() {

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var job: Job? = null

    private val http = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> stopRecording()
            ACTION_START -> {
                val url = intent.getStringExtra(EXTRA_URL).orEmpty()
                val path = intent.getStringExtra(EXTRA_PATH).orEmpty()
                val title = intent.getStringExtra(EXTRA_TITLE).orEmpty()
                val id = intent.getLongExtra(EXTRA_ID, 0)
                if (url.isNotBlank() && path.isNotBlank()) startRecording(id, url, path, title)
                else stopSelf()
            }
            else -> stopSelf()
        }
        return START_NOT_STICKY
    }

    private fun startRecording(id: Long, url: String, path: String, title: String) {
        startForegroundCompat(title.ifBlank { "Enregistrement" })
        activeRecordingId = id
        job = scope.launch {
            val dao = AppDatabase.get(applicationContext).recordingDao()
            val file = File(path)
            var status = RecordingStatus.DONE
            DebugLog.d("DVR", "recording start id=$id → $path")
            try {
                val req = Request.Builder().url(url)
                    .header("User-Agent", "nono-app/1.0 (Android TV)").build()
                http.newCall(req).execute().use { resp ->
                    if (!resp.isSuccessful) throw java.io.IOException("HTTP ${resp.code}")
                    val input = resp.body?.byteStream() ?: throw java.io.IOException("empty body")
                    file.outputStream().use { out ->
                        val buf = ByteArray(64 * 1024)
                        while (isActive) {
                            val n = input.read(buf)
                            if (n < 0) break
                            out.write(buf, 0, n)
                        }
                        out.flush()
                    }
                }
            } catch (c: CancellationException) {
                status = RecordingStatus.DONE // user stopped; keep partial file
            } catch (t: Throwable) {
                status = RecordingStatus.ERROR
                DebugLog.e("DVR", "recording failed id=$id", t)
            } finally {
                withContext(NonCancellable) {
                    dao.finish(id, status, file.length())
                }
                DebugLog.d("DVR", "recording end id=$id status=$status size=${file.length()}")
                activeRecordingId = 0
                stopForegroundCompat()
                stopSelf()
            }
        }
    }

    private fun stopRecording() {
        job?.cancel()
        job = null
    }

    private fun startForegroundCompat(text: String) {
        val mgr = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val ch = NotificationChannel(CHANNEL_ID, "Enregistrements", NotificationManager.IMPORTANCE_LOW)
            mgr.createNotificationChannel(ch)
        }
        val notif: Notification = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Notification.Builder(this, CHANNEL_ID)
        } else {
            @Suppress("DEPRECATION") Notification.Builder(this)
        }
            .setContentTitle("nono app")
            .setContentText(text)
            .setSmallIcon(R.drawable.ic_dvr)
            .setOngoing(true)
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(NOTIF_ID, notif, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
        } else {
            startForeground(NOTIF_ID, notif)
        }
    }

    private fun stopForegroundCompat() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            stopForeground(STOP_FOREGROUND_REMOVE)
        } else {
            @Suppress("DEPRECATION") stopForeground(true)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        job?.cancel()
    }

    companion object {
        const val ACTION_START = "com.nono.iptv.dvr.START"
        const val ACTION_STOP = "com.nono.iptv.dvr.STOP"
        const val EXTRA_URL = "url"
        const val EXTRA_PATH = "path"
        const val EXTRA_TITLE = "title"
        const val EXTRA_ID = "id"
        private const val CHANNEL_ID = "nono_rec"
        private const val NOTIF_ID = 42

        @Volatile
        var activeRecordingId: Long = 0
            private set
    }
}
