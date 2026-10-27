package com.nono.iptv.util

import android.content.Context
import android.util.Log
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Lightweight in-app logger. Keeps the last [MAX] lines in memory and mirrors them to a file so
 * logs survive a crash. The Débogage settings screen shows [snapshot]; the user can screenshot it.
 */
object DebugLog {

    private const val MAX = 600
    private val lines = ArrayDeque<String>()
    private val fmt = SimpleDateFormat("HH:mm:ss.SSS", Locale.US)
    private var logFile: File? = null

    fun init(context: Context) {
        logFile = File(context.filesDir, "nono-debug.log")
    }

    @Synchronized
    fun log(level: String, tag: String, msg: String) {
        val line = "${fmt.format(Date())} $level/$tag: $msg"
        lines.addLast(line)
        while (lines.size > MAX) lines.removeFirst()
        runCatching { logFile?.appendText(line + "\n") }
        Log.println(if (level == "E") Log.ERROR else Log.DEBUG, "nono/$tag", msg)
    }

    fun d(tag: String, msg: String) = log("D", tag, msg)

    fun e(tag: String, msg: String, t: Throwable? = null) {
        val full = if (t == null) msg else "$msg\n${stackTrace(t)}"
        log("E", tag, full)
    }

    private fun stackTrace(t: Throwable): String {
        val sw = StringWriter()
        t.printStackTrace(PrintWriter(sw))
        return sw.toString()
    }

    @Synchronized
    fun snapshot(): String = lines.joinToString("\n")

    @Synchronized
    fun clear() {
        lines.clear()
        runCatching { logFile?.writeText("") }
    }
}
