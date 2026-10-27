package com.nono.iptv.parser

import com.nono.iptv.data.ChannelEntity
import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader

/**
 * Streaming parser for extended M3U (#EXTM3U) playlists.
 *
 * Reads one line at a time through a [BufferedReader] so that playlists with tens of
 * thousands of channels never have to be held in memory as a single string. Each
 * `#EXTINF` line carries attributes (tvg-id, tvg-logo, group-title) and a display name;
 * the following non-comment line is the stream URL.
 *
 * Example entry:
 * ```
 * #EXTINF:-1 tvg-id="bbc1.uk" tvg-logo="http://logo/bbc1.png" group-title="UK",BBC One
 * http://server/stream/bbc1.m3u8
 * ```
 */
object M3uParser {

    private val ATTR_REGEX = Regex("""([\w-]+)="([^"]*)"""")

    fun parse(stream: InputStream): List<ChannelEntity> {
        val channels = ArrayList<ChannelEntity>()
        BufferedReader(InputStreamReader(stream, Charsets.UTF_8)).use { reader ->
            var pending: Pending? = null
            var autoNumber = 0

            reader.forEachLine { rawLine ->
                val line = rawLine.trim()
                when {
                    line.isEmpty() -> Unit
                    line.startsWith("#EXTM3U") -> Unit
                    line.startsWith("#EXTINF") -> pending = parseExtInf(line)
                    line.startsWith("#") -> Unit // other tags (#EXTGRP, #KODIPROP, …) ignored for now
                    else -> {
                        // A URL line completes the most recent #EXTINF.
                        val info = pending
                        if (info != null) {
                            autoNumber++
                            channels.add(
                                ChannelEntity(
                                    tvgId = info.tvgId,
                                    name = info.name.ifBlank { "Channel $autoNumber" },
                                    logoUrl = info.logoUrl,
                                    groupName = info.group.ifBlank { DEFAULT_GROUP },
                                    streamUrl = line,
                                    channelNumber = autoNumber
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
        // Split attributes from the trailing display name at the first unquoted comma.
        val commaIndex = line.indexOf(',')
        val attrsPart = if (commaIndex >= 0) line.substring(0, commaIndex) else line
        val name = if (commaIndex >= 0) line.substring(commaIndex + 1).trim() else ""

        var tvgId: String? = null
        var logo: String? = null
        var group = ""

        ATTR_REGEX.findAll(attrsPart).forEach { match ->
            val key = match.groupValues[1].lowercase()
            val value = match.groupValues[2]
            when (key) {
                "tvg-id" -> tvgId = value.ifBlank { null }
                "tvg-logo" -> logo = value.ifBlank { null }
                "group-title" -> group = value
            }
        }
        return Pending(tvgId, name, logo, group)
    }

    private data class Pending(
        val tvgId: String?,
        val name: String,
        val logoUrl: String?,
        val group: String
    )

    const val DEFAULT_GROUP = "Ungrouped"
}
