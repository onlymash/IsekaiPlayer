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

package com.fiepi.media.domain.utils

/**
 * Result of parsing a subtitle file name or path.
 *
 * @property baseName Clean file name without extension and without language code.
 * @property language Extracted language identifier (e.g. "zh", "eng", "zh-Hans") or null if none.
 * @property extension Lowercase file extension (e.g. "srt", "ass") or null if none.
 */
data class ParsedSubtitleInfo(
    val baseName: String,
    val language: String?,
    val extension: String?
)

/**
 * Utility functions for subtitle file parsing and language identification.
 */
object SubtitleUtils {

    /**
     * Parses a subtitle path, URL, or file name in a single pass into a [ParsedSubtitleInfo].
     */
    fun parse(fileNameOrPath: String?): ParsedSubtitleInfo {
        if (fileNameOrPath.isNullOrBlank()) {
            return ParsedSubtitleInfo(baseName = "", language = null, extension = null)
        }

        val fileName = FileUtils.extractFileName(fileNameOrPath)
        if (fileName.isBlank() || (fileName.startsWith('.') && !fileName.substring(1)
                .contains('.'))
        ) {
            return ParsedSubtitleInfo(baseName = fileName, language = null, extension = null)
        }

        val parts = fileName.split('.')
        if (parts.size < 2) {
            return ParsedSubtitleInfo(baseName = fileName, language = null, extension = null)
        }

        val ext = parts.last().lowercase().takeIf { it.isNotEmpty() && it != fileName }

        if (parts.size >= 3) {
            val langPart = parts[parts.size - 2].trim()
            if (langPart.length in 2..8) {
                val baseName = parts.take(parts.size - 2).joinToString(".")
                return ParsedSubtitleInfo(
                    baseName = baseName,
                    language = langPart,
                    extension = ext
                )
            }
        }

        val baseName = parts.take(parts.size - 1).joinToString(".")
        return ParsedSubtitleInfo(
            baseName = baseName,
            language = null,
            extension = ext
        )
    }

    /**
     * Extracts a subtitle language identifier from a file name, file path, or URL.
     */
    fun extractLanguageCode(fileNameOrPath: String?): String? = parse(fileNameOrPath).language

    /**
     * Extracts clean subtitle file name without extension and without language code.
     */
    fun extractSubtitleName(fileNameOrPath: String?): String = parse(fileNameOrPath).baseName

    /**
     * Checks if a subtitle name is associated with a video base name using boundary-aware prefix matching.
     */
    fun isAssociatedWithVideo(subtitleName: String, videoBaseName: String): Boolean {
        if (!subtitleName.startsWith(videoBaseName, ignoreCase = true)) return false
        if (subtitleName.length == videoBaseName.length) return true
        val nextChar = subtitleName[videoBaseName.length]
        return !nextChar.isLetterOrDigit()
    }
}

/**
 * Extension function to parse subtitle info in a single pass.
 */
fun String.parseSubtitleInfo(): ParsedSubtitleInfo = SubtitleUtils.parse(this)

/**
 * Extension function for convenient language code extraction from a String.
 */
fun String.extractSubtitleLanguage(): String? = SubtitleUtils.extractLanguageCode(this)

/**
 * Extension function for convenient subtitle name extraction from a String.
 */
fun String.extractSubtitleName(): String = SubtitleUtils.extractSubtitleName(this)
