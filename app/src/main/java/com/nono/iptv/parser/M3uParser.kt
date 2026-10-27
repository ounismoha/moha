package com.nono.iptv.parser

import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader

/** A channel parsed from an M3U line, before it is mapped to a Room entity. */
data class ParsedChannel(
    val tvgId: String?,
    val name: String,
    val logoUrl: String?,
    val group: String,
    val streamUrl: String
)

/**
 * Streaming parser for extended M3U (#EXTM3U) playlists. Reads line-by-line through a
 * [BufferedReader] so very large playlists aren't held in memory as one string.
 */
object M3uParser {

    private val ATTR_REGEX = Regex("""([\w-]+)="([^"]*)"""")
    const val DEFAULT_GROUP = "Ungrouped"

    fun parse(stream: InputStream): List<ParsedChannel> {
        val channels = ArrayList<ParsedChannel>()
        BufferedReader(InputStreamReader(stream, Charsets.UTF_8)).use { reader ->
            var pending: Pending? = null
            reader.forEachLine { raw ->
                val line = raw.trim()
                when {
                    line.isEmpty() -> Unit
                    line.startsWith("#EXTM3U") -> Unit
                    line.startsWith("#EXTINF") -> pending = parseExtInf(line)
                    line.startsWith("#") -> Unit
                    else -> {
                        val info = pending
                        if (info != null) {
                            channels.add(
                                ParsedChannel(
                                    tvgId = info.tvgId,
                                    name = info.name.ifBlank { "Channel ${channels.size + 1}" },
                                    logoUrl = info.logo,
                                    group = info.group.ifBlank { DEFAULT_GROUP },
                                    streamUrl = line
                                )
                            )
                            pending = null
                        }
                    }
                }
            }
        }
        return channels
    }

    private fun parseExtInf(line: String): Pending {
        val comma = line.indexOf(',')
        val attrs = if (comma >= 0) line.substring(0, comma) else line
        val name = if (comma >= 0) line.substring(comma + 1).trim() else ""
        var tvgId: String? = null
        var logo: String? = null
        var group = ""
        ATTR_REGEX.findAll(attrs).forEach { m ->
            when (m.groupValues[1].lowercase()) {
                "tvg-id" -> tvgId = m.groupValues[2].ifBlank { null }
                "tvg-logo" -> logo = m.groupValues[2].ifBlank { null }
                "group-title" -> group = m.groupValues[2]
            }
        }
        return Pending(tvgId, name, logo, group)
    }

    private data class Pending(val tvgId: String?, val name: String, val logo: String?, val group: String)
}
