/*
 * IsekaiPlayer - Sovereign above myriad realms; shatter every mortal cipher.
 * Copyright (C) 2026 onlymash
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.fiepi.media.data.repository.playlist.parser

import com.fiepi.media.domain.model.playlist.M3uEntry
import java.io.InputStream
import java.util.Locale

/**
 * Encapsulates mutable parsing state while processing M3U / M3U8 tags.
 */
class M3uParserState {
    var pendingTitle: String? = null
    var pendingDuration: Long = -1
    var pendingLogo: String? = null
    var pendingGroup: String? = null
    val pendingHeaders: MutableMap<String, String> = mutableMapOf()

    fun reset() {
        pendingTitle = null
        pendingDuration = -1
        pendingLogo = null
        pendingGroup = null
        pendingHeaders.clear()
    }

    fun buildEntry(resolvedUrl: String, fallbackTitle: String): M3uEntry {
        val finalTitle = pendingTitle.takeUnless { it.isNullOrBlank() } ?: fallbackTitle
        val entry = M3uEntry(
            title = finalTitle,
            url = resolvedUrl,
            durationSeconds = pendingDuration,
            logoUrl = pendingLogo,
            groupTitle = pendingGroup,
            headers = pendingHeaders.toMap()
        )
        reset()
        return entry
    }
}

/**
 * Line-by-line streaming parser for M3U and M3U8 playlists.
 */
object M3uParser {

    private val ATTRIBUTE_PAIR_REGEX = Regex("""([\w-]+)\s*=\s*(?:"([^"]*)"|'([^']*)'|([^\s,]+))""")
    private val SPACES_REGEX = Regex("""\s+""")
    private val WINDOWS_PATH_REGEX = Regex("""^[a-zA-Z]:[\\/].*""")

