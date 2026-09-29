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

import io.ktor.http.ContentType
import io.ktor.http.defaultForFileExtension

/**
 * Pure Kotlin MIME type resolver backed by Ktor's [ContentType] utilities.
 * Does not depend on Android framework classes.
 */
object MimeTypeUtils {

    /**
     * Additional MIME type overrides for subtitle and media formats to ensure standard player MIME strings.
     */
    private val EXTRA_MIME_MAP = mapOf(
        "srt" to "application/x-subrip",
        "ass" to "text/x-ssa",
        "ssa" to "text/x-ssa",
        "vtt" to "text/vtt",
        "webvtt" to "text/vtt",
        "idx" to "application/x-vobsub",
        "sub" to "application/x-vobsub",
        "lrc" to "text/x-lrc",
        "smi" to "application/x-sami",
        "sami" to "application/x-sami",
        "sup" to "application/x-pgs",
        "flac" to "audio/flac",
        "avi" to "video/x-msvideo"
    )

    /**
     * Resolves the MIME type string for a given file name, path, or extension using Ktor's [ContentType].
     *
     * @param fileNameOrExtension File extension (e.g. "mp4", "srt") or full file name/path (e.g. "movie.mkv").
     * @return MIME type string if recognized, null otherwise.
     */
    fun getMimeType(fileNameOrExtension: String?): String? {
        if (fileNameOrExtension.isNullOrBlank()) return null

        val ext = FileUtils.extractExtension(fileNameOrExtension)
            ?: fileNameOrExtension.trim().lowercase().removePrefix(".")

        // 1. Check extra overrides
        EXTRA_MIME_MAP[ext]?.let { return it }

        // 2. Resolve via Ktor's built-in ContentType map
        val contentType = ContentType.defaultForFileExtension(ext)
        return if (contentType != ContentType.Application.OctetStream) {
            contentType.withoutParameters().toString()
        } else {
            null
        }
    }
}

/**
 * Extension function to resolve MIME type from a String using domain MimeTypeUtils.
 */
fun String.getMimeType(): String? = MimeTypeUtils.getMimeType(this)
