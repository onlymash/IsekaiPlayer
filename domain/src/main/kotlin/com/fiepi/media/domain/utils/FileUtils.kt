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

import io.ktor.http.decodeURLPart

/**
 * Utility functions for file path, URL, file name, and file extension parsing.
 */
object FileUtils {

    /**
     * Extracts the clean file name from a path, file name, or URL.
     * Strips path directories, URL query parameters, and fragments.
     *
     * Example:
     * - "http://example.com/videos/movie.mp4?token=123#play" -> "movie.mp4"
     * - "/storage/emulated/0/Download/movie.mp4" -> "movie.mp4"
     * - "movie.mp4" -> "movie.mp4"
     */
    fun extractFileName(pathOrUrl: String?): String {
        if (pathOrUrl.isNullOrBlank()) return ""
        return pathOrUrl
            .substringAfterLast('/')
            .substringAfterLast('\\')
            .substringBefore('?')
            .substringBefore('#')
    }

    /**
     * Extracts the lowercase file extension (without leading dot) from a path, file name, or URL.
     *
     * Example:
     * - "movie.mp4" -> "mp4"
     * - "http://example.com/video.MKV?token=123" -> "mkv"
     * - "archive.tar.gz" -> "gz"
     * - "filename_no_extension" -> null
     *
     * @return Lowercase file extension or null if no valid extension exists.
     */
    fun extractExtension(pathOrUrl: String?): String? {
        val fileName = extractFileName(pathOrUrl)
        if (fileName.isBlank() || (fileName.startsWith('.') && !fileName.substring(1)
                .contains('.'))
        ) return null

        val ext = fileName.substringAfterLast('.', "")
        return if (ext.isNotEmpty() && ext != fileName) {
            ext.lowercase()
        } else {
            null
        }
    }

    /**
     * Extracts the file name without extension from a path, file name, or URL.
     *
     * Example:
     * - "/storage/emulated/0/Download/movie.mp4" -> "movie"
     * - "http://example.com/videos/movie.zh.srt?token=123#play" -> "movie.zh"
     * - "filename_no_extension" -> "filename_no_extension"
     */
    fun extractNameWithoutExtension(pathOrUrl: String?): String {
        val fileName = extractFileName(pathOrUrl)
        val ext = extractExtension(fileName)
        return if (ext != null) {
            fileName.substringBeforeLast('.')
        } else {
            fileName
        }
    }

    /**
     * Decodes a URL-encoded string or URL component using Ktor's URL decoding utilities.
     * Returns the original string if decoding fails or if it is null/blank.
     *
     * Example:
     * - "My%20Video%20%282026%29" -> "My Video (2026)"
     * - "%E6%B5%8B%E8%AF%95" -> "测试"
     */
    fun decodeUrl(urlComponent: String?): String {
        if (urlComponent.isNullOrBlank()) return ""
        return try {
            urlComponent.decodeURLPart()
        } catch (_: Exception) {
            urlComponent
        }
    }
}

/**
 * Extension function to extract clean file name from a String.
 */
fun String.extractFileName(): String = FileUtils.extractFileName(this)

/**
 * Extension function to extract lowercase file extension from a String.
 */
fun String.extractExtension(): String? = FileUtils.extractExtension(this)

/**
 * Extension function to extract file name without extension from a String.
 */
fun String.extractNameWithoutExtension(): String = FileUtils.extractNameWithoutExtension(this)

/**
 * Extension function to decode a URL-encoded String using Ktor's URL decoding utilities.
 */
fun String.decodeUrl(): String = FileUtils.decodeUrl(this)