    /**
     * Parses an M3U or M3U8 input stream line-by-line into a list of [M3uEntry].
     *
     * @param inputStream The stream to read playlist content from.
     * @param baseUri Optional base URI or parent path used to resolve relative URLs.
     * @param customTagHandler Optional callback to handle custom or non-standard M3U tags.
     * @throws IllegalArgumentException If the stream contains non-M3U content (e.g., JSON, HTML, XML, PLS) or no valid entries.
     */
    fun parse(
        inputStream: InputStream,
        baseUri: String? = null,
        customTagHandler: ((line: String, state: M3uParserState) -> Boolean)? = null
    ): List<M3uEntry> {
        val entries = mutableListOf<M3uEntry>()
        val state = M3uParserState()

        var hasExtM3uHeader = false
        var isFirstNonEmptyLine = true
        var nonCommentLineCount = 0

        inputStream.bufferedReader(Charsets.UTF_8).useLines { lines ->
            for (rawLine in lines) {
                val line = rawLine.removePrefix("\uFEFF").trim()
                if (line.isEmpty()) continue

                if (isFirstNonEmptyLine) {
                    isFirstNonEmptyLine = false
                    validateFirstLineFormat(line)
                }

                if (customTagHandler?.invoke(line, state) == true) {
                    continue
                }

                when {
                    line.startsWith(M3uTags.EXTM3U, ignoreCase = true) -> {
                        hasExtM3uHeader = true
                    }

                    line.startsWith(M3uTags.EXTINF, ignoreCase = true) -> {
                        val extInfData = parseExtInf(line.substring(M3uTags.EXTINF.length))
                        state.pendingDuration = extInfData.duration
                        state.pendingTitle = extInfData.title
                        state.pendingLogo = extInfData.attributes["tvg-logo"]
                            ?: extInfData.attributes["logo"]
                                    ?: extInfData.attributes["tvg-cover"]
                                    ?: state.pendingLogo
                        state.pendingGroup = extInfData.attributes["group-title"]
                            ?: extInfData.attributes["group"]
                                    ?: state.pendingGroup

                        extInfData.attributes["http-user-agent"]?.let {
                            state.pendingHeaders["User-Agent"] = it
                        }
                        extInfData.attributes["user-agent"]?.let {
                            state.pendingHeaders["User-Agent"] = it
                        }
                        extInfData.attributes["http-referrer"]?.let {
                            state.pendingHeaders["Referer"] = it
                        }
                        extInfData.attributes["referrer"]?.let {
                            state.pendingHeaders["Referer"] = it
                        }
                    }

                    line.startsWith(M3uTags.EXTVLCOPT, ignoreCase = true) -> {
                        val opt = line.substring(M3uTags.EXTVLCOPT.length)
                        val kv = opt.split("=", limit = 2)
                        if (kv.size == 2) {
                            val key = kv[0].trim()
                            val value = kv[1].trim()
                            if (key.equals("http-user-agent", ignoreCase = true)) {
                                state.pendingHeaders["User-Agent"] = value
                            } else if (key.equals("http-referrer", ignoreCase = true)) {
                                state.pendingHeaders["Referer"] = value
                            }
                        }
                    }

                    line.startsWith(M3uTags.EXTGRP, ignoreCase = true) -> {
                        val group = line.substring(M3uTags.EXTGRP.length).trim()
                        if (group.isNotEmpty()) {
                            state.pendingGroup = group
                        }
                    }

                    line.startsWith(M3uTags.KODIPROP, ignoreCase = true) -> {
                        val prop = line.substring(M3uTags.KODIPROP.length).trim()
                        val kv = prop.split("=", limit = 2)
                        if (kv.size == 2) {
                            val key = kv[0].trim().lowercase(Locale.ROOT)
                            val value = kv[1].trim()
                            when (key) {
                                "http-user-agent", "user-agent" -> state.pendingHeaders["User-Agent"] =
                                    value

                                "http-referrer", "http-referer", "referer", "referrer" -> state.pendingHeaders["Referer"] =
                                    value
                            }
                        }
                    }

                    line.startsWith(M3uTags.EXT_X_STREAM_INF, ignoreCase = true) -> {
                        val attrString = line.substring(M3uTags.EXT_X_STREAM_INF.length)
                        val attributes = parseAttributes(attrString)
                        val bandwidth = attributes["bandwidth"]?.toLongOrNull()
                        val resolution = attributes["resolution"]
                        val codecs = attributes["codecs"]

                        val titleParts = mutableListOf<String>()
                        if (!resolution.isNullOrBlank()) {
                            titleParts.add(resolution)
                        }
                        if (bandwidth != null && bandwidth > 0) {
                            val mbps =
                                String.format(Locale.US, "%.1f Mbps", bandwidth / 1_000_000.0)
                            titleParts.add(mbps)
                        }
                        if (!codecs.isNullOrBlank()) {
                            titleParts.add(codecs)
                        }
                        if (titleParts.isNotEmpty()) {
                            state.pendingTitle = "Stream (${titleParts.joinToString(" - ")})"
                        }
                    }

                    !line.startsWith("#") -> {
                        nonCommentLineCount++
                        if (!isValidCandidateUrl(line)) {
                            state.reset()
                            continue
                        }

                        val resolvedUrl = resolveUrl(line, baseUri)
                        val fallbackTitle = extractFallbackTitle(resolvedUrl)
                        entries.add(state.buildEntry(resolvedUrl, fallbackTitle))
                    }
                }
            }
        }

        if (entries.isEmpty() && !hasExtM3uHeader) {
            if (nonCommentLineCount > 0) {
                throw IllegalArgumentException("Invalid M3U playlist format: content does not contain valid stream URLs or paths")
            } else {
                throw IllegalArgumentException("Invalid M3U playlist format: file is empty or contains no M3U entries")
            }
        }

        return entries
    }

