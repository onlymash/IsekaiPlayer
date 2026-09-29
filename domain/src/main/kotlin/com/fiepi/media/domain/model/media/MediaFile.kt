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

package com.fiepi.media.domain.model.media

import com.fiepi.media.domain.model.source.SourceType

sealed class MediaFile {
    abstract val path: String
    abstract val name: String
    abstract val sourceType: SourceType
    abstract val sourceId: String?
    abstract val size: Long?
    abstract val lastModified: Long?
    abstract val extension: String?

    val scheme: UriScheme get() = UriScheme.fromPath(path)

    data class Folder(
        override val path: String,
        override val name: String,
        override val sourceType: SourceType,
        override val sourceId: String? = null,
        override val size: Long? = null,
        override val lastModified: Long? = null,
        override val extension: String? = null,
        val itemCount: Int? = null
    ) : MediaFile()

    data class Video(
        override val path: String,
        override val name: String,
        override val sourceType: SourceType,
        override val sourceId: String? = null,
        override val size: Long? = null,
        override val lastModified: Long? = null,
        override val extension: String? = null,
        val id: String,
        val duration: Long? = null,
        val mimeType: String? = null,
        val width: Int? = null,
        val height: Int? = null,
        val thumbnailUrl: String? = null,
        val externalSubtitles: List<Subtitle> = emptyList(),
        val extraHeaders: Map<String, String> = emptyMap()
    ) : MediaFile() {
        val userAgent: String? get() = extraHeaders["User-Agent"] ?: extraHeaders["user-agent"]
    }

    data class Subtitle(
        override val path: String,
        override val name: String,
        override val sourceType: SourceType,
        override val sourceId: String? = null,
        override val size: Long? = null,
        override val lastModified: Long? = null,
        override val extension: String? = null,
        val language: String? = null,
        val mimeType: String? = null
    ) : MediaFile()
}
