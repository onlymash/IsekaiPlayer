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

package com.fiepi.media.domain.model.source

sealed class MediaSource {
    abstract val id: String
    abstract val name: String
    abstract val rootPath: String
    abstract val type: SourceType
    abstract val totalSpace: Long?
    abstract val freeSpace: Long?

    data class Local(
        override val id: String,
        override val name: String,
        override val rootPath: String,
        override val totalSpace: Long? = null,
        override val freeSpace: Long? = null,
        val isRemovable: Boolean = false,
    ) : MediaSource() {
        override val type: SourceType = SourceType.Local
    }

    data class Remote(
        val source: RemoteSource
    ) : MediaSource() {
        override val id: String = source.id
        override val name: String = source.name
        override val rootPath: String = when {
            source.path.isNullOrEmpty() -> "/"
            source.path.startsWith("/") -> source.path
            else -> "/${source.path}"
        }
        override val type: SourceType = source.type
        override val totalSpace: Long? = source.totalSpace
        override val freeSpace: Long? = source.freeSpace
    }

    data class External(
        override val id: String = EXTERNAL_STORAGE_ID,
        override val name: String = "External Media",
        override val rootPath: String = "",
        override val totalSpace: Long? = null,
        override val freeSpace: Long? = null
    ) : MediaSource() {
        override val type: SourceType = SourceType.External
    }

    companion object {
        const val INTERNAL_STORAGE_ID = "local_internal"
        const val EXTERNAL_STORAGE_ID = "external_media"
        const val NETWORK_STREAM_ID = "network_stream"
    }
}