    private fun validateFirstLineFormat(line: String) {
        if (line.startsWith("{") || line.startsWith("[")) {
            throw IllegalArgumentException("Invalid M3U playlist format: JSON content detected")
        }
        if (line.startsWith("<")) {
            throw IllegalArgumentException("Invalid M3U playlist format: HTML/XML content detected")
        }
        if (line.startsWith("[playlist]", ignoreCase = true)) {
            throw IllegalArgumentException("Invalid M3U playlist format: PLS format detected")
        }
    }

    private fun isValidCandidateUrl(line: String): Boolean {
        if (line.isEmpty() || line.length > 4096) return false

        val firstChar = line.first()
        val lastChar = line.last()

        if (firstChar == '{' || firstChar == '}' || firstChar == '[' || firstChar == ']' || firstChar == '<' || firstChar == '>') {
            return false
        }
        if (lastChar == '{' || lastChar == '}' || lastChar == '[' || lastChar == ']' || lastChar == '<' || lastChar == '>') {
            return false
        }

        if (line.contains('"') || line.contains('\'')) return false
        if (line.contains("\":") || line.contains(":\"")) return false

        if (line.contains("://")) {
            val scheme = line.substringBefore("://")
            if (scheme.all { it.isLetterOrDigit() || it == '+' || it == '-' || it == '.' }) {
                val rest = line.substringAfter("://")
                return rest.isNotBlank() && !rest.contains('{') && !rest.contains('}')
            }
        }

        if (line.contains('{') || line.contains('}') || line.contains('<') || line.contains('>')) {
            return false
        }

        if (line.startsWith("/") || line.startsWith("\\") || WINDOWS_PATH_REGEX.matches(line)) {
            return true
        }

        if (line.contains('/') || line.contains('\\')) {
            return true
        }

        val lastDot = line.lastIndexOf('.')
        if (lastDot > 0 && lastDot < line.length - 1) {
            val ext = line.substring(lastDot + 1)
            if (ext.length in 1..10 && ext.all { it.isLetterOrDigit() }) {
                return true
            }
        }

        return false
    }

    private data class ExtInfData(
        val duration: Long,
        val attributes: Map<String, String>,
        val title: String
    )

    private fun parseExtInf(content: String): ExtInfData {
        val commaIndex = content.lastIndexOf(',')
        val metadataPart = if (commaIndex != -1) content.substring(0, commaIndex) else content
        val titlePart = if (commaIndex != -1) content.substring(commaIndex + 1).trim() else ""

        val tokens = metadataPart.trim().split(SPACES_REGEX, limit = 2)
        val durationDouble = tokens.firstOrNull()?.toDoubleOrNull() ?: -1.0
        val duration = durationDouble.toLong()

        val attributes = if (tokens.size > 1) parseAttributes(tokens[1]) else emptyMap()
        return ExtInfData(duration, attributes, titlePart)
    }

    private fun parseAttributes(attrString: String): Map<String, String> {
        val attributes = mutableMapOf<String, String>()
        ATTRIBUTE_PAIR_REGEX.findAll(attrString).forEach { match ->
            val key = match.groupValues[1].lowercase(Locale.ROOT)
            val value = match.groupValues[2].ifEmpty {
                match.groupValues[3].ifEmpty {
                    match.groupValues[4]
                }
            }
            attributes[key] = value
        }
        return attributes
    }

    private fun resolveUrl(line: String, baseUri: String?): String {
        if (line.contains("://") || line.startsWith("/")) {
            return line
        }
        if (baseUri.isNullOrEmpty()) {
            return line
        }
        val parent = if (baseUri.endsWith("/")) {
            baseUri.dropLast(1)
        } else {
            baseUri.substringBeforeLast('/', missingDelimiterValue = baseUri)
        }
        return "$parent/$line"
    }

    private fun extractFallbackTitle(url: String): String {
        val cleanUrl = url.substringBefore('?').substringBefore('#')
        val fileName = cleanUrl.substringAfterLast('/')
        return fileName.ifBlank { url }
    }
}
